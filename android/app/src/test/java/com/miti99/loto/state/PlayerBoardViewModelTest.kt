package com.miti99.loto.state

import com.miti99.loto.InMemoryDataStore
import com.miti99.loto.audio.FakeVoicePlayer
import com.miti99.loto.settings.AppMode
import com.miti99.loto.settings.Settings
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
        val viewModel = PlayerBoardViewModel(repository, masterStore, settings, voice)
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
        assertEquals("Chờ $waitingNumber", env.viewModel.uiState.value.toast)
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
        assertNotNull(env.viewModel.uiState.value.toast)
        advanceTimeBy(5001)
        assertNull(env.viewModel.uiState.value.toast)
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
        assertNotNull(env.viewModel.uiState.value.toast)
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
        assertNull(restored.uiState.value.toast)
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
}
