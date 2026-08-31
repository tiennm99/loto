package com.miti99.loto.state

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.miti99.loto.InMemoryDataStore
import com.miti99.loto.ThrowingDataStore
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
        // L3: called/remaining must partition the full 90-number deck, so
        // this fixture (unlike a handful of arbitrary numbers) reflects a
        // real mid-game snapshot.
        val called = listOf(5, 88, 12)
        val remaining = (1..90).filterNot { it in called }
        val state = MasterRoundState(called = called, remaining = remaining)
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

    @Test
    fun `duplicate number in the grid falls back to no saved card (L3)`() = runTest {
        val store = InMemoryDataStore()
        val repo = GameStateRepository(store)
        val nums = MutableList(81) { 0 }
        nums[0] = 5
        nums[1] = 5 // duplicate non-zero value — never legal in a real card
        store.edit { it[stringPreferencesKey("player_grid")] = nums.joinToString(",") }
        assertNull(repo.loadPlayerState())
    }

    @Test
    fun `duplicate manual unticks fall back to no unticks rather than silently deduping (L3)`() =
        runTest {
            val store = InMemoryDataStore()
            val repo = GameStateRepository(store)
            val grid = CardGenerator.generateGrid(Random(3))
            repo.savePlayerState(PlayerRoundState(grid, grid.map { r -> r.map { false } }, emptySet()))
            store.edit { it[stringPreferencesKey("player_manual_unticks")] = "7,7,42" }
            val restored = repo.loadPlayerState()
            assertEquals(grid, restored?.grid)
            assertEquals(emptySet<Int>(), restored?.manualUnticks)
        }

    @Test
    fun `shape-invalid master deck falls back to no saved round (L3)`() = runTest {
        val store = InMemoryDataStore()
        val repo = GameStateRepository(store)

        // Duplicate within one field (already covered by parseNumberList,
        // reconfirmed here at the loadMasterState level).
        store.edit {
            it[stringPreferencesKey("master_called")] = "1,1,2"
            it[stringPreferencesKey("master_remaining")] = (3..90).joinToString(",")
        }
        assertNull(repo.loadMasterState())

        // Overlap between called and remaining (5..10 appear in both).
        store.edit {
            it[stringPreferencesKey("master_called")] = (1..10).joinToString(",")
            it[stringPreferencesKey("master_remaining")] = (5..90).joinToString(",")
        }
        assertNull(repo.loadMasterState())

        // Valid individually, but doesn't add up to a full 90-number deck.
        store.edit {
            it[stringPreferencesKey("master_called")] = "1,2,3"
            it[stringPreferencesKey("master_remaining")] = "4,5,6"
        }
        assertNull(repo.loadMasterState())
    }

    @Test
    fun `IO failures on read fall back to empty state instead of throwing (L4)`() = runTest {
        val repo = GameStateRepository(ThrowingDataStore())
        assertNull(repo.loadPlayerState())
        assertNull(repo.loadMasterState())
    }

    @Test
    fun `IO failures on write are swallowed instead of throwing (L4)`() = runTest {
        val repo = GameStateRepository(ThrowingDataStore())
        val grid = CardGenerator.generateGrid(Random(4))
        // Must not throw even though every underlying write fails.
        repo.savePlayerState(PlayerRoundState(grid, grid.map { r -> r.map { false } }, emptySet()))
        repo.saveMasterState(MasterRoundState(called = emptyList(), remaining = (1..90).toList()))
    }
}
