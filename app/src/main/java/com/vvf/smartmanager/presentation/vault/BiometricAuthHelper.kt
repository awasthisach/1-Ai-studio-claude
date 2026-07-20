package com.vvf.smartmanager.presentation.vault

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Wraps [BiometricPrompt] for Vault unlock (Phase 6). Requires the hosting Activity to be a
 * [FragmentActivity] — see MainActivity. Falls back gracefully: if no biometric hardware/
 * enrollment is available, [isBiometricAvailable] returns false and the UI should only offer PIN.
 */
class BiometricAuthHelper(private val activity: FragmentActivity) {

    fun isBiometricAvailable(): Boolean {
        val manager = BiometricManager.from(activity)
        return manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS
    }

    fun authenticate(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    onError(errString.toString())
                }
            }
        )

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Vault Unlock")
            .setSubtitle("अपनी पहचान verify करें")
            .setNegativeButtonText("PIN इस्तेमाल करें")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()

        prompt.authenticate(promptInfo)
    }
}

@Composable
fun rememberBiometricAuthHelper(): BiometricAuthHelper? {
    val context = LocalContext.current
    val activity = context as? FragmentActivity ?: return null
    return remember(activity) { BiometricAuthHelper(activity) }
}
