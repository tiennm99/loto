package com.miti99.loto.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miti99.loto.R
import com.miti99.loto.settings.SettingsState

/**
 * Voice-announcement toggle rows. Mirrors `SettingsButton.svelte` gating:
 *
 * - Master row visible when mode != PLAYER. Hint copy varies by mode
 *   (BOTH includes "+ báo Chờ/Kinh", MASTER-only just "Đọc số đã xổ.").
 * - Player + waiting-number rows visible when mode != MASTER.
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
        if (state.mode != SettingsState.Mode.PLAYER) {
            SwitchRow(
                label = stringResource(R.string.voice_enabled_master),
                checked = state.voiceEnabledMaster,
                onCheckedChange = onSetVoiceEnabledMaster,
            )
            val hintRes = if (state.mode == SettingsState.Mode.BOTH)
                R.string.voice_master_hint_both
            else
                R.string.voice_master_hint_master_only
            Text(
                text = stringResource(hintRes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp, bottom = 8.dp),
            )
        }

        if (state.mode != SettingsState.Mode.MASTER) {
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
