package com.toshlol.personalfinance.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ToshlolLightColors: ColorScheme =
    lightColorScheme(
        primary = Color(0xFF0F766E),
        secondary = Color(0xFF0F766E),
        background = Color(0xFFF8FAFC),
        surface = Color.White,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onBackground = Color(0xFF0F172A),
        onSurface = Color(0xFF0F172A),
    )
private val ToshlolTypography: Typography = Typography()

@Composable
fun ToshlolTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ToshlolLightColors, typography = ToshlolTypography, content = content)
}
