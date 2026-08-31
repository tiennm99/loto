package com.miti99.loto.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.miti99.loto.R
import com.miti99.loto.settings.AppMode
import com.miti99.loto.settings.Settings
import com.miti99.loto.settings.ThemeSetting
import com.miti99.loto.state.SettingsViewModel
import com.miti99.loto.ui.player.ConfirmDialog
import com.miti99.loto.ui.theme.LotoTheme

/**
 * The settings sheet, ported control-for-control from the web's
 * `SettingsButton.svelte` dialog: theme, board text size, display mode,
 * auto-call, sound/voice, empty-cell color, reset/done. Every change
 * applies immediately — no save button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    viewModel: SettingsViewModel,
    onDismiss: () -> Unit,
) {
    val palette = LotoTheme.palette
    val settings by viewModel.settings.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = palette.dialogBg,
        // The sheet is tall (7 sections); open fully expanded like the web's
        // full-height settings modal instead of a half-open peek.
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                color = palette.foreground,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )
            Hint(stringResource(R.string.settings_subtitle))

            // Theme
            SectionTitle(stringResource(R.string.settings_theme))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((theme, labelId) in listOf(
                    ThemeSetting.AUTO to R.string.settings_theme_auto,
                    ThemeSetting.LIGHT to R.string.settings_theme_light,
                    ThemeSetting.DARK to R.string.settings_theme_dark,
                )) {
                    ChoiceButton(
                        label = stringResource(labelId),
                        selected = settings.theme == theme,
                        onClick = { viewModel.setTheme(theme) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Board text size
            SectionTitle(stringResource(R.string.settings_text_scale))
            Hint(stringResource(R.string.settings_text_scale_hint))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val labels = listOf(
                    R.string.settings_text_scale_s,
                    R.string.settings_text_scale_m,
                    R.string.settings_text_scale_l,
                    R.string.settings_text_scale_xl,
                )
                Settings.BOARD_TEXT_SCALES.forEachIndexed { i, scale ->
                    ChoiceButton(
                        label = stringResource(labels[i]),
                        selected = settings.boardTextScale == scale,
                        onClick = { viewModel.setBoardTextScale(scale) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }

            // Display mode
            SectionTitle(stringResource(R.string.settings_mode))
            Hint(stringResource(R.string.settings_mode_hint))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((mode, labelId) in listOf(
                    AppMode.PLAYER to R.string.settings_mode_player,
                    AppMode.MASTER to R.string.settings_mode_master,
                    AppMode.BOTH to R.string.settings_mode_both,
                )) {
                    ChoiceButton(
                        label = stringResource(labelId),
                        selected = settings.mode == mode,
                        onClick = { viewModel.setMode(mode) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Text(
                text = stringResource(
                    when (settings.mode) {
                        AppMode.PLAYER -> R.string.settings_mode_hint_player
                        AppMode.MASTER -> R.string.settings_mode_hint_master
                        AppMode.BOTH -> R.string.settings_mode_hint_both
                    },
                ),
                color = palette.subtitle,
                fontSize = 12.sp,
                fontStyle = FontStyle.Italic,
                modifier = Modifier.padding(top = 8.dp),
            )

            // Auto-call (only relevant when the master panel is visible)
            if (settings.mode != AppMode.PLAYER) {
                SectionTitle(stringResource(R.string.settings_auto_call))
                Hint(stringResource(R.string.settings_auto_call_hint))
                SwitchRow(
                    label = stringResource(R.string.settings_auto_call_switch),
                    checked = settings.autoCallEnabled,
                    onToggle = { viewModel.setAutoCallEnabled(!settings.autoCallEnabled) },
                )
                if (settings.autoCallEnabled) {
                    Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp)) {
                        Text(
                            text = stringResource(
                                R.string.settings_auto_call_speed,
                                settings.autoCallSpeed,
                            ),
                            color = palette.buttonSecondaryText,
                            fontSize = 14.sp,
                        )
                        Slider(
                            value = settings.autoCallSpeed.toFloat(),
                            onValueChange = {
                                viewModel.setAutoCallSpeed(it.toInt().coerceIn(1, 10))
                            },
                            valueRange = 1f..10f,
                            steps = 8,
                            colors = SliderDefaults.colors(
                                thumbColor = palette.title,
                                activeTrackColor = palette.title,
                            ),
                        )
                    }
                }
            }

            // Sound / voice
            SectionTitle(stringResource(R.string.settings_sound))
            Hint(stringResource(R.string.settings_sound_hint))
            if (settings.mode != AppMode.PLAYER) {
                SwitchRow(
                    label = stringResource(R.string.settings_voice_master),
                    checked = settings.voiceEnabledMaster,
                    onToggle = { viewModel.setVoiceEnabledMaster(!settings.voiceEnabledMaster) },
                )
                Hint(
                    stringResource(
                        if (settings.mode == AppMode.BOTH) R.string.settings_voice_master_hint_both
                        else R.string.settings_voice_master_hint,
                    ),
                    small = true,
                )
            }
            if (settings.mode != AppMode.MASTER) {
                SwitchRow(
                    label = stringResource(R.string.settings_voice_player),
                    checked = settings.voiceEnabledPlayer,
                    onToggle = { viewModel.setVoiceEnabledPlayer(!settings.voiceEnabledPlayer) },
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (settings.voiceEnabledPlayer && settings.mode != AppMode.BOTH) {
                    Column(modifier = Modifier.padding(start = 12.dp, top = 8.dp)) {
                        SwitchRow(
                            label = stringResource(R.string.settings_voice_waiting),
                            checked = settings.voiceWaitingNumber,
                            onToggle = {
                                viewModel.setVoiceWaitingNumber(!settings.voiceWaitingNumber)
                            },
                        )
                        Hint(stringResource(R.string.settings_voice_waiting_hint), small = true)
                    }
                }
            }
            if (viewModel.voices.size > 1) {
                Text(
                    text = stringResource(R.string.settings_voice_picker),
                    color = palette.buttonSecondaryText,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(top = 12.dp, bottom = 6.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (voice in viewModel.voices) {
                        ChoiceButton(
                            label = voice.label,
                            selected = settings.voice == voice.id,
                            onClick = { viewModel.setVoice(voice.id) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            // Empty-cell color
            SectionTitle(stringResource(R.string.settings_empty_color))
            EmptyCellColorPicker(
                value = settings.emptyCellColor,
                onPick = { viewModel.setEmptyCellColor(it) },
            )

            // Footer actions
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp),
            ) {
                OutlinedButton(
                    onClick = { confirmReset = true },
                    shape = RoundedCornerShape(50),
                ) {
                    Text(
                        text = stringResource(R.string.settings_reset),
                        color = palette.buttonSecondaryText,
                        fontSize = 14.sp,
                    )
                }
                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = palette.buttonPrimary,
                        contentColor = Color.White,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.settings_done),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }

    if (confirmReset) {
        ConfirmDialog(
            message = stringResource(R.string.settings_reset_confirm),
            onConfirm = {
                confirmReset = false
                viewModel.reset()
            },
            onDismiss = { confirmReset = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        color = LotoTheme.palette.foreground,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(top = 20.dp, bottom = 8.dp),
    )
}

@Composable
private fun Hint(text: String, small: Boolean = false) {
    Text(
        text = text,
        color = LotoTheme.palette.subtitle,
        fontSize = if (small) 12.sp else 14.sp,
        modifier = Modifier.padding(bottom = if (small) 0.dp else 8.dp, top = if (small) 6.dp else 0.dp),
    )
}
