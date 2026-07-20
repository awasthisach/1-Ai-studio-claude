package com.vvf.smartmanager.presentation.storage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vvf.smartmanager.domain.model.StorageSummary
import com.vvf.smartmanager.domain.usecase.storage.GetStorageSummaryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StorageUiState(
    val summary: StorageSummary? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null
)

@HiltViewModel
class StorageViewModel @Inject constructor(
    private val getStorageSummary: GetStorageSummaryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StorageUiState())
    val uiState: StateFlow<StorageUiState> = _uiState.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            getStorageSummary().fold(
                onSuccess = { summary -> _uiState.update { it.copy(summary = summary, isLoading = false) } },
                onFailure = { error -> _uiState.update { it.copy(isLoading = false, errorMessage = error.message) } }
            )
        }
    }
}
