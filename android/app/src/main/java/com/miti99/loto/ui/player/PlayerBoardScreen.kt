package com.miti99.loto.ui.player

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.settings.Settings
import com.miti99.loto.state.PlayerBoardViewModel
import com.miti99.loto.ui.rememberReducedMotion
import com.miti99.loto.ui.toComposeColor
import com.miti99.loto.ui.theme.LotoTheme

/** The player-facing screen: action buttons, the card (or empty state), the chờ toast, and the Kinh celebration. */
@Composable
fun PlayerBoardScreen(
    viewModel: PlayerBoardViewModel,
    settings: Settings,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val palette = LotoTheme.palette
    val view = LocalView.current
    val reducedMotion = rememberReducedMotion()
    var confirmGenerate by remember { mutableStateOf(false) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Action buttons
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
        ) {
            Button(
                onClick = {
                    if (state.grid != null) confirmGenerate = true else viewModel.generate()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = palette.buttonPrimary,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = stringResource(R.string.player_new_card),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }
            if (state.grid != null) {
                Button(
                    onClick = {
                        if (state.crossed.flatten().any { it }) confirmClear = true
                        else viewModel.clearMarks()
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.buttonSecondaryBg,
                        contentColor = palette.buttonSecondaryText,
                    ),
                    border = androidx.compose.foundation.BorderStroke(2.dp, palette.buttonSecondaryBorder),
                ) {
                    Text(
                        text = stringResource(R.string.player_clear_marks),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }

        val grid = state.grid
        if (grid != null) {
            Box {
                PlayerCardGrid(
                    grid = grid,
                    crossed = state.crossed,
                    rowComplete = state.rowComplete,
                    waitingCells = state.waitingCells,
                    sectionWaiting = state.sectionWaiting,
                    emptyCellColor = settings.emptyCellColor.toComposeColor(),
                    textScale = settings.boardTextScale,
                    reducedMotion = reducedMotion,
                    onCellClick = { row, col ->
                        if (!reducedMotion) {
                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                        }
                        viewModel.onCellClick(row, col)
                    },
                )
                // Chờ toast — centered overlay chip, tap to dismiss.
                state.toast?.let { message ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.matchParentSize(),
                    ) {
                        Text(
                            text = message,
                            color = Color.White,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(palette.toastBg)
                                .clickable { viewModel.dismissToast() }
                                .padding(horizontal = 24.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        } else {
            PlayerEmptyState()
        }
    }

    if (state.showCongrats) {
        KinhDialog(congratsRow = state.congratsRow, onDismiss = { viewModel.dismissCongrats() })
        if (state.celebrationTier >= 2 && !reducedMotion) {
            ConfettiOverlay()
        }
    }

    if (confirmGenerate) {
        ConfirmDialog(
            message = stringResource(R.string.player_confirm_new_card),
            onConfirm = {
                confirmGenerate = false
                viewModel.generate()
            },
            onDismiss = { confirmGenerate = false },
        )
    }
    if (confirmClear) {
        ConfirmDialog(
            message = stringResource(R.string.player_confirm_clear_marks),
            onConfirm = {
                confirmClear = false
                viewModel.clearMarks()
            },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
fun ConfirmDialog(message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(stringResource(R.string.dialog_confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.dialog_cancel)) }
        },
    )
}

/** No-card-yet hint with a faded preview mini-grid, mirroring the web copy. */
@Composable
private fun PlayerEmptyState() {
    val palette = LotoTheme.palette
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 320.dp)
                .alpha(0.3f)
                .clip(RoundedCornerShape(6.dp))
                .border(1.dp, palette.buttonSecondaryBorder, RoundedCornerShape(6.dp))
                .padding(bottom = 24.dp),
        ) {
            for (rowStart in 0 until 27 step 9) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (i in rowStart until rowStart + 9) {
                        val filled = i % 3 == 0
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .background(
                                    if (filled) palette.cellBg
                                    else Settings.DEFAULT_EMPTY_CELL_COLOR.toComposeColor(),
                                ),
                        ) {
                            if (filled) {
                                Text(
                                    text = "${(i * 7) % 90 + 1}",
                                    fontSize = 9.sp,
                                    color = palette.cellText,
                                )
                            }
                        }
                    }
                }
            }
        }
        Text(
            text = buildAnnotatedString {
                append(stringResource(R.string.player_empty_hint_prefix))
                append(" ")
                withStyle(
                    SpanStyle(
                        color = palette.title,
                        fontWeight = FontWeight.SemiBold,
                        fontStyle = FontStyle.Normal,
                    ),
                ) {
                    append("\"" + stringResource(R.string.player_new_card) + "\"")
                }
                append(" ")
                append(stringResource(R.string.player_empty_hint_suffix))
            },
            color = palette.subtitle,
            fontSize = 16.sp,
            fontStyle = FontStyle.Italic,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.player_empty_wish),
            color = palette.subtitle,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
