package com.vvf.smartmanager.presentation.filemanager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import com.vvf.smartmanager.presentation.components.FileListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileManagerScreen(
    viewModel: FileManagerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = uiState.currentPath.substringAfterLast('/').ifBlank { "Storage" }) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "ऊपर जाएँ")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.items.isEmpty()) {
                Text(
                    text = "यह folder खाली है",
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn {
                    items(uiState.items, key = { it.path }) { item ->
                        FileListItem(
                            item = item,
                            onClick = { viewModel.onItemClicked(item) },
                            onLongClick = { viewModel.requestDelete(item) }
                        )
                    }
                }
            }
        }
    }

    when (val action = uiState.pendingAction) {
        is PendingAction.ConfirmDelete -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissPendingAction,
                title = { Text("Recycle Bin में भेजें?") },
                text = { Text("\"${action.item.name}\" को Recycle Bin में भेजा जाएगा।") },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmDelete(action.item) }) { Text("भेजें") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissPendingAction) { Text("रद्द करें") }
                }
            )
        }
        is PendingAction.Rename -> {
            var newName by remember(action.item.path) { mutableStateOf(action.item.name) }
            AlertDialog(
                onDismissRequest = viewModel::dismissPendingAction,
                title = { Text("नाम बदलें") },
                text = {
                    OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true)
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.confirmRename(action.item, newName) }) { Text("सेव करें") }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissPendingAction) { Text("रद्द करें") }
                }
            )
        }
        else -> Unit
    }
}
