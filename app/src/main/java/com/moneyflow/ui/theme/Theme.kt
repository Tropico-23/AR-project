package com.moneyflow.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.moneyflow.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Primary,
    secondary = Primary.copy(alpha = 0.8f),
    background = SurfaceLight
)

private val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    secondary = PrimaryDark.copy(alpha = 0.8f),
    background = SurfaceDark
)

@Composable
fun MoneyFlowTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        content = content
    )
}
