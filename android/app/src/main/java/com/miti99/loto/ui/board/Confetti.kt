package com.miti99.loto.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val CONFETTI_EMOJI = listOf("🎊", "✨", "🎉", "🥳")
private const val PARTICLE_COUNT = 12
private const val DURATION_NS = 3_000_000_000L  // 3 seconds

private data class Particle(
    val emoji: String,
    val startXFraction: Float,   // 0..1 fraction of screen width
    val speed: Float,             // px per second (screen height fraction)
    val rotationSpeed: Float,     // degrees per second
    val delayNs: Long,            // stagger start
)

/**
 * Canvas-free confetti rain. 12 emoji particles fall from the top using
 * a single `withFrameNanos` loop — one recompose per frame, no allocations
 * inside the loop.
 *
 * Visible only when `visible == true`. Particles auto-stop after 3 s.
 */
@Composable
fun Confetti(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (!visible) return

    val screenHeightPx = LocalConfiguration.current.screenHeightDp * 3f  // approx density-independent to px ratio
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.toFloat()

    val particles = remember {
        List(PARTICLE_COUNT) { i ->
            Particle(
                emoji = CONFETTI_EMOJI[i % CONFETTI_EMOJI.size],
                startXFraction = (i.toFloat() / PARTICLE_COUNT) + (((i * 17) % 10) / 100f),
                speed = screenHeightPx * (0.25f + (i % 5) * 0.05f),
                rotationSpeed = 60f + (i % 7) * 30f,
                delayNs = (i * 150_000_000L),  // 150 ms stagger
            )
        }
    }

    var elapsedNs by remember { mutableLongStateOf(0L) }
    var running by remember { mutableStateOf(true) }

    LaunchedEffect(visible) {
        elapsedNs = 0L
        running = true
        var lastFrameNs = 0L
        while (running && elapsedNs < DURATION_NS) {
            withFrameNanos { frameNs ->
                if (lastFrameNs == 0L) lastFrameNs = frameNs
                elapsedNs += (frameNs - lastFrameNs).coerceAtMost(32_000_000L) // cap at 32ms
                lastFrameNs = frameNs
            }
        }
        running = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        particles.forEach { p ->
            val adjustedElapsed = (elapsedNs - p.delayNs).coerceAtLeast(0L)
            val progress = adjustedElapsed / 1_000_000_000f  // seconds
            val yDp = (p.speed * progress / 3f)  // approx dp
            val xDp = p.startXFraction * screenWidthDp
            val rotation = p.rotationSpeed * progress

            Text(
                text = p.emoji,
                fontSize = 20.sp,
                modifier = Modifier
                    .offset(x = xDp.dp, y = yDp.dp)
                    .graphicsLayer { rotationZ = rotation % 360f },
            )
        }
    }
}
