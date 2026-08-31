package com.miti99.loto.state

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.miti99.loto.game.CardGenerator
import java.io.IOException
import kotlinx.coroutines.flow.first

/** The player's persisted round: card, marks, and explicit unticks. */
data class PlayerRoundState(
    val grid: List<List<Int>>,
    val crossed: List<List<Boolean>>,
    val manualUnticks: Set<Int>,
)

/** The master's persisted round: draw history and the rest of the deck. */
data class MasterRoundState(
    val called: List<Int>,
    val remaining: List<Int>,
)

/**
 * Round-state persistence so a process death or reboot restores the exact
 * game — the native counterpart of the web's `loto_*`/`loto_master`
 * localStorage keys. Matrices are stored as compact CSV/bitmask strings.
 * Both failure classes fall back instead of crashing (the web's
 * try/catch-everything localStorage posture): malformed payloads
 * shape-validate to "no saved round", and IO errors are swallowed on write
 * and read as empty state on load.
 */
class GameStateRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val PLAYER_GRID = stringPreferencesKey("player_grid")
        val PLAYER_CROSSED = stringPreferencesKey("player_crossed")
        val PLAYER_MANUAL_UNTICKS = stringPreferencesKey("player_manual_unticks")
        val MASTER_CALLED = stringPreferencesKey("master_called")
        val MASTER_REMAINING = stringPreferencesKey("master_remaining")
    }

    private val cells = CardGenerator.NUM_ROWS * CardGenerator.NUM_COLS

    suspend fun loadPlayerState(): PlayerRoundState? {
        val prefs = readPrefs()
        val grid = parseGrid(prefs[Keys.PLAYER_GRID]) ?: return null
        val crossed = parseCrossed(prefs[Keys.PLAYER_CROSSED])
            ?: grid.map { row -> row.map { false } }
        val unticks = parseNumberList(prefs[Keys.PLAYER_MANUAL_UNTICKS])?.toSet() ?: emptySet()
        return PlayerRoundState(grid, crossed, unticks)
    }

    suspend fun savePlayerState(state: PlayerRoundState) {
        write { prefs ->
            prefs[Keys.PLAYER_GRID] = state.grid.flatten().joinToString(",")
            prefs[Keys.PLAYER_CROSSED] =
                state.crossed.flatten().joinToString("") { if (it) "1" else "0" }
            prefs[Keys.PLAYER_MANUAL_UNTICKS] = state.manualUnticks.sorted().joinToString(",")
        }
    }

    suspend fun loadMasterState(): MasterRoundState? {
        val prefs = readPrefs()
        val called = parseNumberList(prefs[Keys.MASTER_CALLED]) ?: return null
        val remaining = parseNumberList(prefs[Keys.MASTER_REMAINING]) ?: return null
        return MasterRoundState(called, remaining)
    }

    suspend fun saveMasterState(state: MasterRoundState) {
        write { prefs ->
            prefs[Keys.MASTER_CALLED] = state.called.joinToString(",")
            prefs[Keys.MASTER_REMAINING] = state.remaining.joinToString(",")
        }
    }

    private suspend fun readPrefs(): Preferences = try {
        dataStore.data.first()
    } catch (_: IOException) {
        emptyPreferences()
    }

    private suspend fun write(block: (MutablePreferences) -> Unit) {
        try {
            dataStore.edit(block)
        } catch (_: IOException) {
        }
    }

    /** 81 comma-separated ints, row-major; cell values 0 (empty) or 1..90. */
    private fun parseGrid(raw: String?): List<List<Int>>? {
        if (raw.isNullOrEmpty()) return null
        val nums = raw.split(",").map { it.toIntOrNull() ?: return null }
        if (nums.size != cells || nums.any { it !in 0..90 }) return null
        return nums.chunked(CardGenerator.NUM_COLS)
    }

    /** 81-char row-major bitmask of '0'/'1'. */
    private fun parseCrossed(raw: String?): List<List<Boolean>>? {
        if (raw == null || raw.length != cells || raw.any { it != '0' && it != '1' }) return null
        return raw.map { it == '1' }.chunked(CardGenerator.NUM_COLS)
    }

    /** Comma-separated ints, each in 1..90. Empty string = empty list. */
    private fun parseNumberList(raw: String?): List<Int>? {
        if (raw == null) return null
        if (raw.isEmpty()) return emptyList()
        val nums = raw.split(",").map { it.toIntOrNull() ?: return null }
        if (nums.any { it !in 1..90 }) return null
        return nums
    }
}
