package com.vvf.smartmanager.data.worker

import android.content.Context
import android.os.Environment
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.vvf.smartmanager.domain.repository.FileRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * Periodic background re-index (Phase 12). Battery-friendly: WorkManager schedules this
 * under system constraints (charging/idle-aware when configured by the caller), and it
 * only touches the Room index — never re-encrypts or moves user files.
 */
@HiltWorker
class IndexingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val fileRepository: FileRepository
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val root = Environment.getExternalStorageDirectory().absolutePath
        val result = fileRepository.rescanAndIndex(root)
        return if (result.isSuccess) Result.success() else Result.retry()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "vvf_background_indexing"
        const val IMMEDIATE_WORK_NAME = "vvf_immediate_first_indexing"
    }
}
