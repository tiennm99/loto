package com.miti99.loto.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miti99.loto.R
import com.miti99.loto.audio.VoiceEntry

/**
 * Radio-button list of available voices loaded from the asset manifest.
 * Hidden if [voices] is empty (manifest failed to load).
 */
@Composable
internal fun VoicePicker(
    voices: List<VoiceEntry>,
    currentVoiceId: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (voices.isEmpty()) return

    Column(modifier = modifier) {
        Text(
            text = stringResource(R.string.voice_picker_label),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 4.dp),
        )
        voices.forEach { voice ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelect(voice.id) }
                    .padding(vertical = 4.dp),
            ) {
                RadioButton(
                    selected = currentVoiceId == voice.id,
                    onClick = { onSelect(voice.id) },
                )
                Text(
                    text = voice.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
