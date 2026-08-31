package com.miti99.loto.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.miti99.loto.settings.ThemeSetting

/**
 * Palette lifted from the web app (`web/src/app.css` + the Tailwind classes
 * used in its components) so the native board reads identically. Light =
 * warm cream festive; dark = near-black navy.
 */
data class LotoPalette(
    val background: Color,
    val foreground: Color,
    /** Sky-blue brand accent for section bands and frames. */
    val sectionAccent: Color,
    val sectionBandBg: Color,
    /** Page title + primary action (web rose-600 / rose-400). */
    val title: Color,
    val subtitle: Color,
    val buttonPrimary: Color,
    val buttonSecondaryBg: Color,
    val buttonSecondaryText: Color,
    val buttonSecondaryBorder: Color,
    // Player card cells
    val cellBorder: Color,
    val cellBg: Color,
    val cellText: Color,
    val cellCrossedBg: Color,
    val cellCrossedText: Color,
    val cellWinBg: Color,
    val cellWinText: Color,
    val crossStroke: Color,
    val crossWinStroke: Color,
    val waitingRing: Color,
    val toastBg: Color,
    // Kinh dialog
    val dialogBg: Color,
    val dialogText: Color,
    val kinhTitle: Color,
    val kinhRow: Color,
    // Master panel
    val masterHeading: Color,
    val masterNewGame: Color,
    val masterDraw: Color,
    val masterStop: Color,
    val tokenBg: Color,
    val tokenBorder: Color,
    val tokenText: Color,
    val tokenUncalledBorder: Color,
    val tokenUncalledBg: Color,
    val tokenUncalledText: Color,
    val lastCalledRing: Color,
    val boardBg: Color,
    val boardCellBorder: Color,
    val countdownTrack: Color,
    val countdownArc: Color,
    /** Dim overlay drawn over empty cells in dark mode (web: black/15). */
    val emptyCellDim: Color,
)

private val Light = LotoPalette(
    background = Color(0xFFFFFBEB),
    foreground = Color(0xFF1E293B),
    sectionAccent = Color(0xFF1565C0),
    sectionBandBg = Color(0xC7E8F0FA),
    title = Color(0xFFE11D48),
    subtitle = Color(0xFF475569),
    buttonPrimary = Color(0xFFE11D48),
    buttonSecondaryBg = Color.White,
    buttonSecondaryText = Color(0xFF334155),
    buttonSecondaryBorder = Color(0xFFCBD5E1),
    cellBorder = Color(0x8094A3B8),
    cellBg = Color.White,
    cellText = Color.Black,
    cellCrossedBg = Color(0xFFFEF2F2),
    cellCrossedText = Color(0xFFB91C1C),
    cellWinBg = Color(0xFFE0F2FE),
    cellWinText = Color(0xFF0369A1),
    crossStroke = Color(0xFFEF4444),
    crossWinStroke = Color(0xFF0284C7),
    waitingRing = Color(0xFFF59E0B),
    toastBg = Color(0xBFF59E0B),
    dialogBg = Color.White,
    dialogText = Color(0xFF334155),
    kinhTitle = Color(0xFFD97706),
    kinhRow = Color(0xFFE11D48),
    masterHeading = Color(0xFFB45309),
    masterNewGame = Color(0xFFD97706),
    masterDraw = Color(0xFF0369A1),
    masterStop = Color(0xFFE11D48),
    tokenBg = Color(0xFFFFFBEB),
    tokenBorder = Color(0xFF0284C7),
    tokenText = Color(0xFF0369A1),
    tokenUncalledBorder = Color(0xFFCBD5E1),
    tokenUncalledBg = Color(0x66F8FAFC),
    tokenUncalledText = Color(0xFF94A3B8),
    lastCalledRing = Color(0xFFEF4444),
    boardBg = Color.White,
    boardCellBorder = Color(0xCCE2E8F0),
    countdownTrack = Color(0xFFE2E8F0),
    countdownArc = Color(0xFFF59E0B),
    emptyCellDim = Color.Transparent,
)

private val Dark = LotoPalette(
    background = Color(0xFF050813),
    foreground = Color(0xFFE2E8F0),
    sectionAccent = Color(0xFF64B5F6),
    sectionBandBg = Color(0x660D47A1),
    title = Color(0xFFFB7185),
    subtitle = Color(0xFFCBD5E1),
    buttonPrimary = Color(0xFFE11D48),
    buttonSecondaryBg = Color(0xFF1E293B),
    buttonSecondaryText = Color(0xFFE2E8F0),
    buttonSecondaryBorder = Color(0xFF475569),
    cellBorder = Color(0x66475569),
    cellBg = Color(0xFF1E293B),
    cellText = Color(0xFFF1F5F9),
    cellCrossedBg = Color(0x66450A0A),
    cellCrossedText = Color(0xFFFCA5A5),
    cellWinBg = Color(0x990C4A6E),
    cellWinText = Color(0xFFBAE6FD),
    crossStroke = Color(0xFFEF4444),
    crossWinStroke = Color(0xFF0284C7),
    waitingRing = Color(0xFFF59E0B),
    toastBg = Color(0xBFD97706),
    dialogBg = Color(0xFF1E293B),
    dialogText = Color(0xFFE2E8F0),
    kinhTitle = Color(0xFFFBBF24),
    kinhRow = Color(0xFFFB7185),
    masterHeading = Color(0xFFFBBF24),
    masterNewGame = Color(0xFFD97706),
    masterDraw = Color(0xFF0369A1),
    masterStop = Color(0xFFE11D48),
    tokenBg = Color(0xFFFEF3C7),
    tokenBorder = Color(0xFF38BDF8),
    tokenText = Color(0xFF0369A1),
    tokenUncalledBorder = Color(0xFF475569),
    tokenUncalledBg = Color(0x4D334155),
    tokenUncalledText = Color(0xFF64748B),
    lastCalledRing = Color(0xFFF87171),
    boardBg = Color(0xFF1E293B),
    boardCellBorder = Color(0x99334155),
    countdownTrack = Color(0xFF334155),
    countdownArc = Color(0xFFFBBF24),
    emptyCellDim = Color(0x26000000),
)

val LocalLotoPalette = staticCompositionLocalOf { Light }

/**
 * Condensed heavy digits — the Tân Tân card look (web `.tan-tan-num`, which
 * self-hosts Roboto Condensed Bold; Android ships it as the system
 * sans-serif-condensed family).
 */
val CondensedNumberFont = FontFamily(
    Font(DeviceFontFamilyName("sans-serif-condensed"), weight = FontWeight.Bold),
)

object LotoTheme {
    val palette: LotoPalette
        @Composable @ReadOnlyComposable get() = LocalLotoPalette.current
}

@Composable
fun LotoTheme(
    theme: ThemeSetting,
    content: @Composable () -> Unit,
) {
    val dark = when (theme) {
        ThemeSetting.AUTO -> isSystemInDarkTheme()
        ThemeSetting.LIGHT -> false
        ThemeSetting.DARK -> true
    }
    val palette = if (dark) Dark else Light
    val colorScheme = if (dark) {
        darkColorScheme(
            primary = palette.buttonPrimary,
            background = palette.background,
            surface = palette.dialogBg,
            onBackground = palette.foreground,
            onSurface = palette.foreground,
        )
    } else {
        lightColorScheme(
            primary = palette.buttonPrimary,
            background = palette.background,
            surface = palette.dialogBg,
            onBackground = palette.foreground,
            onSurface = palette.foreground,
        )
    }
    CompositionLocalProvider(LocalLotoPalette provides palette) {
        MaterialTheme(colorScheme = colorScheme, content = content)
    }
}

/** Weight used by all board digits. */
val BoardDigitWeight = FontWeight.Bold
