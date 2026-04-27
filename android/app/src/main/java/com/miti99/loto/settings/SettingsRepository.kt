package com.miti99.loto.settings

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.miti99.loto.settings.SettingsKeys as K
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * DataStore-backed settings. Reads validate per-field; invalid values fall
 * back to defaults (never crashes, matches web `settings-store.svelte.js`).
 *
 * The legacy `masterMode: true` web key is migrated to `mode = "both"` once
 * via [legacyMasterModeMigration] — see [LotoApp] DataStore creation.
 */
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    appScope: CoroutineScope,
    private val validVoiceIds: Set<String> = DEFAULT_VOICE_IDS,
) {

    val flow: Flow<SettingsState> = dataStore.data.map(::mapToState).distinctUntilChanged()

    /** Hot voice id flow consumed by VoicePlayer. */
    val voiceFlow: StateFlow<String> = flow
        .map { it.voice }
        .stateIn(appScope, SharingStarted.Eagerly, SettingsState.DEFAULT.voice)

    suspend fun setEmptyCellColor(hex: String) {
        if (!HEX6.matches(hex)) return
        dataStore.edit { it[K.EMPTY_CELL_COLOR] = hex }
    }

    suspend fun setTheme(theme: SettingsState.Theme) =
        dataStore.edit { it[K.THEME] = theme.storageKey }

    suspend fun setMode(mode: SettingsState.Mode) =
        dataStore.edit { it[K.MODE] = mode.storageKey }

    suspend fun setAutoCallEnabled(enabled: Boolean) =
        dataStore.edit { it[K.AUTO_CALL_ENABLED] = enabled }

    suspend fun setAutoCallSpeed(s: Int) {
        val v = s.coerceIn(1, 10)
        dataStore.edit { it[K.AUTO_CALL_SPEED] = v }
    }

    suspend fun setVoiceEnabledMaster(b: Boolean) =
        dataStore.edit { it[K.VOICE_ENABLED_MASTER] = b }

    suspend fun setVoiceEnabledPlayer(b: Boolean) =
        dataStore.edit { it[K.VOICE_ENABLED_PLAYER] = b }

    suspend fun setVoiceWaitingNumber(b: Boolean) =
        dataStore.edit { it[K.VOICE_WAITING_NUMBER] = b }

    suspend fun setVoice(id: String) {
        if (id !in validVoiceIds) return
        dataStore.edit { it[K.VOICE] = id }
    }

    suspend fun reset() {
        dataStore.edit { it.clear() }
    }

    private fun mapToState(prefs: Preferences): SettingsState {
        val d = SettingsState.DEFAULT
        return SettingsState(
            emptyCellColor = prefs[K.EMPTY_CELL_COLOR]
                ?.takeIf { HEX6.matches(it) } ?: d.emptyCellColor,
            theme = prefs[K.THEME]?.let(::themeFromStorage) ?: d.theme,
            mode = prefs[K.MODE]?.let(::modeFromStorage) ?: d.mode,
            autoCallEnabled = prefs[K.AUTO_CALL_ENABLED] ?: d.autoCallEnabled,
            autoCallSpeed = prefs[K.AUTO_CALL_SPEED]?.takeIf { it in 1..10 } ?: d.autoCallSpeed,
            voiceEnabledMaster = prefs[K.VOICE_ENABLED_MASTER] ?: d.voiceEnabledMaster,
            voiceEnabledPlayer = prefs[K.VOICE_ENABLED_PLAYER] ?: d.voiceEnabledPlayer,
            voiceWaitingNumber = prefs[K.VOICE_WAITING_NUMBER] ?: d.voiceWaitingNumber,
            voice = prefs[K.VOICE]?.takeIf { it in validVoiceIds } ?: d.voice,
        )
    }

    companion object {
        private val HEX6 = Regex("^#[0-9a-fA-F]{6}$")
        private val DEFAULT_VOICE_IDS = setOf("hoai-my", "nam-minh")

        /**
         * Legacy migration: web app stored `masterMode: true` for "both" mode.
         * Translate once and forget.
         */
        val legacyMasterModeMigration: DataMigration<Preferences> =
            object : DataMigration<Preferences> {
                override suspend fun shouldMigrate(currentData: Preferences): Boolean =
                    currentData[K.MODE] == null && currentData[K.LEGACY_MASTER_MODE] == true

                override suspend fun migrate(currentData: Preferences): Preferences {
                    val mut: MutablePreferences = currentData.toMutablePreferences()
                    mut[K.MODE] = SettingsState.Mode.BOTH.storageKey
                    mut.remove(K.LEGACY_MASTER_MODE)
                    return mut
                }

                override suspend fun cleanUp() = Unit
            }
    }
}

private val SettingsState.Theme.storageKey: String
    get() = when (this) {
        SettingsState.Theme.AUTO -> "auto"
        SettingsState.Theme.LIGHT -> "light"
        SettingsState.Theme.DARK -> "dark"
    }

private val SettingsState.Mode.storageKey: String
    get() = when (this) {
        SettingsState.Mode.PLAYER -> "player"
        SettingsState.Mode.MASTER -> "master"
        SettingsState.Mode.BOTH -> "both"
    }

private fun themeFromStorage(s: String): SettingsState.Theme? = when (s) {
    "auto" -> SettingsState.Theme.AUTO
    "light" -> SettingsState.Theme.LIGHT
    "dark" -> SettingsState.Theme.DARK
    else -> null
}

private fun modeFromStorage(s: String): SettingsState.Mode? = when (s) {
    "player" -> SettingsState.Mode.PLAYER
    "master" -> SettingsState.Mode.MASTER
    "both" -> SettingsState.Mode.BOTH
    else -> null
}
