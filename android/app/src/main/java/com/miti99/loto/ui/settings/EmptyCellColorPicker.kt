package com.miti99.loto.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.ui.theme.LotoTheme
import com.miti99.loto.ui.toComposeColor
import kotlin.math.roundToInt

/** Office "Standard Colors" palette (10 swatches), same as the web. Default is Purple. */
private val PRESETS = listOf(
    "#C00000", "#FF0000", "#FFC000", "#FFFF00", "#92D050",
    "#00B050", "#00B0F0", "#0070C0", "#002060", "#7030A0",
)

/**
 * Empty-cell color control: RGB sliders as the custom picker (the native
 * stand-in for the web's `<input type=color>`) plus the Office preset
 * swatches the web ships.
 */
@Composable
fun EmptyCellColorPicker(
    value: String,
    onPick: (String) -> Unit,
) {
    val palette = LotoTheme.palette
    // The sliders preview through local state and commit once on drag end —
    // per-frame commits would rewrite the preferences file ~250× per drag.
    var draft by remember { mutableStateOf<String?>(null) }
    val shown = draft ?: value
    val color = shown.toComposeColor()
    // roundToInt, not toInt: truncation drifted the untouched channels down
    // on every drag since `draft` is rebuilt from these derived values (M1).
    val r = (color.red * 255).roundToInt()
    val g = (color.green * 255).roundToInt()
    val b = (color.blue * 255).roundToInt()
    val commitDraft: () -> Unit = {
        draft?.let(onPick)
        draft = null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(1.dp, palette.buttonSecondaryBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
    ) {
        SectionCaption(stringResource(R.string.settings_empty_color_custom))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp, 40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(color)
                    .border(1.dp, palette.buttonSecondaryBorder, RoundedCornerShape(8.dp)),
            )
            Text(
                text = shown.uppercase(),
                color = palette.buttonSecondaryText,
                fontSize = 14.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        ChannelSlider("R", r, onChange = { draft = hex(it, g, b) }, onChangeFinished = commitDraft)
        ChannelSlider("G", g, onChange = { draft = hex(r, it, b) }, onChangeFinished = commitDraft)
        ChannelSlider("B", b, onChange = { draft = hex(r, g, it) }, onChangeFinished = commitDraft)

        SectionCaption(
            stringResource(R.string.settings_empty_color_presets),
            modifier = Modifier.padding(top = 12.dp),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (hexValue in PRESETS.take(5)) {
                PresetSwatch(hexValue, value, onPick, Modifier.weight(1f))
            }
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 8.dp),
        ) {
            for (hexValue in PRESETS.drop(5)) {
                PresetSwatch(hexValue, value, onPick, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionCaption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        color = LotoTheme.palette.subtitle,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em,
        modifier = modifier.padding(bottom = 6.dp),
    )
}

@Composable
private fun ChannelSlider(
    label: String,
    channel: Int,
    onChange: (Int) -> Unit,
    onChangeFinished: () -> Unit,
) {
    val palette = LotoTheme.palette
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            color = palette.subtitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 8.dp),
        )
        Slider(
            value = channel.toFloat(),
            onValueChange = { onChange(it.toInt().coerceIn(0, 255)) },
            onValueChangeFinished = onChangeFinished,
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = palette.title,
                activeTrackColor = palette.title,
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = channel.toString(),
            color = palette.subtitle,
            fontSize = 12.sp,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun PresetSwatch(
    hexValue: String,
    selectedValue: String,
    onPick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LotoTheme.palette
    val selected = selectedValue.equals(hexValue, ignoreCase = true)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(hexValue.toComposeColor())
            .border(
                width = if (selected) 3.dp else 2.dp,
                color = if (selected) palette.title else palette.buttonSecondaryBorder,
                shape = RoundedCornerShape(8.dp),
            )
            .clickable { onPick(hexValue) },
    )
}

private fun hex(r: Int, g: Int, b: Int): String =
    "#%02X%02X%02X".format(r, g, b)
