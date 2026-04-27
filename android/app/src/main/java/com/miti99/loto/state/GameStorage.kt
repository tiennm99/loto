package com.miti99.loto.state

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * DataStore-backed persistence for player grid/crossed and master deck.
 * Single shared DataStore (settings file) — JSON-encoded under string keys.
 */
class GameStorage(private val dataStore: DataStore<Preferences>) {

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun savePlayerGrid(grid: List<List<Int>>) =
        dataStore.edit { it[KEY_PLAYER_GRID] = json.encodeToString(grid) }

    suspend fun savePlayerCrossed(crossed: List<List<Boolean>>) =
        dataStore.edit { it[KEY_PLAYER_CROSSED] = json.encodeToString(crossed) }

    suspend fun loadPlayer(): Pair<List<List<Int>>, List<List<Boolean>>>? {
        val prefs = dataStore.data.first()
        val gridStr = prefs[KEY_PLAYER_GRID] ?: return null
        val crossedStr = prefs[KEY_PLAYER_CROSSED]
        val grid = runCatching { json.decodeFromString<List<List<Int>>>(gridStr) }
            .getOrNull()?.takeIf { it.size == 9 && it.all { row -> row.size == 9 } }
            ?: return null
        val crossed = crossedStr?.let {
            runCatching { json.decodeFromString<List<List<Boolean>>>(it) }
                .getOrNull()?.takeIf { c -> c.size == 9 && c.all { row -> row.size == 9 } }
        } ?: List(9) { List(9) { false } }
        return grid to crossed
    }

    suspend fun saveMasterDeck(deck: DeckState) =
        dataStore.edit { it[KEY_MASTER_DECK] = json.encodeToString(deck) }

    suspend fun loadMasterDeck(): DeckState? {
        val prefs = dataStore.data.first()
        val str = prefs[KEY_MASTER_DECK] ?: return null
        return runCatching { json.decodeFromString<DeckState>(str) }.getOrNull()
    }

    suspend fun clearAll() = dataStore.edit {
        it.remove(KEY_PLAYER_GRID)
        it.remove(KEY_PLAYER_CROSSED)
        it.remove(KEY_MASTER_DECK)
    }

    private companion object {
        val KEY_PLAYER_GRID = stringPreferencesKey("player_grid_json")
        val KEY_PLAYER_CROSSED = stringPreferencesKey("player_crossed_json")
        val KEY_MASTER_DECK = stringPreferencesKey("master_deck_json")
    }
}
