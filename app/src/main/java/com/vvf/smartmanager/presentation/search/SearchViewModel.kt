package com.vvf.smartmanager.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vvf.smartmanager.domain.model.FileItem
import com.vvf.smartmanager.domain.usecase.search.SearchFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val query: String = "",
    val results: List<FileItem> = emptyList(),
    val isSearching: Boolean = false,
    val hasSearched: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchFiles: SearchFilesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private var debounceJob: Job? = null

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(query = query) }
        debounceJob?.cancel()
        if (query.isBlank()) {
            _uiState.update { it.copy(results = emptyList(), hasSearched = false) }
            return
        }
        debounceJob = viewModelScope.launch {
            delay(300) // debounce — filename/metadata search is instant, this just avoids per-keystroke DB hits
            runSearch(query)
        }
    }

    private suspend fun runSearch(query: String) {
        _uiState.update { it.copy(isSearching = true) }
        searchFiles(query).fold(
            onSuccess = { results ->
                _uiState.update { it.copy(results = results, isSearching = false, hasSearched = true) }
            },
            onFailure = {
                _uiState.update { it.copy(results = emptyList(), isSearching = false, hasSearched = true) }
            }
        )
    }
}
