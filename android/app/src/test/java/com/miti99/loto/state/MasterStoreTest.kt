package com.miti99.loto.state

import com.miti99.loto.InMemoryDataStore
import com.miti99.loto.SlowDataStore
import com.miti99.loto.game.DrawDeck
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Covers H1 on the master side: `MasterStore.restore()` suspends on the
 * DataStore read the same way `PlayerBoardViewModel.restore()` does, and
 * "Ván mới" (`startNewGame()`) can land while that read is still in flight.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MasterStoreTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loading starts true and flips false once restore settles`() = runTest(dispatcher) {
        val store = MasterStore(GameStateRepository(InMemoryDataStore()), backgroundScope, DrawDeck(Random(1)))
        assertTrue(store.loading.value)
        store.restore()
        assertFalse(store.loading.value)
    }

    @Test
    fun `restore does not clobber a new game started while the load is still in flight`() =
        runTest(dispatcher) {
            // Seed a previously-saved round the same way a prior app run would.
            val backing = InMemoryDataStore()
            val seedRepo = GameStateRepository(backing)
            seedRepo.saveMasterState(
                MasterRoundState(called = (1..10).toList(), remaining = (11..90).toList()),
            )
            runCurrent()

            val gate = CompletableDeferred<Unit>()
            val repository = GameStateRepository(SlowDataStore(backing, gate))
            val store = MasterStore(repository, backgroundScope, DrawDeck(Random(7)))

            val restoreJob = launch { store.restore() }
            runCurrent() // restore() is now suspended on gate.await()
            assertTrue(store.loading.value)
            assertEquals(emptyList<Int>(), store.state.value.called)

            // "Ván mới" lands while the restore is still in flight (the same
            // race PlayerBoardScreen's generate button has, gated by
            // MasterPanelViewModel.loading in production).
            store.startNewGame()
            runCurrent()
            assertEquals(emptyList<Int>(), store.state.value.called)
            assertEquals(90, store.state.value.remaining.size)

            // Let the stale restore resume and resolve.
            gate.complete(Unit)
            restoreJob.join()
            runCurrent()

            // The new game must survive — restore must not overwrite it with
            // the stale persisted round (H1).
            assertEquals(emptyList<Int>(), store.state.value.called)
            assertEquals(90, store.state.value.remaining.size)
            assertFalse(store.loading.value)
        }
}
