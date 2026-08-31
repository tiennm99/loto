package com.miti99.loto

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Master panel critical interactions: draw updates hero + history, reset
 * asks for confirmation mid-round. (Auto-call relabeling needs the
 * autoCallEnabled setting — toggled through the repository.)
 */
@RunWith(AndroidJUnit4::class)
class MasterPanelScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val app: LotoApplication
        get() = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as LotoApplication

    @Before
    fun switchToMasterMode() {
        // Reset first — instrumentation tests share the app's DataStore.
        runBlocking {
            app.settingsRepository.reset()
            app.settingsRepository.setMode(com.miti99.loto.settings.AppMode.MASTER)
        }
        composeRule.waitForIdle()
    }

    /** Starts a round, clicking through the confirm dialog a persisted round raises. */
    private fun startNewGame() {
        composeRule.onNodeWithText("Ván mới").performClick()
        composeRule.waitForIdle()
        val confirm = composeRule.onAllNodesWithText("Đồng ý").fetchSemanticsNodes()
        if (confirm.isNotEmpty()) {
            composeRule.onNodeWithText("Đồng ý").performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun drawUpdatesHeroAndHistory() {
        startNewGame()
        composeRule.onNodeWithText("Xổ số").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("SỐ VỪA XỔ").assertExists()
        composeRule.onNodeWithText("Thứ tự đã xổ:").assertExists()
    }

    @Test
    fun midRoundResetAsksForConfirmation() {
        startNewGame()
        composeRule.onNodeWithText("Xổ số").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Ván mới").performClick()
        composeRule.onNodeWithText("Bạn có muốn tạo ván mới không?").assertExists()
    }

    @Test
    fun autoCallSettingRelabelsTheDrawButton() {
        runBlocking { app.settingsRepository.setAutoCallEnabled(true) }
        startNewGame()
        composeRule.onNodeWithText("Bắt đầu").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Dừng").assertExists()
        composeRule.onNodeWithText("Dừng").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Bắt đầu").assertExists()
    }
}
