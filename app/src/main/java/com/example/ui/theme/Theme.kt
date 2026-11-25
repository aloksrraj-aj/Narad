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

private val NaradLightColorScheme = lightColorScheme(
    primary = SaffronPrimary,
    onPrimary = Color.White,
    primaryContainer = SaffronContainerLight,
    onPrimaryContainer = Color(0xFF3E1900),
    secondary = AshokaIndigo,
    onSecondary = Color.White,
    secondaryContainer = IndigoContainerLight,
    onSecondaryContainer = Color(0xFF071B3D),
    tertiary = SovereignEmerald,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainerLight,
    onTertiaryContainer = Color(0xFF032618),
    background = IvoryBackground,
    onBackground = CharcoalInk,
    surface = IvorySurface,
    onSurface = CharcoalInk,
    surfaceVariant = IvorySurfaceVariant,
    onSurfaceVariant = MutedInk,
    outline = Color(0xFFD6CFC3),
    error = CrimsonAlert,
    onError = Color.White
)

private val NaradDarkColorScheme = darkColorScheme(
    primary = SaffronBright,
    onPrimary = Color(0xFF2D1200),
    primaryContainer = SaffronContainerDark,
    onPrimaryContainer = Color(0xFFFFDCBE),
    secondary = AshokaIndigoLight,
    onSecondary = Color(0xFF061B42),
    secondaryContainer = IndigoContainerDark,
    onSecondaryContainer = Color(0xFFD9E6FF),
    tertiary = SovereignEmeraldBright,
    onTertiary = Color(0xFF002415),
    tertiaryContainer = EmeraldContainerDark,
    onTertiaryContainer = Color(0xFFB5F9DA),
    background = ObsidianBackground,
    onBackground = ChalkInk,
    surface = ObsidianSurface,
    onSurface = ChalkInk,
    surfaceVariant = ObsidianSurfaceVariant,
    onSurfaceVariant = SlateMutedInk,
    outline = Color(0xFF28354C),
    error = CrimsonAlertBright,
    onError = Color(0xFF3A0000)
)

val NaradShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun NaradTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) NaradDarkColorScheme else NaradLightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = NaradTypography,
        shapes = NaradShapes,
        content = content
    )
}
