package com.miti99.loto.ui

import androidx.compose.ui.graphics.Color

/**
 * Parse a hex6 setting value ("#RRGGBB") into a Compose color. The settings
 * layer already validates the format; the fallback guards direct callers.
 */
fun String.toComposeColor(): Color = try {
    Color(android.graphics.Color.parseColor(this))
} catch (_: IllegalArgumentException) {
    Color(0xFF7030A0)
}
