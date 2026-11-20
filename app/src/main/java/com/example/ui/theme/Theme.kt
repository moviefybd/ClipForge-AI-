package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val ClipForgeDarkColorScheme = darkColorScheme(
    primary = PrimaryPurple,
    onPrimary = TextPrimary,
    primaryContainer = PrimaryPurpleContainer,
    onPrimaryContainer = AccentLavender,
    secondary = AccentLavender,
    onSecondary = StudioBackground,
    secondaryContainer = StudioCardElevated,
    onSecondaryContainer = TextPrimary,
    tertiary = StatusSuccess,
    onTertiary = StudioBackground,
    tertiaryContainer = StatusSuccessContainer,
    onTertiaryContainer = StatusSuccess,
    background = StudioBackground,
    onBackground = TextPrimary,
    surface = StudioSecondaryBg,
    onSurface = TextPrimary,
    surfaceVariant = StudioCardBg,
    onSurfaceVariant = TextSecondary,
    outline = StudioBorder,
    outlineVariant = ColorTokens.SubtleBorder,
    error = StatusError,
    onError = TextPrimary,
    errorContainer = StatusErrorContainer,
    onErrorContainer = StatusError
)

private object ColorTokens {
    val SubtleBorder = androidx.compose.ui.graphics.Color(0xFF252A3D)
}

val ClipForgeShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ClipForgeTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClipForgeDarkColorScheme,
        typography = ClipForgeTypography,
        shapes = ClipForgeShapes,
        content = content
    )
}
