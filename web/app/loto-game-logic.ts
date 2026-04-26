/** Number ranges for each column (0-8) in the lô tô grid */
const NUM_IN_COL: number[][] = [
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

/** Weighted random selection of a column index */
function randomANumberInRow(weights: number[]): number {
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

/** Select NUM_PER_ROW columns for a row using weighted random */
function randomARow(baseWeight: number[]): number[] {
  const tempWeight = [...baseWeight];
  const selectedCols: number[] = [];
  for (let i = 0; i < NUM_PER_ROW; i++) {
    const col = randomANumberInRow(tempWeight);
    selectedCols.push(col);
    tempWeight[col] = 0;
    baseWeight[col]--;
  }
  return selectedCols;
}

/** Pick random numbers from a column's range */
function randomNumbersInCol(num: number, col: number): number[] {
  const arr = [...NUM_IN_COL[col]];
  arr.sort(() => 0.5 - Math.random());
  return arr.slice(0, num);
}

/** Generate a 9x9 lô tô grid. Returns cell values (0 = empty, >0 = number). */
export function generateGrid(): number[][] {
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

function safeParse<T>(raw: string | null, validate: (v: unknown) => v is T): T | null {
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw) as unknown;
    return validate(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

function isNumberMatrix(v: unknown): v is number[][] {
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

function isBoolMatrix(v: unknown): v is boolean[][] {
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

export function saveGrid(grid: number[][], prefix = "loto"): void {
  try {
    localStorage.setItem(`${prefix}_grid`, JSON.stringify(grid));
  } catch {
    // localStorage disabled or quota exceeded — ignore, app still works in-memory
  }
}

export function loadGrid(prefix = "loto"): number[][] | null {
  try {
    return safeParse(localStorage.getItem(`${prefix}_grid`), isNumberMatrix);
  } catch {
    return null;
  }
}

export function saveCrossedState(crossed: boolean[][], prefix = "loto"): void {
  try {
    localStorage.setItem(`${prefix}_crossed`, JSON.stringify(crossed));
  } catch {
    // see saveGrid
  }
}

export function loadCrossedState(prefix = "loto"): boolean[][] | null {
  try {
    return safeParse(localStorage.getItem(`${prefix}_crossed`), isBoolMatrix);
  } catch {
    return null;
  }
}

/** Check if a row has all its numbers crossed (and has at least one number) */
export function isRowComplete(
  grid: number[][],
  crossed: boolean[][],
  row: number
): boolean {
  let hasNumber = false;
  for (let col = 0; col < NUM_COLS; col++) {
    if (grid[row][col] > 0) {
      hasNumber = true;
      if (!crossed[row]?.[col]) return false;
    }
  }
  return hasNumber;
}

/** Find the single remaining uncrossed number in a row, or null if != 1 remaining */
export function getWaitingNumber(
  grid: number[][],
  crossed: boolean[][],
  row: number
): number | null {
  let remaining: number | null = null;
  for (let col = 0; col < NUM_COLS; col++) {
    if (grid[row][col] > 0 && !crossed[row]?.[col]) {
      if (remaining !== null) return null;
      remaining = grid[row][col];
    }
  }
  return remaining;
}
