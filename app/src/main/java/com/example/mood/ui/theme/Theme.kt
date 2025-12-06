package com.example.mood.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color


private val LedgerDarkColorScheme = darkColorScheme(
    primary = LedgerTeal,
    secondary = LedgerSage,
    tertiary = LedgerGold,

    background = DeepNavyCharcoal,
    surface = DeepNavyCharcoal,

    onBackground = Color(0xFFE6E6E6),
    onSurface = Color(0xFFE6E6E6)
)

@Composable
fun MoodTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LedgerDarkColorScheme,
        typography = Typography,
        content = content
    )
}
