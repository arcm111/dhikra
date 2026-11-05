package com.example.dhikra.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/** Material 3 roles built around the emerald seed, with gold as the tertiary accent. */
private val LightColors = lightColorScheme(
    primary = Emerald,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFC2ECDD),
    onPrimaryContainer = Color(0xFF002118),
    inversePrimary = Mint,
    secondary = Color(0xFF4B635A),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E8DF),
    onSecondaryContainer = Color(0xFF0B1F18),
    tertiary = Color(0xFF805B12),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFBE4B8),
    onTertiaryContainer = Color(0xFF291800),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = Ivory,
    onBackground = Color(0xFF1B1C19),
    surface = Ivory,
    onSurface = Color(0xFF1B1C19),
    surfaceVariant = Color(0xFFE6E3DA),
    onSurfaceVariant = Color(0xFF4A4941),
    surfaceTint = Emerald,
    inverseSurface = Color(0xFF30312D),
    inverseOnSurface = Color(0xFFF3F0EA),
    outline = Color(0xFF79776E),
    outlineVariant = Color(0xFFDCD8CD),
    scrim = Color.Black,
    surfaceBright = Ivory,
    surfaceDim = Color(0xFFDBD8D1),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF4F2EC),
    surfaceContainer = Color(0xFFEFEDE6),
    surfaceContainerHigh = Color(0xFFE9E7E0),
    surfaceContainerHighest = Color(0xFFE3E1DA),
)

private val DarkColors = darkColorScheme(
    primary = Mint,
    onPrimary = Color(0xFF00382C),
    primaryContainer = Color(0xFF0B5143),
    onPrimaryContainer = Color(0xFFA3F4DA),
    inversePrimary = Emerald,
    secondary = Color(0xFFB4CCC2),
    onSecondary = Color(0xFF1F352D),
    secondaryContainer = Color(0xFF354B43),
    onSecondaryContainer = Color(0xFFD0E8DD),
    tertiary = GoldLight,
    onTertiary = Color(0xFF412D00),
    tertiaryContainer = Color(0xFF5D4206),
    onTertiaryContainer = Color(0xFFFFDFA6),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = Color(0xFF101412),
    onBackground = Color(0xFFE1E3DF),
    surface = Color(0xFF101412),
    onSurface = Color(0xFFE1E3DF),
    surfaceVariant = Color(0xFF3F4943),
    onSurfaceVariant = Color(0xFFBEC9C2),
    surfaceTint = Mint,
    inverseSurface = Color(0xFFE1E3DF),
    inverseOnSurface = Color(0xFF2D312F),
    outline = Color(0xFF89938D),
    outlineVariant = Color(0xFF3F4943),
    scrim = Color.Black,
    surfaceBright = Color(0xFF363A38),
    surfaceDim = Color(0xFF101412),
    surfaceContainerLowest = Color(0xFF0B0F0D),
    surfaceContainerLow = Color(0xFF181D1B),
    surfaceContainer = Color(0xFF1C211F),
    surfaceContainerHigh = Color(0xFF272B29),
    surfaceContainerHighest = Color(0xFF313634),
)

/** Soft, consistently rounded geometry shared by every surface of the app. */
val DhikraShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/**
 * App theme. System bar icon colors are handled by `enableEdgeToEdge` in the activity: the
 * branded header is always dark, so the status bar always uses light icons.
 */
@Composable
fun DhikraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = Typography,
        shapes = DhikraShapes,
    ) {
        CompositionLocalProvider(
            LocalDhikraPalette provides if (darkTheme) DarkPalette else LightPalette,
            // The whole app is Arabic: always lay it out right-to-left, whatever the device language.
            LocalLayoutDirection provides LayoutDirection.Rtl,
            content = content,
        )
    }
}
