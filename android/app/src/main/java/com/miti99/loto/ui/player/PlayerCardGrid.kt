package com.miti99.loto.ui.player

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.game.PlayerCard
import com.miti99.loto.ui.theme.LotoTheme

/*
 * Visual parity notes (from web PlayerBoard.svelte + app.css):
 * - The sheet is 3 stacked 3×9 sections (Tân Tân physical card), each headed
 *   by an italic uppercase label band in the sky-blue section accent over a
 *   pale blue band, flanked by 2px rules; a bare band closes the sheet.
 * - 4px vertical accent bars frame the sheet left and right.
 * - Cells: white bg, slate half-alpha 1px borders, condensed bold digits;
 *   crossed = pale red bg + red diagonal slash; winning row = pale sky bg +
 *   sky slash; empty cells take the user's emptyCellColor (dimmed 15% in
 *   dark mode).
 * - A waiting section's label band pulses an inset amber ring (2.4s); the
 *   awaited cell pulses a 3px amber ring (1.6s).
 */

private val sectionLabelIds =
    listOf(R.string.section_label_1, R.string.section_label_2, R.string.section_label_3)

@Composable
fun PlayerCardGrid(
    grid: List<List<Int>>,
    crossed: List<List<Boolean>>,
    rowComplete: List<Boolean>,
    waitingCells: Set<PlayerCard.Cell>,
    sectionWaiting: List<Boolean>,
    emptyCellColor: Color,
    textScale: Float,
    reducedMotion: Boolean,
    onCellClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LotoTheme.palette
    // IntrinsicSize.Min lets the framing accent bars match the sheet height.
    Row(modifier = modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        VerticalAccentBar()
        Column(modifier = Modifier.weight(1f)) {
            for ((sectionIdx, startRow) in listOf(0, 3, 6).withIndex()) {
                SectionLabel(
                    text = androidx.compose.ui.res.stringResource(sectionLabelIds[sectionIdx]),
                    waiting = sectionWaiting.getOrNull(sectionIdx) == true,
                    reducedMotion = reducedMotion,
                )
                for (row in startRow until startRow + 3) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0 until 9) {
                            PlayerCell(
                                num = grid[row][col],
                                isCrossed = crossed.getOrNull(row)?.getOrNull(col) == true,
                                rowComplete = rowComplete.getOrNull(row) == true,
                                isWaiting = waitingCells.contains(PlayerCard.Cell(row, col)),
                                emptyCellColor = emptyCellColor,
                                textScale = textScale,
                                reducedMotion = reducedMotion,
                                onClick = { onCellClick(row, col) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
            // Decorative bottom band matching the label bands.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(16.dp)
                    .background(palette.sectionBandBg),
            )
        }
        VerticalAccentBar()
    }
}

@Composable
private fun VerticalAccentBar() {
    Box(
        modifier = Modifier
            .width(4.dp)
            .fillMaxHeight()
            .background(LotoTheme.palette.sectionAccent),
    )
}

@Composable
private fun SectionLabel(text: String, waiting: Boolean, reducedMotion: Boolean) {
    val palette = LotoTheme.palette
    // Waiting ring: inset amber ring breathing at 2.4s; static when reduced.
    val ringAlpha: Float = if (waiting && !reducedMotion) {
        val transition = rememberInfiniteTransition(label = "section-waiting")
        val alpha by transition.animateFloat(
            initialValue = 0.45f,
            targetValue = 0.85f,
            animationSpec = infiniteRepeatable(
                animation = tween(1200, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "section-waiting-alpha",
        )
        alpha
    } else if (waiting) {
        0.6f
    } else {
        0f
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.sectionBandBg)
            .then(
                if (waiting) {
                    Modifier.border(2.dp, palette.waitingRing.copy(alpha = ringAlpha))
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
    ) {
        LabelRule(Modifier.weight(1f))
        Text(
            text = text.uppercase(),
            color = palette.sectionAccent,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            fontStyle = FontStyle.Italic,
            letterSpacing = 0.14.em,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
        LabelRule(Modifier.weight(1f))
    }
}

@Composable
private fun LabelRule(modifier: Modifier) {
    Box(
        modifier = modifier
            .height(2.dp)
            .background(LotoTheme.palette.sectionAccent.copy(alpha = 0.7f)),
    )
}
