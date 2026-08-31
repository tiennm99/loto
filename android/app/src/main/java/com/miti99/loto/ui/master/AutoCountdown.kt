package com.miti99.loto.ui.master

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.ui.theme.LotoTheme
import kotlin.math.ceil

/**
 * Visual countdown ring for auto-call, ported from `AutoCountdown.svelte`.
 * Render-only: the ViewModel owns the draw timer; this animation is a
 * presentation clock re-baselined on every [tickKey] bump, `running` rising
 * edge, or [durationSeconds] change — never a second draw timer.
 */
@Composable
fun AutoCountdown(
    running: Boolean,
    durationSeconds: Int,
    tickKey: Int,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LotoTheme.palette
    // Wall-clock frame loop rather than an animation: Compose animations
    // scale with the system animator duration setting, which would complete
    // the tween instantly (and freeze the number) when animations are off.
    var progress by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(running, durationSeconds, tickKey) {
        progress = 0f
        if (!running) return@LaunchedEffect
        val startNanos = withFrameNanos { it }
        val totalNanos = durationSeconds * 1_000_000_000L
        while (progress < 1f) {
            withFrameNanos { now ->
                progress = ((now - startNanos).toFloat() / totalNanos).coerceIn(0f, 1f)
            }
        }
    }
    // Clamp to [1, duration] while running — avoids flashing 0 between the
    // timer edge and the tickKey bump (same clamp as the web).
    val secondsRemaining = if (running) {
        ceil(durationSeconds * (1f - progress)).toInt().coerceIn(1, durationSeconds)
    } else {
        durationSeconds
    }
    val label = stringResource(R.string.master_countdown, secondsRemaining)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(88.dp)
            .semantics { contentDescription = label },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 8.dp.toPx()
            val inset = stroke / 2
            drawArc(
                color = palette.countdownTrack,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(stroke),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
            )
            // Reduced motion keeps a static full ring; the number still
            // ticks because the frame loop above is wall-clock based.
            val sweep = if (reducedMotion) 360f else 360f * (1f - progress)
            drawArc(
                color = palette.countdownArc,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(stroke, cap = StrokeCap.Round),
                topLeft = androidx.compose.ui.geometry.Offset(inset, inset),
                size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
            )
        }
        Text(
            text = secondsRemaining.toString(),
            color = palette.foreground,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
        )
    }
}
