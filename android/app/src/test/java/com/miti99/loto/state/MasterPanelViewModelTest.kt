package com.miti99.loto.state

import com.miti99.loto.InMemoryDataStore
import com.miti99.loto.audio.FakeVoicePlayer
import com.miti99.loto.settings.Settings
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
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

@OptIn(ExperimentalCoroutinesApi::class)
class MasterPanelViewModelTest {

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
        val masterStore: MasterStore,
        val repository: GameStateRepository,
        val settings: MutableStateFlow<Settings>,
        val voice: FakeVoicePlayer,
        val viewModel: MasterPanelViewModel,
    )

    private fun TestScope.env(initial: Settings): Env {
        val repository = GameStateRepository(InMemoryDataStore())
        val masterStore = MasterStore(repository, backgroundScope, DrawDeckSeeded)
        val settings = MutableStateFlow(initial)
        val voice = FakeVoicePlayer()
        val viewModel = MasterPanelViewModel(masterStore, settings, voice)
        runCurrent() // start the ticker collector
        return Env(masterStore, repository, settings, voice, viewModel)
    }

    private val autoSettings =
        Settings(voice = "hoai-my", autoCallEnabled = true, autoCallSpeed = 2)

    @Test
    fun `auto-call draws one number per interval`() = runTest(dispatcher) {
        val env = env(autoSettings)
        env.masterStore.startNewGame()
        env.viewModel.toggleAuto()
        runCurrent()

        advanceTimeBy(2001)
        assertEquals(1, env.masterStore.state.value.called.size)
        advanceTimeBy(6000)
        assertEquals(4, env.masterStore.state.value.called.size)
        // Each draw was announced (master voice defaults on).
        assertEquals(4, env.voice.utterances.size)
    }

    @Test
    fun `mid-run speed change re-arms without double-ticking`() = runTest(dispatcher) {
        val env = env(autoSettings)
        env.masterStore.startNewGame()
        env.viewModel.toggleAuto()
        runCurrent()

        advanceTimeBy(1000) // halfway through the 2s tick
        assertEquals(0, env.masterStore.state.value.called.size)

        env.settings.value = autoSettings.copy(autoCallSpeed = 5)
        runCurrent()
        // The old 2s timer must be gone…
        advanceTimeBy(2001)
        assertEquals(0, env.masterStore.state.value.called.size)
        // …and the new 5s cadence takes over (restarted at the change).
        advanceTimeBy(3000)
        assertEquals(1, env.masterStore.state.value.called.size)
    }

    @Test
    fun `auto-call stops when the deck is exhausted`() = runTest(dispatcher) {
        val env = env(autoSettings)
        env.repository.saveMasterState(
            MasterRoundState(called = (1..89).toList(), remaining = listOf(90)),
        )
        env.masterStore.restore()
        env.viewModel.toggleAuto()
        runCurrent()

        advanceTimeBy(2001)
        assertEquals(90, env.masterStore.state.value.called.size)
        advanceTimeBy(2001)
        assertFalse(env.viewModel.autoRunning.value)
    }

    @Test
    fun `disabling the auto setting mid-run stops the run`() = runTest(dispatcher) {
        val env = env(autoSettings)
        env.masterStore.startNewGame()
        env.viewModel.toggleAuto()
        runCurrent()
        assertTrue(env.viewModel.autoRunning.value)

        env.settings.value = autoSettings.copy(autoCallEnabled = false)
        runCurrent()
        assertFalse(env.viewModel.autoRunning.value)
        advanceTimeBy(10_000)
        assertEquals(0, env.masterStore.state.value.called.size)
    }

    @Test
    fun `toggleAuto no-ops when no numbers remain`() = runTest(dispatcher) {
        val env = env(autoSettings)
        env.viewModel.toggleAuto() // no game yet — remaining is empty
        runCurrent()
        assertFalse(env.viewModel.autoRunning.value)
    }

    @Test
    fun `newGame stops auto, cancels voice, and refills the deck`() = runTest(dispatcher) {
        val env = env(autoSettings)
        env.masterStore.startNewGame()
        env.viewModel.toggleAuto()
        runCurrent()

        env.viewModel.newGame()
        runCurrent()
        assertFalse(env.viewModel.autoRunning.value)
        assertEquals(emptyList<Int>(), env.masterStore.state.value.called)
        assertEquals(90, env.masterStore.state.value.remaining.size)
    }

    @Test
    fun `backgrounding pauses auto-call and returning resumes it`() = runTest(dispatcher) {
        val env = env(autoSettings)
        env.masterStore.startNewGame()
        env.viewModel.toggleAuto()
        runCurrent()
        advanceTimeBy(2001)
        assertEquals(1, env.masterStore.state.value.called.size)

        env.viewModel.setForeground(false)
        runCurrent()
        // No draws while backgrounded — but autoRunning stays on.
        advanceTimeBy(10_000)
        assertEquals(1, env.masterStore.state.value.called.size)
        assertTrue(env.viewModel.autoRunning.value)

        env.viewModel.setForeground(true)
        runCurrent()
        advanceTimeBy(2001)
        assertEquals(2, env.masterStore.state.value.called.size)
    }

    @Test
    fun `drawNext announces only when the master voice is enabled`() = runTest(dispatcher) {
        val env = env(autoSettings.copy(voiceEnabledMaster = false))
        env.masterStore.startNewGame()
        env.viewModel.drawNext()
        runCurrent()
        assertEquals(1, env.masterStore.state.value.called.size)
        assertEquals(0, env.voice.utterances.size)
        assertEquals(1, env.viewModel.tickKey.value)
    }

    private companion object {
        val DrawDeckSeeded get() = com.miti99.loto.game.DrawDeck(Random(1234))
    }
}
