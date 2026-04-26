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
 * Choose which columns are filled in each row so that every row has exactly
 * NUM_PER_ROW filled cells AND every column ends up with exactly NUM_PER_ROW
 * filled cells. Forces any column whose remaining quota equals the number of
 * rows left — otherwise that column could not reach its target — then picks
 * the rest at random from columns with quota > 0. The forced set never
 * exceeds NUM_PER_ROW because total remaining quota = NUM_PER_ROW * rowsLeft.
 * @returns {number[][]}
 */
function pickFilledCols() {
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
    for (let i = candidates.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [candidates[i], candidates[j]] = [candidates[j], candidates[i]];
    }
    const selected = [
      ...forced,
      ...candidates.slice(0, NUM_PER_ROW - forced.length),
    ].sort((a, b) => a - b);
    for (const col of selected) quota[col]--;
    result.push(selected);
  }
  return result;
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
