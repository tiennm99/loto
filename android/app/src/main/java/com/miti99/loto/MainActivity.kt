package com.miti99.loto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.miti99.loto.state.LotoViewModelFactory
import com.miti99.loto.state.MasterPanelViewModel
import com.miti99.loto.state.PlayerBoardViewModel
import com.miti99.loto.state.SettingsViewModel
import com.miti99.loto.ui.LotoAppRoot
import com.miti99.loto.ui.settings.SettingsSheet
import com.miti99.loto.ui.theme.LotoTheme

class MainActivity : ComponentActivity() {

    private val factory by lazy { LotoViewModelFactory(application as LotoApplication) }
    private val playerViewModel: PlayerBoardViewModel by viewModels { factory }
    private val masterViewModel: MasterPanelViewModel by viewModels { factory }
    private val settingsViewModel: SettingsViewModel by viewModels { factory }

    override fun onStart() {
        super.onStart()
        masterViewModel.setForeground(true)
    }

    override fun onStop() {
        super.onStop()
        // Pause auto-call while backgrounded — numbers nobody hears must not
        // drain the deck (the web's hidden tab was throttled for free).
        masterViewModel.setForeground(false)
    }

    override fun onDestroy() {
        super.onDestroy()
        // L1: release the app-scoped ExoPlayer only on an explicit, final
        // exit (the "Thoát" confirm below calls finish()), not on a
        // recreate (rotation/config change also calls onDestroy without
        // isFinishing). finish() does not guarantee the process dies —
        // Android can relaunch into the same cached process/Application
        // instance — so LotoApplication.releaseVoicePlayer() (H1) drops the
        // reference rather than leaving a terminal `by lazy` player behind;
        // the next voicePlayer access rebuilds a working one.
        if (isFinishing) {
            (application as LotoApplication).releaseVoicePlayer()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as LotoApplication
        setContent {
            val settings by app.settingsState.collectAsState()
            var showSettings by remember { mutableStateOf(false) }
            var showExitConfirm by remember { mutableStateOf(false) }
            LotoTheme(theme = settings.theme) {
                // Compose dialogs and the settings sheet consume back in
                // their own windows; this fires only at the root.
                BackHandler { showExitConfirm = true }
                LotoAppRoot(
                    settings = settings,
                    playerViewModel = playerViewModel,
                    masterViewModel = masterViewModel,
                    settingsButton = {
                        IconButton(onClick = { showSettings = true }) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = stringResource(R.string.settings_open),
                                tint = LotoTheme.palette.subtitle,
                            )
                        }
                    },
                )
                if (showSettings) {
                    SettingsSheet(
                        viewModel = settingsViewModel,
                        onDismiss = { showSettings = false },
                    )
                }
                if (showExitConfirm) {
                    // Same copy as the wrapper's exit dialog; state is already
                    // persisted continuously, so "Thoát" can simply finish.
                    AlertDialog(
                        onDismissRequest = { showExitConfirm = false },
                        title = { Text(stringResource(R.string.exit_title)) },
                        text = { Text(stringResource(R.string.exit_message)) },
                        confirmButton = {
                            TextButton(onClick = { finish() }) {
                                Text(stringResource(R.string.exit_confirm))
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showExitConfirm = false }) {
                                Text(stringResource(R.string.exit_cancel))
                            }
                        },
                    )
                }
            }
        }
    }
}
