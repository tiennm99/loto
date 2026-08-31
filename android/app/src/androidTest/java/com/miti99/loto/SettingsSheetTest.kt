package com.miti99.loto

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Cross-cutting settings: display mode switches the root layout live. */
@RunWith(AndroidJUnit4::class)
class SettingsSheetTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app: LotoApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as LotoApplication

    @Before
    fun resetSettings() {
        runBlocking { app.settingsRepository.reset() }
        composeRule.waitForIdle()
    }

    private fun openSheet() {
        composeRule.onNodeWithContentDescription("Cài đặt").performClick()
        composeRule.waitForIdle()
    }

    @Test
    fun switchingToMasterModeSwapsThePanels() {
        composeRule.onNodeWithText("Tạo bảng mới").assertExists()
        openSheet()
        composeRule.onNodeWithText("Quản trò").performClick()
        // The write goes through DataStore; wait for the mode to land.
        composeRule.waitUntil(timeoutMillis = 5_000) {
            app.settingsState.value.mode == com.miti99.loto.settings.AppMode.MASTER
        }
        composeRule.waitForIdle()
        // Back closes the sheet (also a device-QA item); the mode change
        // already applied live.
        Espresso.pressBack()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ván mới").assertExists()
    }

    @Test
    fun sheetExposesEverydaySections() {
        openSheet()
        composeRule.onNodeWithText("Giao diện").assertExists()
        composeRule.onNodeWithText("Cỡ chữ bảng").assertExists()
        composeRule.onNodeWithText("Chế độ hiển thị").assertExists()
        composeRule.onNodeWithText("Âm thanh").assertExists()
        composeRule.onNodeWithText("Màu ô trống").assertExists()
    }
}
