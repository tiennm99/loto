package com.miti99.loto.ui

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Android's analog of the web's `prefers-reduced-motion`: animations are
 * globally disabled when the animator duration scale is 0 (system
 * "Remove animations" accessibility setting). Gates decorative pulses,
 * confetti, and tap haptics — same events the web suppresses.
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
}
