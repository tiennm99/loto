package com.miti99.loto.settings

/** UI settings state. Mirrors web `settings-store.svelte.js:16-35`. */
data class SettingsState(
    val emptyCellColor: String,
    val theme: Theme,
    val mode: Mode,
    val autoCallEnabled: Boolean,
    val autoCallSpeed: Int,
    val voiceEnabledMaster: Boolean,
    val voiceEnabledPlayer: Boolean,
    val voiceWaitingNumber: Boolean,
    val voice: String,
) {
    enum class Theme { AUTO, LIGHT, DARK }
    enum class Mode { PLAYER, MASTER, BOTH }

    companion object {
        const val DEFAULT_VOICE = "hoai-my"
        const val DEFAULT_EMPTY_CELL_COLOR = "#7030A0"

        val DEFAULT = SettingsState(
            emptyCellColor = DEFAULT_EMPTY_CELL_COLOR,
            theme = Theme.AUTO,
            mode = Mode.PLAYER,
            autoCallEnabled = false,
            autoCallSpeed = 5,
            voiceEnabledMaster = true,
            voiceEnabledPlayer = false,
            voiceWaitingNumber = false,
            voice = DEFAULT_VOICE,
        )
    }
}
