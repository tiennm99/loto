package com.miti99.loto.ui.master

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.settings.Settings
import com.miti99.loto.state.MasterPanelViewModel
import com.miti99.loto.ui.player.ConfirmDialog
import com.miti99.loto.ui.rememberReducedMotion
import com.miti99.loto.ui.theme.CondensedNumberFont
import com.miti99.loto.ui.theme.LotoTheme
import com.miti99.loto.ui.toComposeColor

/**
 * The host (quản trò) surface, ported from `MasterPanel.svelte`: controls,
 * countdown, current-number hero, called history in draw order, and the
 * 11×9 ones-digit tracking board (or the ghost empty state).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MasterPanelScreen(
    viewModel: MasterPanelViewModel,
    settings: Settings,
    modifier: Modifier = Modifier,
) {
    val palette = LotoTheme.palette
    val master by viewModel.masterState.collectAsState()
    val loading by viewModel.loading.collectAsState()
    val autoRunning by viewModel.autoRunning.collectAsState()
    val tickKey by viewModel.tickKey.collectAsState()
    val reducedMotion = rememberReducedMotion()
    var confirmNewGame by remember { mutableStateOf(false) }

    val hasGame = master.called.isNotEmpty() || master.remaining.isNotEmpty()
    val lastCalled = master.called.lastOrNull()
    val heroRequester = remember { BringIntoViewRequester() }

    // Keep the hero visible on each draw, including in `both` mode: web
    // parity confirmed against MasterPanel.svelte's `handleDrawNext`, which
    // sets `scrollOnNextDraw = true` unconditionally for both the manual
    // draw button and the auto-call interval, regardless of `settings.mode`.
    LaunchedEffect(tickKey) {
        if (tickKey > 0 && lastCalled != null) heroRequester.bringIntoView()
    }

    Column(modifier = modifier.fillMaxWidth()) {
        // Controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        ) {
            PillButton(
                text = stringResource(R.string.master_new_game),
                container = palette.masterNewGame,
                // Gated on loading (H1): while the startup restore is still
                // resolving, `hasGame` reads false regardless of what is
                // actually persisted, so an early tap would call newGame()
                // with no confirmation and race the restore.
                enabled = !loading,
                onClick = { if (hasGame) confirmNewGame = true else viewModel.newGame() },
            )
            if (hasGame && master.remaining.isNotEmpty()) {
                if (settings.autoCallEnabled) {
                    PillButton(
                        text = stringResource(
                            if (autoRunning) R.string.master_auto_stop
                            else R.string.master_auto_start,
                        ),
                        container = if (autoRunning) palette.masterStop else palette.masterDraw,
                        onClick = { viewModel.toggleAuto() },
                    )
                } else {
                    PillButton(
                        text = stringResource(R.string.master_draw),
                        container = palette.masterDraw,
                        onClick = { viewModel.drawNext() },
                    )
                }
            }
        }

        if (settings.autoCallEnabled && hasGame && master.remaining.isNotEmpty()) {
            Text(
                text = stringResource(R.string.master_auto_speed, settings.autoCallSpeed),
                color = palette.subtitle,
                fontSize = 14.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }

        if (autoRunning && master.remaining.isNotEmpty()) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            ) {
                AutoCountdown(
                    running = autoRunning,
                    durationSeconds = settings.autoCallSpeed,
                    tickKey = tickKey,
                    reducedMotion = reducedMotion,
                )
            }
        }

        // Current-number hero
        if (lastCalled != null) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
                    .bringIntoViewRequester(heroRequester),
            ) {
                Text(
                    text = stringResource(R.string.master_last_called).uppercase(),
                    color = palette.subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.2.em,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(144.dp)
                        .clip(CircleShape)
                        .background(palette.tokenBg)
                        .border(7.dp, palette.tokenBorder, CircleShape),
                ) {
                    Text(
                        text = lastCalled.toString(),
                        color = palette.tokenText,
                        fontFamily = CondensedNumberFont,
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                    )
                }
                Text(
                    text = stringResource(
                        R.string.master_called_count,
                        master.called.size,
                        master.remaining.size,
                    ),
                    color = palette.subtitle,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
        }

        // Called history (draw order)
        if (master.called.isNotEmpty()) {
            Text(
                text = stringResource(R.string.master_call_order),
                color = palette.subtitle,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(bottom = 6.dp),
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            ) {
                for (num in master.called) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(palette.tokenBg)
                            .border(2.dp, palette.tokenBorder, CircleShape),
                    ) {
                        Text(
                            text = num.toString(),
                            color = palette.tokenText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                        )
                    }
                }
            }
        }

        if (hasGame) {
            MasterBoard(
                called = master.called,
                lastCalled = lastCalled,
                emptyCellColor = settings.emptyCellColor.toComposeColor(),
                textScale = settings.boardTextScale,
            )
        } else {
            MasterEmptyState()
        }
    }

    if (confirmNewGame) {
        ConfirmDialog(
            message = stringResource(R.string.master_confirm_new_game),
            onConfirm = {
                confirmNewGame = false
                viewModel.newGame()
            },
            onDismiss = { confirmNewGame = false },
        )
    }
}

@Composable
private fun PillButton(
    text: String,
    container: Color,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = container, contentColor = Color.White),
    ) {
        Text(
            text = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        )
    }
}

/** 11×9 ones-digit tracking board. Called numbers get the cream/sky token; the last draw wears a red ring. */
@Composable
private fun MasterBoard(
    called: List<Int>,
    lastCalled: Int?,
    emptyCellColor: Color,
    textScale: Float,
) {
    val palette = LotoTheme.palette
    // number -> 1-based draw order for the corner badges.
    val callOrder = remember(called) { called.withIndex().associate { (i, n) -> n to i + 1 } }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, palette.boardCellBorder, RoundedCornerShape(16.dp))
            .background(palette.boardBg),
    ) {
        for (row in MasterBoardLayout.BOARD) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (num in row) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .background(if (num > 0) palette.boardBg else emptyCellColor)
                            .border(0.5.dp, palette.boardCellBorder),
                    ) {
                        if (num > 0) {
                            val order = callOrder[num]
                            val isCalled = order != null
                            val isLast = isCalled && num == lastCalled
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .fillMaxSize(0.82f)
                                    .scale(if (isLast) 1.1f else 1f)
                                    .clip(CircleShape)
                                    .background(if (isCalled) palette.tokenBg else palette.tokenUncalledBg)
                                    .border(
                                        width = if (isLast) 3.dp else 2.dp,
                                        color = when {
                                            isLast -> palette.lastCalledRing
                                            isCalled -> palette.tokenBorder
                                            else -> palette.tokenUncalledBorder
                                        },
                                        shape = CircleShape,
                                    ),
                            ) {
                                Text(
                                    text = num.toString(),
                                    color = if (isCalled) palette.tokenText else palette.tokenUncalledText,
                                    fontSize = (14 * textScale).sp,
                                    fontWeight = FontWeight.Black,
                                    maxLines = 1,
                                )
                            }
                            if (isCalled) {
                                Text(
                                    text = order.toString(),
                                    color = palette.subtitle,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(top = 1.dp, end = 2.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
