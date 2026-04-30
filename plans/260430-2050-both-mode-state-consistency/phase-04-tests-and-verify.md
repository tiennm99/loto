---
phase: 4
title: "Tests and verify"
status: completed
priority: P1
effort: "45m"
dependencies: [1, 2, 3]
---

# Phase 4: Tests and verify

## Overview

Add coverage for the new helper + flows, run the full suite, and
manually verify the four locked behaviors in the browser.

## Requirements

- New unit tests for `applyMasterCalls`
- New unit tests for `master-store.svelte.js` (load/save/draw/new/reset)
- Manual browser verification of every locked behavior
- All existing tests still pass

## Test Matrix

### `player-auto-cross.test.js` (new)

| Case | Expected |
|------|----------|
| empty called[] | no-op, cursor stays at 0 |
| cursor = called.length | no-op |
| mode="player", called grows | cursor advances to length, no flips |
| mode="both", called=[5], grid has 5 | crosses cell, cursor=1 |
| mode="both", called=[5,5,5] (impossible but test) | first match crosses, second / third no-op (already crossed) |
| mode="both", manualUnticks={5}, called=[5] | no flip, cursor advances |
| mode="both", grid=null | no flip, cursor stays |

### `master-store.test.js` (new)

| Case | Expected |
|------|----------|
| `loadMaster` with empty storage | masterState stays empty |
| `loadMaster` with corrupt JSON | falls back to empty |
| `loadMaster` with > 16 KB | rejected |
| `startNewGame` | called=[], remaining=shuffle(1..90) |
| `drawNext` | called appends, remaining shifts; returns drawn num |
| `drawNext` with empty remaining | returns null, no mutation |
| `resetMaster` | both arrays empty |

### `game-logic.test.js` (extend)

| Case | Expected |
|------|----------|
| `saveManualUnticks` round-trip | Set in → Set out, sorted on disk |
| `loadManualUnticks` with garbage | empty Set |
| `loadManualUnticks` with out-of-range nums | filtered |

### Manual browser verification

Enable both mode + auto-call. Click through:

| # | Action | Expected |
|---|--------|----------|
| 1 | Master "Ván mới" with player marks present | Player crossed wipes |
| 2 | Master draws 3 numbers, then player "Tạo bảng mới" | New grid, those 3 numbers (if on grid) crossed |
| 3 | Master draws 5 numbers, player "Xoá đánh dấu" | All 5 immediately cross again |
| 4 | Player manually unticks #42 after auto-cross, master re-broadcast not possible (each num drawn once) | n/a — verify unticks persist across reload instead |
| 5 | Reload mid-game | Master state restored, player crossed restored, new master draws still auto-cross |
| 6 | Mode toggle both → player → both | New draws auto-cross; back-history NOT replayed (phase 3 open Q) |
| 7 | "Bắt đầu" auto-call → countdown shows + draws every N seconds | (regression check for last task) |

## Implementation Steps

1. Write `player-auto-cross.test.js`
2. Write `master-store.test.js`
3. Extend `game-logic.test.js`
4. `npm test` — expect green (delete old auto-tick.test.js if it broke
   per phase 3)
5. `npm run lint`, `npx svelte-check`, `npm run build`
6. `npm run dev`, walk through the 7-case manual matrix
7. Commit per-phase or as one feat commit (sếp's call)

## Success Criteria

- [ ] All new test files green
- [ ] Full suite green (count > 123 minus retired tests + new tests)
- [ ] Lint clean (only pre-existing errors in `verify-build-inline-scripts.mjs`,
      `MasterEmptyState.svelte`, `PlayerBoard.svelte` 396 — those are pre-existing,
      not from this refactor)
- [ ] All 7 manual cases pass
- [ ] No console errors / warnings during the walkthrough

## Risk Assessment

- **Test fallout**: removing `auto-tick.js` retires 53 tests' worth of
  coverage. The replacement helper covers equivalent ground; verify
  count parity before declaring done.
- **Manual case 6 (mode toggle replay)**: this is currently OUT of scope
  per the open question in phase 3. If sếp wants replay-on-toggle later,
  it's a one-line tweak in `applyMasterCalls`'s mode-mismatch branch
  (don't advance cursor on mismatch — let the next both-mode pass replay).

## Docs Impact

- Update `docs/codebase-summary.md`:
  - Replace "call-bus.svelte.js" entry with "master-store.svelte.js"
  - Replace "auto-tick.js" entry with "player-auto-cross.js"
  - Update PlayerBoard / MasterPanel descriptions
- Update `docs/system-architecture.md` if it diagrams the bus
- Update `plans/todo.md` carryover items if any
