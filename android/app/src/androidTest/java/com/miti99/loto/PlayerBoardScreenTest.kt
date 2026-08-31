package com.miti99.loto

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
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
 * Critical player interactions: generate a card, tap-cross a cell, drive a
 * row to chờ and then to Kinh. Cells are found via their state descriptions
 * ("Số N[, đã đánh dấu][, đang chờ]"); composition order is row-major, so
 * the first five number cells belong to row 0.
 */
@RunWith(AndroidJUnit4::class)
class PlayerBoardScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetSettings() {
        // Instrumentation tests share the app's DataStore across classes;
        // make sure the player board is visible regardless of run order.
        val app = InstrumentationRegistry.getInstrumentation()
            .targetContext.applicationContext as LotoApplication
        runBlocking { app.settingsRepository.reset() }
        composeRule.waitForIdle()
    }

    private fun numberCells() = composeRule.onAllNodes(
        SemanticsMatcher("has number-cell state") {
            it.config.getOrNull(SemanticsProperties.StateDescription)?.startsWith("Số ") == true
        },
    )

    private fun generateCard() {
        composeRule.onNodeWithText("Tạo bảng mới").performClick()
        composeRule.waitForIdle()
        // A persisted round from an earlier test raises the confirm dialog.
        val confirm = composeRule.onAllNodesWithText("Đồng ý").fetchSemanticsNodes()
        if (confirm.isNotEmpty()) {
            composeRule.onNodeWithText("Đồng ý").performClick()
            composeRule.waitForIdle()
        }
    }

    @Test
    fun generateShowsA45NumberCard() {
        generateCard()
        numberCells().assertCountEquals(45)
    }

    @Test
    fun tappingACellMarksIt() {
        generateCard()
        numberCells()[0].performClick()
        composeRule.waitForIdle()
        composeRule.onAllNodes(
            SemanticsMatcher("crossed cell") {
                it.config.getOrNull(SemanticsProperties.StateDescription)
                    ?.contains("đã đánh dấu") == true
            },
        ).assertCountEquals(1)
    }

    @Test
    fun completingARowShowsChoThenKinh() {
        generateCard()
        // First five number cells are row 0's numbers (row-major order).
        repeat(4) { i -> numberCells()[i].performClick() }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Chờ ", substring = true).assertExists()

        numberCells()[4].performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Kinh!").assertExists()
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteractionCollection.assertCountEquals(
    expected: Int,
) = fetchSemanticsNodes().let { nodes ->
    check(nodes.size == expected) { "expected $expected nodes, found ${nodes.size}" }
}
