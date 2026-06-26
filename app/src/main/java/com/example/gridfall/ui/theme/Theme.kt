package com.example.gridfall.ui.theme

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

private val DarkColorScheme = darkColorScheme(
    primary = ArcCyan,
    secondary = TacticalViolet,
    tertiary = SignalAmber,
    background = MidnightNavy,
    surface = DeepGraphite,
    onPrimary = NearBlackNavy,
    onSecondary = IceWhite,
    onTertiary = NearBlackNavy,
    onBackground = IceWhite,
    onSurface = SoftIce
)

private val LightColorScheme = lightColorScheme(
    primary = ArcCyan,
    secondary = TacticalViolet,
    tertiary = SignalAmber,
    background = MidnightNavy,
    surface = DeepGraphite,
    onPrimary = NearBlackNavy,
    onSecondary = IceWhite,
    onTertiary = NearBlackNavy,
    onBackground = IceWhite,
    onSurface = SoftIce
)

@Composable
fun GridfallTheme(
    darkTheme: Boolean = true, // Force dark theme for the tactical look
    dynamicColor: Boolean = false, // Disable dynamic color to maintain visual identity
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
