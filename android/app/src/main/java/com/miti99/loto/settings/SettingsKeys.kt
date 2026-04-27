package com.miti99.loto.settings

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

internal object SettingsKeys {
    val EMPTY_CELL_COLOR = stringPreferencesKey("empty_cell_color")
    val THEME = stringPreferencesKey("theme")
    val MODE = stringPreferencesKey("mode")
    val AUTO_CALL_ENABLED = booleanPreferencesKey("auto_call_enabled")
    val AUTO_CALL_SPEED = intPreferencesKey("auto_call_speed")
    val VOICE_ENABLED_MASTER = booleanPreferencesKey("voice_enabled_master")
    val VOICE_ENABLED_PLAYER = booleanPreferencesKey("voice_enabled_player")
    val VOICE_WAITING_NUMBER = booleanPreferencesKey("voice_waiting_number")
    val VOICE = stringPreferencesKey("voice")

    /** Legacy key from web `masterMode: true` → migrate to `mode = "both"`. */
    val LEGACY_MASTER_MODE = booleanPreferencesKey("master_mode")
}
