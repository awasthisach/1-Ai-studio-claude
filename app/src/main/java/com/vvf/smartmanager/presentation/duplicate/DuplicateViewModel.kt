package com.vvf.smartmanager.presentation.duplicate

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vvf.smartmanager.domain.model.DuplicateGroup
import com.vvf.smartmanager.domain.usecase.duplicate.FindExactDuplicatesUseCase
import com.vvf.smartmanager.domain.usecase.duplicate.FindSimilarImagesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DuplicateUiState(
    val groups: List<DuplicateGroup> = emptyList(),
    val isScanning: Boolean = false,
    val hasScanned: Boolean = false,
    val similarityThreshold: Float = 0.85f, // 70%-95% slider range, default 85% — Blueprint design
    val errorMessage: String? = null
)

@HiltViewModel
class DuplicateViewModel @Inject constructor(
    private val findExactDuplicates: FindExactDuplicatesUseCase,
    private val findSimilarImages: FindSimilarImagesUseCase
) : ViewModel() {

    private val rootPath = Environment.getExternalStorageDirectory().absolutePath

    private val _uiState = MutableStateFlow(DuplicateUiState())
    val uiState: StateFlow<DuplicateUiState> = _uiState.asStateFlow()

    fun onThresholdChanged(value: Float) {
        _uiState.update { it.copy(similarityThreshold = value.coerceIn(0.70f, 0.95f)) }
    }

    fun startScan() {
        _uiState.update { it.copy(isScanning = true, errorMessage = null) }
        viewModelScope.launch {
            val exactResult = findExactDuplicates(rootPath)
            val similarResult = findSimilarImages(rootPath, _uiState.value.similarityThreshold)

            val exactGroups = exactResult.getOrDefault(emptyList())
            val similarGroups = similarResult.getOrDefault(emptyList())
            val firstError = exactResult.exceptionOrNull() ?: similarResult.exceptionOrNull()

            _uiState.update {
                it.copy(
                    groups = exactGroups + similarGroups,
                    isScanning = false,
                    hasScanned = true,
                    errorMessage = firstError?.message
                )
            }
        }
    }
}
