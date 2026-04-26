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
 * Weighted random selection of a column index.
 * @param {number[]} weights
 * @returns {number}
 */
function randomANumberInRow(weights) {
  const tempWeight = [...weights];
  for (let i = 1; i < tempWeight.length; i++) {
    tempWeight[i] += tempWeight[i - 1];
  }
  const rand = Math.floor(Math.random() * tempWeight[tempWeight.length - 1]);
  for (let i = 0; i < tempWeight.length; i++) {
    if (rand < tempWeight[i]) return i;
  }
  return 0;
}

/**
 * Select NUM_PER_ROW columns for a row using weighted random. Mutates baseWeight.
 * @param {number[]} baseWeight
 * @returns {number[]}
 */
function randomARow(baseWeight) {
  const tempWeight = [...baseWeight];
  /** @type {number[]} */
  const selectedCols = [];
  for (let i = 0; i < NUM_PER_ROW; i++) {
    const col = randomANumberInRow(tempWeight);
    selectedCols.push(col);
    tempWeight[col] = 0;
    baseWeight[col]--;
  }
  return selectedCols;
}

/**
 * Pick `num` random numbers from column `col`'s range.
 * @param {number} num
 * @param {number} col
 * @returns {number[]}
 */
function randomNumbersInCol(num, col) {
  const arr = [...NUM_IN_COL[col]];
  arr.sort(() => 0.5 - Math.random());
  return arr.slice(0, num);
}

/**
 * Generate a 9x9 lô tô grid. Cell values: 0 = empty, >0 = number.
 * @returns {number[][]}
 */
export function generateGrid() {
  const cell = Array.from({ length: NUM_ROWS }, () =>
    new Array(NUM_COLS).fill(0)
  );
  const countNumPerCol = new Array(NUM_COLS).fill(0);
  const baseWeight = new Array(NUM_COLS).fill(6);

  for (let i = 0; i < NUM_ROWS; i++) {
    const newRow = randomARow(baseWeight);
    newRow.forEach((col) => {
      countNumPerCol[col]++;
      cell[i][col] = -1;
    });
  }

  for (let i = 0; i < NUM_COLS; i++) {
    const selectedNum = randomNumbersInCol(countNumPerCol[i], i);
    for (let j = 0; j < NUM_ROWS; j++) {
      if (cell[j][i] === -1) {
        cell[j][i] = selectedNum.shift() ?? 0;
      }
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
