package com.kira.kdownloader.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette derived from the app icon: a vivid red "K"/download arrow on near-black.
// Primary + secondary are both reds (secondary a deeper terracotta) so the accent reads red
// throughout, with emerald reserved for success and a distinct orange-red for errors.
private val Red200 = Color(0xFFFFB4AB)
private val Red300 = Color(0xFFFF8A80)
private val Red400 = Color(0xFFF76B62)
private val Red500 = Color(0xFFE53935) // icon red
private val Red600 = Color(0xFFD32F2F)
private val Red900 = Color(0xFF5F1412)
private val RedContainerLight = Color(0xFFFFDAD6)

// Secondary: deeper terracotta red for contrast against the bright primary.
private val Terracotta300 = Color(0xFFEBA79C)
private val Terracotta600 = Color(0xFFB4402E)
private val Terracotta900 = Color(0xFF5C1B12)
private val TerracottaContainerLight = Color(0xFFFFE3DC)

private val Emerald400 = Color(0xFF34D399)
private val Emerald600 = Color(0xFF059669)
private val Emerald900 = Color(0xFF065F46)
private val Emerald950 = Color(0xFF022C22)
private val EmeraldContainerLight = Color(0xFFD1FAE5)

// Error kept distinct from brand red with a warmer orange-red.
private val ErrorRed400 = Color(0xFFFF8A65)
private val ErrorRed600 = Color(0xFFE64A19)
private val ErrorRed900 = Color(0xFF7A2410)
private val ErrorContainerLight = Color(0xFFFFE0D2)

// Neutral (zinc) surfaces to match the icon's near-black background — no blue tint.
private val Neutral50 = Color(0xFFFAFAFA)
private val Neutral100 = Color(0xFFF5F5F5)
private val Neutral200 = Color(0xFFE5E5E5)
private val Neutral300 = Color(0xFFD4D4D4)
private val Neutral400 = Color(0xFFA3A3A3)
private val Neutral600 = Color(0xFF525252)
private val Neutral800 = Color(0xFF262626)
private val Neutral900 = Color(0xFF171717)
private val Neutral950 = Color(0xFF0A0A0A)

val DarkColorScheme = darkColorScheme(
    primary = Red300,
    onPrimary = Red900,
    primaryContainer = Red600,
    onPrimaryContainer = RedContainerLight,
    inversePrimary = Red600,
    secondary = Terracotta300,
    onSecondary = Terracotta900,
    secondaryContainer = Terracotta600,
    onSecondaryContainer = TerracottaContainerLight,
    tertiary = Emerald400,
    onTertiary = Emerald950,
    tertiaryContainer = Emerald900,
    onTertiaryContainer = EmeraldContainerLight,
    error = ErrorRed400,
    onError = ErrorRed900,
    errorContainer = ErrorRed600,
    onErrorContainer = ErrorContainerLight,
    background = Neutral950,
    onBackground = Neutral200,
    surface = Neutral900,
    onSurface = Neutral100,
    surfaceVariant = Neutral800,
    onSurfaceVariant = Neutral400,
    surfaceContainerLowest = Neutral950,
    surfaceContainerLow = Color(0xFF141414),
    surfaceContainer = Color(0xFF1C1C1C),
    surfaceContainerHigh = Color(0xFF232323),
    surfaceContainerHighest = Color(0xFF2C2C2C),
    outline = Color(0xFF4D4D4D),
    outlineVariant = Neutral800,
    inverseSurface = Neutral200,
    inverseOnSurface = Neutral900,
    scrim = Color(0xFF000000),
)

val LightColorScheme = lightColorScheme(
    primary = Red600,
    onPrimary = Color.White,
    primaryContainer = RedContainerLight,
    onPrimaryContainer = Red900,
    inversePrimary = Red300,
    secondary = Terracotta600,
    onSecondary = Color.White,
    secondaryContainer = TerracottaContainerLight,
    onSecondaryContainer = Terracotta900,
    tertiary = Emerald600,
    onTertiary = Color.White,
    tertiaryContainer = EmeraldContainerLight,
    onTertiaryContainer = Emerald900,
    error = ErrorRed600,
    onError = Color.White,
    errorContainer = ErrorContainerLight,
    onErrorContainer = ErrorRed900,
    background = Neutral50,
    onBackground = Neutral900,
    surface = Color.White,
    onSurface = Neutral900,
    surfaceVariant = Neutral100,
    onSurfaceVariant = Neutral600,
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Neutral50,
    surfaceContainer = Neutral100,
    surfaceContainerHigh = Color(0xFFECECEC),
    surfaceContainerHighest = Neutral200,
    outline = Neutral300,
    outlineVariant = Neutral200,
    inverseSurface = Neutral900,
    inverseOnSurface = Neutral100,
    scrim = Color(0xFF000000),
)
