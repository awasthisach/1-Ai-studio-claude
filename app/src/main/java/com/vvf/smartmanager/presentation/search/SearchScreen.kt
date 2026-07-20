package com.vvf.smartmanager.presentation.search

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.hilt.navigation.compose.hiltViewModel
import com.vvf.smartmanager.presentation.components.FileListItem

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        OutlinedTextField(
            value = uiState.query,
            onValueChange = viewModel::onQueryChanged,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("फाइलें खोजें…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) }
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isSearching -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center).padding(top = 32.dp))
                uiState.hasSearched && uiState.results.isEmpty() -> Text(
                    text = "कोई परिणाम नहीं मिला",
                    modifier = Modifier.align(Alignment.Center).padding(top = 32.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                else -> LazyColumn {
                    items(uiState.results, key = { it.path }) { item ->
                        FileListItem(item = item, onClick = {}, onLongClick = {})
                    }
                }
            }
        }
    }
}
