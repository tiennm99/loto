package com.miti99.loto.ui.board

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miti99.loto.R

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
    emptyCellColor: Color,
    onCellClick: (row: Int, col: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SECTION_STARTS.forEachIndexed { sectionIdx, startRow ->
            // Section label
            Text(
                text = stringResource(SECTION_LABEL_RES[sectionIdx]),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = if (sectionIdx > 0) 12.dp else 4.dp, bottom = 2.dp),
            )

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
