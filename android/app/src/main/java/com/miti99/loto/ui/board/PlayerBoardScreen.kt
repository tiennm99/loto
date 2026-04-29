package com.miti99.loto.ui.board

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.miti99.loto.R
import com.miti99.loto.state.PlayerBoardUiState
import com.miti99.loto.ui.theme.BrandRose600

/**
 * Top-level stateless PlayerBoard composable.
 *
 * Hosts the 3-section 9×9 grid, cold-start ghost preview, Chờ toast,
 * Kinh modal, confetti, and confirm dialogs for generate/clear.
 *
 * @param emptyCellColor passed from settings; wired in [LotoAppRoot] via
 *   [com.miti99.loto.ui.common.LocalEmptyCellColor].
 */
@Composable
fun PlayerBoardScreen(
    state: PlayerBoardUiState,
    emptyCellColor: Color,
    onGenerate: () -> Unit,
    onClear: () -> Unit,
    onCellClick: (row: Int, col: Int) -> Unit,
    onDismissBingo: () -> Unit,
    onDismissToast: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showGenerateDialog by remember { mutableStateOf(false) }
    var showClearDialog by remember { mutableStateOf(false) }

    // Confirm dialogs
    if (showGenerateDialog) {
        AlertDialog(
            onDismissRequest = { showGenerateDialog = false },
            title = { Text(stringResource(R.string.prompt_create_new)) },
            confirmButton = {
                TextButton(onClick = {
                    showGenerateDialog = false
                    onGenerate()
                }) { Text(stringResource(R.string.prompt_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showGenerateDialog = false }) {
                    Text(stringResource(R.string.prompt_no))
                }
            },
        )
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.prompt_clear_marks)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    onClear()
                }) { Text(stringResource(R.string.prompt_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.prompt_no))
                }
            },
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Bảng lô tô" },
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                // "Tạo bảng mới" — skip dialog if no grid yet
                Button(
                    onClick = {
                        if (state.grid == null) onGenerate()
                        else showGenerateDialog = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandRose600),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(stringResource(R.string.btn_generate_new))
                }

                // "Xoá đánh dấu" — only shown when a grid exists
                if (state.grid != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            val hasCrossed = state.crossed.any { row -> row.any { it } }
                            if (hasCrossed) showClearDialog = true else onClear()
                        },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = BrandRose600,
                        ),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(stringResource(R.string.btn_clear_marks))
                    }
                }
            }

            // Board or ghost preview
            if (state.grid == null) {
                GhostPreview(modifier = Modifier.padding(top = 8.dp))
            } else {
                PlayerBoardGrid(
                    grid = state.grid,
                    crossed = state.crossed,
                    rowComplete = state.rowComplete,
                    sectionHasWaiting = state.sectionHasWaiting,
                    emptyCellColor = emptyCellColor,
                    onCellClick = onCellClick,
                    modifier = Modifier.padding(horizontal = 4.dp),
                )
            }
        }

        // Toast overlay — centered over the board
        ChoToast(
            toast = state.waitingToast,
            onDismiss = onDismissToast,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(top = 56.dp), // clear the button row
        )

        // Confetti for tier-2 bingo. Tier rule lives in PlayerBoardViewModel.
        if (state.bingoEvent?.tier == 2) {
            Confetti(
                visible = true,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    // Kinh modal — rendered outside the Box so it is not clipped
    if (state.bingoEvent != null) {
        KinhModal(
            bingoEvent = state.bingoEvent,
            onDismiss = onDismissBingo,
        )
    }
}
