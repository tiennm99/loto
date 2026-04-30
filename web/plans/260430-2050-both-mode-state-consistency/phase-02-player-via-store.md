---
phase: 2
title: "Player auto-cross via shared store"
status: completed
priority: P1
effort: "1.5h"
dependencies: [1]
---

# Phase 2: Player auto-cross via shared store

## Overview

Replace `PlayerBoard`'s bus-driven auto-tick with a $effect that
watches `masterState.called`. New numbers are auto-crossed; the
"already-handled" cursor moves to track length, not timestamp. Kills
the F7 (1ms collision) and F8 (single-slot history loss) classes
because we read the array directly.

## Requirements

**Functional**
- Auto-cross still fires only in `mode === "both"`
- Manual untick stays manual — auto-cross does NOT re-cross numbers the
  user explicitly unticked since the last reset
- All previously called numbers still on the board get crossed if the
  cursor catches up (covers F1 player-regen replay in phase 3)

**Non-functional**
- No regression in existing 53 player-side test scenarios
- Auto-tick logic unit-testable as a pure helper

## Architecture

**Replace `auto-tick.js` with `player-auto-cross.js`** (new pure helper):

```js
/**
 * Decide which cells flip given the master's full called[] history
 * and the player's already-applied cursor. Cursor advances strictly,
 * even when no cell flips, so manual unticks don't re-fire.
 *
 * @param {object} args
 * @param {number[][] | null} args.grid
 * @param {boolean[][]} args.crossed
 * @param {number[]} args.called               - master's full history
 * @param {number} args.lastHandledIndex       - index already consumed
 * @param {Set<number>} args.manualUnticks     - numbers user unticked
 * @param {"player" | "master" | "both"} args.mode
 * @returns {{ crossed: boolean[][], lastHandledIndex: number, changed: boolean }}
 */
export function applyMasterCalls({ grid, crossed, called, lastHandledIndex, manualUnticks, mode }) {
  if (lastHandledIndex >= called.length) return { crossed, lastHandledIndex, changed: false };
  if (mode !== "both" || !grid || crossed.length === 0) {
    // Advance cursor anyway to keep player→both transitions catching up
    // only on FUTURE draws, not the whole back-history.
    return { crossed, lastHandledIndex: called.length, changed: false };
  }
  let next = crossed;
  let changed = false;
  for (let i = lastHandledIndex; i < called.length; i++) {
    const num = called[i];
    if (manualUnticks.has(num)) continue;
    const target = findUncrossedCell(grid, next, num);
    if (!target) continue;
    next = next.map((row, ri) =>
      ri === target.row ? row.map((v, ci) => (ci === target.col ? true : v)) : row,
    );
    changed = true;
  }
  return { crossed: next, lastHandledIndex: called.length, changed };
}
```

**Manual untick tracking**

Add `let manualUnticks = $state(new Set())` at PlayerBoard's top. In the
cell click handler, if the user transitions a cell from `true → false`
on a number that's in `masterState.called`, add it to `manualUnticks`.
`true → false` on an uncalled number doesn't need tracking. `false → true`
removes from the set (re-cross overrides the untick).

**Persistence**: `manualUnticks` persisted as a sorted array under key
`{prefix}_manualUnticks`, loaded on mount.

**Player effect** (replaces old bus-watching effect):

```js
let lastHandledIndex = $state(0);

$effect(() => {
  const result = applyMasterCalls({
    grid,
    crossed,
    called: masterState.called,
    lastHandledIndex,
    manualUnticks,
    mode: settings.mode,
  });
  if (result.lastHandledIndex !== lastHandledIndex) {
    lastHandledIndex = result.lastHandledIndex;
  }
  if (result.changed) crossed = result.crossed;
});
```

The `lastHandledIndex` is in-memory only — phase 3 covers the reload
behavior (reload re-applies all called numbers as a catch-up).

## Related Code Files

- Create: `src/lib/player-auto-cross.js` + tests
- Modify: `src/lib/PlayerBoard.svelte`
  - Drop `bus`, `resetBus` import (kept only until phase 3 cleanup)
  - Add `masterState` import from `$lib/master-store.svelte.js`
  - Replace `lastHandledDrawAt` with `lastHandledIndex`
  - Add `manualUnticks` $state + persistence
  - Update cell click handler to track `true → false` transitions
- Keep (read-only ref): `findUncrossedCell` from `game-logic.js`

## Implementation Steps

1. Write `player-auto-cross.js` with `applyMasterCalls` (copy `findUncrossedCell` logic OR import it)
2. Write `player-auto-cross.test.js` covering:
   - mode mismatch advances cursor without flipping
   - mode=both crosses uncrossed cells, skips already-crossed
   - manualUnticks numbers skipped
   - empty called array → no-op
   - cursor at length → no-op
3. Update `PlayerBoard.svelte`:
   - Replace bus auto-tick effect with `applyMasterCalls` effect
   - Add `manualUnticks` state + load/save helpers in `game-logic.js`
   - Track unticks in `handleCellClick` (locate it; add 2-line transition check)
4. Verify mode toggle player→both: cursor advances to `called.length` in
   non-both modes, so toggle-on doesn't replay back-history (intentional;
   F6 explicitly accepts this trade-off — refresh path covers catch-up)
5. Run `npm test` — fix any auto-tick.test.js fallout (delete old tests if helper retired)

## Success Criteria

- [ ] `player-auto-cross.js` ≤ 100 lines, fully unit-tested
- [ ] PlayerBoard auto-crosses correctly on master draws (manual smoke)
- [ ] Manual untick → next master draw of SAME number does NOT re-cross
- [ ] Manual re-cross clears the untick (next draw of same number works)
- [ ] All vitest tests pass (with old `auto-tick.test.js` removed if helper retired)

## Risk Assessment

- **`manualUnticks` persistence shape**: stored as array of numbers,
  reconstructed to `Set<number>` on load. Validate `Number.isInteger`
  + range 1..90 to defend against poisoned localStorage.
- **Cursor-vs-history mismatch on first run**: `lastHandledIndex` defaults
  to 0; on mount, $effect runs, applies all `called` history. This is
  the desired catch-up behavior for reload (covers F4).
- **Effect re-entry**: writing `crossed` inside an effect that reads
  `crossed` — same pattern as today, dedup via `result.changed` makes
  it stable. No new exposure.
