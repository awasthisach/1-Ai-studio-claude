package com.vvf.smartmanager.core.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val VvfLightColors = lightColorScheme(
    primary = VvfBhagwaOrange,
    secondary = VvfEmeraldGreen,
    tertiary = VvfSkyCyan,
    background = VvfSurfaceLight,
    surface = VvfSurfaceLight,
    error = VvfError,
    onPrimary = VvfSurfaceLight,
    onBackground = VvfCosmicBlue,
    onSurface = VvfCosmicBlue
)

private val VvfDarkColors = darkColorScheme(
    primary = VvfBhagwaOrange,
    secondary = VvfEmeraldGreen,
    tertiary = VvfSkyCyan,
    background = VvfSurfaceDark,
    surface = VvfSurfaceDark,
    error = VvfError,
    onPrimary = VvfSurfaceDark,
    onBackground = VvfSoftGold,
    onSurface = VvfSoftGold
)

@Composable
fun VvfSmartManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // brand palette takes priority over Material You by default
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> VvfDarkColors
        else -> VvfLightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        val currentActivityWindow = (view.context as? Activity)?.window
        if (currentActivityWindow != null) {
            // NOTE (audit finding, 19 July 2026): Window.setStatusBarColor() is deprecated AND
            // a confirmed no-op on API 35+ (targetSdk here) — status bars are transparent by
            // design once enableEdgeToEdge() is active (see MainActivity), so setting a
            // background color here does nothing on the primary target platform and actively
            // fights the edge-to-edge model on older versions. Only the icon-appearance
            // (light/dark) part below is still valid and needed.
            WindowCompat.getInsetsController(currentActivityWindow, view)
                .isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = VvfTypography,
        content = content
    )
}
