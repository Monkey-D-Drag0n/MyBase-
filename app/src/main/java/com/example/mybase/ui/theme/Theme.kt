package com.example.mybase.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val MyBaseLightColorScheme = lightColorScheme(
    primary = MyBaseColors.ButtonRose,
    onPrimary = MyBaseColors.White,
    secondary = ColorTokens.MintDark,
    tertiary = ColorTokens.LavenderDark,
    background = MyBaseColors.Cream,
    onBackground = MyBaseColors.Ink,
    surface = MyBaseColors.White,
    onSurface = MyBaseColors.Ink
)

@Composable
fun MyBaseTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = MyBaseLightColorScheme, typography = Typography, content = content)
}

private object ColorTokens {
    val MintDark = androidx.compose.ui.graphics.Color(0xFF758F7D)
    val LavenderDark = androidx.compose.ui.graphics.Color(0xFF8B7A9E)
}
