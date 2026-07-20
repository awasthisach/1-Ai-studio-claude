package com.vvf.smartmanager.presentation.duplicate

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vvf.smartmanager.core.util.FileSizeFormatter
import com.vvf.smartmanager.domain.model.DuplicateLevel

@Composable
fun DuplicateScreen(
    viewModel: DuplicateViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().safeDrawingPadding().padding(16.dp)) {
        Text("Duplicate Cleaner", style = MaterialTheme.typography.titleLarge)
        Text(
            "Level 1 (Exact) + Level 2 (Metadata) + Level 3 (Visual Similarity)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Text(
            "Visual Similarity Threshold: ${(uiState.similarityThreshold * 100).toInt()}%",
            modifier = Modifier.padding(top = 16.dp)
        )
        Slider(
            value = uiState.similarityThreshold,
            onValueChange = viewModel::onThresholdChanged,
            valueRange = 0.70f..0.95f
        )

        Button(
            onClick = viewModel::startScan,
            enabled = !uiState.isScanning,
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
            Text(if (uiState.isScanning) "Scan चल रहा है…" else "Duplicate Scan शुरू करें")
        }

        if (uiState.isScanning) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }

        uiState.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        if (uiState.hasScanned && uiState.groups.isEmpty() && !uiState.isScanning) {
            Text("कोई duplicate नहीं मिला 🎉", modifier = Modifier.padding(top = 24.dp))
        }

        LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
            items(uiState.groups, key = { it.groupId }) { group ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row {
                            Text(
                                text = when (group.level) {
                                    DuplicateLevel.EXACT_HASH -> "Exact Duplicate"
                                    DuplicateLevel.METADATA_MATCH -> "Same Metadata"
                                    DuplicateLevel.VISUAL_SIMILAR -> "Visually Similar"
                                },
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text("${group.items.size} फाइलें — ${FileSizeFormatter.format(group.potentialSavingsBytes)} बचाए जा सकते हैं")
                        group.items.forEach { item ->
                            Text("• ${item.name}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}
