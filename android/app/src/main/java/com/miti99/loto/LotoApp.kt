package com.miti99.loto

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.miti99.loto.audio.VoiceManifest
import com.miti99.loto.audio.VoicePlayer
import com.miti99.loto.settings.SettingsRepository
import com.miti99.loto.state.CallBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** DataStore extension on the app Context — used only inside [LotoApp]. */
private val Context.lotoDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "loto_settings",
    produceMigrations = { listOf(SettingsRepository.legacyMasterModeMigration) },
)

/**
 * Application singleton. Wires app-scoped state in onCreate.
 *
 * Composables retrieve singletons through `LocalContext.current.applicationContext as LotoApp`
 * and pass them into a `ViewModelProvider.Factory` (see [com.miti99.loto.state.lotoViewModelFactory]).
 */
class LotoApp : Application() {

    val appScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    val callBus: CallBus = CallBus()

    lateinit var settingsRepo: SettingsRepository
        private set

    lateinit var voicePlayer: VoicePlayer
        private set

    /** Internal accessor for [com.miti99.loto.state.GameStorage]. */
    internal val lotoDataStoreInternal: DataStore<Preferences>
        get() = lotoDataStore

    override fun onCreate() {
        super.onCreate()
        val voiceIds = runCatching { VoiceManifest.load(this).map { it.id }.toSet() }
            .getOrDefault(setOf("hoai-my", "nam-minh"))

        settingsRepo = SettingsRepository(
            dataStore = lotoDataStore,
            appScope = appScope,
            validVoiceIds = voiceIds,
        )
        voicePlayer = VoicePlayer(
            appContext = applicationContext,
            voiceFlow = settingsRepo.voiceFlow,
            appScope = appScope,
        )
    }

    override fun onTerminate() {
        voicePlayer.release()
        super.onTerminate()
    }
}
