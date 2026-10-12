package com.miti99.loto.settings

import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.miti99.loto.audio.Voice
import com.miti99.loto.audio.VoiceCatalog
import java.io.IOException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withTimeoutOrNull

/**
 * DataStore-backed settings store. Every field is validated independently on
 * read and falls back to its own default (the web's per-field `valid*` ??
 * default pattern) — a bad value never wholesale-resets the rest.
 *
 * @param voices the bundled voice manifest, parsed off the main thread. Its
 *   ids are the voice allowlist and its first entry the default voice;
 *   [settingsFlow] awaits it, so no settings snapshot is ever published
 *   from a not-yet-loaded manifest.
 * @param readTimeout how long the first DataStore read may take before
 *   [settingsFlow] publishes defaults instead, so consumers gated on
 *   "settings loaded" never wait forever on a hung store.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val voices: Deferred<List<Voice>>,
    private val readTimeout: Duration = 5.seconds,
) {

    private object Keys {
        val EMPTY_CELL_COLOR = stringPreferencesKey("emptyCellColor")
        val THEME = stringPreferencesKey("theme")
        val MODE = stringPreferencesKey("mode")
        val AUTO_CALL_ENABLED = booleanPreferencesKey("autoCallEnabled")
        val AUTO_CALL_SPEED = intPreferencesKey("autoCallSpeed")
        val VOICE_ENABLED_MASTER = booleanPreferencesKey("voiceEnabledMaster")
        val VOICE_ENABLED_PLAYER = booleanPreferencesKey("voiceEnabledPlayer")
        val VOICE_WAITING_NUMBER = booleanPreferencesKey("voiceWaitingNumber")
        val VOICE = stringPreferencesKey("voice")
        val BOARD_TEXT_SCALE = floatPreferencesKey("boardTextScale")
    }

    /**
     * Placeholder for the window before [settingsFlow]'s first emission.
     * Must not wait for the manifest, so the voice comes from
     * [VoiceCatalog.FALLBACK_VOICES], whose first entry matches the shipped
     * manifest's (pinned by `VoiceCatalogTest`). Loaded settings always
     * carry the real manifest-derived default.
     */
    val defaults: Settings = Settings(voice = VoiceCatalog.defaultVoiceId(VoiceCatalog.FALLBACK_VOICES))

    // Every read failure — IOException (full disk, revoked storage
    // permission) or anything else — falls back to defaults instead of
    // failing the app-scoped stateIn collector (the web swallows every
    // localStorage read error). M4: an earlier version only swallowed
    // IOException and re-threw everything else, which cancelled every
    // collector of this flow forever on a non-IO failure — "settings never
    // save" (acceptable, L4) had regressed into "settings never load and
    // auto-cross is dead for the rest of the process" (not acceptable). A
    // broken settings store must degrade to defaults, not to a dead board.
    private val persistedPrefs: Flow<Preferences> = dataStore.data
        .catch {
            Log.w(TAG, "Failed to read settings; falling back to defaults", it)
            emit(emptyPreferences())
        }

    // A first read that hangs (rather than fails) publishes defaults once,
    // then keeps following the store so the real values still land if the
    // read eventually resolves.
    val settingsFlow: Flow<Settings> = flow {
        if (withTimeoutOrNull(readTimeout) { persistedPrefs.first() } == null) {
            Log.w(TAG, "Settings read timed out after $readTimeout; using defaults")
            emit(toSettings(emptyPreferences(), voices.await()))
        }
        emitAll(persistedPrefs.map { prefs -> toSettings(prefs, voices.await()) })
    }

    /**
     * True once [settingsFlow] has produced its first value — either the
     * real persisted read, the [catch] fallback after a read failure, or the
     * defaults published when the first read times out.
     * False only for the brief startup window before that read lands (M4
     * residual): [LotoApplication]'s `settingsOrNull` is
     * `stateIn(..., SharingStarted.Eagerly, null)`, so its *initial* value
     * cannot be confused with a real `mode = PLAYER` — a consumer that
     * needs to tell "not loaded yet" apart from "loaded and actually
     * PLAYER" (e.g. gating a master-history replay on the real mode) reads
     * that instead. This property is kept for callers that only need a
     * plain boolean signal (e.g. tests) rather than the settings snapshot.
     */
    val loaded: Flow<Boolean> = settingsFlow.map { true }.onStart { emit(false) }

    private fun toSettings(prefs: Preferences, voices: List<Voice>): Settings = Settings(
        emptyCellColor = prefs[Keys.EMPTY_CELL_COLOR]
            ?.takeIf { Settings.HEX6.matches(it) }
            ?: Settings.DEFAULT_EMPTY_CELL_COLOR,
        theme = ThemeSetting.fromStorage(prefs[Keys.THEME]) ?: ThemeSetting.AUTO,
        mode = AppMode.fromStorage(prefs[Keys.MODE]) ?: AppMode.PLAYER,
        autoCallEnabled = prefs[Keys.AUTO_CALL_ENABLED] ?: false,
        autoCallSpeed = prefs[Keys.AUTO_CALL_SPEED]
            ?.takeIf { it in Settings.AUTO_CALL_SPEED_RANGE }
            ?: Settings.DEFAULT_AUTO_CALL_SPEED,
        voiceEnabledMaster = prefs[Keys.VOICE_ENABLED_MASTER] ?: true,
        voiceEnabledPlayer = prefs[Keys.VOICE_ENABLED_PLAYER] ?: false,
        voiceWaitingNumber = prefs[Keys.VOICE_WAITING_NUMBER] ?: false,
        voice = prefs[Keys.VOICE]
            ?.takeIf { id -> voices.any { it.id == id } }
            ?: VoiceCatalog.defaultVoiceId(voices),
        boardTextScale = prefs[Keys.BOARD_TEXT_SCALE]
            ?.takeIf { scale -> Settings.BOARD_TEXT_SCALES.any { it == scale } }
            ?: Settings.DEFAULT_BOARD_TEXT_SCALE,
    )

    suspend fun setEmptyCellColor(value: String) =
        write { it[Keys.EMPTY_CELL_COLOR] = value }

    suspend fun setTheme(value: ThemeSetting) =
        write { it[Keys.THEME] = value.storageValue }

    suspend fun setMode(value: AppMode) =
        write { it[Keys.MODE] = value.storageValue }

    suspend fun setAutoCallEnabled(value: Boolean) =
        write { it[Keys.AUTO_CALL_ENABLED] = value }

    suspend fun setAutoCallSpeed(value: Int) =
        write { it[Keys.AUTO_CALL_SPEED] = value }

    suspend fun setVoiceEnabledMaster(value: Boolean) =
        write { it[Keys.VOICE_ENABLED_MASTER] = value }

    suspend fun setVoiceEnabledPlayer(value: Boolean) =
        write { it[Keys.VOICE_ENABLED_PLAYER] = value }

    suspend fun setVoiceWaitingNumber(value: Boolean) =
        write { it[Keys.VOICE_WAITING_NUMBER] = value }

    suspend fun setVoice(value: String) =
        write { it[Keys.VOICE] = value }

    suspend fun setBoardTextScale(value: Float) =
        write { it[Keys.BOARD_TEXT_SCALE] = value }

    /** Return every setting to its default (and persist that). */
    suspend fun reset() = write { it.clear() }

    /** Write failures are swallowed — the app keeps working in-memory. */
    private suspend fun write(block: (MutablePreferences) -> Unit) {
        try {
            dataStore.edit(block)
        } catch (e: IOException) {
            Log.w(TAG, "Failed to persist settings; change was not saved", e)
        }
    }

    private companion object {
        const val TAG = "SettingsRepository"
    }
}
