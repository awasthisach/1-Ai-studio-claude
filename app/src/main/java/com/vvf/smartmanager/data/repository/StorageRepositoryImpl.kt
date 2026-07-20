package com.vvf.smartmanager.data.repository

import android.os.Environment
import android.os.StatFs
import com.vvf.smartmanager.core.di.IoDispatcher
import com.vvf.smartmanager.data.local.dao.FileEntryDao
import com.vvf.smartmanager.domain.model.FileCategory
import com.vvf.smartmanager.domain.model.StorageSummary
import com.vvf.smartmanager.domain.repository.StorageRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepositoryImpl @Inject constructor(
    private val dao: FileEntryDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : StorageRepository {

    override suspend fun getStorageSummary(): Result<StorageSummary> =
        withContext(ioDispatcher) {
            runCatching {
                // NOT hardcoded "/storage/emulated/0" (audit finding, 18 July 2026) — that literal
                // path is wrong on secondary Android user/work profiles (they get
                // /storage/emulated/<userId>, not always 0). Environment.getExternalStorageDirectory()
                // resolves the correct path for whichever user profile the app is actually running
                // under, matching the pattern already used in IndexingWorker/FileManagerViewModel.
                val stat = StatFs(Environment.getExternalStorageDirectory().absolutePath)
                val totalBytes = stat.totalBytes
                val freeBytes = stat.availableBytes
                val usedBytes = totalBytes - freeBytes

                val indexedFiles = dao.listAllFiles()
                val byCategory: Map<FileCategory, Long> = indexedFiles
                    .groupBy { FileCategory.fromExtension(it.name.substringAfterLast('.', "")) }
                    .mapValues { (_, files) -> files.sumOf { it.sizeBytes } }

                StorageSummary(
                    totalBytes = totalBytes,
                    usedBytes = usedBytes,
                    freeBytes = freeBytes,
                    byCategory = byCategory
                )
            }
        }
}
