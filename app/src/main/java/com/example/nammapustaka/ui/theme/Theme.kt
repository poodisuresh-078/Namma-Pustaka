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
    primary = DeepGreen,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFC8E6C9),
    onPrimaryContainer = Color(0xFF002106),
    secondary = AmberAccent,
    onSecondary = Color(0xFF1C1B00),
    secondaryContainer = Color(0xFFFFE082),
    onSecondaryContainer = Color(0xFF3E2E00),
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color(0xFFFFFFFF),
    background = PageBackground,
    onBackground = Color(0xFF1A1C1A),
    surface = SurfaceLight,
    onSurface = Color(0xFF1A1C1A),
    surfaceVariant = SurfaceMuted,
    onSurfaceVariant = Color(0xFF424942),
    surfaceContainerLowest = SurfaceLight,
    surfaceContainerLow = Color(0xFFF0F4F0),
    surfaceContainer = Color(0xFFECEFEC),
    surfaceContainerHigh = Color(0xFFE6EBE6),
    surfaceContainerHighest = Color(0xFFE0E6E0),
    outline = OutlineSoft,
    outlineVariant = Color(0xFFD0D9D0),
    error = OverdueRed,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF81C784),
    onPrimary = Color(0xFF003909),
    primaryContainer = Color(0xFF00531B),
    onPrimaryContainer = Color(0xFFA5D6A7),
    secondary = AmberAccent,
    onSecondary = Color(0xFF1C1B00),
    background = Color(0xFF101510),
    onBackground = Color(0xFFE1E4E1),
    surface = Color(0xFF1A1F1A),
    onSurface = Color(0xFFE1E4E1),
    surfaceVariant = Color(0xFF3F4A3F),
    onSurfaceVariant = Color(0xFFBFC9BF),
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
