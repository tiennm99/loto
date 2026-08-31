package com.miti99.loto.game

import kotlin.random.Random

/**
 * Lô tô card generation, ported one-to-one from the web app's
 * `web/src/lib/game-logic.js` (`generateGrid` and its helpers). The web
 * implementation is the behavioral spec; any change there must be mirrored
 * here.
 */
object CardGenerator {

    const val NUM_ROWS = 9
    const val NUM_COLS = 9
    const val NUM_PER_ROW = 5

    /** Number ranges for each column (0-8) in the lô tô grid. */
    private val NUM_IN_COL: List<List<Int>> = listOf(
        (1..9).toList(),
        (10..19).toList(),
        (20..29).toList(),
        (30..39).toList(),
        (40..49).toList(),
        (50..59).toList(),
        (60..69).toList(),
        (70..79).toList(),
        (80..90).toList(),
    )

    /**
     * Generate a 9x9 lô tô grid with exactly [NUM_PER_ROW] filled cells per
     * row AND per column. Cell values: 0 = empty, >0 = number.
     */
    fun generateGrid(random: Random = Random.Default): List<List<Int>> {
        val cell = Array(NUM_ROWS) { IntArray(NUM_COLS) }
        val colsPerRow = pickFilledCols(random)
        for (row in 0 until NUM_ROWS) {
            for (col in colsPerRow[row]) cell[row][col] = -1
        }
        for (col in 0 until NUM_COLS) {
            val picked = ArrayDeque(randomNumbersInCol(NUM_PER_ROW, col, random))
            for (row in 0 until NUM_ROWS) {
                if (cell[row][col] == -1) cell[row][col] = picked.removeFirstOrNull() ?: 0
            }
        }
        return cell.map { it.toList() }
    }

    /**
     * Pick [num] random numbers from column [col]'s range, returned ascending
     * so they sit top-to-bottom in the column (lô tô hội chợ convention).
     */
    private fun randomNumbersInCol(num: Int, col: Int, random: Random): List<Int> {
        val arr = NUM_IN_COL[col].toMutableList()
        // Fisher-Yates, matching the web implementation exactly.
        for (i in arr.indices.reversed()) {
            if (i == 0) break
            val j = random.nextInt(i + 1)
            val tmp = arr[i]
            arr[i] = arr[j]
            arr[j] = tmp
        }
        return arr.take(num).sorted()
    }

    /** Sorted strictly-ascending column indices contain 3 consecutive integers? */
    internal fun hasThreeInARow(cols: List<Int>): Boolean {
        for (i in 0..cols.size - 3) {
            if (cols[i + 1] == cols[i] + 1 && cols[i + 2] == cols[i] + 2) return true
        }
        return false
    }

    /** Enumerate every k-sized combination of [arr] (preserves input order). */
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
     * One attempt at picking the row-by-row column selection. Per-row picker
     * prefers triple-free completions; if any row's forced set is already a
     * triple (or no completion is triple-free), that row falls back to an
     * unconstrained pick so the hard column-quota invariant never breaks.
     */
    private fun pickFilledColsOnce(random: Random): List<List<Int>> {
        val quota = IntArray(NUM_COLS) { NUM_PER_ROW }
        val result = mutableListOf<List<Int>>()
        for (row in 0 until NUM_ROWS) {
            val rowsLeft = NUM_ROWS - row
            val forced = mutableListOf<Int>()
            val candidates = mutableListOf<Int>()
            for (col in 0 until NUM_COLS) {
                if (quota[col] == rowsLeft) forced.add(col)
                else if (quota[col] > 0) candidates.add(col)
            }
            val need = NUM_PER_ROW - forced.size

            val validCompletions = mutableListOf<List<Int>>()
            if (!hasThreeInARow(forced)) {
                for (combo in combinations(candidates, need)) {
                    val merged = (forced + combo).sorted()
                    if (!hasThreeInARow(merged)) validCompletions.add(merged)
                }
            }

            val selected: List<Int>
            if (validCompletions.isNotEmpty()) {
                selected = validCompletions[random.nextInt(validCompletions.size)]
            } else {
                for (i in candidates.indices.reversed()) {
                    if (i == 0) break
                    val j = random.nextInt(i + 1)
                    val tmp = candidates[i]
                    candidates[i] = candidates[j]
                    candidates[j] = tmp
                }
                selected = (forced + candidates.take(need)).sorted()
            }

            for (col in selected) quota[col]--
            result.add(selected)
        }
        return result
    }

    /**
     * Choose which columns are filled in each row so that every row has
     * exactly [NUM_PER_ROW] filled cells AND every column ends up with exactly
     * [NUM_PER_ROW] filled cells. Soft constraint: no row has 3 consecutive
     * filled columns.
     *
     * Strategy: per-row picker greedily prefers triple-free completions.
     * Because early-row choices can still corner late rows into a forced
     * triple, the whole pass is wrapped in rejection sampling. If every
     * attempt fails (extremely rare), the last attempt is returned — column
     * quotas hold either way.
     */
    private fun pickFilledCols(random: Random): List<List<Int>> {
        val maxAttempts = 200
        var last = pickFilledColsOnce(random)
        repeat(maxAttempts) {
            if (last.all { !hasThreeInARow(it) }) return last
            last = pickFilledColsOnce(random)
        }
        return last
    }
}
