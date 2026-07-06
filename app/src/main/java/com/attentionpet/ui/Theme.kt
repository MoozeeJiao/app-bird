package com.attentionpet.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AttentionColors = lightColorScheme(
    primary = Color(0xFF24383F),
    secondary = Color(0xFF45D6A1),
    tertiary = Color(0xFFFFD86F),
    background = Color(0xFFF3F8F7),
    surface = Color.White,
    error = Color(0xFFF45E63)
)

@Composable
fun AttentionPetTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AttentionColors, content = content)
}
