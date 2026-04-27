package com.miti99.loto.ui.settings

import androidx.compose.ui.graphics.Color

/**
 * Excel "Standard Colors" palette — 10 swatches matching the web app's preset list
 * from SettingsButton.svelte. Default empty-cell color is the last entry (purple).
 */
internal val EXCEL_PRESETS = listOf(
    "#C00000", // dark red
    "#FF0000", // red
    "#FFC000", // orange
    "#FFFF00", // yellow
    "#92D050", // light green
    "#00B050", // green
    "#00B0F0", // light blue
    "#0070C0", // blue
    "#002060", // dark blue
    "#7030A0", // purple (default)
)

/**
 * Parse a "#RRGGBB" hex string to a Compose [Color].
 * The `#` prefix is optional. Returns [Color.Gray] as a safe fallback on parse error.
 */
internal fun parseHex(hex: String): Color {
    return try {
        val clean = hex.trimStart('#')
        require(clean.length == 6)
        val r = clean.substring(0, 2).toInt(16)
        val g = clean.substring(2, 4).toInt(16)
        val b = clean.substring(4, 6).toInt(16)
        Color(r, g, b)
    } catch (_: Exception) {
        Color.Gray
    }
}

/**
 * Convert a Compose [Color] to a "#RRGGBB" hex string.
 * Ignores the alpha channel.
 */
internal fun colorToHex(color: Color): String {
    val r = (color.red * 255).toInt().coerceIn(0, 255)
    val g = (color.green * 255).toInt().coerceIn(0, 255)
    val b = (color.blue * 255).toInt().coerceIn(0, 255)
    return "#%02X%02X%02X".format(r, g, b)
}
