package com.vvf.smartmanager.presentation.vault

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.vvf.smartmanager.core.util.FileSizeFormatter

@Composable
fun VaultScreen(
    viewModel: VaultViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val biometricHelper = rememberBiometricAuthHelper()

    Box(modifier = Modifier.safeDrawingPadding()) {
        when {
            uiState.needsPinSetup -> VaultSetPinContent(
                errorMessage = uiState.pinError,
                onConfirm = { pin, confirm -> viewModel.onSetPin(pin, confirm) }
            )
            !uiState.isUnlocked -> VaultLockedContent(
                errorMessage = uiState.pinError,
                biometricAvailable = biometricHelper?.isBiometricAvailable() == true,
                onPinSubmit = viewModel::onUnlockAttempt,
                onBiometricClick = {
                    biometricHelper?.authenticate(
                        onSuccess = viewModel::onBiometricSuccess,
                        onError = { }
                    )
                }
            )
            else -> VaultUnlockedContent(
                items = uiState.items,
                onLock = viewModel::lock
            )
        }
    }
}

@Composable
private fun VaultSetPinContent(errorMessage: String?, onConfirm: (String, String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("Vault के लिए नया PIN सेट करें", style = MaterialTheme.typography.titleLarge)
        Text("4 से 8 अंकों का PIN", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("नया PIN") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )
        OutlinedTextField(
            value = confirmPin,
            onValueChange = { confirmPin = it },
            label = { Text("PIN दोबारा डालें") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        )

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Button(
            onClick = { onConfirm(pin, confirmPin) },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) { Text("PIN सेव करें") }
    }
}

@Composable
private fun VaultLockedContent(
    errorMessage: String?,
    biometricAvailable: Boolean,
    onPinSubmit: (String) -> Unit,
    onBiometricClick: () -> Unit
) {
    var pin by remember { mutableStateOf("") }

    LaunchedEffect(biometricAvailable) {
        if (biometricAvailable) onBiometricClick()
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Filled.Lock, contentDescription = null, modifier = Modifier.padding(bottom = 16.dp))
        Text("Vault Locked", style = MaterialTheme.typography.titleLarge)
        Text("जारी रखने के लिए unlock करें", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it },
            label = { Text("PIN डालें") },
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        )

        errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Button(onClick = { onPinSubmit(pin) }, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            Text("Unlock करें")
        }

        if (biometricAvailable) {
            OutlinedButton(onClick = onBiometricClick, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Icon(Icons.Filled.Fingerprint, contentDescription = null)
                Text(" Biometric से Unlock करें", modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun VaultUnlockedContent(
    items: List<com.vvf.smartmanager.domain.model.VaultItem>,
    onLock: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedButton(onClick = onLock, modifier = Modifier.padding(16.dp)) {
            Icon(Icons.Filled.Lock, contentDescription = null)
            Text(" Lock करें", modifier = Modifier.padding(start = 8.dp))
        }

        if (items.isEmpty()) {
            Text(
                "Vault खाली है। File Manager में किसी फाइल को long-press करके \"Add to Vault\" चुनें।",
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn {
                items(items, key = { it.id }) { item ->
                    ListItem(
                        headlineContent = { Text(item.displayName) },
                        supportingContent = { Text(FileSizeFormatter.format(item.sizeBytes)) },
                        leadingContent = { Icon(Icons.Filled.Lock, contentDescription = null) }
                    )
                }
            }
        }
    }
}
