// @vitest-environment happy-dom

import { describe, expect, it } from "vitest";
import { applyMasterCalls } from "./player-auto-cross.js";

/** @param {number} num */
function gridWith(num) {
  // 9x9 grid with `num` placed at (0,0) and zeros elsewhere — sufficient
  // for unit testing applyMasterCalls's flip path.
  const g = Array.from({ length: 9 }, () => new Array(9).fill(0));
  g[0][0] = num;
  return g;
}

function emptyCrossed() {
  return Array.from({ length: 9 }, () => new Array(9).fill(false));
}

describe("applyMasterCalls", () => {
  it("no-op when called[] is empty", () => {
    const grid = gridWith(7);
    const r = applyMasterCalls({
      grid,
      crossed: emptyCrossed(),
      called: [],
      lastHandledIndex: 0,
      manualUnticks: new Set(),
      mode: "both",
    });
    expect(r.changed).toBe(false);
    expect(r.lastHandledIndex).toBe(0);
  });

  it("no-op when cursor at length", () => {
    const r = applyMasterCalls({
      grid: gridWith(7),
      crossed: emptyCrossed(),
      called: [7],
      lastHandledIndex: 1,
      manualUnticks: new Set(),
      mode: "both",
    });
    expect(r.changed).toBe(false);
    expect(r.lastHandledIndex).toBe(1);
  });

  it("mode=player advances cursor without flipping", () => {
    const r = applyMasterCalls({
      grid: gridWith(7),
      crossed: emptyCrossed(),
      called: [7, 8, 9],
      lastHandledIndex: 0,
      manualUnticks: new Set(),
      mode: "player",
    });
    expect(r.changed).toBe(false);
    expect(r.lastHandledIndex).toBe(3);
    expect(r.crossed[0][0]).toBe(false);
  });

  it("mode=both crosses uncrossed cell on match", () => {
    const r = applyMasterCalls({
      grid: gridWith(7),
      crossed: emptyCrossed(),
      called: [7],
      lastHandledIndex: 0,
      manualUnticks: new Set(),
      mode: "both",
    });
    expect(r.changed).toBe(true);
    expect(r.lastHandledIndex).toBe(1);
    expect(r.crossed[0][0]).toBe(true);
  });

  it("mode=both replays full back-history when cursor=0", () => {
    const grid = Array.from({ length: 9 }, () => new Array(9).fill(0));
    grid[0][0] = 5;
    grid[1][1] = 12;
    grid[2][2] = 88;
    const r = applyMasterCalls({
      grid,
      crossed: emptyCrossed(),
      called: [5, 12, 88, 99 /* off-board */],
      lastHandledIndex: 0,
      manualUnticks: new Set(),
      mode: "both",
    });
    expect(r.changed).toBe(true);
    expect(r.lastHandledIndex).toBe(4);
    expect(r.crossed[0][0]).toBe(true);
    expect(r.crossed[1][1]).toBe(true);
    expect(r.crossed[2][2]).toBe(true);
  });

  it("manualUnticks numbers are skipped", () => {
    const r = applyMasterCalls({
      grid: gridWith(7),
      crossed: emptyCrossed(),
      called: [7],
      lastHandledIndex: 0,
      manualUnticks: new Set([7]),
      mode: "both",
    });
    expect(r.changed).toBe(false);
    expect(r.lastHandledIndex).toBe(1);
    expect(r.crossed[0][0]).toBe(false);
  });

  it("grid=null no-ops in both mode (still advances cursor)", () => {
    const r = applyMasterCalls({
      grid: null,
      crossed: [],
      called: [7],
      lastHandledIndex: 0,
      manualUnticks: new Set(),
      mode: "both",
    });
    expect(r.changed).toBe(false);
    expect(r.lastHandledIndex).toBe(1);
  });

  it("returns same crossed reference when no flip happens", () => {
    const crossed = emptyCrossed();
    const r = applyMasterCalls({
      grid: gridWith(7),
      crossed,
      called: [99 /* not on grid */],
      lastHandledIndex: 0,
      manualUnticks: new Set(),
      mode: "both",
    });
    expect(r.changed).toBe(false);
    expect(r.crossed).toBe(crossed);
  });
});
