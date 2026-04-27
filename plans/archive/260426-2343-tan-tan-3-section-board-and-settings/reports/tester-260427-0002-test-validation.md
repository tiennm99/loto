# Test Validation Report — SvelteKit Lô Tô Test Infrastructure

**Date:** 2026-04-27  
**Scope:** Fresh vitest setup, 2 test files (game-logic.test.js, settings-store.test.js)

---

## Executive Summary

✅ **28/28 tests pass** across 2 files with consistent, reproducible results (no flakiness detected).
✅ **svelte-check:** 0 errors, 0 warnings.
✅ **Build:** Production static export succeeds.
❌ **Lint:** 1 error — eslint config missing Svelte 5 rune globals.

**Coverage:** Game logic core + settings store fully tested. **Storage helpers (`saveGrid`, `loadGrid`, `saveCrossedState`, `loadCrossedState`) are currently untested** — risky gap for persistence layer.

---

## Test Execution Results

### npm test
```
✓ 28 passed (28)
✓ 2 test files
Duration: 2.09s (tests: 323ms)
```

Confirmed stable across 3 consecutive runs:
- Run 1: 28 passed
- Run 2: 28 passed
- Run 3: 28 passed

**No flakiness detected.**

### svelte-check
```
COMPLETED 5 FILES
0 ERRORS | 0 WARNINGS
```

✅ Passes threshold.

### npm run build
```
✓ built in 1.14s (client)
✓ built in 5.35s (server)
✓ Wrote site to "build" (static adapter)
```

✅ Production build succeeds.

### npm run lint
```
❌ FAILED
/config/workspace/tiennm99/loto/src/lib/settings-store.svelte.js
  18:25  error  '$state' is not defined  no-undef
```

**Issue:** eslint config (eslint.config.mjs) doesn't include Svelte 5 rune globals (`$state`, `$derived`, `$effect`, etc.). These are valid in `.svelte.js` files per Svelte 5 spec. Config only has `globals.browser` + `globals.node`, no Svelte globals.

**Workaround:** Add eslint-plugin-svelte's rune globals to config or suppress error on `.svelte.js` files. Not blocking tests/build, but CI will fail on lint.

---

## Coverage Analysis

### TESTED (28 tests)

**game-logic.test.js (16 tests):**
- `generateGrid()`: Shape invariants (9×9, 5/row, 5/col, no dupes) ✓
- Column ranges & ascending sort (col 0: 1-9, col 8: 80-90, ascending per column) ✓
- `isRowComplete()`: All crossed, partial crossed, empty row, zero-cell edge cases ✓
- `getWaitingNumber()`: Single remaining, multiple remaining, none remaining, empty row ✓

Uses 200 trial loops on randomized generators → strong probabilistic coverage.

**settings-store.test.js (12 tests):**
- Defaults (frozen object, default color #7a4a2b) ✓
- `loadSettings()`: Empty storage, valid color, invalid color, wrong shape, corrupt JSON, 3-digit hex rejection, uppercase hex ✓
- `saveSettings()`: localStorage persistence, CSS var injection ✓
- `resetSettings()`: State + persistence reset ✓

Uses happy-dom environment for localStorage + document.documentElement.

---

## UNTESTED Code Paths (Critical Gap)

**In `game-logic.js` (5 functions/helpers — 0 tests):**

### 1. `saveGrid(grid, prefix = "loto")` [line 144–150]
- **Risk:** Grid persistence layer. No test for localStorage write, quota error handling, or serialization edge cases.
- **Behavior tested:** None.
- **Scenario:** App relies on this to save player state; failure = lost game in progress.

### 2. `loadGrid(prefix = "loto")` [line 156–162]
- **Risk:** Grid deserialization. Missing tests for corrupt JSON, wrong shape, null/undefined, migration scenarios.
- **Behavior tested:** None.
- **Scenario:** Corrupted localStorage could crash game initialization.

### 3. `saveCrossedState(crossed, prefix = "loto")` [line 168–174]
- **Risk:** Crossed-cell persistence (game progress). No test for data loss, quota exceeded, or race conditions.
- **Behavior tested:** None.

### 4. `loadCrossedState(prefix = "loto")` [line 180–186]
- **Risk:** Crossed-cell deserialization. Missing validation of boolean matrix structure.
- **Behavior tested:** None.

### 5. Helper Functions (private, but load-bearing):
- `safeParse(raw, validate)` [line 102–110]: Core JSON parse + validate guard. No direct tests.
- `isNumberMatrix(v)` [line 113–124]: Shape validator for grid. No edge case tests (e.g., missing rows, non-number values).
- `isBoolMatrix(v)` [line 127–138]: Shape validator for crossed state. Same gaps.
- `randomNumbersInCol(num, col)` [line 29–35]: Helper for grid generation. No test for edge cases (e.g., num > column size).
- `pickFilledCols()` [line 46–72]: Core quota algorithm. No test for distribution, edge rows, quota correctness.

**Net Impact:** Entire grid persistence layer is untested. If `saveGrid`/`loadGrid` fail, the app silently falls back to in-memory only, but no coverage validates that fallback or data integrity.

---

## Critical Questions (Unresolved)

1. **Is `localStorage` quota/disabled handling sufficient?** Tests use try-catch for quota in save functions, but no test validates recovery. If localStorage is disabled (private mode), the app still works in-memory — but is that intentional? Should it warn the user?

2. **Do `loadGrid` / `loadCrossedState` need stricter shape validation?** Current validators check length & type, but don't validate:
   - Row/column numeric ranges (e.g., grid cells must be 0–90)
   - Cross-state consistency (e.g., crossed cell at empty position should fail?)
   
3. **Migration path for corrupted localStorage?** If user has old/malformed data, `loadGrid` silently returns null and app starts fresh. Is this documented? Should there be a user-facing error?

4. **Flakiness under load?** Tests pass cleanly in isolation. Have they been run under simulated storage quota exceeded or in private-mode environments?

---

## Recommendations (Prioritized)

### Must Fix (blocks lint)
- **Fix eslint config** to include Svelte 5 rune globals or suppress `.svelte.js` files:
  ```js
  // Option 1: Add globals for .svelte.js files in eslint.config.mjs
  {
    files: ["**/*.svelte.js"],
    languageOptions: {
      globals: { $state: "readonly", $derived: "readonly", ... }
    }
  }
  ```

### Should Add (medium risk — persistence)
- Test `saveGrid` / `loadGrid` with:
  - Valid 9×9 grid (round-trip serialization)
  - Corrupt JSON payloads
  - Wrong matrix shape (e.g., 8×9, 3×9)
  - localStorage quota exceeded (mock error)
  - null/undefined input
- Test `saveCrossedState` / `loadCrossedState` similarly
- Test `isNumberMatrix` / `isBoolMatrix` validators with invalid inputs:
  - Non-array, wrong dimensions, mixed types, null values

### Nice to Have (edge case hardening)
- Test `pickFilledCols()` distribution (e.g., all rows/cols hit exactly 5 cells after 1000 iterations)
- Test `randomNumbersInCol()` edge cases (e.g., pick 5 from col 0, which has exactly 9 options)
- Add integration test: full game workflow (generate → save → load → cross → save → load → verify)

---

## Build & Environment Notes

- **Node environment:** happy-dom for DOM mocking in settings tests (✓ working)
- **jsconfig.json:** Valid; extends SvelteKit conventions (but missing explicit tsconfig extension hint)
- **Vite/SvelteKit:** v7 + v2, fully compatible with test setup
- **Test framework:** vitest 4.1.5, ESM modules, no issues

---

## Summary Table

| Command | Status | Notes |
|---------|--------|-------|
| `npm test` | ✅ | 28/28 pass, stable across runs |
| `svelte-check` | ✅ | 0 errors, 0 warnings |
| `npm run build` | ✅ | Static export succeeds |
| `npm run lint` | ❌ | $state rune not declared in eslint globals |

---

**Status:** DONE_WITH_CONCERNS

**Summary:** Test infrastructure is functional & stable (28/28 pass, no flakiness). Lint must be fixed. **Critical gap: storage layer (saveGrid, loadGrid, saveCrossedState, loadCrossedState, and helper validators) is untested—recommend adding coverage for persistence before launch.**

**Concerns:**
1. Lint error blocks CI (eslint config missing Svelte 5 globals)
2. Persistence layer untested (risky for game state recovery)
3. No integration test covering save/load round-trip
4. Validators (isNumberMatrix, isBoolMatrix) not stress-tested with edge cases
