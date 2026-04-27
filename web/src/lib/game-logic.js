/**
 * Lô tô card generation, persistence, and row-state helpers.
 * @module lib/game-logic
 */

/** Number ranges for each column (0-8) in the lô tô grid */
const NUM_IN_COL = [
  [1, 2, 3, 4, 5, 6, 7, 8, 9],
  [10, 11, 12, 13, 14, 15, 16, 17, 18, 19],
  [20, 21, 22, 23, 24, 25, 26, 27, 28, 29],
  [30, 31, 32, 33, 34, 35, 36, 37, 38, 39],
  [40, 41, 42, 43, 44, 45, 46, 47, 48, 49],
  [50, 51, 52, 53, 54, 55, 56, 57, 58, 59],
  [60, 61, 62, 63, 64, 65, 66, 67, 68, 69],
  [70, 71, 72, 73, 74, 75, 76, 77, 78, 79],
  [80, 81, 82, 83, 84, 85, 86, 87, 88, 89, 90],
];

const NUM_ROWS = 9;
const NUM_COLS = 9;
const NUM_PER_ROW = 5;

/**
 * Pick `num` random numbers from column `col`'s range.
 * @param {number} num
 * @param {number} col
 * @returns {number[]}
 */
function randomNumbersInCol(num, col) {
  const arr = [...NUM_IN_COL[col]];
  arr.sort(() => 0.5 - Math.random());
  // Pick `num` at random, then return them ascending so they sit
  // top-to-bottom in the column (lô tô hội chợ convention).
  return arr.slice(0, num).sort((a, b) => a - b);
}

/**
 * Sorted strictly-ascending column indices contain 3 consecutive integers?
 * Soft "no triple" constraint enforcer for a single row.
 * @param {number[]} cols
 */
function hasThreeInARow(cols) {
  for (let i = 0; i + 2 < cols.length; i++) {
    if (cols[i + 1] === cols[i] + 1 && cols[i + 2] === cols[i] + 2) return true;
  }
  return false;
}

/**
 * Enumerate every k-sized combination of `arr` (preserves input order).
 * @param {number[]} arr
 * @param {number} k
 * @returns {number[][]}
 */
function combinations(arr, k) {
  if (k === 0) return [[]];
  if (arr.length < k) return [];
  /** @type {number[][]} */
  const out = [];
  for (let i = 0; i <= arr.length - k; i++) {
    const head = arr[i];
    for (const tail of combinations(arr.slice(i + 1), k - 1)) {
      out.push([head, ...tail]);
    }
  }
  return out;
}

/**
 * One attempt at picking the row-by-row column selection. Per-row picker
 * prefers triple-free completions; if any row's forced set is already a
 * triple (or no completion is triple-free), that row falls back to an
 * unconstrained pick so the hard column-quota invariant never breaks.
 * @returns {number[][]}
 */
function pickFilledColsOnce() {
  const quota = new Array(NUM_COLS).fill(NUM_PER_ROW);
  /** @type {number[][]} */
  const result = [];
  for (let row = 0; row < NUM_ROWS; row++) {
    const rowsLeft = NUM_ROWS - row;
    /** @type {number[]} */
    const forced = [];
    /** @type {number[]} */
    const candidates = [];
    for (let col = 0; col < NUM_COLS; col++) {
      if (quota[col] === rowsLeft) forced.push(col);
      else if (quota[col] > 0) candidates.push(col);
    }
    const need = NUM_PER_ROW - forced.length;

    /** @type {number[][]} */
    const validCompletions = [];
    if (!hasThreeInARow(forced)) {
      for (const combo of combinations(candidates, need)) {
        const merged = [...forced, ...combo].sort((a, b) => a - b);
        if (!hasThreeInARow(merged)) validCompletions.push(merged);
      }
    }

    let selected;
    if (validCompletions.length > 0) {
      selected =
        validCompletions[Math.floor(Math.random() * validCompletions.length)];
    } else {
      for (let i = candidates.length - 1; i > 0; i--) {
        const j = Math.floor(Math.random() * (i + 1));
        [candidates[i], candidates[j]] = [candidates[j], candidates[i]];
      }
      selected = [...forced, ...candidates.slice(0, need)].sort((a, b) => a - b);
    }

    for (const col of selected) quota[col]--;
    result.push(selected);
  }
  return result;
}

/**
 * Choose which columns are filled in each row so that every row has exactly
 * NUM_PER_ROW filled cells AND every column ends up with exactly NUM_PER_ROW
 * filled cells. Soft constraint: no row has 3 consecutive filled columns.
 *
 * Strategy: per-row picker greedily prefers triple-free completions. Because
 * early-row choices can still corner late rows into a forced triple, we wrap
 * the whole pass in rejection sampling. If every attempt fails (extremely
 * rare), the last attempt is returned — column quotas hold either way.
 * @returns {number[][]}
 */
function pickFilledCols() {
  const MAX_ATTEMPTS = 200;
  let last = pickFilledColsOnce();
  for (let attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
    if (last.every((row) => !hasThreeInARow(row))) return last;
    last = pickFilledColsOnce();
  }
  return last;
}

/**
 * Generate a 9x9 lô tô grid with exactly NUM_PER_ROW filled cells per row
 * AND per column. Cell values: 0 = empty, >0 = number.
 * @returns {number[][]}
 */
export function generateGrid() {
  const cell = Array.from({ length: NUM_ROWS }, () =>
    new Array(NUM_COLS).fill(0)
  );
  const colsPerRow = pickFilledCols();
  for (let row = 0; row < NUM_ROWS; row++) {
    for (const col of colsPerRow[row]) cell[row][col] = -1;
  }
  for (let col = 0; col < NUM_COLS; col++) {
    const picked = randomNumbersInCol(NUM_PER_ROW, col);
    for (let row = 0; row < NUM_ROWS; row++) {
      if (cell[row][col] === -1) cell[row][col] = picked.shift() ?? 0;
    }
  }
  return cell;
}

/**
 * Parse JSON and validate its shape with a runtime guard. Returns null on
 * either parse failure or validation failure.
 * @param {string | null} raw
 * @param {(v: any) => boolean} validate
 */
function safeParse(raw, validate) {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    return validate(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

/** @param {any} v */
function isNumberMatrix(v) {
  return (
    Array.isArray(v) &&
    v.length === NUM_ROWS &&
    v.every(
      (row) =>
        Array.isArray(row) &&
        row.length === NUM_COLS &&
        row.every((n) => typeof n === "number")
    )
  );
}

/** @param {any} v */
function isBoolMatrix(v) {
  return (
    Array.isArray(v) &&
    v.length === NUM_ROWS &&
    v.every(
      (row) =>
        Array.isArray(row) &&
        row.length === NUM_COLS &&
        row.every((b) => typeof b === "boolean")
    )
  );
}

/**
 * @param {number[][]} grid
 * @param {string} [prefix]
 */
export function saveGrid(grid, prefix = "loto") {
  try {
    localStorage.setItem(`${prefix}_grid`, JSON.stringify(grid));
  } catch {
    // localStorage disabled or quota exceeded — ignore, app still works in-memory
  }
}

/**
 * @param {string} [prefix]
 * @returns {number[][] | null}
 */
export function loadGrid(prefix = "loto") {
  try {
    return safeParse(localStorage.getItem(`${prefix}_grid`), isNumberMatrix);
  } catch {
    return null;
  }
}

/**
 * @param {boolean[][]} crossed
 * @param {string} [prefix]
 */
export function saveCrossedState(crossed, prefix = "loto") {
  try {
    localStorage.setItem(`${prefix}_crossed`, JSON.stringify(crossed));
  } catch {
    // see saveGrid
  }
}

/**
 * @param {string} [prefix]
 * @returns {boolean[][] | null}
 */
export function loadCrossedState(prefix = "loto") {
  try {
    return safeParse(localStorage.getItem(`${prefix}_crossed`), isBoolMatrix);
  } catch {
    return null;
  }
}

/**
 * Check if a row has all its numbers crossed (and has at least one number).
 * @param {number[][]} grid
 * @param {boolean[][]} crossed
 * @param {number} row
 * @returns {boolean}
 */
export function isRowComplete(grid, crossed, row) {
  let hasNumber = false;
  for (let col = 0; col < NUM_COLS; col++) {
    if (grid[row][col] > 0) {
      hasNumber = true;
      if (!crossed[row]?.[col]) return false;
    }
  }
  return hasNumber;
}

/**
 * Find the single remaining uncrossed number in a row, or null if != 1 remaining.
 * @param {number[][]} grid
 * @param {boolean[][]} crossed
 * @param {number} row
 * @returns {number | null}
 */
export function getWaitingNumber(grid, crossed, row) {
  /** @type {number | null} */
  let remaining = null;
  for (let col = 0; col < NUM_COLS; col++) {
    if (grid[row][col] > 0 && !crossed[row]?.[col]) {
      if (remaining !== null) return null;
      remaining = grid[row][col];
    }
  }
  return remaining;
}
