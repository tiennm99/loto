package com.miti99.loto.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ported one-to-one from `web/src/lib/game-logic.test.js` (isRowComplete,
 * getWaitingNumber and findUncrossedCell describe blocks). Test names mirror
 * the web `it` strings.
 */
class PlayerCardTest {

    // -- isRowComplete --

    @Test
    fun `returns true when every number in the row is crossed`() {
        val grid = listOf(listOf(0, 1, 0, 2, 0, 3, 0, 4, 5))
        val crossed = listOf(listOf(false, true, false, true, false, true, false, true, true))
        assertTrue(PlayerCard.isRowComplete(grid, crossed, 0))
    }

    @Test
    fun `returns false when at least one number is uncrossed`() {
        val grid = listOf(listOf(0, 1, 0, 2, 0, 3, 0, 4, 5))
        val crossed = listOf(listOf(false, true, false, false, false, true, false, true, true))
        assertFalse(PlayerCard.isRowComplete(grid, crossed, 0))
    }

    @Test
    fun `returns false for an all-zero row (no numbers, not a win)`() {
        val grid = listOf(List(9) { 0 })
        val crossed = listOf(List(9) { false })
        assertFalse(PlayerCard.isRowComplete(grid, crossed, 0))
    }

    @Test
    fun `ignores 0 cells when checking crossed state`() {
        val grid = listOf(listOf(0, 7, 0, 0, 0, 0, 0, 0, 0))
        // 7 is crossed; the zeros are not (and shouldn't matter)
        val crossed = listOf(listOf(false, true, false, false, false, false, false, false, false))
        assertTrue(PlayerCard.isRowComplete(grid, crossed, 0))
    }

    // -- getWaitingNumber --

    @Test
    fun `returns the single uncrossed number when exactly one remains`() {
        val grid = listOf(listOf(0, 1, 0, 2, 0, 3, 0, 4, 5))
        val crossed = listOf(listOf(false, true, false, false, false, true, false, true, true))
        assertEquals(2, PlayerCard.getWaitingNumber(grid, crossed, 0))
    }

    @Test
    fun `returns null when more than one number remains`() {
        val grid = listOf(listOf(0, 1, 0, 2, 0, 3, 0, 4, 5))
        val crossed = listOf(listOf(false, true, false, false, false, false, false, true, true))
        assertNull(PlayerCard.getWaitingNumber(grid, crossed, 0))
    }

    @Test
    fun `returns null when zero numbers remain (row complete)`() {
        val grid = listOf(listOf(0, 1, 0, 2, 0, 3, 0, 4, 5))
        val crossed = listOf(listOf(false, true, false, true, false, true, false, true, true))
        assertNull(PlayerCard.getWaitingNumber(grid, crossed, 0))
    }

    @Test
    fun `returns null for an empty row`() {
        val grid = listOf(List(9) { 0 })
        val crossed = listOf(List(9) { false })
        assertNull(PlayerCard.getWaitingNumber(grid, crossed, 0))
    }

    // -- findUncrossedCell --

    private val grid = listOf(
        listOf(0, 11, 0, 33, 0, 55, 0, 77, 88),
        listOf(1, 0, 22, 0, 44, 0, 66, 0, 89),
        listOf(0, 12, 23, 0, 0, 56, 0, 78, 0),
    )

    private fun uncrossed() = grid.map { row -> row.map { false } }

    @Test
    fun `returns coords of the matching cell when uncrossed`() {
        assertEquals(PlayerCard.Cell(0, 3), PlayerCard.findUncrossedCell(grid, uncrossed(), 33))
        assertEquals(PlayerCard.Cell(1, 0), PlayerCard.findUncrossedCell(grid, uncrossed(), 1))
    }

    @Test
    fun `returns null when the number isn't on the card`() {
        assertNull(PlayerCard.findUncrossedCell(grid, uncrossed(), 99))
        assertNull(PlayerCard.findUncrossedCell(grid, uncrossed(), 5))
    }

    @Test
    fun `returns null when the cell is already crossed`() {
        val crossed = uncrossed().map { it.toMutableList() }
        crossed[0][3] = true // 33 already marked
        assertNull(PlayerCard.findUncrossedCell(grid, crossed, 33))
    }

    @Test
    fun `ignores empty (0) cells even when num=0`() {
        assertNull(PlayerCard.findUncrossedCell(grid, uncrossed(), 0))
    }

    @Test
    fun `scans rows top-down, columns left-right`() {
        // Both row 0 col 1 and row 2 col 1 hold 11/12 — first match wins.
        assertEquals(PlayerCard.Cell(0, 1), PlayerCard.findUncrossedCell(grid, uncrossed(), 11))
    }
}
