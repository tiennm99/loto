package com.miti99.loto.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import com.miti99.loto.settings.AppMode

/**
 * Whether the screen must stay awake: a master round is still being drawn
 * (auto-call advances with no touch input, so the display would otherwise
 * sleep mid-round). Keyed on remaining — a finished board releases the
 * screen — and never held in player-only mode, where the player's own taps
 * keep the screen alive (web `wake-lock` parity).
 */
fun shouldKeepScreenOn(mode: AppMode, remainingCount: Int): Boolean =
    mode != AppMode.PLAYER && remainingCount > 0

/** Holds the window's keep-screen-on flag while [enabled]; releases on leave. */
@Composable
fun KeepScreenOn(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled) {
        view.keepScreenOn = enabled
        onDispose { view.keepScreenOn = false }
    }
}
