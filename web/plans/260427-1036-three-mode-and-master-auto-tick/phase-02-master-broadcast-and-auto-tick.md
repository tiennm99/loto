# Phase 2 — Master broadcast + player auto-tick

## Overview

In `mode === "both"`, when MasterPanel draws a number, PlayerBoard
auto-marks that number on its grid (if present). Achieved with a tiny
shared store that broadcasts `lastDrawn`. PlayerBoard reacts via
`$effect`. Manual cell taps still work as before.

**Status**: not started
**Priority**: P1
**Effort**: ~35 min
**Depends on**: Phase 1 (uses `settings.mode === "both"`)

## Files to create

- `src/lib/call-bus.svelte.js` — single reactive `lastDrawn` slot

## Files to modify

- `src/lib/MasterPanel.svelte` — write to bus on draw
- `src/lib/PlayerBoard.svelte` — read bus, auto-tick if present
- `src/lib/call-bus.test.js` — new test file

## Design

### call-bus.svelte.js

```js
/**
 * Tiny one-slot bus to coordinate master draws → player auto-tick.
 * Each draw publishes a fresh object so even repeat numbers fire a
 * fresh reactive change. Consumers read `bus.lastDrawn?.num` in an
 * effect.
 *
 * @module lib/call-bus
 */

export const bus = $state({
  /** @type {{ num: number, at: number } | null} */
  lastDrawn: null,
});

/** @param {number} num */
export function broadcastDraw(num) {
  bus.lastDrawn = { num, at: Date.now() };
}

export function resetBus() {
  bus.lastDrawn = null;
}
```

`at` is a tiebreaker: if master draws 17, player marks; player un-marks
manually; master draws 17 again (impossible in normal play but
possible after "Ván mới") — the new object reference triggers the
effect cleanly.

Reset is called from MasterPanel's `handleNewGame` so a stale
`lastDrawn` from a previous game can't race the new player grid.

### MasterPanel changes

In `handleDrawNext`:

```js
function handleDrawNext() {
  if (!state || state.remaining.length === 0) return;
  const next = state.remaining[0];
  state = {
    called: [...state.called, next],
    remaining: state.remaining.slice(1),
  };
  lastCalled = next;
  scrollOnNextDraw = true;
  broadcastDraw(next);                       // NEW
  if (settings.voiceEnabledMaster) playNumber(next);
}
```

In `handleNewGame`, after creating fresh state, call `resetBus()`.

### PlayerBoard changes

Add a new `$effect` watching the bus:

```js
import { bus } from "$lib/call-bus.svelte.js";
import { settings } from "$lib/settings-store.svelte.js";

$effect(() => {
  const drawn = bus.lastDrawn;
  if (!drawn) return;
  if (settings.mode !== "both") return;
  if (!grid || crossed.length === 0) return;

  // Find first occurrence (numbers are unique on a 9×9 lô tô card)
  for (let r = 0; r < grid.length; r++) {
    for (let c = 0; c < grid[r].length; c++) {
      if (grid[r][c] === drawn.num && !crossed[r][c]) {
        crossed = crossed.map((row, ri) =>
          ri === r ? row.map((v, ci) => (ci === c ? true : v)) : row,
        );
        return;
      }
    }
  }
});
```

**Important**: only sets to `true`, never toggles off. If the cell is
already crossed (manually), the effect short-circuits — no double-toggle
bug.

Mode check is inside the effect, not a top-level guard, so the effect
remains reactive to `settings.mode` (Svelte 5 runes track reads).

### Test plan (call-bus.test.js)

- `broadcastDraw(n)` updates `bus.lastDrawn.num` to `n`.
- Calling twice with the same number yields a different object
  reference (so `$effect` re-fires).
- `resetBus()` sets `bus.lastDrawn = null`.

## Todo

- [ ] Create `src/lib/call-bus.svelte.js`
- [ ] Wire `broadcastDraw` into `handleDrawNext`
- [ ] Wire `resetBus` into `handleNewGame`
- [ ] Add bus-watching `$effect` in PlayerBoard
- [ ] Confirm auto-tick fires Bingo/Chờ effects naturally (existing
      `$effect` on `crossed` will pick up the change)
- [ ] Write `call-bus.test.js`
- [ ] `npm test && npm run build` — green
- [ ] Manual: in "Cả hai", draw → matching cell marks itself.

## Success criteria

- All tests pass.
- In `both` mode, drawing a number visible on the player grid
  auto-marks it; the existing waiting/bingo logic fires correctly.
- In `player` or `master` mode alone, no cross-component effects.
- Manual taps still toggle cells normally (no interference).
- Auto-tick never un-marks a cell that's already crossed.

## Risks

- **Effect runs on hot-reload with stale state**: Svelte 5 effects
  re-run on dep changes. The mode/grid guard makes this safe.
- **Visual surprise**: host might want "preview" before marking.
  Decision: auto-tick is the point of this phase; not making it
  optional in v1. Can add a `autoTick` setting later if requested.
