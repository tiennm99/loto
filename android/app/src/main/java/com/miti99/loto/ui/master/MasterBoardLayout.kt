package com.miti99.loto.ui.master

/**
 * 11×9 master board layout.
 *
 * Row index = ones digit (0..10); column index = tens digit (0..8).
 *
 * Rules (port of MasterPanel.svelte:10-30):
 *   row=0, col=0        → 0  (empty corner)
 *   row=0, col=1..8     → col*10  (10, 20, …, 80)
 *   row=1..9, col=0     → row  (1..9)
 *   row=1..9, col=1..8  → col*10 + row
 *   row=10, col=8       → 90
 *   row=10, other cols  → 0  (empty)
 */
internal val MASTER_BOARD: List<List<Int>> = buildList {
    for (row in 0..10) {
        add(buildList {
            for (col in 0..8) {
                val num = when {
                    row == 10 && col == 8 -> 90
                    row == 10            -> 0
                    row == 0 && col > 0  -> col * 10
                    row == 0             -> 0
                    col == 0             -> row
                    else                 -> col * 10 + row
                }
                add(num)
            }
        })
    }
}
