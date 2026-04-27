package com.miti99.loto.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.miti99.loto.LotoApp

/**
 * Single ViewModelProvider.Factory entry that builds every app VM from the
 * [LotoApp] singleton graph. Composables get a VM via:
 *
 *   val vm: PlayerBoardViewModel = viewModel(factory = LotoViewModelFactory(app))
 */
fun lotoViewModelFactory(app: LotoApp): ViewModelProvider.Factory = viewModelFactory {
    val storage = GameStorage(app.lotoDataStoreInternal)
    initializer {
        PlayerBoardViewModel(
            settingsRepo = app.settingsRepo,
            callBus = app.callBus,
            voicePlayer = app.voicePlayer,
            storage = storage,
        )
    }
    initializer {
        MasterPanelViewModel(
            settingsRepo = app.settingsRepo,
            callBus = app.callBus,
            voicePlayer = app.voicePlayer,
            storage = storage,
        )
    }
    initializer {
        SettingsViewModel(repo = app.settingsRepo)
    }
}

internal inline fun <reified VM : ViewModel> ViewModelProvider.get(): VM = get(VM::class.java)
