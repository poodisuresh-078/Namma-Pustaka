package com.example.nammapustaka.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = VividPurple,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = LightPurple,
    onPrimaryContainer = DeepPurple,
    secondary = TealAccent,
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = LightTeal,
    onSecondaryContainer = Color(0xFF00382E),
    tertiary = Color(0xFF00897B),
    onTertiary = Color(0xFFFFFFFF),
    background = PageBackground,
    onBackground = Color(0xFF1A1A2E),
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1A2E),
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = Color(0xFF49454F),
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = Color(0xFFF5F0FF),
    surfaceContainer = Color(0xFFEFEAF8),
    surfaceContainerHigh = Color(0xFFE8E0F0),
    surfaceContainerHighest = Color(0xFFE1D8E8),
    outline = OutlineSoft,
    outlineVariant = Color(0xFFD6CEE0),
    error = OverdueRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFBB86FC),
    onPrimary = Color(0xFF1A0040),
    primaryContainer = Color(0xFF4A148C),
    onPrimaryContainer = Color(0xFFE8DEF8),
    secondary = Color(0xFF80CBC4),
    onSecondary = Color(0xFF003830),
    background = Color(0xFF121218),
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B22),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = Color(0xFF3F3848),
    onSurfaceVariant = Color(0xFFCAC4D0),
    error = OverdueRed,
    onError = Color(0xFFFFFFFF),
)

@Composable
fun NammaPustakaTheme(
    darkTheme: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> LightColorScheme
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surfaceContainer.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}
