package com.vvf.smartmanager.domain.usecase.vault

import com.vvf.smartmanager.domain.model.FileItem
import com.vvf.smartmanager.domain.model.VaultItem
import com.vvf.smartmanager.domain.repository.VaultRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveVaultUnlockedUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    operator fun invoke(): Flow<Boolean> = repository.isUnlocked
}

class ObserveVaultItemsUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    operator fun invoke(): Flow<List<VaultItem>> = repository.observeVaultItems()
}

class HasPinConfiguredUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(): Boolean = repository.hasPinConfigured()
}

class SetVaultPinUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(pin: String): Result<Unit> = repository.setPin(pin)
}

class UnlockVaultUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(pin: String): Result<Boolean> = repository.unlockWithPin(pin)
}

class UnlockVaultWithBiometricUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    operator fun invoke() = repository.unlockWithBiometric()
}

class LockVaultUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    operator fun invoke() = repository.lock()
}

class AddToVaultUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(file: FileItem): Result<Unit> = repository.addToVault(file)
}

class RemoveFromVaultUseCase @Inject constructor(
    private val repository: VaultRepository
) {
    suspend operator fun invoke(item: VaultItem, restoreTo: String): Result<Unit> =
        repository.removeFromVault(item, restoreTo)
}
