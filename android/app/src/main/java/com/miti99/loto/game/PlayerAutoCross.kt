package com.miti99.loto.game

import com.miti99.loto.settings.AppMode

/**
 * Pure helper for the master→player auto-cross path, ported one-to-one from
 * `web/src/lib/player-auto-cross.js`. Reads the master's full call history
 * (not a single-slot bus) so player regen, "Xoá đánh dấu", and restore can
 * replay missed draws by passing `lastHandledIndex = 0`.
 *
 * The cursor advances strictly even when no cell flips (mode mismatch,
 * manual untick, off-board) so the same draw never re-fires.
 */
object PlayerAutoCross {

    data class Result(
        val crossed: List<List<Boolean>>,
        val lastHandledIndex: Int,
        val changed: Boolean,
    )

    fun applyMasterCalls(
        grid: List<List<Int>>?,
        crossed: List<List<Boolean>>,
        called: List<Int>,
        lastHandledIndex: Int,
        manualUnticks: Set<Int>,
        mode: AppMode,
    ): Result {
        // Hardening beyond the web: a shrunken history means the round was
        // reset, so a stale cursor restarts from 0 instead of silently
        // disabling auto-cross for the rest of the new round.
        val cursor = if (lastHandledIndex > called.size) 0 else lastHandledIndex
        if (cursor >= called.size) {
            return Result(crossed, cursor, changed = false)
        }
        // In non-both modes the player isn't auto-following the master, but
        // the cursor still advances — otherwise toggling player→both would
        // dump the entire back-history at once. Only future draws auto-cross.
        if (mode != AppMode.BOTH || grid == null || crossed.isEmpty()) {
            return Result(crossed, called.size, changed = false)
        }
        var next = crossed
        var changed = false
        for (i in cursor until called.size) {
            val num = called[i]
            if (manualUnticks.contains(num)) continue
            val target = PlayerCard.findUncrossedCell(grid, next, num) ?: continue
            next = next.mapIndexed { ri, row ->
                if (ri == target.row) {
                    row.mapIndexed { ci, v -> if (ci == target.col) true else v }
                } else {
                    row
                }
            }
            changed = true
        }
        return Result(next, called.size, changed)
    }
}
