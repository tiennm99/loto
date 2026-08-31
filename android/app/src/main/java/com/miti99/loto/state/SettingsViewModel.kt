package com.miti99.loto.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miti99.loto.audio.Voice
import com.miti99.loto.settings.AppMode
import com.miti99.loto.settings.Settings
import com.miti99.loto.settings.SettingsRepository
import com.miti99.loto.settings.ThemeSetting
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Bridges the settings sheet UI to [SettingsRepository]. Reads come from the
 * app-scoped [settings] state; writes go through the repository (validation
 * happens on read, mirroring the web store).
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    val settings: StateFlow<Settings>,
    /** Bundled voices for the voice picker. */
    val voices: List<Voice>,
) : ViewModel() {

    fun setEmptyCellColor(value: String) = update { repository.setEmptyCellColor(value) }
    fun setTheme(value: ThemeSetting) = update { repository.setTheme(value) }
    fun setMode(value: AppMode) = update { repository.setMode(value) }
    fun setAutoCallEnabled(value: Boolean) = update { repository.setAutoCallEnabled(value) }
    fun setAutoCallSpeed(value: Int) = update { repository.setAutoCallSpeed(value) }
    fun setVoiceEnabledMaster(value: Boolean) = update { repository.setVoiceEnabledMaster(value) }
    fun setVoiceEnabledPlayer(value: Boolean) = update { repository.setVoiceEnabledPlayer(value) }
    fun setVoiceWaitingNumber(value: Boolean) = update { repository.setVoiceWaitingNumber(value) }
    fun setVoice(value: String) = update { repository.setVoice(value) }
    fun setBoardTextScale(value: Float) = update { repository.setBoardTextScale(value) }
    fun reset() = update { repository.reset() }

    private fun update(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
