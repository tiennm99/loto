package com.miti99.loto.ui.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.miti99.loto.R

/**
 * Empty-cell color picker with 10 Excel-preset swatches + custom RGB sliders.
 *
 * Live preview bar reflects [currentHex] immediately. The custom tile expands
 * [RgbSliderRow] inline; selecting a swatch collapses it.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun EmptyCellColorPicker(
    currentHex: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var customExpanded by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        // Live preview bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(parseHex(currentHex)),
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 5-per-row swatch grid + custom tile
        FlowRow(
            maxItemsInEachRow = 5,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EXCEL_PRESETS.forEach { hex ->
                ColorSwatchTile(
                    hex = hex,
                    selected = currentHex.equals(hex, ignoreCase = true),
                    onClick = {
                        customExpanded = false
                        onSelect(hex)
                    },
                )
            }
            CustomTile(
                active = customExpanded,
                onClick = { customExpanded = !customExpanded },
            )
        }

        // Inline "Tùy chỉnh" label
        if (customExpanded) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.empty_cell_custom),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 2.dp),
            )
        }

        // RGB slider panel
        AnimatedVisibility(visible = customExpanded) {
            RgbSliderRow(
                currentHex = currentHex,
                onSelect = onSelect,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}
