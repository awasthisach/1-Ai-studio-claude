package com.vvf.smartmanager.presentation.storage

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vvf.smartmanager.core.util.FileSizeFormatter

@Composable
fun StorageScreen(
    viewModel: StorageViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().safeDrawingPadding()) {
        when {
            uiState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            uiState.summary != null -> {
                val summary = uiState.summary!!
                Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                    Text("Storage Summary", style = MaterialTheme.typography.titleLarge)

                    val usedFraction = if (summary.totalBytes > 0) {
                        (summary.usedBytes.toFloat() / summary.totalBytes.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    LinearProgressIndicator(
                        progress = { usedFraction },
                        modifier = Modifier.fillMaxWidth().height(8.dp).padding(vertical = 12.dp)
                    )
                    Text(
                        "${FileSizeFormatter.format(summary.usedBytes)} used of ${FileSizeFormatter.format(summary.totalBytes)}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "${FileSizeFormatter.format(summary.freeBytes)} free",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Text(
                        "Category के अनुसार",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(top = 24.dp, bottom = 8.dp)
                    )

                    LazyColumn {
                        items(summary.byCategory.entries.toList()) { (category, bytes) ->
                            ListItem(
                                headlineContent = { Text(category.name) },
                                trailingContent = { Text(FileSizeFormatter.format(bytes)) }
                            )
                        }
                    }
                }
            }
            uiState.errorMessage != null -> Text(
                uiState.errorMessage!!,
                modifier = Modifier.align(Alignment.Center).padding(24.dp)
            )
        }
    }
}
