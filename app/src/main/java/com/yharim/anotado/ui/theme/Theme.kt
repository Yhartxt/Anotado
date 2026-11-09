package com.yharim.anotado.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AnotadoColors = lightColorScheme(
    primary = Color(0xFF1D7958), onPrimary = Color.White,
    secondary = Color(0xFF176047), background = Color(0xFFF8FAF9),
    surface = Color.White, onSurface = Color(0xFF203D33),
    onBackground = Color(0xFF203D33), error = Color(0xFFB3261E)
)

@Composable
fun AnotadoTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AnotadoColors, typography = Typography, content = content)
}
