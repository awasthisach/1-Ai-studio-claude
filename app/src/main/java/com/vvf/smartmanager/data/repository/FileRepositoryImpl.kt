package com.vvf.smartmanager.data.repository

import android.content.Context
import com.vvf.smartmanager.core.di.IoDispatcher
import com.vvf.smartmanager.core.util.MimeTypeResolver
import com.vvf.smartmanager.data.local.dao.FileEntryDao
import com.vvf.smartmanager.data.local.entity.FileEntryEntity
import com.vvf.smartmanager.data.mapper.toDomain
import com.vvf.smartmanager.domain.model.FileItem
import com.vvf.smartmanager.domain.repository.FileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dao: FileEntryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : FileRepository {

    private val trashRoot: File
        get() = File(context.getExternalFilesDir(null), "vvf_trash").apply { mkdirs() }

    override suspend fun listFiles(directoryPath: String): Result<List<FileItem>> =
        withContext(ioDispatcher) {
            runCatching {
                val dir = File(directoryPath)
                require(dir.isDirectory) { "$directoryPath एक directory नहीं है" }
                dir.listFiles()
                    ?.sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase(Locale.ROOT) }))
                    ?.map { it.toFileItem() }
                    ?: emptyList()
            }
        }

    override suspend fun copy(source: FileItem, destinationDir: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val src = File(source.path)
                val dest = File(destinationDir, src.name)
                require(!dest.exists()) { "${dest.name} पहले से मौजूद है" }
                if (src.isDirectory) src.copyRecursively(dest, overwrite = false)
                else src.copyTo(dest, overwrite = false)
                indexSingle(dest)
                Unit
            }
        }

    override suspend fun move(source: FileItem, destinationDir: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val src = File(source.path)
                val dest = File(destinationDir, src.name)
                require(!dest.exists()) { "${dest.name} पहले से मौजूद है" }
                val ok = src.renameTo(dest)
                if (!ok) {
                    // Cross-volume move (e.g. internal -> SD card) — copy then delete.
                    if (src.isDirectory) src.copyRecursively(dest, overwrite = false) else src.copyTo(dest)
                    src.deleteRecursively()
                }
                dao.deleteByPath(source.path)
                indexSingle(dest)
                Unit
            }
        }

    override suspend fun rename(source: FileItem, newName: String): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val src = File(source.path)
                val dest = File(src.parentFile, newName)
                require(!dest.exists()) { "$newName पहले से मौजूद है" }
                require(src.renameTo(dest)) { "Rename असफल रहा" }
                dao.deleteByPath(source.path)
                indexSingle(dest)
                Unit
            }
        }

    override suspend fun moveToTrash(source: FileItem): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val src = File(source.path)
                val trashName = "${UUID.randomUUID()}_${src.name}"
                val dest = File(trashRoot, trashName)
                require(src.renameTo(dest)) { "Trash में move करना असफल रहा" }

                // Build the entity from the already-known FileItem, NOT by re-reading `src`
                // (which no longer exists on disk at its old path after the rename above).
                // NOTE: when `existing` is non-null, entity.copy() preserves its `id`, so the
                // upsert below (REPLACE strategy) updates that SAME row's path column in place —
                // there is no separate old-path row left to delete afterward, which is why the
                // deleteByPath call that used to be here was always a no-op against zero rows.
                val existing = dao.findByPath(source.path)
                val entity = (existing ?: source.toFreshEntity()).copy(
                    path = dest.absolutePath,
                    isInTrash = true,
                    originalPathBeforeTrash = source.path
                )
                dao.upsert(entity)
                Unit
            }
        }

    override suspend fun restoreFromTrash(source: FileItem): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val originalPath = source.originalPathBeforeTrash
                    ?: error("Original path अज्ञात है, restore नहीं हो सकता")
                val trashed = File(source.path)
                val restored = File(originalPath)
                restored.parentFile?.mkdirs()
                require(trashed.renameTo(restored)) { "Restore असफल रहा" }
                dao.deleteByPath(source.path)
                indexSingle(restored)
                Unit
            }
        }

    override suspend fun permanentlyDelete(source: FileItem): Result<Unit> =
        withContext(ioDispatcher) {
            runCatching {
                val target = File(source.path)
                val deleted = if (target.isDirectory) target.deleteRecursively() else target.delete()
                require(deleted) { "स्थायी रूप से delete करना असफल रहा" }
                dao.deleteByPath(source.path)
                Unit
            }
        }

    override suspend fun listTrash(): Result<List<FileItem>> =
        withContext(ioDispatcher) {
            runCatching { dao.listTrash().map { it.toDomain() } }
        }

    override suspend fun rescanAndIndex(rootPath: String): Result<Int> =
        withContext(ioDispatcher) {
            runCatching {
                val root = File(rootPath)
                if (!root.exists()) return@runCatching 0
                var count = 0
                val batch = mutableListOf<FileEntryEntity>()
                root.walkTopDown()
                    .onEnter { dir -> !dir.name.startsWith(".") && dir != trashRoot }
                    .forEach { file ->
                        if (file == root) return@forEach
                        batch.add(file.toEntity())
                        count++
                        if (batch.size >= 200) {
                            dao.upsertAll(batch.toList())
                            batch.clear()
                        }
                    }
                if (batch.isNotEmpty()) dao.upsertAll(batch)
                count
            }
        }

    private suspend fun indexSingle(file: File) {
        dao.upsert(file.toEntity())
    }

    private fun File.toFileItem(): FileItem = FileItem(
        path = absolutePath,
        name = name,
        isDirectory = isDirectory,
        sizeBytes = if (isDirectory) 0L else length(),
        lastModifiedEpochMillis = lastModified(),
        mimeType = if (isDirectory) null else MimeTypeResolver.resolve(this)
    )

    private fun File.toEntity(): FileEntryEntity = FileEntryEntity(
        path = absolutePath,
        name = name,
        isDirectory = isDirectory,
        sizeBytes = if (isDirectory) 0L else length(),
        lastModifiedEpochMillis = lastModified(),
        mimeType = if (isDirectory) null else MimeTypeResolver.resolve(this)
    )

    private fun FileItem.toFreshEntity(): FileEntryEntity = FileEntryEntity(
        path = path,
        name = name,
        isDirectory = isDirectory,
        sizeBytes = sizeBytes,
        lastModifiedEpochMillis = lastModifiedEpochMillis,
        mimeType = mimeType
    )
}
