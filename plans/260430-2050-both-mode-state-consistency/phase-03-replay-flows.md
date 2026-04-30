---
phase: 3
title: "Replay flows + retire bus"
status: completed
priority: P1
effort: "1h"
dependencies: [1, 2]
---

# Phase 3: Replay flows + retire bus

## Overview

Wire up the four cross-panel flows (master "Ván mới", player "Tạo bảng
mới", player "Xoá đánh dấu", master draw → player auto-cross) to the
new shared store. Delete the now-dead `call-bus.svelte.js` and remove
the F3 violation (player handlers calling `resetBus()`).

## Requirements

**Functional**
- **Master "Ván mới"** in both mode → player crossed clears + manualUnticks clears (locked decision #1)
- **Player "Tạo bảng mới"** mid-game → new grid, then auto-cross all current `masterState.called` numbers found on it (covers F1)
- **Player "Xoá đánh dấu"** in both mode → clear crossed + manualUnticks, then immediately re-apply `masterState.called` so all called numbers cross again (locked decision #2)
- **Master draw** → player auto-cross via phase 2's effect, no bus needed

**Non-functional**
- No reference to `call-bus.svelte.js` remains in source
- `resetBus` import in PlayerBoard removed (F3 fixed)

## Architecture

**Master "Ván mới" propagation**

Detection: PlayerBoard runs an `$effect` watching `masterState.called.length`.
When it transitions from `> 0` to `0` AND `settings.mode === "both"`,
clear player crossed + manualUnticks + reset cursor.

```js
let prevCalledLen = $state(0);
$effect(() => {
  const len = masterState.called.length;
  const wasReset = prevCalledLen > 0 && len === 0;
  prevCalledLen = len;
  if (wasReset && settings.mode === "both" && grid) {
    crossed = grid.map(row => row.map(() => false));
    manualUnticks = new Set();
    lastHandledIndex = 0;
    celebratedRows.clear();
    notifiedWaitingRows.clear();
  }
});
```

**Player "Tạo bảng mới" replay** (`handleGenerate`)

```js
function handleGenerate() {
  if (grid && !confirm("Bạn có muốn tạo lại bảng không?")) return;
  cancelPlayback();
  const newGrid = generateGrid();
  let newCrossed = newGrid.map(row => row.map(() => false));
  // Replay master's called[] onto the new grid (no-op outside both mode).
  if (settings.mode === "both") {
    const result = applyMasterCalls({
      grid: newGrid, crossed: newCrossed,
      called: masterState.called, lastHandledIndex: 0,
      manualUnticks: new Set(), mode: "both",
    });
    newCrossed = result.crossed;
    lastHandledIndex = result.lastHandledIndex;
  } else {
    lastHandledIndex = masterState.called.length;
  }
  grid = newGrid;
  crossed = newCrossed;
  manualUnticks = new Set();
  saveGrid(newGrid, STORAGE_PREFIX);
  saveCrossedState(newCrossed, STORAGE_PREFIX);
  saveManualUnticks(manualUnticks, STORAGE_PREFIX);
  celebratedRows.clear();
  notifiedWaitingRows.clear();
  dismissToast();
  showCongrats = false;
  // No resetBus — bus is gone.
}
```

**Player "Xoá đánh dấu" replay** (`handleClear`)

```js
function handleClear() {
  if (!grid) return;
  const hasMarks = crossed.some(row => row.some(Boolean));
  if (hasMarks && !confirm("Bạn có muốn xoá tất cả đánh dấu không?")) return;
  cancelPlayback();
  manualUnticks = new Set();
  let cleared = grid.map(row => row.map(() => false));
  if (settings.mode === "both") {
    const result = applyMasterCalls({
      grid, crossed: cleared,
      called: masterState.called, lastHandledIndex: 0,
      manualUnticks: new Set(), mode: "both",
    });
    cleared = result.crossed;
    lastHandledIndex = result.lastHandledIndex;
  } else {
    lastHandledIndex = masterState.called.length;
  }
  crossed = cleared;
  saveCrossedState(crossed, STORAGE_PREFIX);
  saveManualUnticks(manualUnticks, STORAGE_PREFIX);
  celebratedRows.clear();
  notifiedWaitingRows.clear();
  dismissToast();
  showCongrats = false;
}
```

**Bus retirement**

After phase 2 + the above wiring, nothing imports from `call-bus.svelte.js`
or `auto-tick.js`. Delete the source files and their tests:

- `src/lib/call-bus.svelte.js`
- `src/lib/call-bus.test.js`
- `src/lib/auto-tick.js`
- `src/lib/auto-tick.test.js`

Remove `MasterPanel`'s `import { broadcastDraw, resetBus } ...` and the
`broadcastDraw(next)` / `resetBus()` calls — they're no-ops now since
`masterState` itself is the signal.

## Related Code Files

- Modify: `src/lib/PlayerBoard.svelte` (handleGenerate, handleClear, new $effect for master-reset detection)
- Modify: `src/lib/MasterPanel.svelte` (drop bus imports + calls)
- Modify: `src/lib/game-logic.js` (add `saveManualUnticks` / `loadManualUnticks`)
- Delete: `src/lib/call-bus.svelte.js`, `src/lib/call-bus.test.js`,
  `src/lib/auto-tick.js`, `src/lib/auto-tick.test.js`

## Implementation Steps

1. Add `saveManualUnticks` / `loadManualUnticks` to `game-logic.js`
   (mirror existing patterns; validate ints in [1,90])
2. Update `handleGenerate` per architecture above
3. Update `handleClear` per architecture above
4. Add `prevCalledLen` $state + master-reset $effect
5. Strip bus imports + calls from MasterPanel and PlayerBoard
6. Delete the four files above
7. `npm test` — should still pass (after old auto-tick.test removal)
8. `npm run lint`, `npm run build`, `npx svelte-check`

## Success Criteria

- [ ] No source file imports from `call-bus` or `auto-tick`
- [ ] All four flows behave per locked decisions:
  - Master "Ván mới" wipes player crossed in both mode
  - Player regen replays master.called onto new grid
  - Player "Xoá đánh dấu" in both mode replays immediately
  - Master draw auto-crosses on player side
- [ ] Lint, build, svelte-check clean
- [ ] All remaining tests pass

## Risk Assessment

- **Mode-aware reset detection**: a player toggling mode AWAY from
  "both" mid-game shouldn't accidentally trigger the reset clear. The
  $effect gates on `settings.mode === "both"` at trigger time, so toggling
  to "player" then master "Ván mới" in another window won't wipe player
  crossed (multi-tab is out of scope, but local mode-toggle is covered).
- **`prevCalledLen` race on mount**: it initializes to 0; first effect run
  sees `len = stored.called.length`, transition `0 → N` is NOT a reset.
  Only `>0 → 0` triggers, so safe.
- **Replay performance**: replay loop is O(called × grid) ≈ O(90 × 81) =
  ~7k ops worst case. Trivial.

## Open question

If the user wants the same "force-clear" behavior on mode toggle
player→both (auto-replay back-history), phase 2's cursor logic needs a
tweak. Current locked decisions don't cover this. Flag for sếp post-impl.
