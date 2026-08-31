package com.miti99.loto.game

/**
 * Row-state helpers over a player card, ported one-to-one from
 * `web/src/lib/game-logic.js`. A card is a 9x9 grid (0 = empty cell,
 * >0 = number) plus a parallel crossed matrix.
 *
 * Row semantics: chờ (waiting) = exactly one uncrossed number remains in a
 * row; kinh (win) = every number in a row is crossed. Both are row-based,
 * never full-card.
 */
object PlayerCard {

    /** Coordinates of a cell on the 9x9 grid. */
    data class Cell(val row: Int, val col: Int)

    /**
     * Locate the first non-crossed cell holding [num] on the grid, scanning
     * rows top-down and columns left-right. Returns null when the number is
     * absent or already crossed everywhere. Used by the master→player
     * auto-tick path.
     */
    fun findUncrossedCell(grid: List<List<Int>>, crossed: List<List<Boolean>>, num: Int): Cell? {
        if (num <= 0) return null
        for (r in grid.indices) {
            val row = grid[r]
            for (c in row.indices) {
                if (row[c] == num && crossed.getOrNull(r)?.getOrNull(c) != true) {
                    return Cell(r, c)
                }
            }
        }
        return null
    }

    /** Check if a row has all its numbers crossed (and has at least one number). */
    fun isRowComplete(grid: List<List<Int>>, crossed: List<List<Boolean>>, row: Int): Boolean {
        var hasNumber = false
        for (col in 0 until CardGenerator.NUM_COLS) {
            if (grid[row][col] > 0) {
                hasNumber = true
                if (crossed.getOrNull(row)?.getOrNull(col) != true) return false
            }
        }
        return hasNumber
    }

    /**
     * Find the single remaining uncrossed number in a row, or null when the
     * remaining count differs from 1.
     */
    fun getWaitingNumber(grid: List<List<Int>>, crossed: List<List<Boolean>>, row: Int): Int? {
        var remaining: Int? = null
        for (col in 0 until CardGenerator.NUM_COLS) {
            if (grid[row][col] > 0 && crossed.getOrNull(row)?.getOrNull(col) != true) {
                if (remaining != null) return null
                remaining = grid[row][col]
            }
        }
        return remaining
    }
}
