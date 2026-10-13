package com.toshlol.personalfinance.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ToshlolLightColors = lightColorScheme(
    primary = Color(0xFF0F766E),
    secondary = Color(0xFF14B8A6),
    tertiary = Color(0xFFEEF2FF),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF0F172A),
    onSurface = Color(0xFF0F172A)
)

private val ToshlolTypography = Typography()

@Composable
fun ToshlolTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) ToshlolLightColors else ToshlolLightColors
    MaterialTheme(
        colorScheme = colors,
        typography = ToshlolTypography,
        content = content
    )
}
