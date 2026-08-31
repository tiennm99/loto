package com.miti99.loto.game

import com.miti99.loto.settings.AppMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ported one-to-one from `web/src/lib/player-auto-cross.test.js`. Test names
 * mirror the web `it` strings.
 */
class PlayerAutoCrossTest {

    private fun gridWith(num: Int): List<List<Int>> =
        List(9) { r -> List(9) { c -> if (r == 0 && c == 0) num else 0 } }

    private fun emptyCrossed(): List<List<Boolean>> = List(9) { List(9) { false } }

    @Test
    fun `no-op when called is empty`() {
        val r = PlayerAutoCross.applyMasterCalls(
            grid = gridWith(7),
            crossed = emptyCrossed(),
            called = emptyList(),
            lastHandledIndex = 0,
            manualUnticks = emptySet(),
            mode = AppMode.BOTH,
        )
        assertFalse(r.changed)
        assertEquals(0, r.lastHandledIndex)
    }

    @Test
    fun `no-op when cursor at length`() {
        val r = PlayerAutoCross.applyMasterCalls(
            grid = gridWith(7),
            crossed = emptyCrossed(),
            called = listOf(7),
            lastHandledIndex = 1,
            manualUnticks = emptySet(),
            mode = AppMode.BOTH,
        )
        assertFalse(r.changed)
        assertEquals(1, r.lastHandledIndex)
    }

    @Test
    fun `mode=player advances cursor without flipping`() {
        val r = PlayerAutoCross.applyMasterCalls(
            grid = gridWith(7),
            crossed = emptyCrossed(),
            called = listOf(7, 8, 9),
            lastHandledIndex = 0,
            manualUnticks = emptySet(),
            mode = AppMode.PLAYER,
        )
        assertFalse(r.changed)
        assertEquals(3, r.lastHandledIndex)
        assertFalse(r.crossed[0][0])
    }

    @Test
    fun `mode=both crosses uncrossed cell on match`() {
        val r = PlayerAutoCross.applyMasterCalls(
            grid = gridWith(7),
            crossed = emptyCrossed(),
            called = listOf(7),
            lastHandledIndex = 0,
            manualUnticks = emptySet(),
            mode = AppMode.BOTH,
        )
        assertTrue(r.changed)
        assertEquals(1, r.lastHandledIndex)
        assertTrue(r.crossed[0][0])
    }

    @Test
    fun `mode=both replays full back-history when cursor=0`() {
        val grid = List(9) { r ->
            List(9) { c ->
                when {
                    r == 0 && c == 0 -> 5
                    r == 1 && c == 1 -> 12
                    r == 2 && c == 2 -> 88
                    else -> 0
                }
            }
        }
        val r = PlayerAutoCross.applyMasterCalls(
            grid = grid,
            crossed = emptyCrossed(),
            called = listOf(5, 12, 88, 89 /* off-board */),
            lastHandledIndex = 0,
            manualUnticks = emptySet(),
            mode = AppMode.BOTH,
        )
        assertTrue(r.changed)
        assertEquals(4, r.lastHandledIndex)
        assertTrue(r.crossed[0][0])
        assertTrue(r.crossed[1][1])
        assertTrue(r.crossed[2][2])
    }

    @Test
    fun `manualUnticks numbers are skipped`() {
        val r = PlayerAutoCross.applyMasterCalls(
            grid = gridWith(7),
            crossed = emptyCrossed(),
            called = listOf(7),
            lastHandledIndex = 0,
            manualUnticks = setOf(7),
            mode = AppMode.BOTH,
        )
        assertFalse(r.changed)
        assertEquals(1, r.lastHandledIndex)
        assertFalse(r.crossed[0][0])
    }

    @Test
    fun `grid=null no-ops in both mode (still advances cursor)`() {
        val r = PlayerAutoCross.applyMasterCalls(
            grid = null,
            crossed = emptyList(),
            called = listOf(7),
            lastHandledIndex = 0,
            manualUnticks = emptySet(),
            mode = AppMode.BOTH,
        )
        assertFalse(r.changed)
        assertEquals(1, r.lastHandledIndex)
    }

    @Test
    fun `stale cursor beyond a shrunken history resets and replays from zero`() {
        // A round reset conflated with the new round's first draws must not
        // leave auto-cross dead (native hardening beyond the web spec).
        val r = PlayerAutoCross.applyMasterCalls(
            grid = gridWith(7),
            crossed = emptyCrossed(),
            called = listOf(7),
            lastHandledIndex = 31,
            manualUnticks = emptySet(),
            mode = AppMode.BOTH,
        )
        assertTrue(r.changed)
        assertEquals(1, r.lastHandledIndex)
        assertTrue(r.crossed[0][0])
    }

    @Test
    fun `returns same crossed reference when no flip happens`() {
        val crossed = emptyCrossed()
        val r = PlayerAutoCross.applyMasterCalls(
            grid = gridWith(7),
            crossed = crossed,
            called = listOf(89 /* not on grid */),
            lastHandledIndex = 0,
            manualUnticks = emptySet(),
            mode = AppMode.BOTH,
        )
        assertFalse(r.changed)
        assertSame(crossed, r.crossed)
    }
}
