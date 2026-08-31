package com.miti99.loto.ui.master

import org.junit.Assert.assertEquals
import org.junit.Test

/** Ones-digit cell mapping of the 11×9 master board, including the 90 edge cell. */
class MasterBoardLayoutTest {

    private val board = MasterBoardLayout.BOARD

    @Test
    fun `board is 11 rows by 9 columns`() {
        assertEquals(11, board.size)
        board.forEach { assertEquals(9, it.size) }
    }

    @Test
    fun `row 0 holds the round tens, col 0 blank`() {
        assertEquals(listOf(0, 10, 20, 30, 40, 50, 60, 70, 80), board[0])
    }

    @Test
    fun `rows 1-9 hold tens+ones with col 0 as bare ones digit`() {
        assertEquals(listOf(1, 11, 21, 31, 41, 51, 61, 71, 81), board[1])
        assertEquals(listOf(9, 19, 29, 39, 49, 59, 69, 79, 89), board[9])
        assertEquals(45, board[5][4])
    }

    @Test
    fun `90 sits alone in row 10 col 8`() {
        assertEquals(90, board[10][8])
        assertEquals(List(8) { 0 }, board[10].take(8))
    }

    @Test
    fun `board holds every number 1 to 90 exactly once`() {
        val nums = board.flatten().filter { it > 0 }
        assertEquals(90, nums.size)
        assertEquals((1..90).toSet(), nums.toSet())
    }
}
