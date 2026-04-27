package com.miti99.loto.ui.settings

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.miti99.loto.R
import com.miti99.loto.settings.SettingsState

private fun themeLabel(theme: SettingsState.Theme): Int = when (theme) {
    SettingsState.Theme.AUTO  -> R.string.theme_auto
    SettingsState.Theme.LIGHT -> R.string.theme_light
    SettingsState.Theme.DARK  -> R.string.theme_dark
}

private fun modeLabel(mode: SettingsState.Mode): Int = when (mode) {
    SettingsState.Mode.PLAYER -> R.string.mode_player
    SettingsState.Mode.MASTER -> R.string.mode_master
    SettingsState.Mode.BOTH   -> R.string.mode_both
}

/**
 * 3-segment row for theme selection (Auto / Sáng / Tối).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ThemePicker(
    current: SettingsState.Theme,
    onChange: (SettingsState.Theme) -> Unit,
    modifier: Modifier = Modifier,
) {
    val themes = SettingsState.Theme.values()
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        themes.forEachIndexed { i, theme ->
            SegmentedButton(
                selected = current == theme,
                onClick = { onChange(theme) },
                shape = SegmentedButtonDefaults.itemShape(i, themes.size),
            ) {
                Text(stringResource(themeLabel(theme)))
            }
        }
    }
}

/**
 * 3-segment row for mode selection (Người chơi / Quản trò / Cả hai).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ModePicker(
    current: SettingsState.Mode,
    onChange: (SettingsState.Mode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val modes = SettingsState.Mode.values()
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        modes.forEachIndexed { i, mode ->
            SegmentedButton(
                selected = current == mode,
                onClick = { onChange(mode) },
                shape = SegmentedButtonDefaults.itemShape(i, modes.size),
            ) {
                Text(stringResource(modeLabel(mode)))
            }
        }
    }
}
