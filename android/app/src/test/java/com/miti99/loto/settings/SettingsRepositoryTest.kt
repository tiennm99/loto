package com.miti99.loto.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.miti99.loto.InMemoryDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Twins of the validation cases in `web/src/lib/settings-store.test.js`.
 * Type-mismatch cases from the web (e.g. `autoCallSpeed: "5"`) have no
 * counterpart: DataStore keys are typed, so a wrong-typed value is
 * unrepresentable — range/allowlist violations are what remain.
 */
class SettingsRepositoryTest {

    private val voiceIds = setOf("hoai-my", "nam-minh")

    private fun repo(store: DataStore<Preferences>) =
        SettingsRepository(store, voiceIds, defaultVoiceId = "hoai-my")

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
}
