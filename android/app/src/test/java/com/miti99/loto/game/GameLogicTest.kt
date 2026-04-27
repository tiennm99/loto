package com.miti99.loto.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import com.miti99.loto.game.GameLogic.NUM_COLS
import com.miti99.loto.game.GameLogic.NUM_PER_ROW
import com.miti99.loto.game.GameLogic.NUM_ROWS
import com.miti99.loto.game.GameLogic.generateGrid
import com.miti99.loto.game.GameLogic.getWaitingNumber
import com.miti99.loto.game.GameLogic.isRowComplete

/**
 * Port of `tiennm99/loto/src/lib/game-logic.test.js`. Persistence tests
 * (saveGrid/loadGrid) intentionally NOT ported — that role moved to
 * DataStore in phase 05.
 */
class GameLogicTest {

    private val COL_RANGES = listOf(
        1..9, 10..19, 20..29, 30..39, 40..49,
        50..59, 60..69, 70..79, 80..90,
    )

    private fun rowCounts(grid: Array<IntArray>): IntArray =
        IntArray(NUM_ROWS) { r -> grid[r].count { it > 0 } }

    private fun colCounts(grid: Array<IntArray>): IntArray =
        IntArray(NUM_COLS) { c -> (0 until NUM_ROWS).count { r -> grid[r][c] > 0 } }

    @Test
    fun `returns a 9x9 matrix`() {
        val g = generateGrid()
        assertEquals(NUM_ROWS, g.size)
        for (row in g) assertEquals(NUM_COLS, row.size)
    }

    @RepeatedTest(200)
    fun `every row has exactly 5 non-zero numbers`() {
        val counts = rowCounts(generateGrid())
        assertTrue(counts.all { it == NUM_PER_ROW }) { counts.joinToString() }
    }

    @RepeatedTest(200)
    fun `every column has exactly 5 non-zero numbers`() {
        val counts = colCounts(generateGrid())
        assertTrue(counts.all { it == NUM_PER_ROW }) { counts.joinToString() }
    }

    @RepeatedTest(50)
    fun `never produces duplicates in a single card`() {
        val flat = generateGrid().flatMap { it.toList() }.filter { it > 0 }
        assertEquals(flat.size, flat.toSet().size)
    }

    @RepeatedTest(50)
    fun `each non-zero cell sits in its column's tens range`() {
        val g = generateGrid()
        for (r in 0 until NUM_ROWS) for (c in 0 until NUM_COLS) {
            val n = g[r][c]
            if (n == 0) continue
            assertTrue(n in COL_RANGES[c]) { "row=$r col=$c num=$n" }
        }
    }

    @RepeatedTest(50)
    fun `numbers within each column are sorted ascending top-to-bottom`() {
        val g = generateGrid()
        for (c in 0 until NUM_COLS) {
            val colNums = (0 until NUM_ROWS).map { g[it][c] }.filter { it > 0 }
            assertEquals(colNums.sorted(), colNums)
        }
    }

    @RepeatedTest(300)
    fun `no row has 3 consecutive filled columns (soft constraint)`() {
        val g = generateGrid()
        for (r in 0 until NUM_ROWS) {
            for (c in 0..NUM_COLS - 3) {
                assertFalse(g[r][c] > 0 && g[r][c + 1] > 0 && g[r][c + 2] > 0) {
                    "row=$r cols=$c,${c + 1},${c + 2}"
                }
            }
        }
    }

    @Test
    fun `col 0 only holds numbers from 1 to 9 (5 per card)`() {
        val g = generateGrid()
        val col0 = (0 until NUM_ROWS).map { g[it][0] }.filter { it > 0 }
        assertEquals(NUM_PER_ROW, col0.size)
        assertAll(col0.map { n -> Runnable { assertTrue(n in 1..9) } })
    }

    @Test
    fun `col 8 only holds numbers from 80 to 90 (5 per card)`() {
        val g = generateGrid()
        val col8 = (0 until NUM_ROWS).map { g[it][8] }.filter { it > 0 }
        assertEquals(NUM_PER_ROW, col8.size)
        assertAll(col8.map { n -> Runnable { assertTrue(n in 80..90) } })
    }

    // ----- isRowComplete -----

    @Test
    fun `isRowComplete true when every number in row is crossed`() {
        val grid = arrayOf(intArrayOf(0, 1, 0, 2, 0, 3, 0, 4, 5))
        val crossed = arrayOf(booleanArrayOf(false, true, false, true, false, true, false, true, true))
        assertTrue(isRowComplete(grid, crossed, 0))
    }

    @Test
    fun `isRowComplete false when at least one number uncrossed`() {
        val grid = arrayOf(intArrayOf(0, 1, 0, 2, 0, 3, 0, 4, 5))
        val crossed = arrayOf(booleanArrayOf(false, true, false, false, false, true, false, true, true))
        assertFalse(isRowComplete(grid, crossed, 0))
    }

    @Test
    fun `isRowComplete false for all-zero row (no numbers, not a win)`() {
        val grid = arrayOf(IntArray(9))
        val crossed = arrayOf(BooleanArray(9))
        assertFalse(isRowComplete(grid, crossed, 0))
    }

    @Test
    fun `isRowComplete ignores zero cells when checking crossed state`() {
        val grid = arrayOf(intArrayOf(0, 7, 0, 0, 0, 0, 0, 0, 0))
        val crossed = arrayOf(booleanArrayOf(false, true, false, false, false, false, false, false, false))
        assertTrue(isRowComplete(grid, crossed, 0))
    }

    // ----- getWaitingNumber -----

    @Test
    fun `getWaitingNumber returns single uncrossed number when exactly one remains`() {
        val grid = arrayOf(intArrayOf(0, 1, 0, 2, 0, 3, 0, 4, 5))
        val crossed = arrayOf(booleanArrayOf(false, true, false, false, false, true, false, true, true))
        assertEquals(2, getWaitingNumber(grid, crossed, 0))
    }

    @Test
    fun `getWaitingNumber null when more than one number remains`() {
        val grid = arrayOf(intArrayOf(0, 1, 0, 2, 0, 3, 0, 4, 5))
        val crossed = arrayOf(booleanArrayOf(false, true, false, false, false, false, false, true, true))
        assertNull(getWaitingNumber(grid, crossed, 0))
    }

    @Test
    fun `getWaitingNumber null when zero numbers remain (row complete)`() {
        val grid = arrayOf(intArrayOf(0, 1, 0, 2, 0, 3, 0, 4, 5))
        val crossed = arrayOf(booleanArrayOf(false, true, false, true, false, true, false, true, true))
        assertNull(getWaitingNumber(grid, crossed, 0))
    }

    @Test
    fun `getWaitingNumber null for empty row`() {
        val grid = arrayOf(IntArray(9))
        val crossed = arrayOf(BooleanArray(9))
        assertNull(getWaitingNumber(grid, crossed, 0))
    }
}
