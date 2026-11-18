package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val DarkColorScheme = darkColorScheme(
    primary = EmeraldPrimaryDark,
    onPrimary = Color(0xFF003827),
    primaryContainer = EmeraldContainerDark,
    onPrimaryContainer = OnEmeraldContainerDark,
    secondary = JadeSecondaryDark,
    onSecondary = Color(0xFF003827),
    secondaryContainer = JadeContainerDark,
    onSecondaryContainer = OnJadeContainerDark,
    tertiary = AmberTertiaryDark,
    onTertiary = Color(0xFF3E2200),
    tertiaryContainer = AmberContainerDark,
    onTertiaryContainer = OnAmberContainerDark,
    background = ObsidianBackground,
    onBackground = CreamOnSurface,
    surface = ObsidianSurface,
    onSurface = CreamOnSurface,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = CreamOnSurfaceVariant,
    outline = ObsidianOutline,
    outlineVariant = Color(0xFF28332E),
    error = ErrorCrimsonDark,
    errorContainer = ErrorContainerDark,
)

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainerLight,
    onPrimaryContainer = OnEmeraldContainerLight,
    secondary = JadeSecondary,
    onSecondary = Color.White,
    secondaryContainer = JadeContainerLight,
    onSecondaryContainer = OnJadeContainerLight,
    tertiary = AmberTertiary,
    onTertiary = Color.White,
    tertiaryContainer = AmberContainerLight,
    onTertiaryContainer = OnAmberContainerLight,
    background = AlabasterBackground,
    onBackground = InkOnSurface,
    surface = AlabasterSurface,
    onSurface = InkOnSurface,
    surfaceVariant = AlabasterSurfaceVariant,
    onSurfaceVariant = InkOnSurfaceVariant,
    outline = HairlineOutline,
    outlineVariant = Color(0xFFE7E4DC),
    error = ErrorCrimson,
    errorContainer = ErrorContainerLight,
)

val KasirShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun KasirKuTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KasirTypography,
        shapes = KasirShapes,
        content = content,
    )
}
