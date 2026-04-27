package com.miti99.loto.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.miti99.loto.settings.SettingsRepository
import com.miti99.loto.settings.SettingsState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Thin wrapper over [SettingsRepository]. */
class SettingsViewModel(
    private val repo: SettingsRepository,
) : ViewModel() {

    val state: StateFlow<SettingsState> = repo.flow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsState.DEFAULT)

    fun setEmptyCellColor(hex: String) = launch { repo.setEmptyCellColor(hex) }
    fun setTheme(theme: SettingsState.Theme) = launch { repo.setTheme(theme) }
    fun setMode(mode: SettingsState.Mode) = launch { repo.setMode(mode) }
    fun setAutoCallEnabled(enabled: Boolean) = launch { repo.setAutoCallEnabled(enabled) }
    fun setAutoCallSpeed(s: Int) = launch { repo.setAutoCallSpeed(s) }
    fun setVoiceEnabledMaster(b: Boolean) = launch { repo.setVoiceEnabledMaster(b) }
    fun setVoiceEnabledPlayer(b: Boolean) = launch { repo.setVoiceEnabledPlayer(b) }
    fun setVoiceWaitingNumber(b: Boolean) = launch { repo.setVoiceWaitingNumber(b) }
    fun setVoice(id: String) = launch { repo.setVoice(id) }
    fun reset() = launch { repo.reset() }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
