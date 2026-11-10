package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.R

data class ThemeColorDefinition(
    val key: String,
    val nameRes: Int,
    val primary: Color,
    val primaryLight: Color,
    val primaryDark: Color
)

val AvailableThemeColors = listOf(
    ThemeColorDefinition("orange", R.string.color_orange, WallhavenOrange, WallhavenOrangeLight, WallhavenOrangeDark),
    ThemeColorDefinition("blue", R.string.color_blue, Color(0xFF1976D2), Color(0xFF42A5F5), Color(0xFF0D47A1)),
    ThemeColorDefinition("green", R.string.color_green, Color(0xFF2E7D32), Color(0xFF4CAF50), Color(0xFF1B5E20)),
    ThemeColorDefinition("purple", R.string.color_purple, Color(0xFF7B1FA2), Color(0xFFAB47BC), Color(0xFF4A148C)),
    ThemeColorDefinition("pink", R.string.color_pink, Color(0xFFE91E63), Color(0xFFF06292), Color(0xFF880E4F)),
    ThemeColorDefinition("teal", R.string.color_teal, Color(0xFF00897B), Color(0xFF26A69A), Color(0xFF004D40)),
    ThemeColorDefinition("amber", R.string.color_amber, Color(0xFFFFA000), Color(0xFFFFB300), Color(0xFFFF6F00)),
    ThemeColorDefinition("red", R.string.color_red, Color(0xFFD32F2F), Color(0xFFEF5350), Color(0xFFB71C1C)),
    ThemeColorDefinition("slate", R.string.color_slate, Color(0xFF546E7A), Color(0xFF78909C), Color(0xFF263238))
)

fun getThemeColorDefinition(key: String): ThemeColorDefinition {
    return AvailableThemeColors.firstOrNull { it.key == key } ?: AvailableThemeColors.first()
}

fun buildCustomColorScheme(
    themeColorKey: String,
    colorStyle: String,
    isDark: Boolean,
    amoledBlack: Boolean
): ColorScheme {
    val def = getThemeColorDefinition(themeColorKey)

    val scheme = if (isDark) {
        when (colorStyle) {
            "expressive" -> darkColorScheme(
                primary = def.primaryLight,
                onPrimary = Color.Black,
                primaryContainer = def.primary.copy(alpha = 0.6f),
                onPrimaryContainer = Color.White,
                secondary = def.primaryLight,
                onSecondary = Color.Black,
                secondaryContainer = def.primaryDark.copy(alpha = 0.45f),
                onSecondaryContainer = Color.White,
                background = WallhavenDarkBg,
                onBackground = Color(0xFFF5F5F7),
                surface = WallhavenDarkSurface,
                onSurface = Color(0xFFF5F5F7),
                surfaceVariant = WallhavenDarkCard,
                onSurfaceVariant = Color(0xFFCED2DA),
                outline = WallhavenDarkOutline
            )
            "neutral" -> darkColorScheme(
                primary = def.primaryLight.copy(alpha = 0.85f),
                onPrimary = Color.Black,
                primaryContainer = Color(0xFF2E3136),
                onPrimaryContainer = Color(0xFFE0E0E0),
                secondary = Color(0xFFA0A5AD),
                onSecondary = Color.Black,
                secondaryContainer = Color(0xFF25272B),
                onSecondaryContainer = Color(0xFFD0D3D9),
                background = WallhavenDarkBg,
                onBackground = Color(0xFFEAEAEA),
                surface = WallhavenDarkSurface,
                onSurface = Color(0xFFEAEAEA),
                surfaceVariant = WallhavenDarkCard,
                onSurfaceVariant = Color(0xFFB0B3BA),
                outline = WallhavenDarkOutline
            )
            "high_contrast" -> darkColorScheme(
                primary = def.primaryLight,
                onPrimary = Color.Black,
                primaryContainer = Color(0xFF252528),
                onPrimaryContainer = Color.White,
                secondary = Color.White,
                onSecondary = Color.Black,
                secondaryContainer = def.primaryDark,
                onSecondaryContainer = Color.White,
                background = Color(0xFF101012),
                onBackground = Color.White,
                surface = Color(0xFF18181B),
                onSurface = Color.White,
                surfaceVariant = Color(0xFF26262B),
                onSurfaceVariant = Color(0xFFE0E0E0),
                outline = Color.White.copy(alpha = 0.6f)
            )
            else -> darkColorScheme( // vibrant
                primary = def.primaryLight,
                onPrimary = Color.Black,
                primaryContainer = def.primaryDark.copy(alpha = 0.65f),
                onPrimaryContainer = Color.White,
                secondary = def.primary,
                onSecondary = Color.White,
                secondaryContainer = def.primaryDark.copy(alpha = 0.35f),
                onSecondaryContainer = Color.White,
                background = WallhavenDarkBg,
                onBackground = Color(0xFFF0F0F0),
                surface = WallhavenDarkSurface,
                onSurface = Color(0xFFF0F0F0),
                surfaceVariant = WallhavenDarkCard,
                onSurfaceVariant = Color(0xFFC0C2C8),
                outline = WallhavenDarkOutline
            )
        }
    } else {
        when (colorStyle) {
            "expressive" -> lightColorScheme(
                primary = def.primary,
                onPrimary = Color.White,
                primaryContainer = def.primaryLight.copy(alpha = 0.28f),
                onPrimaryContainer = def.primaryDark,
                secondary = def.primaryDark,
                onSecondary = Color.White,
                secondaryContainer = def.primaryLight.copy(alpha = 0.18f),
                onSecondaryContainer = def.primaryDark,
                background = WallhavenLightBg,
                onBackground = Color(0xFF16171A),
                surface = WallhavenLightSurface,
                onSurface = Color(0xFF16171A),
                surfaceVariant = WallhavenLightCard,
                onSurfaceVariant = Color(0xFF3E4148),
                outline = WallhavenLightOutline
            )
            "neutral" -> lightColorScheme(
                primary = def.primary.copy(alpha = 0.88f),
                onPrimary = Color.White,
                primaryContainer = Color(0xFFEAECEF),
                onPrimaryContainer = Color(0xFF1E2024),
                secondary = Color(0xFF5A606A),
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFF0F2F5),
                onSecondaryContainer = Color(0xFF2C2F36),
                background = WallhavenLightBg,
                onBackground = Color(0xFF1B1B1D),
                surface = WallhavenLightSurface,
                onSurface = Color(0xFF1B1B1D),
                surfaceVariant = WallhavenLightCard,
                onSurfaceVariant = Color(0xFF49454F),
                outline = WallhavenLightOutline
            )
            "high_contrast" -> lightColorScheme(
                primary = def.primaryDark,
                onPrimary = Color.White,
                primaryContainer = Color(0xFFE2E4E8),
                onPrimaryContainer = Color.Black,
                secondary = Color.Black,
                onSecondary = Color.White,
                secondaryContainer = Color(0xFFEEEEEE),
                onSecondaryContainer = Color.Black,
                background = Color(0xFFFFFFFF),
                onBackground = Color.Black,
                surface = Color(0xFFFFFFFF),
                onSurface = Color.Black,
                surfaceVariant = Color(0xFFECEFF1),
                onSurfaceVariant = Color.Black,
                outline = Color.Black
            )
            else -> lightColorScheme( // vibrant
                primary = def.primary,
                onPrimary = Color.White,
                primaryContainer = def.primary.copy(alpha = 0.16f),
                onPrimaryContainer = def.primaryDark,
                secondary = def.primaryDark,
                onSecondary = Color.White,
                secondaryContainer = def.primaryLight.copy(alpha = 0.12f),
                onSecondaryContainer = def.primaryDark,
                background = WallhavenLightBg,
                onBackground = Color(0xFF1B1B1D),
                surface = WallhavenLightSurface,
                onSurface = Color(0xFF1B1B1D),
                surfaceVariant = WallhavenLightCard,
                onSurfaceVariant = Color(0xFF49454F),
                outline = WallhavenLightOutline
            )
        }
    }

    return if (isDark && amoledBlack) {
        scheme.copy(
            background = WallhavenAmoledBg,
            surface = WallhavenAmoledSurface,
            surfaceVariant = Color(0xFF161616)
        )
    } else {
        scheme
    }
}

@Composable
fun WallhavenTheme(
    darkModePreference: String = "system", // "system", "light", "dark"
    dynamicColor: Boolean = true,
    themeColor: String = "orange",
    colorStyle: String = "vibrant",
    amoledBlack: Boolean = false,
    content: @Composable () -> Unit
) {
    val isSystemDark = isSystemInDarkTheme()
    val isDark = when (darkModePreference) {
        "light" -> false
        "dark" -> true
        else -> isSystemDark
    }

    val context = LocalContext.current
    var colorScheme = when {
        dynamicColor -> {
            val dynamic = if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (isDark && amoledBlack) {
                dynamic.copy(
                    background = WallhavenAmoledBg,
                    surface = WallhavenAmoledSurface,
                    surfaceVariant = Color(0xFF161616)
                )
            } else {
                dynamic
            }
        }
        else -> {
            buildCustomColorScheme(
                themeColorKey = themeColor,
                colorStyle = colorStyle,
                isDark = isDark,
                amoledBlack = amoledBlack
            )
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

