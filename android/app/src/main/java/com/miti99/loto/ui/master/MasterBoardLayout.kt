package com.miti99.loto.ui.master

/**
 * The 11×9 master tracking board, aligned by ones digit — ported from the
 * `buildBoard()` module block of `web/src/lib/MasterPanel.svelte`.
 * Row = ones digit, col = tens digit. Col 0 holds 1..9, col 8 holds 80..90
 * (90 sits alone in row 10, col 8). Empty slots = 0.
 */
object MasterBoardLayout {

    const val ROWS = 11
    const val COLS = 9

    val BOARD: List<List<Int>> = List(ROWS) { row ->
        List(COLS) { col ->
            when {
                row == 10 -> if (col == 8) 90 else 0
                row == 0 -> if (col > 0) col * 10 else 0
                else -> if (col == 0) row else col * 10 + row
            }
        }
    }
}
