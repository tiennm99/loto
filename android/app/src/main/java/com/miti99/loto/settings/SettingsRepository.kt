package com.miti99.loto.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

/**
 * DataStore-backed settings store. Every field is validated independently on
 * read and falls back to its own default (the web's per-field `valid*` ??
 * default pattern) — a bad value never wholesale-resets the rest.
 *
 * @param validVoiceIds allowlist from the audio manifest
 * @param defaultVoiceId first manifest entry
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val validVoiceIds: Set<String>,
    private val defaultVoiceId: String,
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

    /** Defaults with the manifest-derived voice filled in. */
    val defaults: Settings = Settings(voice = defaultVoiceId)

    // IO failures fall back to defaults instead of failing the app-scoped
    // stateIn collector (the web swallows every localStorage read error).
    val settingsFlow: Flow<Settings> = dataStore.data
        .catch { if (it is IOException) emit(emptyPreferences()) else throw it }
        .map { prefs -> toSettings(prefs) }

    private fun toSettings(prefs: Preferences): Settings = Settings(
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
            ?.takeIf { validVoiceIds.contains(it) }
            ?: defaultVoiceId,
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
        } catch (_: IOException) {
        }
    }
}
