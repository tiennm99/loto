// @vitest-environment happy-dom
import { describe, expect, it } from "vitest";
import { processAutoTick } from "./auto-tick.js";

/**
 * Build a minimal grid where row 0 col 2 = 42 and row 2 col 5 = 17.
 * Other cells are 0 (empty).
 */
function makeGrid() {
  const grid = Array.from({ length: 9 }, () => new Array(9).fill(0));
  grid[0][2] = 42;
  grid[2][5] = 17;
  return grid;
}

function makeCrossed() {
  return Array.from({ length: 9 }, () => new Array(9).fill(false));
}

describe("processAutoTick", () => {
  it("crosses the cell on a NEW draw when mode=both", () => {
    const grid = makeGrid();
    const crossed = makeCrossed();
    const result = processAutoTick({
      grid,
      crossed,
      lastDraw: { num: 42, at: 1000 },
      lastHandledAt: 0,
      mode: "both",
    });
    expect(result.changed).toBe(true);
    expect(result.lastHandledAt).toBe(1000);
    expect(result.crossed[0][2]).toBe(true);
    // Original input not mutated (immutable update).
    expect(crossed[0][2]).toBe(false);
  });

  it("ignores a re-fire with the same `at` (dedup guard)", () => {
    const crossed = makeCrossed();
    const result = processAutoTick({
      grid: makeGrid(),
      crossed,
      lastDraw: { num: 42, at: 5000 },
      lastHandledAt: 5000,
      mode: "both",
    });
    expect(result.changed).toBe(false);
    expect(result.lastHandledAt).toBe(5000);
    expect(result.crossed).toBe(crossed);
  });

  it("re-crosses on a NEW `at` after a manual untick", () => {
    // Step 1: auto-tick fires.
    const grid = makeGrid();
    const first = processAutoTick({
      grid,
      crossed: makeCrossed(),
      lastDraw: { num: 42, at: 1000 },
      lastHandledAt: 0,
      mode: "both",
    });
    expect(first.crossed[0][2]).toBe(true);

    // Step 2: user manually unticks (simulated by editing).
    const manualUntick = first.crossed.map((row) => row.slice());
    manualUntick[0][2] = false;

    // Step 3: same number arrives with NEW timestamp → re-crosses.
    const second = processAutoTick({
      grid,
      crossed: manualUntick,
      lastDraw: { num: 42, at: 2000 },
      lastHandledAt: first.lastHandledAt,
      mode: "both",
    });
    expect(second.changed).toBe(true);
    expect(second.lastHandledAt).toBe(2000);
    expect(second.crossed[0][2]).toBe(true);
  });

  it("ignores draws when mode=master but still consumes the timestamp", () => {
    const crossed = makeCrossed();
    const result = processAutoTick({
      grid: makeGrid(),
      crossed,
      lastDraw: { num: 42, at: 4000 },
      lastHandledAt: 0,
      mode: "master",
    });
    expect(result.changed).toBe(false);
    expect(result.crossed).toBe(crossed);
    expect(result.lastHandledAt).toBe(4000);
  });

  it("ignores draws when mode=player but still consumes the timestamp", () => {
    const crossed = makeCrossed();
    const result = processAutoTick({
      grid: makeGrid(),
      crossed,
      lastDraw: { num: 42, at: 9000 },
      lastHandledAt: 0,
      mode: "player",
    });
    expect(result.changed).toBe(false);
    expect(result.crossed).toBe(crossed);
    // lastHandledAt still advances — solo player switching to "both"
    // mid-game shouldn't replay a stale draw.
    expect(result.lastHandledAt).toBe(9000);
  });

  it("no-ops when the number is not on the grid", () => {
    const crossed = makeCrossed();
    const result = processAutoTick({
      grid: makeGrid(),
      crossed,
      lastDraw: { num: 88, at: 3000 },
      lastHandledAt: 0,
      mode: "both",
    });
    expect(result.changed).toBe(false);
    expect(result.crossed).toBe(crossed);
    expect(result.lastHandledAt).toBe(3000);
  });

  it("returns unchanged state when lastDraw is null", () => {
    const crossed = makeCrossed();
    const result = processAutoTick({
      grid: makeGrid(),
      crossed,
      lastDraw: null,
      lastHandledAt: 1234,
      mode: "both",
    });
    expect(result.changed).toBe(false);
    expect(result.crossed).toBe(crossed);
    expect(result.lastHandledAt).toBe(1234);
  });

  it("no-ops when grid is null or crossed is empty", () => {
    const result = processAutoTick({
      grid: null,
      crossed: [],
      lastDraw: { num: 42, at: 7000 },
      lastHandledAt: 0,
      mode: "both",
    });
    expect(result.changed).toBe(false);
    expect(result.lastHandledAt).toBe(7000);
  });
});
