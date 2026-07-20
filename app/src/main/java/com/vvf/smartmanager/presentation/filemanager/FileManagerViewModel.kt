package com.vvf.smartmanager.presentation.filemanager

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vvf.smartmanager.domain.model.FileItem
import com.vvf.smartmanager.domain.usecase.filemanager.CopyFileUseCase
import com.vvf.smartmanager.domain.usecase.filemanager.DeleteToTrashUseCase
import com.vvf.smartmanager.domain.usecase.filemanager.ListFilesUseCase
import com.vvf.smartmanager.domain.usecase.filemanager.MoveFileUseCase
import com.vvf.smartmanager.domain.usecase.filemanager.RenameFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FileManagerViewModel @Inject constructor(
    private val listFiles: ListFilesUseCase,
    private val copyFile: CopyFileUseCase,
    private val moveFile: MoveFileUseCase,
    private val renameFile: RenameFileUseCase,
    private val deleteToTrash: DeleteToTrashUseCase
) : ViewModel() {

    private val rootPath = Environment.getExternalStorageDirectory().absolutePath

    private val _uiState = MutableStateFlow(FileManagerUiState(currentPath = rootPath))
    val uiState: StateFlow<FileManagerUiState> = _uiState.asStateFlow()

    init {
        loadDirectory(rootPath)
    }

    fun loadDirectory(path: String) {
        _uiState.update { it.copy(currentPath = path, isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            listFiles(path).fold(
                onSuccess = { items ->
                    _uiState.update { it.copy(items = items, isLoading = false) }
                },
                onFailure = { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
            )
        }
    }

    fun onItemClicked(item: FileItem) {
        if (item.isDirectory) {
            loadDirectory(item.path)
        } else {
            _uiState.update { it.copy(selectedItem = item) }
        }
    }

    fun navigateUp(): Boolean {
        val current = _uiState.value.currentPath
        if (current == rootPath) return false
        val parent = current.substringBeforeLast('/', rootPath)
        loadDirectory(parent.ifBlank { rootPath })
        return true
    }

    fun requestRename(item: FileItem) {
        _uiState.update { it.copy(pendingAction = PendingAction.Rename(item)) }
    }

    fun requestDelete(item: FileItem) {
        _uiState.update { it.copy(pendingAction = PendingAction.ConfirmDelete(item)) }
    }

    fun dismissPendingAction() {
        _uiState.update { it.copy(pendingAction = null) }
    }

    fun confirmRename(item: FileItem, newName: String) {
        viewModelScope.launch {
            renameFile(item, newName).fold(
                onSuccess = { dismissPendingAction(); loadDirectory(_uiState.value.currentPath) },
                onFailure = { error -> _uiState.update { it.copy(errorMessage = error.message, pendingAction = null) } }
            )
        }
    }

    fun confirmDelete(item: FileItem) {
        viewModelScope.launch {
            deleteToTrash(item).fold(
                onSuccess = { dismissPendingAction(); loadDirectory(_uiState.value.currentPath) },
                onFailure = { error -> _uiState.update { it.copy(errorMessage = error.message, pendingAction = null) } }
            )
        }
    }

    fun copyTo(item: FileItem, destinationDir: String) {
        viewModelScope.launch {
            copyFile(item, destinationDir).fold(
                onSuccess = { loadDirectory(_uiState.value.currentPath) },
                onFailure = { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            )
        }
    }

    fun moveTo(item: FileItem, destinationDir: String) {
        viewModelScope.launch {
            moveFile(item, destinationDir).fold(
                onSuccess = { loadDirectory(_uiState.value.currentPath) },
                onFailure = { error -> _uiState.update { it.copy(errorMessage = error.message) } }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
