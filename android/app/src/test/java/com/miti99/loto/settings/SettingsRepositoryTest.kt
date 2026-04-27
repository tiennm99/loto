package com.miti99.loto.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import app.cash.turbine.test
import com.miti99.loto.settings.SettingsKeys as K
import kotlin.io.path.Path
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

/**
 * Port of `tiennm99/loto/src/lib/settings-store.test.js`.
 * Uses real DataStore on a temp file; faster than Robolectric.
 */
class SettingsRepositoryTest {

    @TempDir lateinit var tempDir: File
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var scope: TestScope
    private lateinit var repo: SettingsRepository

    @BeforeEach
    fun setUp() {
        val testScope = TestScope(StandardTestDispatcher())
        scope = testScope
        dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob()),
            produceFile = { File(tempDir, "settings.preferences_pb") },
            migrations = listOf(SettingsRepository.legacyMasterModeMigration),
        )
        repo = SettingsRepository(dataStore, appScope = testScope)
    }

    @Test
    fun `loads defaults when empty`() = runTest {
        assertEquals(SettingsState.DEFAULT, repo.flow.first())
    }

    @Test
    fun `roundtrips each field`() = runTest {
        repo.setEmptyCellColor("#abcdef")
        repo.setTheme(SettingsState.Theme.DARK)
        repo.setMode(SettingsState.Mode.MASTER)
        repo.setAutoCallEnabled(true)
        repo.setAutoCallSpeed(8)
        repo.setVoiceEnabledMaster(false)
        repo.setVoiceEnabledPlayer(true)
        repo.setVoiceWaitingNumber(true)
        repo.setVoice("nam-minh")

        val state = repo.flow.first()
        assertEquals("#abcdef", state.emptyCellColor)
        assertEquals(SettingsState.Theme.DARK, state.theme)
        assertEquals(SettingsState.Mode.MASTER, state.mode)
        assertEquals(true, state.autoCallEnabled)
        assertEquals(8, state.autoCallSpeed)
        assertEquals(false, state.voiceEnabledMaster)
        assertEquals(true, state.voiceEnabledPlayer)
        assertEquals(true, state.voiceWaitingNumber)
        assertEquals("nam-minh", state.voice)
    }

    @Test
    fun `rejects invalid color falls back to default on read`() = runTest {
        dataStore.edit { it[K.EMPTY_CELL_COLOR] = "#zzzzzz" }
        assertEquals(SettingsState.DEFAULT.emptyCellColor, repo.flow.first().emptyCellColor)
    }

    @Test
    fun `clamps out-of-range speed when set`() = runTest {
        repo.setAutoCallSpeed(15)
        assertEquals(10, repo.flow.first().autoCallSpeed)
        repo.setAutoCallSpeed(-3)
        assertEquals(1, repo.flow.first().autoCallSpeed)
    }

    @Test
    fun `out-of-range stored speed falls back to default`() = runTest {
        dataStore.edit { it[K.AUTO_CALL_SPEED] = 99 }
        assertEquals(SettingsState.DEFAULT.autoCallSpeed, repo.flow.first().autoCallSpeed)
    }

    @Test
    fun `rejects unknown theme on read`() = runTest {
        dataStore.edit { it[K.THEME] = "neon" }
        assertEquals(SettingsState.Theme.AUTO, repo.flow.first().theme)
    }

    @Test
    fun `rejects unknown voice on write`() = runTest {
        repo.setVoice("ghost-voice")
        assertEquals(SettingsState.DEFAULT.voice, repo.flow.first().voice)
    }

    @Test
    fun `rejects unknown voice on read`() = runTest {
        dataStore.edit { it[K.VOICE] = "ghost-voice" }
        assertEquals(SettingsState.DEFAULT.voice, repo.flow.first().voice)
    }

    @Test
    fun `migrates legacy masterMode true to mode both`() = runTest {
        dataStore.edit { it[K.LEGACY_MASTER_MODE] = true }
        // Trigger migration by reading
        assertEquals(SettingsState.Mode.BOTH, repo.flow.first().mode)
    }

    @Test
    fun `legacy masterMode false uses default mode player`() = runTest {
        dataStore.edit { it[K.LEGACY_MASTER_MODE] = false }
        assertEquals(SettingsState.Mode.PLAYER, repo.flow.first().mode)
    }

    @Test
    fun `reset restores all defaults`() = runTest {
        repo.setTheme(SettingsState.Theme.DARK)
        repo.setVoice("nam-minh")
        repo.reset()
        assertEquals(SettingsState.DEFAULT, repo.flow.first())
    }
}
