package com.miti99.loto.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.miti99.loto.R
import com.miti99.loto.settings.SettingsState

/**
 * Three voice-announcement toggle rows.
 *
 * The "đọc số khi Chờ" switch is only visible when [state.voiceEnabledPlayer] is on
 * (matching web's conditional render at SettingsButton.svelte).
 */
@Composable
internal fun VoiceToggles(
    state: SettingsState,
    onSetVoiceEnabledMaster: (Boolean) -> Unit,
    onSetVoiceEnabledPlayer: (Boolean) -> Unit,
    onSetVoiceWaitingNumber: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        SwitchRow(
            label = stringResource(R.string.voice_enabled_master),
            checked = state.voiceEnabledMaster,
            onCheckedChange = onSetVoiceEnabledMaster,
        )
        SwitchRow(
            label = stringResource(R.string.voice_enabled_player),
            checked = state.voiceEnabledPlayer,
            onCheckedChange = onSetVoiceEnabledPlayer,
        )
        AnimatedVisibility(visible = state.voiceEnabledPlayer) {
            SwitchRow(
                label = stringResource(R.string.voice_waiting_number),
                checked = state.voiceWaitingNumber,
                onCheckedChange = onSetVoiceWaitingNumber,
            )
        }
    }
}

@Composable
private fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
