package com.vvf.smartmanager

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.fragment.app.FragmentActivity
import com.vvf.smartmanager.core.navigation.VvfNavHost
import com.vvf.smartmanager.core.theme.VvfSmartManagerTheme
import com.vvf.smartmanager.presentation.components.StoragePermissionGate
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host. Extends [FragmentActivity] (not plain [ComponentActivity]) because
 * [androidx.biometric.BiometricPrompt] (Vault unlock, Phase 6) requires a FragmentActivity.
 */
@AndroidEntryPoint
class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // installSplashScreen() MUST be called before super.onCreate() — this is the
        // documented contract of androidx.core:core-splashscreen. It reads the
        // Theme.VvfSmartManager.Splash theme (set on this Activity in AndroidManifest.xml,
        // showing the VVF logo) and hands off to postSplashScreenTheme automatically.
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            VvfSmartManagerTheme {
                StoragePermissionGate {
                    VvfNavHost()
                }
            }
        }
    }
}
