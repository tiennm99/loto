// @vitest-environment happy-dom
import { beforeEach, describe, expect, it } from "vitest";
import {
  generateGrid,
  getWaitingNumber,
  isRowComplete,
  loadCrossedState,
  loadGrid,
  saveCrossedState,
  saveGrid,
} from "./game-logic.js";

const NUM_ROWS = 9;
const NUM_COLS = 9;
const NUM_PER_ROW = 5;

/** Column N (0-indexed) holds numbers in this tens range. */
const COL_RANGE = [
  [1, 9],
  [10, 19],
  [20, 29],
  [30, 39],
  [40, 49],
  [50, 59],
  [60, 69],
  [70, 79],
  [80, 90],
];

/** @param {number[][]} grid */
function rowSums(grid) {
  return grid.map((r) => r.filter((n) => n > 0).length);
}
/** @param {number[][]} grid */
function colSums(grid) {
  return Array.from({ length: NUM_COLS }, (_, c) =>
    grid.reduce((s, r) => s + (r[c] > 0 ? 1 : 0), 0),
  );
}

describe("generateGrid — shape invariants", () => {
  it("returns a 9x9 matrix", () => {
    const g = generateGrid();
    expect(g).toHaveLength(NUM_ROWS);
    for (const row of g) expect(row).toHaveLength(NUM_COLS);
  });

  it("each row has exactly 5 non-zero numbers", () => {
    for (let trial = 0; trial < 200; trial++) {
      const sums = rowSums(generateGrid());
      expect(sums).toEqual(Array(NUM_ROWS).fill(NUM_PER_ROW));
    }
  });

  it("each column has exactly 5 non-zero numbers", () => {
    for (let trial = 0; trial < 200; trial++) {
      const sums = colSums(generateGrid());
      expect(sums).toEqual(Array(NUM_COLS).fill(NUM_PER_ROW));
    }
  });

  it("never produces duplicates in a single card", () => {
    for (let trial = 0; trial < 50; trial++) {
      const flat = generateGrid().flat().filter((n) => n > 0);
      expect(new Set(flat).size).toBe(flat.length);
    }
  });
});

describe("generateGrid — column number ranges (lô tô hội chợ Tân Tân)", () => {
  it("each non-zero cell sits in its column's tens range", () => {
    for (let trial = 0; trial < 50; trial++) {
      const g = generateGrid();
      for (let r = 0; r < NUM_ROWS; r++) {
        for (let c = 0; c < NUM_COLS; c++) {
          const n = g[r][c];
          if (n === 0) continue;
          const [lo, hi] = COL_RANGE[c];
          expect(n, `row=${r} col=${c} num=${n}`).toBeGreaterThanOrEqual(lo);
          expect(n, `row=${r} col=${c} num=${n}`).toBeLessThanOrEqual(hi);
        }
      }
    }
  });

  it("numbers within each column are sorted ascending top-to-bottom", () => {
    for (let trial = 0; trial < 50; trial++) {
      const g = generateGrid();
      for (let c = 0; c < NUM_COLS; c++) {
        const colNums = g.map((r) => r[c]).filter((n) => n > 0);
        const sorted = [...colNums].sort((a, b) => a - b);
        expect(colNums).toEqual(sorted);
      }
    }
  });

  it("no row has 3 consecutive filled columns (rejection-sampled soft constraint)", () => {
    for (let trial = 0; trial < 300; trial++) {
      const g = generateGrid();
      for (let r = 0; r < NUM_ROWS; r++) {
        for (let c = 0; c + 2 < NUM_COLS; c++) {
          expect(
            !(g[r][c] > 0 && g[r][c + 1] > 0 && g[r][c + 2] > 0),
            `trial=${trial} row=${r} cols ${c},${c + 1},${c + 2}`,
          ).toBe(true);
        }
      }
    }
  });

  it("col 0 only holds numbers from 1-9 (5 per card)", () => {
    const g = generateGrid();
    const col0 = g.map((r) => r[0]).filter((n) => n > 0);
    expect(col0).toHaveLength(5);
    for (const n of col0) expect(n).toBeGreaterThanOrEqual(1);
    for (const n of col0) expect(n).toBeLessThanOrEqual(9);
  });

  it("col 8 only holds numbers from 80-90 (5 per card)", () => {
    const g = generateGrid();
    const col8 = g.map((r) => r[8]).filter((n) => n > 0);
    expect(col8).toHaveLength(5);
    for (const n of col8) expect(n).toBeGreaterThanOrEqual(80);
    for (const n of col8) expect(n).toBeLessThanOrEqual(90);
  });
});

describe("isRowComplete", () => {
  /** @param {number[]} nums */
  function rowOf(nums) {
    return nums;
  }

  it("returns true when every number in the row is crossed", () => {
    const grid = [rowOf([0, 1, 0, 2, 0, 3, 0, 4, 5])];
    const crossed = [[false, true, false, true, false, true, false, true, true]];
    expect(isRowComplete(grid, crossed, 0)).toBe(true);
  });

  it("returns false when at least one number is uncrossed", () => {
    const grid = [rowOf([0, 1, 0, 2, 0, 3, 0, 4, 5])];
    const crossed = [[false, true, false, false, false, true, false, true, true]];
    expect(isRowComplete(grid, crossed, 0)).toBe(false);
  });

  it("returns false for an all-zero row (no numbers, not a win)", () => {
    const grid = [rowOf([0, 0, 0, 0, 0, 0, 0, 0, 0])];
    const crossed = [[false, false, false, false, false, false, false, false, false]];
    expect(isRowComplete(grid, crossed, 0)).toBe(false);
  });

  it("ignores 0 cells when checking crossed state", () => {
    const grid = [rowOf([0, 7, 0, 0, 0, 0, 0, 0, 0])];
    // 7 is crossed; the zeros are not (and shouldn't matter)
    const crossed = [[false, true, false, false, false, false, false, false, false]];
    expect(isRowComplete(grid, crossed, 0)).toBe(true);
  });
});

describe("saveGrid / loadGrid roundtrip", () => {
  beforeEach(() => localStorage.clear());

  it("persists and reloads a generated 9x9 grid by prefix", () => {
    const grid = generateGrid();
    saveGrid(grid, "loto");
    const loaded = loadGrid("loto");
    expect(loaded).toEqual(grid);
  });

  it("returns null when no grid is stored under the prefix", () => {
    expect(loadGrid("loto")).toBeNull();
  });

  it("isolates grids by prefix (player vs master card)", () => {
    const a = generateGrid();
    const b = generateGrid();
    saveGrid(a, "loto");
    saveGrid(b, "loto_master_card");
    expect(loadGrid("loto")).toEqual(a);
    expect(loadGrid("loto_master_card")).toEqual(b);
  });

  it("rejects a stored grid with wrong row count (corrupt shape → null)", () => {
    localStorage.setItem("loto_grid", JSON.stringify([[1, 2, 3]]));
    expect(loadGrid("loto")).toBeNull();
  });

  it("rejects a stored grid containing non-numbers (validator catches it)", () => {
    const bad = Array.from({ length: 9 }, () => Array(9).fill("x"));
    localStorage.setItem("loto_grid", JSON.stringify(bad));
    expect(loadGrid("loto")).toBeNull();
  });

  it("returns null for corrupt JSON without throwing", () => {
    localStorage.setItem("loto_grid", "{not json");
    expect(() => loadGrid("loto")).not.toThrow();
    expect(loadGrid("loto")).toBeNull();
  });
});

describe("saveCrossedState / loadCrossedState roundtrip", () => {
  beforeEach(() => localStorage.clear());

  /** @returns {boolean[][]} */
  function freshCrossed() {
    return Array.from({ length: 9 }, () => Array(9).fill(false));
  }

  it("persists and reloads a crossed-state matrix", () => {
    const c = freshCrossed();
    c[0][0] = true;
    c[8][8] = true;
    saveCrossedState(c, "loto");
    expect(loadCrossedState("loto")).toEqual(c);
  });

  it("returns null when nothing stored", () => {
    expect(loadCrossedState("loto")).toBeNull();
  });

  it("rejects non-boolean matrix (validator catches it)", () => {
    const bad = Array.from({ length: 9 }, () => Array(9).fill(0));
    localStorage.setItem("loto_crossed", JSON.stringify(bad));
    expect(loadCrossedState("loto")).toBeNull();
  });

  it("rejects wrong-shape matrix (8 rows instead of 9)", () => {
    const bad = Array.from({ length: 8 }, () => Array(9).fill(false));
    localStorage.setItem("loto_crossed", JSON.stringify(bad));
    expect(loadCrossedState("loto")).toBeNull();
  });
});

describe("getWaitingNumber", () => {
  it("returns the single uncrossed number when exactly one remains", () => {
    const grid = [[0, 1, 0, 2, 0, 3, 0, 4, 5]];
    const crossed = [[false, true, false, false, false, true, false, true, true]];
    expect(getWaitingNumber(grid, crossed, 0)).toBe(2);
  });

  it("returns null when more than one number remains", () => {
    const grid = [[0, 1, 0, 2, 0, 3, 0, 4, 5]];
    const crossed = [[false, true, false, false, false, false, false, true, true]];
    expect(getWaitingNumber(grid, crossed, 0)).toBeNull();
  });

  it("returns null when zero numbers remain (row complete)", () => {
    const grid = [[0, 1, 0, 2, 0, 3, 0, 4, 5]];
    const crossed = [[false, true, false, true, false, true, false, true, true]];
    expect(getWaitingNumber(grid, crossed, 0)).toBeNull();
  });

  it("returns null for an empty row", () => {
    const grid = [[0, 0, 0, 0, 0, 0, 0, 0, 0]];
    const crossed = [[false, false, false, false, false, false, false, false, false]];
    expect(getWaitingNumber(grid, crossed, 0)).toBeNull();
  });
});
