package com.miti99.loto.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.ui.theme.CondensedNumberFont
import com.miti99.loto.ui.theme.LotoTheme

/** Cell aspect ratio — the web card uses 3:4 (taller than wide) on phones. */
private const val CELL_ASPECT = 3f / 4f

/**
 * One cell of the 9×9 player card. Number cells toggle a diagonal cross
 * (red while the row is in progress, sky blue once the row wins); the cell
 * holding a waiting row's awaited number pulses an amber inset ring.
 */
@Composable
fun PlayerCell(
    num: Int,
    isCrossed: Boolean,
    rowComplete: Boolean,
    isWaiting: Boolean,
    emptyCellColor: Color,
    textScale: Float,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LotoTheme.palette
    if (num <= 0) {
        Box(
            modifier = modifier
                .aspectRatio(CELL_ASPECT)
                .background(emptyCellColor)
                .background(palette.emptyCellDim)
                .border(1.dp, palette.cellBorder),
        )
        return
    }

    val background = when {
        isCrossed && rowComplete -> palette.cellWinBg
        isCrossed -> palette.cellCrossedBg
        else -> palette.cellBg
    }
    val textColor = when {
        isCrossed && rowComplete -> palette.cellWinText
        isCrossed -> palette.cellCrossedText
        else -> palette.cellText
    }
    val crossColor = if (rowComplete) palette.crossWinStroke else palette.crossStroke

    // M3/L7-adjacent cleanup: cell accessibility copy lives in strings.xml
    // rather than hardcoded Vietnamese, matching the toast fix.
    val cellNumberDesc = stringResource(R.string.player_cell_number, num)
    val crossedSuffixDesc = stringResource(R.string.player_cell_crossed_suffix)
    val waitingSuffixDesc = stringResource(R.string.player_cell_waiting_suffix)

    // Waiting pulse — 1.6s breathing ring, static at 0.7 alpha under
    // reduced motion (mirrors the web's cell-waiting keyframes).
    val ringAlpha: Float = if (isWaiting && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "cell-waiting")
        val alpha by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.95f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "cell-waiting-alpha",
        )
        alpha
    } else if (isWaiting) {
        0.7f
    } else {
        0f
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .aspectRatio(CELL_ASPECT)
            .background(background)
            .border(1.dp, palette.cellBorder)
            .drawBehind {
                if (isWaiting) {
                    drawRect(
                        color = palette.waitingRing.copy(alpha = ringAlpha),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = 3.dp.toPx() * 2, // inset stroke: half clips outside
                        ),
                    )
                }
                if (isCrossed) {
                    val inset = 6.dp.toPx()
                    drawLine(
                        color = crossColor,
                        start = Offset(inset, inset),
                        end = Offset(size.width - inset, size.height - inset),
                        strokeWidth = 3.dp.toPx() / 2,
                        cap = StrokeCap.Round,
                    )
                }
            }
            .clickable(onClick = onClick)
            .semantics {
                stateDescription = buildString {
                    append(cellNumberDesc)
                    if (isCrossed) append(crossedSuffixDesc)
                    if (isWaiting) append(waitingSuffixDesc)
                }
            },
    ) {
        Text(
            text = num.toString(),
            color = textColor,
            fontFamily = CondensedNumberFont,
            fontSize = (20 * textScale).sp,
            letterSpacing = (-0.04).em,
            textAlign = TextAlign.Center,
            maxLines = 1,
        )
    }
}
