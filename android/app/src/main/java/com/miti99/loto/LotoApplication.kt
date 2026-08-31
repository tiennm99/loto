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
import com.miti99.loto.audio.VoicePlayerHolder
import com.miti99.loto.settings.Settings
import com.miti99.loto.settings.SettingsRepository
import com.miti99.loto.state.GameStateRepository
import com.miti99.loto.state.MasterStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
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

    /**
     * Sole `stateIn` collector over `settingsRepository.settingsFlow`; null =
     * the first DataStore read has not resolved yet. `settingsState` below is
     * *derived* from this rather than collecting `settingsFlow` a second,
     * independent time — two independent collectors of the same upstream
     * cannot be trusted to update in step with each other, which is exactly
     * what let the M2 gate desync from the settings value it was supposed to
     * guard (a consumer needing to correlate "loaded" with "the loaded
     * value" — PlayerBoardViewModel — reads this field directly instead of
     * `settingsState` + a separate loaded flag).
     */
    val settingsOrNull: StateFlow<Settings?> by lazy {
        settingsRepository.settingsFlow
            .map<Settings, Settings?> { it }
            .stateIn(appScope, SharingStarted.Eagerly, null)
    }

    /** App-scoped settings snapshot every other ViewModel/Compose consumer reads from. */
    val settingsState: StateFlow<Settings> by lazy {
        settingsOrNull
            .map { it ?: settingsRepository.defaults }
            .stateIn(appScope, SharingStarted.Eagerly, settingsRepository.defaults)
    }

    val gameStateRepository: GameStateRepository by lazy {
        GameStateRepository(gameStateDataStore)
    }

    val masterStore: MasterStore by lazy {
        MasterStore(gameStateRepository, appScope)
    }

    /**
     * Built on the main thread (ExoPlayer requirement). Recreatable (H1):
     * `finish()` (the "Thoát" confirm) does not guarantee the process dies,
     * so a plain `by lazy` singleton would hand every ViewModel an
     * already-released, permanently-dead player on a relaunch into the same
     * cached process. [releaseVoicePlayer] drops the reference on an
     * explicit exit; the next access here rebuilds a working one.
     */
    private val voicePlayerHolder: VoicePlayerHolder by lazy {
        VoicePlayerHolder {
            ExoVoicePlayer(this).also { it.voiceId = VoiceCatalog.defaultVoiceId(voices) }
        }
    }
    val voicePlayer: VoicePlayerApi get() = voicePlayerHolder.value

    /** Release the app-scoped player on an explicit, final exit (MainActivity.onDestroy(), isFinishing). */
    fun releaseVoicePlayer() = voicePlayerHolder.release()

    override fun onCreate() {
        super.onCreate()
        // Two unrelated jobs on two launches (M4): they used to share one
        // coroutine with the round-state restore first, so a slow/failed
        // DataStore read for the round would stall voice-id sync too. Each
        // still races the other independently — PlayerBoardViewModel's
        // settings/masterStore.state combine() is what makes that race safe
        // rather than this ordering.
        appScope.launch { masterStore.restore() }
        appScope.launch {
            // Keep the announcer on the configured voice.
            settingsState.collect { voicePlayer.voiceId = it.voice }
        }
    }
}
