package com.miti99.loto.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LotoLightColorScheme = lightColorScheme(
    primary = BrandRoseLight,
    onPrimary = BackgroundLight,
    secondary = BrandAmberLight,
    onSecondary = BackgroundLight,
    tertiary = BrandEmeraldLight,
    onTertiary = BackgroundLight,
    background = BackgroundLight,
    onBackground = Color0F172A,
    surface = SurfaceLight,
    onSurface = Color0F172A,
    error = BrandRedLight,
)

private val LotoDarkColorScheme = darkColorScheme(
    primary = BrandRoseDark,
    onPrimary = BackgroundDark,
    secondary = BrandAmberDark,
    onSecondary = BackgroundDark,
    tertiary = BrandEmeraldDark,
    onTertiary = BackgroundDark,
    background = BackgroundDark,
    onBackground = ColorE2E8F0,
    surface = SurfaceDark,
    onSurface = ColorE2E8F0,
    error = BrandRedDark,
)

/**
 * Theme entry point.
 *
 * @param forcedDark when non-null overrides the system dark-mode preference.
 *   The Settings layer (phase 09) feeds the user's `auto|light|dark` choice
 *   into this — `null` means follow the system.
 */
@Composable
fun LotoTheme(
    forcedDark: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val darkTheme = forcedDark ?: isSystemInDarkTheme()
    val colorScheme = if (darkTheme) LotoDarkColorScheme else LotoLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = LotoTypography,
        content = content,
    )
}

// Local color tokens used only inside the theme file — kept private so they
// don't pollute the brand palette namespace exported from Color.kt.
private val Color0F172A = androidx.compose.ui.graphics.Color(0xFF0F172A)
private val ColorE2E8F0 = androidx.compose.ui.graphics.Color(0xFFE2E8F0)
