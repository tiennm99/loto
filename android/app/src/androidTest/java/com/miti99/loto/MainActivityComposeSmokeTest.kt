package com.miti99.loto

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI smoke test — verifies the full cold-start → generate-grid flow.
 *
 * Requires a connected device or emulator. Run via:
 *   ./gradlew :app:connectedDebugAndroidTest
 *
 * Phase 11 spec: launch MainActivity, assert "Tạo bảng mới" shown,
 * click it, assert all 81 player_cell nodes are rendered.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityComposeSmokeTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun generate_button_is_visible_on_cold_start() {
        rule.onNodeWithText("Tạo bảng mới").assertExists()
    }

    @Test
    fun generate_button_creates_81_player_cells() {
        // Tap the generate button — no confirmation dialog on first tap (grid is null)
        rule.onNodeWithText("Tạo bảng mới").performClick()

        // Wait up to 5 s for all 81 cells to appear (grid generation is synchronous
        // but Compose may take a frame to recompose)
        rule.waitUntil(timeoutMillis = 5_000) {
            rule.onAllNodesWithTag("player_cell").fetchSemanticsNodes().size == 81
        }

        rule.onAllNodesWithTag("player_cell").assertCountEquals(81)
    }
}
