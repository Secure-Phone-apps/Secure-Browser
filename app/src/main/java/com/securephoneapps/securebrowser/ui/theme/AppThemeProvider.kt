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
                    darkColorScheme(
                        primary = Color(0xFF84DB7D),
                        onPrimary = Color(0xFF00390A),
                        primaryContainer = Color(0xFF005313),
                        onPrimaryContainer = Color(0xFF9FF896),
                        secondary = Color(0xFFB9CCB3),
                        onSecondary = Color(0xFF253423),
                        secondaryContainer = Color(0xFF3B4B38),
                        onSecondaryContainer = Color(0xFFD5E8CE),
                        tertiary = Color(0xFFA1CED4),
                        onTertiary = Color(0xFF00363D),
                        tertiaryContainer = Color(0xFF1F4D54),
                        onTertiaryContainer = Color(0xFFBCEBEE),
                        background = Color(0xFF121411),
                        onBackground = Color(0xFFE2E3DC),
                        surface = Color(0xFF121411),
                        onSurface = Color(0xFFE2E3DC),
                        surfaceVariant = Color(0xFF43483E),
                        onSurfaceVariant = Color(0xFFC3C8BC),
                        outline = Color(0xFF8D9287),
                        error = Color(0xFFFFB4AB),
                        onError = Color(0xFF690005)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF006E1C),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFF9FF896),
                        onPrimaryContainer = Color(0xFF002203),
                        secondary = Color(0xFF52634F),
                        onSecondary = Color(0xFFFFFFFF),
                        secondaryContainer = Color(0xFFD5E8CE),
                        onSecondaryContainer = Color(0xFF101F10),
                        tertiary = Color(0xFF38656D),
                        onTertiary = Color(0xFFFFFFFF),
                        tertiaryContainer = Color(0xFFBCEBEE),
                        onTertiaryContainer = Color(0xFF002024),
                        background = Color(0xFFFCFDF6),
                        onBackground = Color(0xFF1A1C18),
                        surface = Color(0xFFFCFDF6),
                        onSurface = Color(0xFF1A1C18),
                        surfaceVariant = Color(0xFFDFE4D7),
                        onSurfaceVariant = Color(0xFF43483E),
                        outline = Color(0xFF73796D),
                        error = Color(0xFFBA1A1A),
                        onError = Color(0xFFFFFFFF)
                    )
                }
            }
            "Purple" -> {
                if (isDark) {
                    darkColorScheme(
                        primary = Color(0xFFDEBCFF),
                        onPrimary = Color(0xFF44226E),
                        primaryContainer = Color(0xFF5C3986),
                        onPrimaryContainer = Color(0xFFF1DBFF),
                        secondary = Color(0xFFD0C2DB),
                        onSecondary = Color(0xFF352D40),
                        secondaryContainer = Color(0xFF4C4357),
                        onSecondaryContainer = Color(0xFFECDEF7),
                        tertiary = Color(0xFFEEB8C0),
                        onTertiary = Color(0xFF49252B),
                        tertiaryContainer = Color(0xFF623B41),
                        onTertiaryContainer = Color(0xFFFFD9DF),
                        background = Color(0xFF1C1B1E),
                        onBackground = Color(0xFFE6E1E6),
                        surface = Color(0xFF1C1B1E),
                        onSurface = Color(0xFFE6E1E6),
                        surfaceVariant = Color(0xFF49454E),
                        onSurfaceVariant = Color(0xFFCBC4CF),
                        outline = Color(0xFF948F99),
                        error = Color(0xFFFFB4AB),
                        onError = Color(0xFF690005)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF7551A0),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFFF1DBFF),
                        onPrimaryContainer = Color(0xFF2C0051),
                        secondary = Color(0xFF655A6F),
                        onSecondary = Color(0xFFFFFFFF),
                        secondaryContainer = Color(0xFFECDEF7),
                        onSecondaryContainer = Color(0xFF20182A),
                        tertiary = Color(0xFF7D5258),
                        onTertiary = Color(0xFFFFFFFF),
                        tertiaryContainer = Color(0xFFFFD9DF),
                        onTertiaryContainer = Color(0xFF311017),
                        background = Color(0xFFFDFBFF),
                        onBackground = Color(0xFF1C1B1E),
                        surface = Color(0xFFFDFBFF),
                        onSurface = Color(0xFF1C1B1E),
                        surfaceVariant = Color(0xFFE8E0EB),
                        onSurfaceVariant = Color(0xFF49454E),
                        outline = Color(0xFF7A757F),
                        error = Color(0xFFBA1A1A),
                        onError = Color(0xFFFFFFFF)
                    )
                }
            }
            "Red" -> {
                if (isDark) {
                    darkColorScheme(
                        primary = Color(0xFFFFB4AB),
                        onPrimary = Color(0xFF690005),
                        primaryContainer = Color(0xFF93000A),
                        onPrimaryContainer = Color(0xFFFFDAD6),
                        secondary = Color(0xFFE7BDB7),
                        onSecondary = Color(0xFF442926),
                        secondaryContainer = Color(0xFF5D3F3C),
                        onSecondaryContainer = Color(0xFFFFDAD5),
                        tertiary = Color(0xFFDEC48C),
                        onTertiary = Color(0xFF3E2F04),
                        tertiaryContainer = Color(0xFF564519),
                        onTertiaryContainer = Color(0xFFFBE0A6),
                        background = Color(0xFF201A19),
                        onBackground = Color(0xFFEDE0DE),
                        surface = Color(0xFF201A19),
                        onSurface = Color(0xFFEDE0DE),
                        surfaceVariant = Color(0xFF534341),
                        onSurfaceVariant = Color(0xFFD8C2BF),
                        outline = Color(0xFFA08C8A),
                        error = Color(0xFFFFB4AB),
                        onError = Color(0xFF690005)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFFB32626),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFFFFDAD6),
                        onPrimaryContainer = Color(0xFF410002),
                        secondary = Color(0xFF775652),
                        onSecondary = Color(0xFFFFFFFF),
                        secondaryContainer = Color(0xFFFFDAD5),
                        onSecondaryContainer = Color(0xFF2C1512),
                        tertiary = Color(0xFF715D2C),
                        onTertiary = Color(0xFFFFFFFF),
                        tertiaryContainer = Color(0xFFFBE0A6),
                        onTertiaryContainer = Color(0xFF251A00),
                        background = Color(0xFFFFFBFF),
                        onBackground = Color(0xFF201A19),
                        surface = Color(0xFFFFFBFF),
                        onSurface = Color(0xFF201A19),
                        surfaceVariant = Color(0xFFF5DDDA),
                        onSurfaceVariant = Color(0xFF534341),
                        outline = Color(0xFF857371),
                        error = Color(0xFFBA1A1A),
                        onError = Color(0xFFFFFFFF)
                    )
                }
            }
            "Orange" -> {
                if (isDark) {
                    darkColorScheme(
                        primary = Color(0xFFFFB684),
                        onPrimary = Color(0xFF502400),
                        primaryContainer = Color(0xFF713600),
                        onPrimaryContainer = Color(0xFFFFDCC4),
                        secondary = Color(0xFFE2C0A8),
                        onSecondary = Color(0xFF422C1B),
                        secondaryContainer = Color(0xFF5A4230),
                        onSecondaryContainer = Color(0xFFFFDCC4),
                        tertiary = Color(0xFFC0CA94),
                        onTertiary = Color(0xFF2A340A),
                        tertiaryContainer = Color(0xFF404B1F),
                        onTertiaryContainer = Color(0xFFDCE6AE),
                        background = Color(0xFF1F120A),
                        onBackground = Color(0xFFECE0DB),
                        surface = Color(0xFF1F120A),
                        onSurface = Color(0xFFECE0DB),
                        surfaceVariant = Color(0xFF51443B),
                        onSurfaceVariant = Color(0xFFD5C3B7),
                        outline = Color(0xFF9E8E82),
                        error = Color(0xFFFFB4AB),
                        onError = Color(0xFF690005)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF944A00),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFFFFDCC4),
                        onPrimaryContainer = Color(0xFF301400),
                        secondary = Color(0xFF745945),
                        onSecondary = Color(0xFFFFFFFF),
                        secondaryContainer = Color(0xFFFFDCC4),
                        onSecondaryContainer = Color(0xFF2A1708),
                        tertiary = Color(0xFF586333),
                        onTertiary = Color(0xFFFFFFFF),
                        tertiaryContainer = Color(0xFFDCE6AE),
                        onTertiaryContainer = Color(0xFF161E00),
                        background = Color(0xFFFCFCFC),
                        onBackground = Color(0xFF201A17),
                        surface = Color(0xFFFCFCFC),
                        onSurface = Color(0xFF201A17),
                        surfaceVariant = Color(0xFFF2DFD4),
                        onSurfaceVariant = Color(0xFF51443B),
                        outline = Color(0xFF83746A),
                        error = Color(0xFFBA1A1A),
                        onError = Color(0xFFFFFFFF)
                    )
                }
            }
            else -> { // Blue
                if (isDark) {
                    darkColorScheme(
                        primary = Color(0xFF9ECAFF),
                        onPrimary = Color(0xFF003258),
                        primaryContainer = Color(0xFF00497D),
                        onPrimaryContainer = Color(0xFFD1E4FF),
                        secondary = Color(0xFFBBC7DB),
                        onSecondary = Color(0xFF253140),
                        secondaryContainer = Color(0xFF3B4858),
                        onSecondaryContainer = Color(0xFFD7E3F7),
                        tertiary = Color(0xFFD6BEE6),
                        onTertiary = Color(0xFF3B294A),
                        tertiaryContainer = Color(0xFF533F62),
                        onTertiaryContainer = Color(0xFFF1DBFF),
                        background = Color(0xFF1A1C1E),
                        onBackground = Color(0xFFE2E2E6),
                        surface = Color(0xFF1A1C1E),
                        onSurface = Color(0xFFE2E2E6),
                        surfaceVariant = Color(0xFF43474E),
                        onSurfaceVariant = Color(0xFFC3C7D0),
                        outline = Color(0xFF8D9199),
                        error = Color(0xFFFFB4AB),
                        onError = Color(0xFF690005)
                    )
                } else {
                    lightColorScheme(
                        primary = Color(0xFF0061A4),
                        onPrimary = Color(0xFFFFFFFF),
                        primaryContainer = Color(0xFFD1E4FF),
                        onPrimaryContainer = Color(0xFF001D36),
                        secondary = Color(0xFF535F70),
                        onSecondary = Color(0xFFFFFFFF),
                        secondaryContainer = Color(0xFFD7E3F7),
                        onSecondaryContainer = Color(0xFF101C2B),
                        tertiary = Color(0xFF6B5778),
                        onTertiary = Color(0xFFFFFFFF),
                        tertiaryContainer = Color(0xFFF1DBFF),
                        onTertiaryContainer = Color(0xFF251431),
                        background = Color(0xFFFDFCFF),
                        onBackground = Color(0xFF1A1C1E),
                        surface = Color(0xFFFDFCFF),
                        onSurface = Color(0xFF1A1C1E),
                        surfaceVariant = Color(0xFFDFE2EB),
                        onSurfaceVariant = Color(0xFF43474E),
                        outline = Color(0xFF73777F),
                        error = Color(0xFFBA1A1A),
                        onError = Color(0xFFFFFFFF)
                    )
                }
            }
        }
    }
}
