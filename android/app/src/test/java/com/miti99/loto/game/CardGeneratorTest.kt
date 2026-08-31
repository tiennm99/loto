package com.miti99.loto.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ported one-to-one from `web/src/lib/game-logic.test.js` (generateGrid
 * describe blocks). Test names mirror the web `it` strings.
 */
class CardGeneratorTest {

    private val numRows = CardGenerator.NUM_ROWS
    private val numCols = CardGenerator.NUM_COLS
    private val numPerRow = CardGenerator.NUM_PER_ROW

    /** Column N (0-indexed) holds numbers in this tens range. */
    private val colRange = listOf(
        1 to 9, 10 to 19, 20 to 29, 30 to 39, 40 to 49,
        50 to 59, 60 to 69, 70 to 79, 80 to 90,
    )

    private fun rowSums(grid: List<List<Int>>) = grid.map { row -> row.count { it > 0 } }

    private fun colSums(grid: List<List<Int>>) =
        (0 until numCols).map { c -> grid.count { row -> row[c] > 0 } }

    // -- shape invariants --

    @Test
    fun `returns a 9x9 matrix`() {
        val g = CardGenerator.generateGrid()
        assertEquals(numRows, g.size)
        for (row in g) assertEquals(numCols, row.size)
    }

    @Test
    fun `each row has exactly 5 non-zero numbers`() {
        repeat(200) {
            assertEquals(List(numRows) { numPerRow }, rowSums(CardGenerator.generateGrid()))
        }
    }

    @Test
    fun `each column has exactly 5 non-zero numbers`() {
        repeat(200) {
            assertEquals(List(numCols) { numPerRow }, colSums(CardGenerator.generateGrid()))
        }
    }

    @Test
    fun `never produces duplicates in a single card`() {
        repeat(50) {
            val flat = CardGenerator.generateGrid().flatten().filter { it > 0 }
            assertEquals(flat.size, flat.toSet().size)
        }
    }

    // -- column number ranges (lô tô hội chợ Tân Tân) --

    @Test
    fun `each non-zero cell sits in its column's tens range`() {
        repeat(50) {
            val g = CardGenerator.generateGrid()
            for (r in 0 until numRows) {
                for (c in 0 until numCols) {
                    val n = g[r][c]
                    if (n == 0) continue
                    val (lo, hi) = colRange[c]
                    assertTrue("row=$r col=$c num=$n", n in lo..hi)
                }
            }
        }
    }

    @Test
    fun `numbers within each column are sorted ascending top-to-bottom`() {
        repeat(50) {
            val g = CardGenerator.generateGrid()
            for (c in 0 until numCols) {
                val colNums = g.map { it[c] }.filter { it > 0 }
                assertEquals(colNums.sorted(), colNums)
            }
        }
    }

    @Test
    fun `no row has 3 consecutive filled columns (rejection-sampled soft constraint)`() {
        repeat(300) { trial ->
            val g = CardGenerator.generateGrid()
            for (r in 0 until numRows) {
                for (c in 0 until numCols - 2) {
                    assertTrue(
                        "trial=$trial row=$r cols $c,${c + 1},${c + 2}",
                        !(g[r][c] > 0 && g[r][c + 1] > 0 && g[r][c + 2] > 0),
                    )
                }
            }
        }
    }

    @Test
    fun `col 0 only holds numbers from 1-9 (5 per card)`() {
        val g = CardGenerator.generateGrid()
        val col0 = g.map { it[0] }.filter { it > 0 }
        assertEquals(5, col0.size)
        for (n in col0) assertTrue(n in 1..9)
    }

    @Test
    fun `col 8 only holds numbers from 80-90 (5 per card)`() {
        val g = CardGenerator.generateGrid()
        val col8 = g.map { it[8] }.filter { it > 0 }
        assertEquals(5, col8.size)
        for (n in col8) assertTrue(n in 80..90)
    }

    // -- native-only property test with injected deterministic Random --

    @Test
    fun `1000 seeded cards all satisfy row, column and decade invariants`() {
        repeat(1000) { seed ->
            val g = CardGenerator.generateGrid(Random(seed))
            assertEquals("seed=$seed rows", List(numRows) { numPerRow }, rowSums(g))
            assertEquals("seed=$seed cols", List(numCols) { numPerRow }, colSums(g))
            for (r in 0 until numRows) {
                for (c in 0 until numCols) {
                    val n = g[r][c]
                    if (n == 0) continue
                    val (lo, hi) = colRange[c]
                    assertTrue("seed=$seed row=$r col=$c num=$n", n in lo..hi)
                }
            }
        }
    }

    @Test
    fun `same seed generates the same card`() {
        assertEquals(
            CardGenerator.generateGrid(Random(42)),
            CardGenerator.generateGrid(Random(42)),
        )
    }
}
