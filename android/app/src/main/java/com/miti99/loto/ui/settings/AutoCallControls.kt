package com.miti99.loto.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miti99.loto.R

/**
 * Auto-call section: a switch to enable/disable, and a speed slider (1–10 s)
 * that only appears when the switch is on.
 *
 * Slider uses `steps = 8` because Material 3 counts intermediate stops
 * between the two endpoints — 8 steps gives 10 discrete values (1..10).
 */
@Composable
internal fun AutoCallControls(
    enabled: Boolean,
    speedSeconds: Int,
    onSetEnabled: (Boolean) -> Unit,
    onSetSpeed: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.auto_call_enable),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(
                checked = enabled,
                onCheckedChange = onSetEnabled,
            )
        }

        AnimatedVisibility(visible = enabled) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.auto_call_speed_format, speedSeconds),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Slider(
                    value = speedSeconds.toFloat(),
                    onValueChange = { onSetSpeed(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,  // 8 intermediate stops → 10 discrete positions
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
