package com.miti99.loto.state

import com.miti99.loto.InMemoryDataStore
import com.miti99.loto.SlowDataStore
import com.miti99.loto.audio.FakeVoicePlayer
import com.miti99.loto.game.CardGenerator
import com.miti99.loto.settings.AppMode
import com.miti99.loto.settings.Settings
import com.miti99.loto.settings.SettingsRepository
import kotlin.random.Random
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PlayerBoardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private class Env(
        val repository: GameStateRepository,
        val masterStore: MasterStore,
        val settings: MutableStateFlow<Settings>,
        val voice: FakeVoicePlayer,
        val viewModel: PlayerBoardViewModel,
    )

    private fun TestScope.env(initial: Settings): Env {
        val repository = GameStateRepository(InMemoryDataStore())
        val masterStore =
            MasterStore(repository, backgroundScope, com.miti99.loto.game.DrawDeck(Random(7)))
        val settings = MutableStateFlow(initial)
        val voice = FakeVoicePlayer()
        // Reuse `dispatcher` for generate()'s compute step too (M5), so it
        // shares the virtual clock `runCurrent()`/`advanceUntilIdle()`
        // drive instead of racing a real background thread.
        val viewModel =
            PlayerBoardViewModel(repository, masterStore, settings, voice, dispatcher)
        runCurrent() // run restore + start the master collector
        return Env(repository, masterStore, settings, voice, viewModel)
    }

    private val playerVoiceOn = Settings(voice = "hoai-my", voiceEnabledPlayer = true)

    /** Row-0 cells (col to number) of the current grid. */
    private fun rowCells(env: Env, row: Int): List<Pair<Int, Int>> {
        val grid = env.viewModel.uiState.value.grid!!
        return grid[row].withIndex().filter { it.value > 0 }.map { it.index to it.value }
    }

    @Test
    fun `generate publishes a fresh 9x9 grid with no marks`() = runTest(dispatcher) {
        val env = env(playerVoiceOn)
        env.viewModel.generate()
        runCurrent()
        val state = env.viewModel.uiState.value
        assertEquals(9, state.grid?.size)
        assertTrue(state.crossed.flatten().none { it })
        assertEquals(List(9) { false }, state.rowComplete)
    }

    @Test
    fun `fourth mark in a row raises the chờ toast and announces it`() = runTest(dispatcher) {
        val env = env(playerVoiceOn)
        env.viewModel.generate()
        runCurrent()
        val cells = rowCells(env, 0)
        for ((col, _) in cells.take(4)) {
            env.viewModel.onCellClick(0, col)
        }
        runCurrent()
        val waitingNumber = cells[4].second
        assertEquals(waitingNumber, env.viewModel.uiState.value.waitingNumber)
        // voiceWaitingNumber off → only the "cho" clip.
        assertEquals(listOf("cho"), env.voice.utterances.last())
        // The awaited cell pulses and its section rings.
        assertTrue(
            env.viewModel.uiState.value.waitingCells
                .contains(com.miti99.loto.game.PlayerCard.Cell(0, cells[4].first)),
        )
        assertTrue(env.viewModel.uiState.value.sectionWaiting[0])
    }

    @Test
    fun `toast auto-dismisses after five seconds`() = runTest(dispatcher) {
        val env = env(playerVoiceOn)
        env.viewModel.generate()
        runCurrent()
        rowCells(env, 0).take(4).forEach { (col, _) -> env.viewModel.onCellClick(0, col) }
        runCurrent()
        assertNotNull(env.viewModel.uiState.value.waitingNumber)
        advanceTimeBy(5001)
        assertNull(env.viewModel.uiState.value.waitingNumber)
    }

    @Test
    fun `completing a row shows the Kinh modal and announces kinh`() = runTest(dispatcher) {
        val env = env(playerVoiceOn)
        env.viewModel.generate()
        runCurrent()
        rowCells(env, 0).forEach { (col, _) -> env.viewModel.onCellClick(0, col) }
        runCurrent()
        val state = env.viewModel.uiState.value
        assertTrue(state.showCongrats)
        assertEquals(1, state.congratsRow)
        assertEquals(listOf("kinh"), env.voice.utterances.last())
        assertTrue(state.rowComplete[0])
    }

    @Test
    fun `no announcements when voice flags are off, toast still shows`() = runTest(dispatcher) {
        val env = env(Settings(voice = "hoai-my")) // player voice off, mode player
        env.viewModel.generate()
        runCurrent()
        rowCells(env, 0).take(4).forEach { (col, _) -> env.viewModel.onCellClick(0, col) }
        runCurrent()
        assertNotNull(env.viewModel.uiState.value.waitingNumber)
        assertEquals(0, env.voice.utterances.size)
    }

    @Test
    fun `master draws auto-cross the whole board in both mode`() = runTest(dispatcher) {
        val env = env(playerVoiceOn.copy(mode = AppMode.BOTH))
        env.viewModel.generate()
        runCurrent()
        env.masterStore.startNewGame()
        repeat(90) { env.masterStore.drawNext() }
        runCurrent()
        val state = env.viewModel.uiState.value
        val grid = state.grid!!
        for (r in grid.indices) {
            for (c in grid[r].indices) {
                if (grid[r][c] > 0) {
                    assertTrue("cell $r,$c should be crossed", state.crossed[r][c])
                }
            }
        }
        assertEquals(List(9) { true }, state.rowComplete)
    }

    @Test
    fun `master Ván mới clears player marks in both mode`() = runTest(dispatcher) {
        val env = env(playerVoiceOn.copy(mode = AppMode.BOTH))
        env.viewModel.generate()
        runCurrent()
        env.masterStore.startNewGame()
        repeat(30) { env.masterStore.drawNext() }
        runCurrent()
        assertTrue(env.viewModel.uiState.value.crossed.flatten().any { it })

        env.masterStore.startNewGame() // called goes >0 → 0
        runCurrent()
        assertTrue(env.viewModel.uiState.value.crossed.flatten().none { it })
    }

    @Test
    fun `mode=player ignores master draws`() = runTest(dispatcher) {
        val env = env(playerVoiceOn) // mode defaults to PLAYER
        env.viewModel.generate()
        runCurrent()
        env.masterStore.startNewGame()
        repeat(90) { env.masterStore.drawNext() }
        runCurrent()
        assertTrue(env.viewModel.uiState.value.crossed.flatten().none { it })
    }

    @Test
    fun `kill and restore brings the exact round back`() = runTest(dispatcher) {
        val env = env(playerVoiceOn)
        env.viewModel.generate()
        runCurrent()
        val cells = rowCells(env, 0)
        cells.take(3).forEach { (col, _) -> env.viewModel.onCellClick(0, col) }
        advanceUntilIdle() // let persistence writes land

        val before = env.viewModel.uiState.value
        // Second ViewModel over the same repository = process restart.
        val restored = PlayerBoardViewModel(env.repository, env.masterStore, env.settings, env.voice)
        advanceUntilIdle()
        assertEquals(before.grid, restored.uiState.value.grid)
        assertEquals(before.crossed, restored.uiState.value.crossed)
    }

    @Test
    fun `restore does not re-announce an already-waiting row`() = runTest(dispatcher) {
        val env = env(playerVoiceOn)
        env.viewModel.generate()
        runCurrent()
        rowCells(env, 0).take(4).forEach { (col, _) -> env.viewModel.onCellClick(0, col) }
        advanceUntilIdle()
        val announced = env.voice.utterances.size

        val restored = PlayerBoardViewModel(env.repository, env.masterStore, env.settings, env.voice)
        advanceUntilIdle()
        assertEquals(announced, env.voice.utterances.size)
        assertNull(restored.uiState.value.waitingNumber)
    }

    @Test
    fun `manually unticked called number stays unticked across restore-replay`() =
        runTest(dispatcher) {
            val env = env(playerVoiceOn.copy(mode = AppMode.BOTH))
            env.viewModel.generate()
            runCurrent()
            env.masterStore.startNewGame()
            repeat(90) { env.masterStore.drawNext() } // everything crossed
            runCurrent()
            val grid = env.viewModel.uiState.value.grid!!
            val num = grid[0].first { it > 0 }
            val col = grid[0].indexOf(num)

            env.viewModel.onCellClick(0, col) // manual untick of a called number
            advanceUntilIdle()
            assertFalse(env.viewModel.uiState.value.crossed[0][col])

            // Process restart replays the full call history from cursor 0 —
            // the persisted untick must suppress the re-cross…
            val restored =
                PlayerBoardViewModel(env.repository, env.masterStore, env.settings, env.voice)
            advanceUntilIdle()
            assertFalse(restored.uiState.value.crossed[0][col])
            // …while every other number stays crossed.
            val others = grid.flatten().count { it > 0 } - 1
            assertEquals(others, restored.uiState.value.crossed.flatten().count { it })
        }

    @Test
    fun `clearMarks replay that completes a row fires Kinh immediately`() =
        runTest(dispatcher) {
            val env = env(playerVoiceOn.copy(mode = AppMode.BOTH))
            env.viewModel.generate()
            runCurrent()
            env.masterStore.startNewGame()
            repeat(90) { env.masterStore.drawNext() } // board fully crossed
            runCurrent()
            env.viewModel.dismissCongrats()
            val announcedBefore = env.voice.utterances.size

            // The clear-replay instantly re-completes every row — the Kinh
            // modal and clip must fire from the replay itself, not wait for
            // a next draw that may never come.
            env.viewModel.clearMarks()
            runCurrent()
            assertTrue(env.viewModel.uiState.value.showCongrats)
            assertEquals(listOf("kinh"), env.voice.utterances.last())
            assertTrue(env.voice.utterances.size > announcedBefore)
        }

    @Test
    fun `generate mid-game announces a chờ landed by the replay`() = runTest(dispatcher) {
        val env = env(playerVoiceOn.copy(mode = AppMode.BOTH))
        env.viewModel.generate()
        runCurrent()
        env.masterStore.startNewGame()
        repeat(90) { env.masterStore.drawNext() }
        runCurrent()
        env.viewModel.dismissCongrats()

        // Regenerating against a fully-called history completes every row on
        // the fresh card too — detection must run as part of generate().
        env.viewModel.generate()
        runCurrent()
        assertTrue(env.viewModel.uiState.value.showCongrats)
        assertEquals(listOf("kinh"), env.voice.utterances.last())
    }

    @Test
    fun `manual unticks are cleared and persisted on a master Ván mới outside both mode (L6)`() =
        runTest(dispatcher) {
            val env = env(playerVoiceOn) // mode defaults to PLAYER (non-BOTH)
            env.viewModel.generate()
            runCurrent()
            env.masterStore.startNewGame()
            repeat(90) { env.masterStore.drawNext() } // every board number is now "called"
            runCurrent()

            val grid = env.viewModel.uiState.value.grid!!
            val col = grid[0].indexOfFirst { it > 0 }
            env.viewModel.onCellClick(0, col) // cross
            env.viewModel.onCellClick(0, col) // untick -> manualUnticks gains this number
            advanceUntilIdle()
            assertEquals(setOf(grid[0][col]), env.repository.loadPlayerState()?.manualUnticks)

            // Master resets the round while still in PLAYER mode — the
            // reset branch used to clear manualUnticks only when mode was
            // BOTH.
            env.masterStore.startNewGame()
            advanceUntilIdle()

            assertEquals(emptySet<Int>(), env.repository.loadPlayerState()?.manualUnticks)
        }

    @Test
    fun `clearMarks in both mode re-crosses all called numbers (unticks wiped)`() =
        runTest(dispatcher) {
            val env = env(playerVoiceOn.copy(mode = AppMode.BOTH))
            env.viewModel.generate()
            runCurrent()
            env.masterStore.startNewGame()
            repeat(90) { env.masterStore.drawNext() }
            runCurrent()
            val grid = env.viewModel.uiState.value.grid!!
            val col = grid[0].indexOfFirst { it > 0 }
            env.viewModel.onCellClick(0, col) // manual untick
            runCurrent()

            // Clear wipes the untick memory before replaying, so the whole
            // board re-crosses (locked product decision, mirrors the web).
            env.viewModel.clearMarks()
            runCurrent()
            assertTrue(env.viewModel.uiState.value.crossed[0][col])
        }

    @Test
    fun `loading flips false once restore settles, gating the generate button`() =
        runTest(dispatcher) {
            // env() already runs restore() to completion via runCurrent();
            // this asserts that side effect explicitly rather than assuming it.
            val env = env(playerVoiceOn)
            assertFalse(env.viewModel.uiState.value.loading)
        }

    @Test
    fun `restore does not clobber a card generated while the load is still in flight`() =
        runTest(dispatcher) {
            // Seed a previously-saved round the same way a prior app run would.
            val backing = InMemoryDataStore()
            val seedRepo = GameStateRepository(backing)
            val savedGrid = CardGenerator.generateGrid(Random(1))
            seedRepo.savePlayerState(
                PlayerRoundState(savedGrid, savedGrid.map { row -> row.map { false } }, emptySet()),
            )
            runCurrent()

            // The next "process" reads through a DataStore whose first
            // emission is gated — simulating the real async DataStore read
            // that H1 is about, instead of InMemoryDataStore's synchronous one.
            val gate = CompletableDeferred<Unit>()
            val repository = GameStateRepository(SlowDataStore(backing, gate))
            val masterStore =
                MasterStore(repository, backgroundScope, com.miti99.loto.game.DrawDeck(Random(7)))
            val settings = MutableStateFlow(playerVoiceOn)
            val voice = FakeVoicePlayer()
            val viewModel = PlayerBoardViewModel(repository, masterStore, settings, voice, dispatcher)
            runCurrent() // restore() is now suspended on gate.await()
            assertTrue(viewModel.uiState.value.loading)
            assertNull(viewModel.uiState.value.grid)

            // A user action lands while the restore is still in flight.
            viewModel.generate()
            runCurrent()
            val freshGrid = viewModel.uiState.value.grid
            assertNotNull(freshGrid)
            assertNotEquals(savedGrid, freshGrid)

            // Let the stale restore resume and resolve.
            gate.complete(Unit)
            runCurrent()

            // The fresh card must survive — restore must not silently
            // overwrite it with the stale persisted round (H1).
            assertEquals(freshGrid, viewModel.uiState.value.grid)
            assertFalse(viewModel.uiState.value.loading)
        }

    @Test
    fun `a settings arrival that lands before masterStore's restore still gets the full replay`() =
        runTest(dispatcher) {
            // M4: masterStore.restore() and the settings DataStore read are
            // two independent async reads. This covers the ordering where
            // settings resolves to its real (persisted) value before
            // masterStore.restore() lands — combine(masterStore.state,
            // settings) re-runs the replay on the settings change, so the
            // mode is already correct by the time the master history first
            // arrives. (The reverse ordering — masterStore's restore landing
            // with real history *before* settings ever resolves — is
            // covered separately below, M3/M4 residual.)
            val repository = GameStateRepository(InMemoryDataStore())
            val masterStore =
                MasterStore(repository, backgroundScope, com.miti99.loto.game.DrawDeck(Random(7)))
            // masterStore.restore() is deliberately NOT called yet.
            val settings = MutableStateFlow(playerVoiceOn.copy(mode = AppMode.PLAYER))
            val voice = FakeVoicePlayer()
            val viewModel = PlayerBoardViewModel(repository, masterStore, settings, voice, dispatcher)
            runCurrent()
            viewModel.generate()
            runCurrent()

            // The real persisted mode lands while masterStore still reads
            // as empty (its own restore has not resolved yet).
            settings.value = settings.value.copy(mode = AppMode.BOTH)
            runCurrent()
            assertTrue(viewModel.uiState.value.crossed.flatten().none { it })

            // masterStore's restore resolves afterwards with the full
            // history — this is the first time that history is evaluated,
            // and mode is already BOTH.
            repository.saveMasterState(MasterRoundState(called = (1..90).toList(), remaining = emptyList()))
            masterStore.restore()
            runCurrent()

            assertEquals(List(9) { true }, viewModel.uiState.value.rowComplete)
        }

    @Test
    fun `master restore landing with full history before settings resolves still replays under the real mode (M4 residual)`() =
        runTest(dispatcher) {
            // masterStore.restore() resolves synchronously (InMemoryDataStore)
            // with the full called history while the settings DataStore read
            // is still in flight (SlowDataStore) — and, unlike an earlier
            // version of this test, generate() is tapped only *after* that
            // master history has already landed. That is the ordering M3
            // says a generate()-before-restore() tap dodges: without the
            // settingsOrNull guard, generate() would compute its auto-cross
            // cursor against the still-null (placeholder) mode, silently
            // discard the already-available BOTH-mode history via
            // `lastHandledIndex = called.size`, and the replay would be lost
            // for good once settings resolved (masterStore.state does not
            // re-emit on its own).
            val repository = GameStateRepository(InMemoryDataStore())
            val masterStore =
                MasterStore(repository, backgroundScope, com.miti99.loto.game.DrawDeck(Random(7)))
            repository.saveMasterState(
                MasterRoundState(called = (1..90).toList(), remaining = emptyList()),
            )

            val voiceIds = setOf("hoai-my")
            val settingsBacking = InMemoryDataStore()
            // Persist the real mode ahead of time (BOTH != the PLAYER
            // default) via an ungated repository over the same backing store.
            SettingsRepository(settingsBacking, voiceIds, "hoai-my").setMode(AppMode.BOTH)

            val gate = CompletableDeferred<Unit>()
            val gatedSettingsRepo =
                SettingsRepository(SlowDataStore(settingsBacking, gate), voiceIds, "hoai-my")
            // Single source of truth (M2), matching LotoApplication's
            // settingsOrNull wiring: null until the gated read resolves.
            val settingsOrNull: StateFlow<Settings?> = gatedSettingsRepo.settingsFlow
                .map<Settings, Settings?> { it }
                .stateIn(backgroundScope, SharingStarted.Eagerly, null)
            val voice = FakeVoicePlayer()

            val viewModel =
                PlayerBoardViewModel(repository, masterStore, settingsOrNull, voice, dispatcher)

            // Master's restore lands with the full history first, while
            // settings is still gated.
            masterStore.restore()
            runCurrent()
            assertTrue(viewModel.uiState.value.loading)

            // A tap in this window — the UI's button is disabled on
            // `loading`, but generate() guards itself too in case a caller
            // acts before that gate resolves (M3) — must not consume the
            // already-available history under the wrong mode.
            viewModel.generate()
            runCurrent()
            assertNull(viewModel.uiState.value.grid)

            // Settings resolves afterwards to the real, persisted BOTH mode.
            gate.complete(Unit)
            runCurrent()
            assertFalse(viewModel.uiState.value.loading)

            // The tap the user actually makes once the button re-enables.
            viewModel.generate()
            runCurrent()

            assertEquals(List(9) { true }, viewModel.uiState.value.rowComplete)
        }

    @Test
    fun `clearMarks() is also a no-op while settings have not resolved yet (M3)`() =
        runTest(dispatcher) {
            val repository = GameStateRepository(InMemoryDataStore())
            val masterStore =
                MasterStore(repository, backgroundScope, com.miti99.loto.game.DrawDeck(Random(7)))
            // A persisted round makes `grid` non-null independently of
            // settings, so clearMarks()'s own `grid ?: return` guard alone
            // would not catch this window.
            val savedGrid = CardGenerator.generateGrid(Random(3))
            repository.savePlayerState(
                PlayerRoundState(savedGrid, savedGrid.map { row -> row.map { true } }, emptySet()),
            )
            val settingsOrNull = MutableStateFlow<Settings?>(null)
            val voice = FakeVoicePlayer()
            val viewModel =
                PlayerBoardViewModel(repository, masterStore, settingsOrNull, voice, dispatcher)
            runCurrent() // restore() resolves; settingsOrNull is still null

            val before = viewModel.uiState.value.crossed
            viewModel.clearMarks()
            runCurrent()
            assertEquals(before, viewModel.uiState.value.crossed)
        }

    @Test
    fun `loading stays true while settings have not resolved even after restore settles, and flips false once they do (M3)`() =
        runTest(dispatcher) {
            val repository = GameStateRepository(InMemoryDataStore())
            val masterStore =
                MasterStore(repository, backgroundScope, com.miti99.loto.game.DrawDeck(Random(7)))
            val settingsOrNull = MutableStateFlow<Settings?>(null)
            val voice = FakeVoicePlayer()
            val viewModel =
                PlayerBoardViewModel(repository, masterStore, settingsOrNull, voice, dispatcher)
            runCurrent() // restore() resolves (InMemoryDataStore is synchronous)

            assertTrue(viewModel.uiState.value.loading)

            settingsOrNull.value = playerVoiceOn
            runCurrent()
            assertFalse(viewModel.uiState.value.loading)
        }

    @Test
    fun `a second rapid generate() tap while the first is still computing is a no-op (L-a)`() =
        runTest(dispatcher) {
            val env = env(playerVoiceOn)
            env.viewModel.generate()
            env.viewModel.generate() // enqueued before the first call's compute step ran
            runCurrent()
            assertEquals(1, env.voice.cancelCount)
            assertNotNull(env.viewModel.uiState.value.grid)
        }
}
