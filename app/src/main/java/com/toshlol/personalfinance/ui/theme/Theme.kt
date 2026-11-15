package com.toshlol.personalfinance.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val ToshlolLightColors: ColorScheme =
    lightColorScheme(
        primary = Color(0xFFFFD23F),
        secondary = Color(0xFF292B2F),
        background = Color(0xFFF1F2F4),
        surface = Color.White,
        onPrimary = Color(0xFF292B2F),
        onSecondary = Color.White,
        onBackground = Color(0xFF0F172A),
        onSurface = Color(0xFF0F172A),
    )
private val ToshlolTypography: Typography = Typography()

@Composable
fun ToshlolTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = ToshlolLightColors, typography = ToshlolTypography, content = content)
}
