/**
 * Pure helper for the master→player auto-cross path. Reads the master's
 * full `called[]` history (not a single-slot bus) so player regen,
 * "Xoá đánh dấu", and reload can replay missed draws by passing
 * `lastHandledIndex: 0`.
 *
 * Cursor advances strictly even when no cell flips (mode mismatch,
 * manual untick, off-board) so the same draw never re-fires.
 *
 * @module lib/player-auto-cross
 */

import { findUncrossedCell } from "$lib/game-logic.js";

/**
 * @param {object} args
 * @param {number[][] | null} args.grid
 * @param {boolean[][]} args.crossed
 * @param {number[]} args.called               - master's full history
 * @param {number} args.lastHandledIndex       - index already consumed
 * @param {Set<number>} args.manualUnticks     - numbers user explicitly unticked
 * @param {"player" | "master" | "both"} args.mode
 * @returns {{ crossed: boolean[][], lastHandledIndex: number, changed: boolean }}
 */
export function applyMasterCalls({
  grid,
  crossed,
  called,
  lastHandledIndex,
  manualUnticks,
  mode,
}) {
  if (lastHandledIndex >= called.length) {
    return { crossed, lastHandledIndex, changed: false };
  }
  // In non-both modes the player isn't auto-following the master, but we
  // still advance the cursor — otherwise toggling player→both would dump
  // the entire back-history at once. Only future draws should auto-cross.
  if (mode !== "both" || !grid || crossed.length === 0) {
    return { crossed, lastHandledIndex: called.length, changed: false };
  }
  let next = crossed;
  let changed = false;
  for (let i = lastHandledIndex; i < called.length; i++) {
    const num = called[i];
    if (manualUnticks.has(num)) continue;
    const target = findUncrossedCell(grid, next, num);
    if (!target) continue;
    next = next.map((row, ri) =>
      ri === target.row
        ? row.map((v, ci) => (ci === target.col ? true : v))
        : row,
    );
    changed = true;
  }
  return { crossed: next, lastHandledIndex: called.length, changed };
}
