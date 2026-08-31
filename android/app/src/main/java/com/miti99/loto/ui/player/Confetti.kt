package com.miti99.loto.ui.player

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Falling-emoji burst for tier-2 celebrations, ported from the web's
 * `.confetti` keyframes: 12 particles, staggered delays, full-height fall
 * with rotation over 1.6s. One shared progress clock drives all particles —
 * no per-particle animators. Hội-chợ flavour set (lantern, bamboo,
 * chopsticks) so it reads as a Vietnamese fair, not a generic party.
 */
private val CONFETTI_EMOJI = listOf("🎊", "✨", "🎉", "🥳", "🥢", "🎋", "🏮")
private const val PARTICLES = 12
private const val FALL_MS = 1600
private const val MAX_DELAY_MS = 400

@Composable
fun ConfettiOverlay(modifier: Modifier = Modifier) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        clock.animateTo(
            targetValue = 1f,
            animationSpec = tween(FALL_MS + MAX_DELAY_MS, easing = LinearEasing),
        )
    }
    Box(modifier = modifier.fillMaxSize().onSizeChanged { size = it }) {
        val elapsed = clock.value * (FALL_MS + MAX_DELAY_MS)
        for (i in 0 until PARTICLES) {
            // Same deterministic spread the web uses.
            val xFraction = ((i * 8.3f + (i % 3) * 11f) % 100f) / 100f
            val delay = (i * 37) % MAX_DELAY_MS
            val rotation = ((i * 67) % 360).toFloat()
            val sizeSp = 24 + ((i * 13) % 11) * 1.6f
            val t = ((elapsed - delay) / FALL_MS).coerceIn(0f, 1f)
            if (t <= 0f) continue
            Text(
                text = CONFETTI_EMOJI[i % CONFETTI_EMOJI.size],
                fontSize = sizeSp.sp,
                modifier = Modifier.graphicsLayer {
                    translationX = xFraction * size.width
                    translationY = (-0.1f + 1.2f * t) * size.height
                    rotationZ = rotation * t
                    alpha = when {
                        t < 0.1f -> t / 0.1f
                        else -> 0.85f
                    }
                },
            )
        }
    }
}
