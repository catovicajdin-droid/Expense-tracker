package com.catovicajdin.expensetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFFB39DFF),
    secondary = Color(0xFF80CBC4),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onPrimary = Color(0xFF1A1A1A),
    onBackground = Color(0xFFECECEC),
    onSurface = Color(0xFFECECEC),
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF6A4FE0),
    secondary = Color(0xFF00796B),
)

@Composable
fun ExpenseTrackerTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(colorScheme = colorScheme, content = content)
}
