/**
 * Pure helper for the master→player auto-tick path. Extracted from
 * PlayerBoard.svelte so the dedup-by-`at` invariant is unit-testable
 * without mounting the component.
 * @module lib/auto-tick
 */

import { findUncrossedCell } from "$lib/game-logic.js";

/**
 * Decide what `crossed` should become given a new bus draw.
 *
 * Always advance `lastHandledAt` to the draw's timestamp on a NEW draw,
 * even when no cell ends up flipped (mode mismatch, number off-board,
 * already crossed). This blocks reactive re-runs caused by `crossed` /
 * `grid` changes (manual untick, clear, regen) from re-firing the same
 * draw — only a fresh `at` should ever advance state.
 *
 * @param {object} args
 * @param {number[][] | null} args.grid
 * @param {boolean[][]} args.crossed
 * @param {{ num: number, at: number } | null} args.lastDraw
 * @param {number} args.lastHandledAt
 * @param {"player" | "master" | "both"} args.mode
 * @returns {{ crossed: boolean[][], lastHandledAt: number, changed: boolean }}
 */
export function processAutoTick({
  grid,
  crossed,
  lastDraw,
  lastHandledAt,
  mode,
}) {
  if (!lastDraw) return { crossed, lastHandledAt, changed: false };
  if (lastDraw.at === lastHandledAt) {
    return { crossed, lastHandledAt, changed: false };
  }
  // From here on, the draw is consumed: lastHandledAt advances.
  const advanced = lastDraw.at;
  if (mode !== "both") return { crossed, lastHandledAt: advanced, changed: false };
  if (!grid || crossed.length === 0) {
    return { crossed, lastHandledAt: advanced, changed: false };
  }
  const target = findUncrossedCell(grid, crossed, lastDraw.num);
  if (!target) return { crossed, lastHandledAt: advanced, changed: false };
  const updated = crossed.map((row, ri) =>
    ri === target.row
      ? row.map((v, ci) => (ci === target.col ? true : v))
      : row,
  );
  return { crossed: updated, lastHandledAt: advanced, changed: true };
}
