package com.miti99.loto.ui

import androidx.compose.ui.graphics.Color
import androidx.core.graphics.toColorInt

/**
 * Parse a hex6 setting value ("#RRGGBB") into a Compose color. The settings
 * layer already validates the format; the fallback guards direct callers.
 *
 * L7: `android.graphics.Color.parseColor` is deprecated in favor of
 * `androidx.core`'s `String.toColorInt()`, which keeps the same
 * `IllegalArgumentException` contract for an unparsable string.
 */
fun String.toComposeColor(): Color = try {
    Color(this.toColorInt())
} catch (_: IllegalArgumentException) {
    Color(0xFF7030A0)
}
