package com.miti99.loto.settings

/**
 * Global UI settings, mirroring the web app's `DEFAULT_SETTINGS` contract in
 * `web/src/lib/settings-store.svelte.js` — same fields, defaults, and
 * validation rules. (The web's legacy `masterMode` migration is web-only;
 * the native store starts clean.)
 */
data class Settings(
    /** Hex6 background of empty board cells. Default: Excel "Standard Color: Purple". */
    val emptyCellColor: String = DEFAULT_EMPTY_CELL_COLOR,
    /** AUTO follows the OS dark-mode preference; LIGHT/DARK override it. */
    val theme: ThemeSetting = ThemeSetting.AUTO,
    /** Which panels are visible: player only, master only, or both inline. */
    val mode: AppMode = AppMode.PLAYER,
    /** When true, the master "Xổ số" button becomes "Bắt đầu/Dừng" + auto interval. */
    val autoCallEnabled: Boolean = false,
    /** Auto-call interval, seconds per number. Integer 1..10. */
    val autoCallSpeed: Int = DEFAULT_AUTO_CALL_SPEED,
    /** Speak the called number aloud when master draws. */
    val voiceEnabledMaster: Boolean = true,
    /** Speak "Chờ" / "Kinh" on player events. */
    val voiceEnabledPlayer: Boolean = false,
    /** When voiceEnabledPlayer is on, also speak the awaited number after "Chờ". */
    val voiceWaitingNumber: Boolean = false,
    /** Active voice id; matches an entry in the audio manifest. */
    val voice: String,
    /** Multiplier on board number size; one of [BOARD_TEXT_SCALES]. */
    val boardTextScale: Float = DEFAULT_BOARD_TEXT_SCALE,
) {
    companion object {
        const val DEFAULT_EMPTY_CELL_COLOR = "#7030A0"
        const val DEFAULT_AUTO_CALL_SPEED = 5
        const val DEFAULT_BOARD_TEXT_SCALE = 1f
        val AUTO_CALL_SPEED_RANGE = 1..10

        /**
         * Board number sizes, as a multiplier on the base size. The rungs
         * need to reach genuinely large — this replaces the wrapper-era
         * textZoom pin as the in-app size control.
         */
        val BOARD_TEXT_SCALES = listOf(0.9f, 1f, 1.15f, 1.3f)

        val HEX6 = Regex("^#[0-9a-fA-F]{6}$")
    }
}

/** Storage values match the web strings ("auto" | "light" | "dark"). */
enum class ThemeSetting(val storageValue: String) {
    AUTO("auto"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemeSetting? =
            entries.firstOrNull { it.storageValue == value }
    }
}

/** Storage values match the web strings ("player" | "master" | "both"). */
enum class AppMode(val storageValue: String) {
    PLAYER("player"),
    MASTER("master"),
    BOTH("both");

    companion object {
        fun fromStorage(value: String?): AppMode? =
            entries.firstOrNull { it.storageValue == value }
    }
}
