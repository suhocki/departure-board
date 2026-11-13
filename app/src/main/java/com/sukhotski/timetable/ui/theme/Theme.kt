package com.sukhotski.timetable.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Text on the green bar is dark: white on this green has too little contrast.
private val LightColors = lightColorScheme(
    primary = Color(0xFF3F7A00),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF73D700),
    onPrimaryContainer = Color(0xFF0F1F00),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FE84A),
    onPrimary = Color(0xFF153300),
    primaryContainer = Color(0xFF2A5200),
    onPrimaryContainer = Color(0xFFD5F5A8),
)

@Composable
fun TimetableTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) DarkColors else LightColors, content = content)
}
