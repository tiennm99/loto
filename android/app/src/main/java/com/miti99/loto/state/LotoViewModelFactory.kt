package com.miti99.loto.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.miti99.loto.LotoApplication

/**
 * Manual-DI ViewModel factory over the [LotoApplication] singletons — the
 * app is small enough that a DI framework would cost more than it saves.
 */
class LotoViewModelFactory(private val app: LotoApplication) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(PlayerBoardViewModel::class.java) ->
            PlayerBoardViewModel(
                repository = app.gameStateRepository,
                masterStore = app.masterStore,
                settingsOrNull = app.settingsOrNull,
                voicePlayer = app.voicePlayer,
                fallbackSettings = app.settingsRepository.defaults,
            ) as T

        modelClass.isAssignableFrom(MasterPanelViewModel::class.java) ->
            MasterPanelViewModel(
                masterStore = app.masterStore,
                settings = app.settingsState,
                voicePlayer = app.voicePlayer,
            ) as T

        modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
            SettingsViewModel(
                repository = app.settingsRepository,
                settings = app.settingsState,
                voices = app.voices,
            ) as T

        else -> throw IllegalArgumentException("Unknown ViewModel class: $modelClass")
    }
}
