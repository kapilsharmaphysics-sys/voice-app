package com.app.voiceapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val VoiceAppColorScheme = darkColorScheme(
    primary = VioletPrimary,
    onPrimary = OnSurfaceLight,
    primaryContainer = Violet40,
    onPrimaryContainer = VioletLight,
    secondary = CyanAccent,
    onSecondary = OnSurfaceLight,
    background = DeepBackground,
    onBackground = OnSurfaceLight,
    surface = SurfaceDark,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = ErrorRed,
    onError = OnSurfaceLight
)

/**
 * App-wide Material3 theme — always dark, no dynamic color, no light variant.
 */
@Composable
fun VoiceAPPTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VoiceAppColorScheme,
        typography = Typography,
        content = content
    )
}
