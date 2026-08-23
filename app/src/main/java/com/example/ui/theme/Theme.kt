package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = ObsidianDark,
    primaryContainer = Color(0xFF382D06),
    onPrimaryContainer = GoldLight,
    secondary = GoldSecondary,
    onSecondary = ObsidianDark,
    secondaryContainer = SlateSubtleDark,
    onSecondaryContainer = TextPrimaryDark,
    tertiary = BullishGreen,
    onTertiary = Color.White,
    background = ObsidianDark,
    onBackground = TextPrimaryDark,
    surface = SlateSurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SlateCardDark,
    onSurfaceVariant = TextSecondaryDark,
    outline = SlateBorderDark,
    outlineVariant = SlateSubtleDark
)

private val LightColorScheme = lightColorScheme(
    primary = GoldDark,
    onPrimary = Color.White,
    primaryContainer = GoldLight,
    onPrimaryContainer = GoldDark,
    secondary = GoldSecondary,
    onSecondary = Color.White,
    secondaryContainer = CardLight,
    onSecondaryContainer = TextPrimaryLight,
    tertiary = BullishGreen,
    onTertiary = Color.White,
    background = GoldCanvasLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = CardLight,
    onSurfaceVariant = TextSecondaryLight,
    outline = BorderLight,
    outlineVariant = Color(0xFFCBD5E1)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Default to sleek financial dark mode
    dynamicColor: Boolean = false, // Keep branded luxury gold palette
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
