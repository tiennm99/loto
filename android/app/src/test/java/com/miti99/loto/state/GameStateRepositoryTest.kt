package com.miti99.loto.state

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.miti99.loto.InMemoryDataStore
import com.miti99.loto.game.CardGenerator
import kotlin.random.Random
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameStateRepositoryTest {

    @Test
    fun `returns null when nothing is stored`() = runTest {
        val repo = GameStateRepository(InMemoryDataStore())
        assertNull(repo.loadPlayerState())
        assertNull(repo.loadMasterState())
    }

    @Test
    fun `player state survives a repository recreation (process death)`() = runTest {
        val store = InMemoryDataStore()
        val grid = CardGenerator.generateGrid(Random(1))
        val crossed = grid.mapIndexed { r, row -> row.mapIndexed { c, _ -> (r + c) % 3 == 0 } }
        val state = PlayerRoundState(grid, crossed, manualUnticks = setOf(7, 42, 88))

        GameStateRepository(store).savePlayerState(state)
        // Fresh repository over the same store = process restart.
        assertEquals(state, GameStateRepository(store).loadPlayerState())
    }

    @Test
    fun `master state survives a repository recreation`() = runTest {
        val store = InMemoryDataStore()
        val state = MasterRoundState(called = listOf(5, 88, 12), remaining = listOf(1, 2, 3))
        GameStateRepository(store).saveMasterState(state)
        assertEquals(state, GameStateRepository(store).loadMasterState())
    }

    @Test
    fun `empty called and remaining lists round-trip (exhausted deck)`() = runTest {
        val repo = GameStateRepository(InMemoryDataStore())
        val state = MasterRoundState(called = (1..90).toList(), remaining = emptyList())
        repo.saveMasterState(state)
        assertEquals(state, repo.loadMasterState())
    }

    @Test
    fun `corrupt payloads fall back to no saved round, never crash`() = runTest {
        val store = InMemoryDataStore()
        val repo = GameStateRepository(store)

        val corruptGrids = listOf(
            "1,2,junk",             // non-numeric
            "1,2,3",                // wrong cell count
            List(81) { "91" }.joinToString(","), // out of range
        )
        for (bad in corruptGrids) {
            store.edit { it[stringPreferencesKey("player_grid")] = bad }
            assertNull("grid payload: $bad", repo.loadPlayerState())
        }

        store.edit { it[stringPreferencesKey("master_called")] = "1,0,5" } // 0 out of range
        assertNull(repo.loadMasterState())
    }

    @Test
    fun `corrupt crossed bitmask falls back to an uncrossed board`() = runTest {
        val store = InMemoryDataStore()
        val repo = GameStateRepository(store)
        val grid = CardGenerator.generateGrid(Random(2))
        repo.savePlayerState(PlayerRoundState(grid, grid.map { r -> r.map { true } }, emptySet()))
        store.edit { it[stringPreferencesKey("player_crossed")] = "10xx01" } // wrong length + charset
        val restored = repo.loadPlayerState()
        assertEquals(grid, restored?.grid)
        assertEquals(grid.map { r -> r.map { false } }, restored?.crossed)
    }
}
