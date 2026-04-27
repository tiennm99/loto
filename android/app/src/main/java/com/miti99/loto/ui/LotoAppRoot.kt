package com.miti99.loto.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.miti99.loto.LotoApp
import com.miti99.loto.R
import com.miti99.loto.audio.VoiceManifest
import com.miti99.loto.settings.SettingsState
import com.miti99.loto.state.MasterPanelViewModel
import com.miti99.loto.state.PlayerBoardViewModel
import com.miti99.loto.state.SettingsViewModel
import com.miti99.loto.state.lotoViewModelFactory
import com.miti99.loto.ui.board.PlayerBoardScreen
import com.miti99.loto.ui.common.LocalEmptyCellColor
import com.miti99.loto.ui.master.MasterPanelScreen
import com.miti99.loto.ui.settings.SettingsSheet
import com.miti99.loto.ui.settings.parseHex
import com.miti99.loto.ui.theme.LotoTheme

/**
 * Root composable. Instantiates all three ViewModels from [lotoViewModelFactory],
 * applies the user-selected theme override, provides [LocalEmptyCellColor], and
 * conditionally renders [PlayerBoardScreen] / [MasterPanelScreen] based on mode.
 */
@Composable
fun LotoAppRoot() {
    val app = LocalContext.current.applicationContext as LotoApp
    val factory = remember(app) { lotoViewModelFactory(app) }

    val settingsVm: SettingsViewModel = viewModel(factory = factory)
    val playerVm: PlayerBoardViewModel = viewModel(factory = factory)
    val masterVm: MasterPanelViewModel = viewModel(factory = factory)

    val settingsState by settingsVm.state.collectAsStateWithLifecycle()
    val playerState by playerVm.state.collectAsStateWithLifecycle()
    val masterState by masterVm.state.collectAsStateWithLifecycle()

    // Load voice list once at composition entry
    val voices = remember(app) {
        try { VoiceManifest.load(app) } catch (_: Exception) { emptyList() }
    }

    val forcedDark: Boolean? = when (settingsState.theme) {
        SettingsState.Theme.AUTO  -> null
        SettingsState.Theme.LIGHT -> false
        SettingsState.Theme.DARK  -> true
    }

    val emptyCellColor = remember(settingsState.emptyCellColor) {
        parseHex(settingsState.emptyCellColor)
    }

    LotoTheme(forcedDark = forcedDark) {
        CompositionLocalProvider(LocalEmptyCellColor provides emptyCellColor) {
            var showSettings by rememberSaveable { mutableStateOf(false) }

            Scaffold(
                topBar = { LotoTopBar(onSettingsClick = { showSettings = true }) },
                contentWindowInsets = WindowInsets.safeDrawing,
            ) { paddingValues ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Wordmark(modifier = Modifier.padding(vertical = 8.dp))

                    when (settingsState.mode) {
                        SettingsState.Mode.PLAYER -> {
                            PlayerBoardScreen(
                                state = playerState,
                                emptyCellColor = emptyCellColor,
                                onGenerate = playerVm::generate,
                                onClear = playerVm::clear,
                                onCellClick = playerVm::toggleCell,
                                onDismissBingo = playerVm::dismissBingo,
                                onDismissToast = playerVm::dismissWaitingToast,
                            )
                        }

                        SettingsState.Mode.MASTER -> {
                            MasterPanelScreen(
                                state = masterState,
                                autoCallEnabled = settingsState.autoCallEnabled,
                                autoCallSpeed = settingsState.autoCallSpeed,
                                onNewGame = masterVm::newGame,
                                onDrawNext = masterVm::drawNext,
                                onToggleAuto = masterVm::toggleAuto,
                            )
                        }

                        SettingsState.Mode.BOTH -> {
                            // Master panel on top, player board below
                            Text(
                                text = stringResource(R.string.mode_master),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 12.dp, top = 4.dp, bottom = 2.dp),
                            )
                            MasterPanelScreen(
                                state = masterState,
                                autoCallEnabled = settingsState.autoCallEnabled,
                                autoCallSpeed = settingsState.autoCallSpeed,
                                onNewGame = masterVm::newGame,
                                onDrawNext = masterVm::drawNext,
                                onToggleAuto = masterVm::toggleAuto,
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                            PlayerBoardScreen(
                                state = playerState,
                                emptyCellColor = emptyCellColor,
                                onGenerate = playerVm::generate,
                                onClear = playerVm::clear,
                                onCellClick = playerVm::toggleCell,
                                onDismissBingo = playerVm::dismissBingo,
                                onDismissToast = playerVm::dismissWaitingToast,
                            )
                        }
                    }
                }
            }

            // Settings bottom sheet — rendered outside Scaffold so it overlays correctly
            if (showSettings) {
                SettingsSheet(
                    state = settingsState,
                    voices = voices,
                    onSetTheme = settingsVm::setTheme,
                    onSetMode = settingsVm::setMode,
                    onSetAutoCallEnabled = settingsVm::setAutoCallEnabled,
                    onSetAutoCallSpeed = settingsVm::setAutoCallSpeed,
                    onSetVoiceEnabledMaster = settingsVm::setVoiceEnabledMaster,
                    onSetVoiceEnabledPlayer = settingsVm::setVoiceEnabledPlayer,
                    onSetVoiceWaitingNumber = settingsVm::setVoiceWaitingNumber,
                    onSetVoice = settingsVm::setVoice,
                    onSetEmptyCellColor = settingsVm::setEmptyCellColor,
                    onReset = settingsVm::reset,
                    onDismiss = { showSettings = false },
                )
            }
        }
    }
}
