package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SonixColorScheme = darkColorScheme(
    primary = StudioColors.Cyan,
    onPrimary = StudioColors.BgTop,
    primaryContainer = StudioColors.ElevatedSurface,
    onPrimaryContainer = StudioColors.Cyan,
    secondary = StudioColors.Violet,
    onSecondary = StudioColors.BgTop,
    secondaryContainer = StudioColors.ElevatedSurface,
    onSecondaryContainer = StudioColors.Violet,
    tertiary = StudioColors.Emerald,
    onTertiary = StudioColors.BgTop,
    background = StudioColors.BgTop,
    onBackground = StudioColors.TextPrimary,
    surface = StudioColors.CardSurface,
    onSurface = StudioColors.TextPrimary,
    surfaceVariant = StudioColors.ElevatedSurface,
    onSurfaceVariant = StudioColors.TextSecondary,
    outline = StudioColors.Border,
    outlineVariant = StudioColors.BorderBright,
    error = StudioColors.Rose,
    onError = StudioColors.TextPrimary,
)

private val StudioShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun SonixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SonixColorScheme,
        typography = StudioTypography,
        shapes = StudioShapes,
        content = content,
    )
}

@Composable
fun StudioDropdownMenuTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = MaterialTheme.colorScheme.copy(surfaceContainer = StudioColors.CardSurface),
        shapes = MaterialTheme.shapes.copy(extraSmall = RoundedCornerShape(12.dp)),
        content = content,
    )
}
