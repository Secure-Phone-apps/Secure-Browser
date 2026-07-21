package com.securephoneapps.securebrowser.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

object AppThemeProvider {
    fun getColorScheme(theme: String, accentColor: String): ColorScheme {
        val isDark = theme == "Dark"
        return when (accentColor) {
            "Green" -> {
                if (isDark) {
                    darkColorScheme(primary = Color(0xFF81C784), secondary = Color(0xFFAED581))
                } else {
                    lightColorScheme(primary = Color(0xFF388E3C), secondary = Color(0xFF689F38))
                }
            }
            "Purple" -> {
                if (isDark) {
                    darkColorScheme(primary = Color(0xFFBA68C8), secondary = Color(0xFF9575CD))
                } else {
                    lightColorScheme(primary = Color(0xFF7B1FA2), secondary = Color(0xFF512DA8))
                }
            }
            "Red" -> {
                if (isDark) {
                    darkColorScheme(primary = Color(0xFFE57373), secondary = Color(0xFFF06292))
                } else {
                    lightColorScheme(primary = Color(0xFFD32F2F), secondary = Color(0xFFC2185B))
                }
            }
            "Orange" -> {
                if (isDark) {
                    darkColorScheme(primary = Color(0xFFFFB74D), secondary = Color(0xFFFF8A65))
                } else {
                    lightColorScheme(primary = Color(0xFFF57C00), secondary = Color(0xFFE64A19))
                }
            }
            else -> { // Blue
                if (isDark) {
                    darkColorScheme(primary = Color(0xFF64B5F6), secondary = Color(0xFF4FC3F7))
                } else {
                    lightColorScheme(primary = Color(0xFF1976D2), secondary = Color(0xFF0288D1))
                }
            }
        }
    }
}
