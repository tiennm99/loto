package com.miti99.loto.game

import kotlin.random.Random

/**
 * Lô tô hội chợ Tân Tân card generator and row-state helpers.
 *
 * Pure Kotlin port of `tiennm99/loto/src/lib/game-logic.js`. Persistence
 * (saveGrid/loadGrid) lives in DataStore (phase 05), not here.
 */
object GameLogic {

    const val NUM_ROWS = 9
    const val NUM_COLS = 9
    const val NUM_PER_ROW = 5

    /** Number ranges per column. Col 8 holds 80..90 (11 candidates). */
    private val NUM_IN_COL: List<IntRange> = listOf(
        1..9, 10..19, 20..29, 30..39, 40..49,
        50..59, 60..69, 70..79, 80..90,
    )

    private const val MAX_REJECTION_ATTEMPTS = 200

    /**
     * Generate a 9x9 grid with exactly NUM_PER_ROW filled cells per row AND
     * per column. Cell value: 0 = empty, >0 = number.
     *
     * Soft constraint (rejection-sampled): no row has 3 consecutive filled
     * column indices. Hard column-quota invariant always wins if both can't
     * be satisfied — matches JS behavior.
     */
    fun generateGrid(random: Random = Random.Default): Array<IntArray> {
        val cells = Array(NUM_ROWS) { IntArray(NUM_COLS) }
        val colsPerRow = pickFilledCols(random)
        for (row in 0 until NUM_ROWS) {
            for (col in colsPerRow[row]) cells[row][col] = -1
        }
        for (col in 0 until NUM_COLS) {
            val picked = randomNumbersInCol(NUM_PER_ROW, col, random).toMutableList()
            for (row in 0 until NUM_ROWS) {
                if (cells[row][col] == -1) {
                    cells[row][col] = picked.removeAt(0)
                }
            }
        }
        return cells
    }

    /** Row complete iff it has ≥1 number AND every number is crossed. */
    fun isRowComplete(
        grid: Array<IntArray>,
        crossed: Array<BooleanArray>,
        row: Int,
    ): Boolean {
        var hasNumber = false
        for (col in 0 until NUM_COLS) {
            if (grid[row][col] > 0) {
                hasNumber = true
                if (!crossed[row][col]) return false
            }
        }
        return hasNumber
    }

    /**
     * The lone uncrossed number in a row, or null when 0 or ≥2 cells remain.
     * Drives the "Chờ N" toast.
     */
    fun getWaitingNumber(
        grid: Array<IntArray>,
        crossed: Array<BooleanArray>,
        row: Int,
    ): Int? {
        var remaining: Int? = null
        for (col in 0 until NUM_COLS) {
            if (grid[row][col] > 0 && !crossed[row][col]) {
                if (remaining != null) return null
                remaining = grid[row][col]
            }
        }
        return remaining
    }

    // ----- internal helpers (1:1 with game-logic.js:29-138) -----

    /** Pick `num` random values from column `col`'s range, sorted ascending. */
    private fun randomNumbersInCol(num: Int, col: Int, random: Random): List<Int> {
        val candidates = NUM_IN_COL[col].toMutableList()
        candidates.shuffle(random)
        return candidates.subList(0, num).sorted()
    }

    /** True when sorted-ascending column indices contain 3 consecutive integers. */
    private fun hasThreeInARow(cols: List<Int>): Boolean {
        for (i in 0..cols.size - 3) {
            if (cols[i + 1] == cols[i] + 1 && cols[i + 2] == cols[i] + 2) return true
        }
        return false
    }

    /** Every k-sized combination of `arr` (preserves input order). */
    private fun combinations(arr: List<Int>, k: Int): List<List<Int>> {
        if (k == 0) return listOf(emptyList())
        if (arr.size < k) return emptyList()
        val out = mutableListOf<List<Int>>()
        for (i in 0..arr.size - k) {
            val head = arr[i]
            for (tail in combinations(arr.subList(i + 1, arr.size), k - 1)) {
                out.add(listOf(head) + tail)
            }
        }
        return out
    }

    /**
     * Single-attempt per-row column picker. Prefers triple-free completions;
     * falls back to forced set when no triple-free completion exists so the
     * column-quota hard invariant never breaks.
     */
    private fun pickFilledColsOnce(random: Random): List<List<Int>> {
        val quota = IntArray(NUM_COLS) { NUM_PER_ROW }
        val result = mutableListOf<List<Int>>()
        for (row in 0 until NUM_ROWS) {
            val rowsLeft = NUM_ROWS - row
            val forced = mutableListOf<Int>()
            val candidates = mutableListOf<Int>()
            for (col in 0 until NUM_COLS) {
                when {
                    quota[col] == rowsLeft -> forced.add(col)
                    quota[col] > 0 -> candidates.add(col)
                }
            }
            val need = NUM_PER_ROW - forced.size

            val validCompletions = mutableListOf<List<Int>>()
            if (!hasThreeInARow(forced)) {
                for (combo in combinations(candidates, need)) {
                    val merged = (forced + combo).sorted()
                    if (!hasThreeInARow(merged)) validCompletions.add(merged)
                }
            }

            val selected = if (validCompletions.isNotEmpty()) {
                validCompletions[random.nextInt(validCompletions.size)]
            } else {
                val shuffled = candidates.toMutableList().also { it.shuffle(random) }
                (forced + shuffled.subList(0, need)).sorted()
            }

            for (col in selected) quota[col]--
            result.add(selected)
        }
        return result
    }

    /** Wraps `pickFilledColsOnce` in rejection sampling. */
    private fun pickFilledCols(random: Random): List<List<Int>> {
        var last = pickFilledColsOnce(random)
        repeat(MAX_REJECTION_ATTEMPTS) {
            if (last.all { !hasThreeInARow(it) }) return last
            last = pickFilledColsOnce(random)
        }
        return last
    }
}
