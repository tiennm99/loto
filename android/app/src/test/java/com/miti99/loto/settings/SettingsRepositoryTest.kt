package com.miti99.loto.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.miti99.loto.InMemoryDataStore
import com.miti99.loto.SlowDataStore
import com.miti99.loto.ThrowingDataStore
import com.miti99.loto.audio.Voice
import com.miti99.loto.audio.VoiceCatalog
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Twins of the validation cases in `web/src/lib/settings-store.test.js`.
 * Type-mismatch cases from the web (e.g. `autoCallSpeed: "5"`) have no
 * counterpart: DataStore keys are typed, so a wrong-typed value is
 * unrepresentable — range/allowlist violations are what remain.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    private fun repo(
        store: DataStore<Preferences>,
        voices: List<Voice> = VoiceCatalog.FALLBACK_VOICES,
    ) = SettingsRepository(store, CompletableDeferred(voices))

    private fun runWithRepo(
        block: suspend (SettingsRepository, DataStore<Preferences>) -> Unit,
    ) = runTest {
        val store = InMemoryDataStore()
        block(repo(store), store)
    }

    @Test
    fun `defaults match the web DEFAULT_SETTINGS contract`() = runWithRepo { repository, _ ->
        val s = repository.settingsFlow.first()
        assertEquals("#7030A0", s.emptyCellColor)
        assertEquals(ThemeSetting.AUTO, s.theme)
        assertEquals(AppMode.PLAYER, s.mode)
        assertEquals(false, s.autoCallEnabled)
        assertEquals(5, s.autoCallSpeed)
        assertEquals(true, s.voiceEnabledMaster)
        assertEquals(false, s.voiceEnabledPlayer)
        assertEquals(false, s.voiceWaitingNumber)
        assertEquals("hoai-my", s.voice)
        assertEquals(1f, s.boardTextScale)
    }

    @Test
    fun `writes all fields and reads them back after recreating the repo`() = runTest {
        // Same backing store, fresh repository instance = process restart.
        val store = InMemoryDataStore()
        val repo1 = repo(store)
        repo1.setEmptyCellColor("#112233")
        repo1.setTheme(ThemeSetting.DARK)
        repo1.setMode(AppMode.BOTH)
        repo1.setAutoCallEnabled(true)
        repo1.setAutoCallSpeed(7)
        repo1.setVoiceEnabledMaster(false)
        repo1.setVoiceEnabledPlayer(true)
        repo1.setVoiceWaitingNumber(true)
        repo1.setVoice("nam-minh")
        repo1.setBoardTextScale(1.15f)

        val repo2 = repo(store)
        val s = repo2.settingsFlow.first()
        assertEquals("#112233", s.emptyCellColor)
        assertEquals(ThemeSetting.DARK, s.theme)
        assertEquals(AppMode.BOTH, s.mode)
        assertEquals(true, s.autoCallEnabled)
        assertEquals(7, s.autoCallSpeed)
        assertEquals(false, s.voiceEnabledMaster)
        assertEquals(true, s.voiceEnabledPlayer)
        assertEquals(true, s.voiceWaitingNumber)
        assertEquals("nam-minh", s.voice)
        assertEquals(1.15f, s.boardTextScale)
    }

    @Test
    fun `ignores an invalid stored color and keeps the default`() = runWithRepo { repository, store ->
        store.edit { it[stringPreferencesKey("emptyCellColor")] = "not-a-color" }
        assertEquals("#7030A0", repository.settingsFlow.first().emptyCellColor)
    }

    @Test
    fun `rejects 3-digit hex shorthand`() = runWithRepo { repository, store ->
        store.edit { it[stringPreferencesKey("emptyCellColor")] = "#fff" }
        assertEquals("#7030A0", repository.settingsFlow.first().emptyCellColor)
    }

    @Test
    fun `accepts uppercase hex`() = runWithRepo { repository, store ->
        store.edit { it[stringPreferencesKey("emptyCellColor")] = "#ABCDEF" }
        assertEquals("#ABCDEF", repository.settingsFlow.first().emptyCellColor)
    }

    @Test
    fun `loads valid theme values`() = runWithRepo { repository, store ->
        for (theme in ThemeSetting.entries) {
            store.edit { it[stringPreferencesKey("theme")] = theme.storageValue }
            assertEquals(theme, repository.settingsFlow.first().theme)
        }
    }

    @Test
    fun `falls back to default theme on invalid value`() = runWithRepo { repository, store ->
        store.edit { it[stringPreferencesKey("theme")] = "neon" }
        assertEquals(ThemeSetting.AUTO, repository.settingsFlow.first().theme)
    }

    @Test
    fun `loads valid mode values`() = runWithRepo { repository, store ->
        for (mode in AppMode.entries) {
            store.edit { it[stringPreferencesKey("mode")] = mode.storageValue }
            assertEquals(mode, repository.settingsFlow.first().mode)
        }
    }

    @Test
    fun `rejects invalid mode value`() = runWithRepo { repository, store ->
        store.edit { it[stringPreferencesKey("mode")] = "spectator" }
        assertEquals(AppMode.PLAYER, repository.settingsFlow.first().mode)
    }

    @Test
    fun `loads autoCallSpeed in range 1 to 10`() = runWithRepo { repository, store ->
        for (n in listOf(1, 5, 10)) {
            store.edit { it[intPreferencesKey("autoCallSpeed")] = n }
            assertEquals(n, repository.settingsFlow.first().autoCallSpeed)
        }
    }

    @Test
    fun `rejects out-of-range autoCallSpeed`() = runWithRepo { repository, store ->
        for (bad in listOf(0, 11, -1)) {
            store.edit { it[intPreferencesKey("autoCallSpeed")] = bad }
            assertEquals(5, repository.settingsFlow.first().autoCallSpeed)
        }
    }

    @Test
    fun `invalid voice id falls back to default while other settings survive`() =
        runWithRepo { repository, store ->
            store.edit {
                it[stringPreferencesKey("voice")] = "made-up-voice-id"
                it[intPreferencesKey("autoCallSpeed")] = 7
            }
            val s = repository.settingsFlow.first()
            assertEquals("hoai-my", s.voice)
            assertEquals(7, s.autoCallSpeed)
        }

    @Test
    fun `boardTextScale round-trips every allowed rung`() = runWithRepo { repository, _ ->
        for (scale in Settings.BOARD_TEXT_SCALES) {
            repository.setBoardTextScale(scale)
            assertEquals(scale, repository.settingsFlow.first().boardTextScale)
        }
    }

    @Test
    fun `boardTextScale falls back to default for a value outside the allowlist`() =
        runWithRepo { repository, store ->
            store.edit { it[floatPreferencesKey("boardTextScale")] = 2.5f }
            assertEquals(1f, repository.settingsFlow.first().boardTextScale)
        }

    @Test
    fun `preserves a single old key without wiping it`() = runWithRepo { repository, store ->
        store.edit { it[stringPreferencesKey("emptyCellColor")] = "#1e88e5" }
        val s = repository.settingsFlow.first()
        assertEquals("#1e88e5", s.emptyCellColor)
        assertEquals(ThemeSetting.AUTO, s.theme)
        assertEquals(AppMode.PLAYER, s.mode)
    }

    @Test
    fun `reset returns all settings to defaults`() = runWithRepo { repository, _ ->
        repository.setEmptyCellColor("#000000")
        repository.setTheme(ThemeSetting.DARK)
        repository.setMode(AppMode.BOTH)
        repository.reset()
        val s = repository.settingsFlow.first()
        assertEquals("#7030A0", s.emptyCellColor)
        assertEquals(ThemeSetting.AUTO, s.theme)
        assertEquals(AppMode.PLAYER, s.mode)
    }

    @Test
    fun `IO failure on read falls back to defaults instead of throwing (L4)`() = runTest {
        val repository = repo(ThrowingDataStore())
        val s = repository.settingsFlow.first()
        assertEquals("#7030A0", s.emptyCellColor)
        assertEquals(ThemeSetting.AUTO, s.theme)
        assertEquals(AppMode.PLAYER, s.mode)
    }

    @Test
    fun `IO failure on write is swallowed instead of throwing (L4)`() = runTest {
        val repository = repo(ThrowingDataStore())
        // Must not throw even though every underlying write fails.
        repository.setEmptyCellColor("#112233")
    }

    @Test
    fun `non-IO failure on read still falls back to defaults instead of cancelling the flow (M4)`() =
        runTest {
            // Before M4's fix, only IOException was caught here; anything
            // else re-threw and cancelled every collector of settingsFlow
            // forever, leaving `loaded` stuck at false and auto-cross dead
            // for the rest of the process.
            val repository = repo(ThrowingDataStore(RuntimeException("boom")))
            val s = repository.settingsFlow.first()
            assertEquals("#7030A0", s.emptyCellColor)
            assertEquals(AppMode.PLAYER, s.mode)
        }

    @Test
    fun `loaded still flips true after a non-IO read failure (M4)`() = runTest {
        val repository = repo(ThrowingDataStore(RuntimeException("boom")))
        val states = mutableListOf<Boolean>()
        backgroundScope.launch { repository.loaded.toList(states) }
        runCurrent()
        assertEquals(listOf(false, true), states)
    }

    @Test
    fun `loaded stays false until the first DataStore read resolves`() = runTest {
        // M4 residual: consumers (PlayerBoardViewModel) need to tell "the
        // read hasn't landed yet" apart from "it landed and is really
        // PLAYER" — SlowDataStore reproduces the real async gap that
        // InMemoryDataStore alone resolves synchronously.
        val gate = CompletableDeferred<Unit>()
        val repository = repo(SlowDataStore(InMemoryDataStore(), gate))
        val states = mutableListOf<Boolean>()
        backgroundScope.launch { repository.loaded.toList(states) }
        runCurrent()
        assertEquals(listOf(false), states)

        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf(false, true), states)
    }

    @Test
    fun `a hung first read publishes defaults, then the real values once it resolves`() = runTest {
        val backing = InMemoryDataStore()
        repo(backing).setMode(AppMode.BOTH)
        val gate = CompletableDeferred<Unit>()
        val repository = SettingsRepository(
            SlowDataStore(backing, gate),
            CompletableDeferred(VoiceCatalog.FALLBACK_VOICES),
            readTimeout = 5.seconds,
        )
        val emitted = mutableListOf<Settings>()
        backgroundScope.launch { repository.settingsFlow.toList(emitted) }

        advanceTimeBy(4.seconds)
        runCurrent()
        assertEquals(emptyList<Settings>(), emitted)

        advanceTimeBy(2.seconds)
        runCurrent()
        assertEquals(listOf(AppMode.PLAYER), emitted.map { it.mode })

        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf(AppMode.PLAYER, AppMode.BOTH), emitted.map { it.mode })
    }

    @Test
    fun `settings are not published until the voice manifest has loaded`() = runTest {
        // The manifest is parsed off the main thread; a snapshot published
        // before it lands would validate the voice against nothing.
        val manifest = CompletableDeferred<List<Voice>>()
        val repository = SettingsRepository(InMemoryDataStore(), manifest)
        val loaded = mutableListOf<Boolean>()
        val emitted = mutableListOf<Settings>()
        backgroundScope.launch { repository.loaded.toList(loaded) }
        backgroundScope.launch { repository.settingsFlow.toList(emitted) }
        runCurrent()
        assertEquals(listOf(false), loaded)
        assertEquals(emptyList<Settings>(), emitted)

        manifest.complete(VoiceCatalog.FALLBACK_VOICES)
        runCurrent()
        assertEquals(listOf(false, true), loaded)
        assertEquals(listOf("hoai-my"), emitted.map { it.voice })
    }

    @Test
    fun `default and allowed voices come from the loaded manifest`() = runTest {
        val manifestVoices = listOf(
            Voice("nam-minh", "vi-VN-NamMinhNeural", "Nam Minh (nam)", "male"),
        )
        val store = InMemoryDataStore()
        val repository = repo(store, manifestVoices)
        assertEquals("nam-minh", repository.settingsFlow.first().voice)

        // Valid under the fallback list, but not in this manifest.
        store.edit { it[stringPreferencesKey("voice")] = "hoai-my" }
        assertEquals("nam-minh", repository.settingsFlow.first().voice)
    }
}
