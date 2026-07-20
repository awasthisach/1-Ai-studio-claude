package com.vvf.smartmanager.presentation.vault

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vvf.smartmanager.domain.model.VaultItem
import com.vvf.smartmanager.domain.usecase.vault.HasPinConfiguredUseCase
import com.vvf.smartmanager.domain.usecase.vault.LockVaultUseCase
import com.vvf.smartmanager.domain.usecase.vault.ObserveVaultItemsUseCase
import com.vvf.smartmanager.domain.usecase.vault.ObserveVaultUnlockedUseCase
import com.vvf.smartmanager.domain.usecase.vault.SetVaultPinUseCase
import com.vvf.smartmanager.domain.usecase.vault.UnlockVaultUseCase
import com.vvf.smartmanager.domain.usecase.vault.UnlockVaultWithBiometricUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class VaultUiState(
    val isUnlocked: Boolean = false,
    val needsPinSetup: Boolean = false,
    val items: List<VaultItem> = emptyList(),
    val pinError: String? = null
)

@HiltViewModel
class VaultViewModel @Inject constructor(
    observeUnlocked: ObserveVaultUnlockedUseCase,
    observeItems: ObserveVaultItemsUseCase,
    private val hasPinConfigured: HasPinConfiguredUseCase,
    private val setPin: SetVaultPinUseCase,
    private val unlockVault: UnlockVaultUseCase,
    private val unlockWithBiometric: UnlockVaultWithBiometricUseCase,
    private val lockVault: LockVaultUseCase
) : ViewModel() {

    // Declared before init{} on purpose — the init block updates these, so they must exist first.
    private val pinError = MutableStateFlow<String?>(null)
    private val needsPinSetup = MutableStateFlow(false)

    val uiState: StateFlow<VaultUiState> = combine(
        observeUnlocked(),
        observeItems(),
        pinError,
        needsPinSetup
    ) { unlocked, items, error, needsSetup ->
        VaultUiState(isUnlocked = unlocked, items = items, pinError = error, needsPinSetup = needsSetup)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VaultUiState())

    init {
        viewModelScope.launch {
            needsPinSetup.update { !hasPinConfigured() }
        }
    }

    fun onSetPin(pin: String, confirmPin: String) {
        if (pin != confirmPin) {
            pinError.update { "दोनों PIN मेल नहीं खाते" }
            return
        }
        viewModelScope.launch {
            setPin(pin).fold(
                onSuccess = {
                    needsPinSetup.update { false }
                    pinError.update { null }
                },
                onFailure = { error -> pinError.update { error.message } }
            )
        }
    }

    fun onUnlockAttempt(pin: String) {
        viewModelScope.launch {
            unlockVault(pin).fold(
                onSuccess = { valid ->
                    pinError.update { if (valid) null else "गलत PIN, फिर कोशिश करें" }
                },
                onFailure = { error -> pinError.update { error.message } }
            )
        }
    }

    fun onBiometricSuccess() {
        unlockWithBiometric()
    }

    fun lock() = lockVault()
}
