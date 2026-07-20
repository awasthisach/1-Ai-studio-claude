package com.vvf.smartmanager.data.repository

import com.vvf.smartmanager.core.di.IoDispatcher
import com.vvf.smartmanager.data.local.dao.FileEntryDao
import com.vvf.smartmanager.data.mapper.toDomain
import com.vvf.smartmanager.domain.model.FileItem
import com.vvf.smartmanager.domain.repository.SearchRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Search Engine (Phase 7) + OCR-text layer (Phase 8). Semantic search (Phase 9, TFLite)
 * is intentionally NOT wired in yet — see docs/PENDING_WORK.md — so this always returns
 * a real, useful result from filename + indexed OCR text, never silently nothing.
 */
@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val dao: FileEntryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SearchRepository {

    override suspend fun search(query: String): Result<List<FileItem>> =
        withContext(ioDispatcher) {
            runCatching {
                val trimmed = query.trim()
                if (trimmed.isEmpty()) return@runCatching emptyList()

                val ftsQuery = buildFtsPrefixQuery(trimmed)
                val ftsResults = runCatching { dao.searchByFtsMatch(ftsQuery) }.getOrDefault(emptyList())

                val results = if (ftsResults.isNotEmpty()) {
                    ftsResults
                } else {
                    // Fallback if FTS tokenizer finds nothing (e.g. very short query) —
                    // keyword search must never come up empty-handed when a plain LIKE
                    // match exists. This is the "semantic search must never replace
                    // keyword search" rule from the Master Specification.
                    dao.searchByNameFallback(trimmed)
                }
                results.map { it.toDomain() }
            }
        }

    /** Sanitizes free text into a safe FTS4 prefix query: strips operators, appends `*` per token. */
    private fun buildFtsPrefixQuery(raw: String): String {
        val sanitized = raw.replace(Regex("[\"*^]"), " ")
        return sanitized.trim()
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
            .joinToString(" ") { "$it*" }
    }
}
