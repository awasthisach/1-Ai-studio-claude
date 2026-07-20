package com.vvf.smartmanager

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.lifecycle.ProcessLifecycleOwner
import androidx.work.Configuration
import com.vvf.smartmanager.core.security.VaultAutoLockObserver
import com.vvf.smartmanager.data.worker.IndexingScheduler
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * VVF Smart Manager — Application entry point.
 *
 * Implements [Configuration.Provider] so Hilt-injected Workers (background indexing,
 * Phase 12) can be created by WorkManager via [HiltWorkerFactory].
 *
 * IMPORTANT (confirmed via official Hilt docs, 18 July 2026 audit): Hilt injects this
 * Application's fields during `super.onCreate()` — NOT in `attachBaseContext()`. Since
 * Android's component startup order is attachBaseContext() -> ContentProvider.onCreate() ->
 * Application.onCreate(), any code that reads Hilt-injected fields (like [workerFactory])
 * MUST run after `super.onCreate()` has returned. This is why WorkManager's default
 * ContentProvider-based initializer is explicitly removed in AndroidManifest.xml (so it
 * never runs before injection completes) and why [indexingScheduler]/[vaultAutoLockObserver]
 * are only used inside this class's own onCreate(), after super.onCreate().
 */
@HiltAndroidApp
class SmartManagerApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var indexingScheduler: IndexingScheduler

    @Inject
    lateinit var vaultAutoLockObserver: VaultAutoLockObserver

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate() // Hilt field injection happens here — must run first.
        // Confirmed dead-code gap (18 July 2026 audit): this was never called from anywhere,
        // so background indexing never ran and Search/Duplicate Cleaner stayed empty for any
        // pre-existing files. Also enqueues a one-time immediate index (see IndexingScheduler).
        indexingScheduler.schedulePeriodicIndexing()

        // Auto Lock (19 July 2026 audit finding — see VaultAutoLockObserver doc comment):
        // locks the Vault whenever the whole app leaves the foreground.
        ProcessLifecycleOwner.get().lifecycle.addObserver(vaultAutoLockObserver)
    }
}
