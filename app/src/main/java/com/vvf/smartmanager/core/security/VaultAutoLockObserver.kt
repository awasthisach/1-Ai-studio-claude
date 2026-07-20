package com.vvf.smartmanager.core.security

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.vvf.smartmanager.domain.usecase.vault.LockVaultUseCase
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Auto Lock (Master Specification v2.0, Section 7 / Master Roadmap Phase 6) — a real,
 * previously-missing requirement confirmed during the 19 July 2026 audit: nothing anywhere
 * locked the Vault automatically, only a manual "Lock करें" button existed. That meant an
 * unlocked Vault stayed unlocked indefinitely, including while the app sat in the background
 * or the device screen was off — a real gap for a feature whose entire purpose is guarding
 * sensitive files.
 *
 * Uses [androidx.lifecycle.ProcessLifecycleOwner] (whole-app foreground/background state,
 * registered once from SmartManagerApp.onCreate()) rather than a single Activity's onStop —
 * Activity onStop also fires on things like screen rotation, which would incorrectly lock the
 * Vault mid-use. ProcessLifecycleOwner only reports STOP when every Activity in the app has
 * left the foreground, which is the correct "app went to background" signal here.
 */
@Singleton
class VaultAutoLockObserver @Inject constructor(
    private val lockVault: LockVaultUseCase
) : DefaultLifecycleObserver {

    override fun onStop(owner: LifecycleOwner) {
        lockVault()
    }
}
