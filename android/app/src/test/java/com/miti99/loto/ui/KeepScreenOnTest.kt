package com.miti99.loto.ui

import com.miti99.loto.settings.AppMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeepScreenOnTest {

    @Test
    fun `held while a master round has numbers left in master and both modes`() {
        assertTrue(shouldKeepScreenOn(AppMode.MASTER, remainingCount = 90))
        assertTrue(shouldKeepScreenOn(AppMode.BOTH, remainingCount = 1))
    }

    @Test
    fun `released when the deck is exhausted or no round exists`() {
        assertFalse(shouldKeepScreenOn(AppMode.MASTER, remainingCount = 0))
        assertFalse(shouldKeepScreenOn(AppMode.BOTH, remainingCount = 0))
    }

    @Test
    fun `never held in player-only mode`() {
        assertFalse(shouldKeepScreenOn(AppMode.PLAYER, remainingCount = 90))
    }
}
