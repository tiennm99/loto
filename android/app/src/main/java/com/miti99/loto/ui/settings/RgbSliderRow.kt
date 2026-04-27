package com.miti99.loto.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import kotlinx.coroutines.delay

/**
 * Three R/G/B sliders (0-255 each) for custom empty-cell color selection.
 *
 * Writes are debounced 150 ms to avoid flooding the DataStore with every
 * frame while the user drags (per phase-09 risk register).
 *
 * @param currentHex  hex string representing the current color (e.g. "#7030A0")
 * @param onSelect    called with new "#RRGGBB" string after debounce
 */
@Composable
internal fun RgbSliderRow(
    currentHex: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val parsed = remember(currentHex) { parseHex(currentHex) }

    var r by remember(currentHex) { mutableFloatStateOf((parsed.red * 255f)) }
    var g by remember(currentHex) { mutableFloatStateOf((parsed.green * 255f)) }
    var b by remember(currentHex) { mutableFloatStateOf((parsed.blue * 255f)) }

    // Debounce: only call onSelect after user stops dragging for 150 ms
    val liveHex = "#%02X%02X%02X".format(r.toInt(), g.toInt(), b.toInt())
    LaunchedEffect(liveHex) {
        delay(150)
        if (!liveHex.equals(currentHex, ignoreCase = true)) {
            onSelect(liveHex)
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        SliderRow(
            label = stringResource(R.string.empty_cell_r),
            value = r,
            onValueChange = { r = it },
        )
        SliderRow(
            label = stringResource(R.string.empty_cell_g),
            value = g,
            onValueChange = { g = it },
        )
        SliderRow(
            label = stringResource(R.string.empty_cell_b),
            value = b,
            onValueChange = { b = it },
        )
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.width(16.dp),
        )
        Spacer(Modifier.width(8.dp))
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = value.toInt().toString(),
            fontSize = 12.sp,
            modifier = Modifier.width(28.dp),
        )
    }
}
