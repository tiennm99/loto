package com.miti99.loto.ui.master

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miti99.loto.R
import com.miti99.loto.state.MasterPanelUiState

/**
 * Stateless master-panel (quản trò) screen.
 *
 * Auto-scroll: hero scrolls into view on each new draw, gated by [hasInteracted]
 * so a cold-load with persisted state does NOT yank the screen.
 *
 * Port of MasterPanel.svelte:120-131.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MasterPanelScreen(
    state: MasterPanelUiState,
    autoCallEnabled: Boolean,
    autoCallSpeed: Int,
    onNewGame: () -> Unit,
    onDrawNext: () -> Unit,
    onToggleAuto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showNewGameDialog by remember { mutableStateOf(false) }
    // Only auto-scroll after first explicit user action in this session
    var hasInteracted by rememberSaveable { mutableStateOf(false) }
    val heroRequester = remember { BringIntoViewRequester() }

    LaunchedEffect(state.lastCalled) {
        if (state.lastCalled != null && hasInteracted) {
            heroRequester.bringIntoView()
        }
    }

    // New-game confirmation dialog
    if (showNewGameDialog) {
        AlertDialog(
            onDismissRequest = { showNewGameDialog = false },
            title = { Text(stringResource(R.string.prompt_new_game)) },
            confirmButton = {
                TextButton(onClick = {
                    showNewGameDialog = false
                    hasInteracted = true
                    onNewGame()
                }) { Text(stringResource(R.string.prompt_yes)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewGameDialog = false }) {
                    Text(stringResource(R.string.prompt_no))
                }
            },
        )
    }

    Column(modifier = modifier.fillMaxWidth()) {
        MasterControls(
            hasState = state.deck != null,
            canDraw = state.deck?.remaining?.isNotEmpty() ?: false,
            autoCallEnabled = autoCallEnabled,
            autoRunning = state.autoRunning,
            onNewGame = {
                if (state.deck != null) showNewGameDialog = true
                else { hasInteracted = true; onNewGame() }
            },
            onDrawNext = { hasInteracted = true; onDrawNext() },
            onToggleAuto = { hasInteracted = true; onToggleAuto() },
        )

        // Auto-call speed chip
        if (autoCallEnabled && state.deck?.remaining?.isNotEmpty() == true) {
            AutoCallChip(
                speedSeconds = autoCallSpeed,
                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp),
            )
        }

        if (state.deck != null) {
            // Hero number
            if (state.lastCalled != null) {
                Spacer(modifier = Modifier.height(8.dp))
                CurrentNumberHero(
                    last = state.lastCalled,
                    calledCount = state.deck.called.size,
                    remainingCount = state.deck.remaining.size,
                    modifier = Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(heroRequester),
                )
            } else {
                // Pre-game hint after new game started but nothing drawn yet
                Text(
                    text = stringResource(R.string.master_empty_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 24.dp),
                )
            }

            // Called history chip row
            if (state.deck.called.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                CalledHistory(
                    called = state.deck.called,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 11×9 master board
            MasterBoard(
                called = state.deck.called,
                lastCalled = state.lastCalled,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        } else {
            // No game started yet
            Text(
                text = stringResource(R.string.master_empty_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(vertical = 32.dp),
            )
        }
    }
}
