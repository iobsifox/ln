package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CosmicDarkColorScheme = darkColorScheme(
    primary = BlueNeon,
    onPrimary = BgDark,
    secondary = PurpleNeon,
    onSecondary = BgDark,
    tertiary = CyanNeon,
    onTertiary = BgDark,
    background = BgDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = CardDark,
    onSurfaceVariant = TextPrimary,
    outline = BorderDark,
    error = DangerNeon,
    onError = BgDark
)

@Composable
fun LastNightTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CosmicDarkColorScheme,
        typography = Typography,
        content = content
    )
}
