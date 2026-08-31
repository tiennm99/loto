package com.miti99.loto

import android.app.Application
import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import com.miti99.loto.audio.ExoVoicePlayer
import com.miti99.loto.audio.Voice
import com.miti99.loto.audio.VoiceCatalog
import com.miti99.loto.audio.VoicePlayerApi
import com.miti99.loto.settings.Settings
import com.miti99.loto.settings.SettingsRepository
import com.miti99.loto.state.GameStateRepository
import com.miti99.loto.state.MasterStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Settings and round state live in separate stores (the web keeps them under
// separate localStorage keys too) so frequent round writes never contend
// with settings writes. A corrupt file falls back to empty preferences —
// losing one round beats crash-looping on every launch (the web's
// try/catch-everything localStorage posture).
private val Context.settingsDataStore by preferencesDataStore(
    name = "loto_settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)
private val Context.gameStateDataStore by preferencesDataStore(
    name = "loto_game_state",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * Process-wide singletons live here (manual DI — the app is small enough
 * that a DI framework would cost more than it saves).
 */
class LotoApplication : Application() {

    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Bundled announcer voices, parsed once from the asset manifest. */
    val voices: List<Voice> by lazy { VoiceCatalog.load(this) }

    val settingsRepository: SettingsRepository by lazy {
        SettingsRepository(
            dataStore = settingsDataStore,
            validVoiceIds = voices.map { it.id }.toSet(),
            defaultVoiceId = VoiceCatalog.defaultVoiceId(voices),
        )
    }

    /** App-scoped settings snapshot every ViewModel reads from. */
    val settingsState: StateFlow<Settings> by lazy {
        settingsRepository.settingsFlow
            .stateIn(appScope, SharingStarted.Eagerly, settingsRepository.defaults)
    }

    val gameStateRepository: GameStateRepository by lazy {
        GameStateRepository(gameStateDataStore)
    }

    val masterStore: MasterStore by lazy {
        MasterStore(gameStateRepository, appScope)
    }

    /** Built on the main thread (ExoPlayer requirement). */
    val voicePlayer: VoicePlayerApi by lazy {
        ExoVoicePlayer(this).also { it.voiceId = VoiceCatalog.defaultVoiceId(voices) }
    }

    override fun onCreate() {
        super.onCreate()
        appScope.launch {
            masterStore.restore()
            // Keep the announcer on the configured voice.
            settingsState.collect { voicePlayer.voiceId = it.voice }
        }
    }
}
