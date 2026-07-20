package com.vvf.smartmanager.data.repository

import com.vvf.smartmanager.core.di.IoDispatcher
import com.vvf.smartmanager.core.util.PerceptualHash
import com.vvf.smartmanager.core.util.Sha256Hasher
import com.vvf.smartmanager.data.local.dao.FileEntryDao
import com.vvf.smartmanager.data.local.entity.FileEntryEntity
import com.vvf.smartmanager.data.mapper.toDomain
import com.vvf.smartmanager.domain.model.DuplicateGroup
import com.vvf.smartmanager.domain.model.DuplicateLevel
import com.vvf.smartmanager.domain.model.FileCategory
import com.vvf.smartmanager.domain.repository.DuplicateRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Duplicate Cleaner engine — Master Specification v2.0, Section 9.
 * Level 1-2 (this class, cheap, offline): exact hash + size-based metadata match.
 * Level 3 (this class, offline, no AI model): perceptual hash visual similarity.
 * Level 4 (semantic AI): NOT implemented — needs a bundled/downloaded embedding model,
 * see docs/PENDING_WORK.md.
 */
@Singleton
class DuplicateRepositoryImpl @Inject constructor(
    private val dao: FileEntryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DuplicateRepository {

    override suspend fun findExactAndMetadataDuplicates(rootPath: String): Result<List<DuplicateGroup>> =
        withContext(ioDispatcher) {
            runCatching {
                val candidates = dao.listAllFiles().filter { it.path.startsWith(rootPath) }
                val withHashes = ensureHashes(candidates)

                val byHash = withHashes.groupBy { it.sha256Hash }
                val exactGroups = byHash.entries
                    .filter { (hash, files) -> hash != null && files.size > 1 }
                    .map { (_, files) -> files.toDuplicateGroup(DuplicateLevel.EXACT_HASH) }

                val pathsInExactGroups = exactGroups.flatMap { it.items.map { item -> item.path } }.toSet()
                val remaining = withHashes.filterNot { it.path in pathsInExactGroups }

                val bySize = remaining.groupBy { it.sizeBytes }
                val metadataGroups = bySize.entries
                    .filter { (size, files) -> size > 0 && files.size > 1 }
                    .map { (_, files) -> files.toDuplicateGroup(DuplicateLevel.METADATA_MATCH) }

                exactGroups + metadataGroups
            }
        }

    override suspend fun findVisuallySimilar(rootPath: String, threshold: Float): Result<List<DuplicateGroup>> =
        withContext(ioDispatcher) {
            runCatching {
                val images = dao.listAllFiles()
                    .filter { it.path.startsWith(rootPath) }
                    .filter { FileCategory.fromExtension(it.name.substringAfterLast('.', "")) == FileCategory.IMAGE }
                val withHashes = ensurePerceptualHashes(images)

                val n = withHashes.size
                // This comparison is O(n²) — confirmed in code review (17-18 July 2026 audit).
                // A real fix (LSH bucketing / BK-tree / prefix partitioning to avoid pairwise
                // comparison entirely) is real, scoped future work — not something to fake with
                // a quick patch. Until then, this hard cap turns "the app hangs for minutes on
                // a huge gallery" into a clear, immediate, actionable error instead.
                check(n <= MAX_IMAGES_FOR_PAIRWISE_SCAN) {
                    "इस folder में $n images हैं, जो अभी की visual-similarity scan सीमा " +
                        "($MAX_IMAGES_FOR_PAIRWISE_SCAN) से ज़्यादा है। बड़ी gallery के लिए यह " +
                        "feature अभी optimize नहीं है — देखें docs/PENDING_WORK.md।"
                }

                val parent = IntArray(n) { it }
                fun find(x: Int): Int {
                    var root = x
                    while (parent[root] != root) root = parent[root]
                    var cur = x
                    while (parent[cur] != root) {
                        val next = parent[cur]
                        parent[cur] = root
                        cur = next
                    }
                    return root
                }
                fun union(a: Int, b: Int) {
                    val ra = find(a); val rb = find(b)
                    if (ra != rb) parent[ra] = rb
                }

                for (i in 0 until n) {
                    val hashI = withHashes[i].perceptualHash ?: continue
                    for (j in i + 1 until n) {
                        val hashJ = withHashes[j].perceptualHash ?: continue
                        val distance = PerceptualHash.hammingDistance(hashI, hashJ)
                        val similarity = PerceptualHash.similarityPercent(distance)
                        if (similarity >= (threshold * 100).toInt()) {
                            union(i, j)
                        }
                    }
                }

                (0 until n).groupBy { find(it) }
                    .values
                    .filter { it.size > 1 }
                    .map { indices -> indices.map { withHashes[it] }.toDuplicateGroup(DuplicateLevel.VISUAL_SIMILAR) }
            }
        }

    private suspend fun ensureHashes(entries: List<FileEntryEntity>): List<FileEntryEntity> =
        entries.map { entry ->
            if (entry.sha256Hash != null) return@map entry
            val file = File(entry.path)
            if (!file.exists() || file.isDirectory) return@map entry
            val hash = runCatching { Sha256Hasher.hash(file) }.getOrNull() ?: return@map entry
            val updated = entry.copy(sha256Hash = hash)
            dao.update(updated)
            updated
        }

    private suspend fun ensurePerceptualHashes(entries: List<FileEntryEntity>): List<FileEntryEntity> =
        entries.map { entry ->
            if (entry.perceptualHash != null) return@map entry
            val hash = runCatching { PerceptualHash.compute(entry.path) }.getOrNull() ?: return@map entry
            val updated = entry.copy(perceptualHash = hash)
            dao.update(updated)
            updated
        }

    private fun List<FileEntryEntity>.toDuplicateGroup(level: DuplicateLevel): DuplicateGroup {
        val items = map { it.toDomain() }
        val savings = if (items.isEmpty()) 0L else items.drop(1).sumOf { it.sizeBytes }
        return DuplicateGroup(
            groupId = UUID.randomUUID().toString(),
            level = level,
            items = items,
            potentialSavingsBytes = savings
        )
    }

    private companion object {
        // Conservative, NOT benchmarked on real hardware — a rough estimate based on the O(n²)
        // cost (n=5000 -> ~12.5M cheap XOR+popcount comparisons). Lower this if real-device
        // testing shows it's still too slow, or raise it once real profiling says it's safe.
        const val MAX_IMAGES_FOR_PAIRWISE_SCAN = 5000
    }
}
