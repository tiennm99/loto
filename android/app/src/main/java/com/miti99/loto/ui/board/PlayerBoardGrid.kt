package com.miti99.loto.ui.board

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.ui.theme.BrandAmberLight

private val SECTION_STARTS = listOf(0, 3, 6)
private val SECTION_LABEL_RES = listOf(
    R.string.section_loto,
    R.string.section_tn1_2014_2017,
    R.string.section_doc_dinh_dien,
)

/**
 * 3 stacked mini-cards (rows 0-2, 3-5, 6-8) with section labels above each.
 *
 * @param grid       9×9 number grid (non-null — caller gates on null)
 * @param crossed    9×9 crossed state
 * @param rowComplete 9-element precomputed completeness
 * @param emptyCellColor passed through to [PlayerCell]
 * @param onCellClick (row, col) callback
 */
@Composable
fun PlayerBoardGrid(
    grid: List<List<Int>>,
    crossed: List<List<Boolean>>,
    rowComplete: List<Boolean>,
    sectionHasWaiting: List<Boolean>,
    emptyCellColor: Color,
    onCellClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Single shared pulse drives every section's amber ring so they
    // animate in lockstep instead of N independent timers.
    val pulse = rememberInfiniteTransition(label = "cho-pulse")
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.55f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cho-alpha",
    )

    Column(modifier = modifier.fillMaxWidth()) {
        SECTION_STARTS.forEachIndexed { sectionIdx, startRow ->
            val isWaiting = sectionHasWaiting.getOrNull(sectionIdx) ?: false
            val labelColor =
                if (isWaiting) BrandAmberLight
                else MaterialTheme.colorScheme.onSurfaceVariant

            // Section label — amber ring + pulse when any of its 3 rows
            // is one cell from bingo.
            Box(
                modifier = Modifier
                    .padding(
                        start = 4.dp,
                        top = if (sectionIdx > 0) 12.dp else 4.dp,
                        bottom = 2.dp,
                    )
                    .then(
                        if (isWaiting) Modifier
                            .background(
                                color = BrandAmberLight.copy(alpha = pulseAlpha * 0.18f),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .border(
                                width = 1.dp,
                                color = BrandAmberLight.copy(alpha = pulseAlpha),
                                shape = RoundedCornerShape(8.dp),
                            )
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                        else Modifier
                    ),
            ) {
                Text(
                    text = stringResource(SECTION_LABEL_RES[sectionIdx]),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = labelColor,
                )
            }

            // 3-row sub-grid
            Column {
                for (rowOffset in 0..2) {
                    val row = startRow + rowOffset
                    if (row >= grid.size) break
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (col in 0..8) {
                            val num = grid.getOrNull(row)?.getOrNull(col) ?: 0
                            val isCrossed = crossed.getOrNull(row)?.getOrNull(col) ?: false
                            val complete = rowComplete.getOrNull(row) ?: false
                            PlayerCell(
                                num = num,
                                crossed = isCrossed,
                                rowComplete = complete,
                                emptyCellColor = emptyCellColor,
                                onClick = if (num > 0) { { onCellClick(row, col) } } else null,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            if (sectionIdx < SECTION_STARTS.lastIndex) {
                Spacer(modifier = Modifier.height(2.dp))
            }
        }
    }
}
