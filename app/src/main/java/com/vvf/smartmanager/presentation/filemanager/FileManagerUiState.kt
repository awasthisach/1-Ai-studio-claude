package com.vvf.smartmanager.presentation.filemanager

import com.vvf.smartmanager.domain.model.FileItem

data class FileManagerUiState(
    val currentPath: String,
    val items: List<FileItem> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedItem: FileItem? = null,
    val pendingAction: PendingAction? = null
)

sealed interface PendingAction {
    data class Rename(val item: FileItem) : PendingAction
    data class ConfirmDelete(val item: FileItem) : PendingAction
    data class CopyOrMove(val item: FileItem, val isMove: Boolean) : PendingAction
}
