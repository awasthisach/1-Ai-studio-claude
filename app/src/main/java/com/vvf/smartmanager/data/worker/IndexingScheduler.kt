package com.vvf.smartmanager.data.worker

import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class IndexingScheduler @Inject constructor(
    private val workManager: WorkManager
) {
    /**
     * Runs at most every 6 hours, only when the device is idle and charging or has
     * plenty of battery — Master Specification v2.0, Section 11 (battery-friendly background).
     * No network constraint: indexing is a purely local filesystem scan.
     *
     * Also enqueues a one-time immediate index run (KEEP policy — safe to call on every
     * app launch, won't duplicate work if one is already pending/running). Without this,
     * a freshly installed app would show an empty Search and Duplicate Cleaner for up to
     * 6 hours until the first periodic run — confirmed gap found during 18 July 2026 audit,
     * since this scheduler was previously never called from anywhere in the app at all.
     */
    fun schedulePeriodicIndexing() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<IndexingWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            IndexingWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicRequest
        )

        val immediateRequest = OneTimeWorkRequestBuilder<IndexingWorker>()
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniqueWork(
            IndexingWorker.IMMEDIATE_WORK_NAME,
            ExistingWorkPolicy.KEEP,
            immediateRequest
        )
    }
}
