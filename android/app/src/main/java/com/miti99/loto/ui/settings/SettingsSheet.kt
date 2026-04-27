package com.miti99.loto.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.miti99.loto.R
import com.miti99.loto.audio.VoiceEntry
import com.miti99.loto.settings.SettingsState

/**
 * Full-app settings exposed as a [ModalBottomSheet].
 *
 * All state is read from [state]; every change fires the corresponding setter.
 * Opened/dismissed via [onDismiss] — caller owns the visibility flag.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    state: SettingsState,
    voices: List<VoiceEntry>,
    onSetTheme: (SettingsState.Theme) -> Unit,
    onSetMode: (SettingsState.Mode) -> Unit,
    onSetAutoCallEnabled: (Boolean) -> Unit,
    onSetAutoCallSpeed: (Int) -> Unit,
    onSetVoiceEnabledMaster: (Boolean) -> Unit,
    onSetVoiceEnabledPlayer: (Boolean) -> Unit,
    onSetVoiceWaitingNumber: (Boolean) -> Unit,
    onSetVoice: (String) -> Unit,
    onSetEmptyCellColor: (String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            Spacer(Modifier.height(16.dp))

            // 1. Theme picker
            SettingsSection(title = stringResource(R.string.settings_theme)) {
                ThemePicker(
                    current = state.theme,
                    onChange = onSetTheme,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            SectionDivider()

            // 2. Mode picker
            SettingsSection(title = stringResource(R.string.settings_mode)) {
                ModePicker(
                    current = state.mode,
                    onChange = onSetMode,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // 3. Auto-call (only relevant when mode != PLAYER)
            if (state.mode != SettingsState.Mode.PLAYER) {
                SectionDivider()
                SettingsSection(title = stringResource(R.string.settings_auto_call)) {
                    AutoCallControls(
                        enabled = state.autoCallEnabled,
                        speedSeconds = state.autoCallSpeed,
                        onSetEnabled = onSetAutoCallEnabled,
                        onSetSpeed = onSetAutoCallSpeed,
                    )
                }
            }

            SectionDivider()

            // 4. Voice toggles + picker
            SettingsSection(title = stringResource(R.string.settings_voice)) {
                VoiceToggles(
                    state = state,
                    onSetVoiceEnabledMaster = onSetVoiceEnabledMaster,
                    onSetVoiceEnabledPlayer = onSetVoiceEnabledPlayer,
                    onSetVoiceWaitingNumber = onSetVoiceWaitingNumber,
                )
                Spacer(Modifier.height(8.dp))
                VoicePicker(
                    voices = voices,
                    currentVoiceId = state.voice,
                    onSelect = onSetVoice,
                )
            }

            SectionDivider()

            // 5. Empty-cell color
            SettingsSection(title = stringResource(R.string.settings_empty_cell_color)) {
                EmptyCellColorPicker(
                    currentHex = state.emptyCellColor,
                    onSelect = onSetEmptyCellColor,
                )
            }

            Spacer(Modifier.height(24.dp))

            // Reset + done buttons
            TextButton(onClick = onReset) {
                Text(stringResource(R.string.settings_reset))
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    content()
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
}
