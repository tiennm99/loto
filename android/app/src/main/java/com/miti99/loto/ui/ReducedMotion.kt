package com.miti99.loto.ui

import android.content.ContentResolver
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/**
 * Android's analog of the web's `prefers-reduced-motion`: animations are
 * globally disabled when the animator duration scale is 0 (system
 * "Remove animations" accessibility setting). Gates decorative pulses,
 * confetti, and tap haptics — same events the web suppresses.
 *
 * Observes the setting while in composition, so toggling it mid-session
 * takes effect without a restart (the web's media query is live too).
 */
@Composable
fun rememberReducedMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    var reduced by remember(resolver) { mutableStateOf(readReducedMotion(resolver)) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                reduced = readReducedMotion(resolver)
            }
        }
        resolver.registerContentObserver(
            Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
            false,
            observer,
        )
        // Catch a toggle that landed between the first read and registering.
        reduced = readReducedMotion(resolver)
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return reduced
}

private fun readReducedMotion(resolver: ContentResolver): Boolean =
    Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
