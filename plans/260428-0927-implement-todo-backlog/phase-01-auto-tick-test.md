---
name: Auto-tick integration test
phase: 1
status: completed
priority: high
effort: 1-2h
completed: 2026-04-28
---

# Phase 1 — Auto-tick integration test

## Context
- TODO entry: highest-leverage item ("dedup-by-`at` fix already caught a P0")
- `src/lib/PlayerBoard.svelte:157-171` (auto-tick effect, dedup guard)
- `src/lib/call-bus.svelte.js` (bus driver — already tested)
- `src/lib/game-logic.js` — `findUncrossedCell` (pure helper)
- Existing test infra: vitest + happy-dom, **no** `@testing-library/svelte`

## Decision
Don't add `@testing-library/svelte` just for one component. Extract the
auto-tick body into a pure helper in `game-logic.js`, test that. The
effect in `PlayerBoard.svelte` becomes a thin wrapper that calls the
helper and applies the result.

## Files
- Create: `src/lib/auto-tick.js`
- Create: `src/lib/auto-tick.test.js`
- Modify: `src/lib/PlayerBoard.svelte` — replace effect body with helper call

## Helper API

```js
/**
 * @param {object} args
 * @param {number[][]} args.grid
 * @param {boolean[][]} args.crossed
 * @param {{num: number, at: number} | null} args.lastDraw
 * @param {number} args.lastHandledAt
 * @param {"player" | "master" | "both"} args.mode
 * @returns {{ crossed: boolean[][], lastHandledAt: number, changed: boolean }}
 */
export function processAutoTick({ grid, crossed, lastDraw, lastHandledAt, mode }) {
  if (!lastDraw) return { crossed, lastHandledAt, changed: false };
  if (lastDraw.at === lastHandledAt) return { crossed, lastHandledAt, changed: false };
  const next = { lastHandledAt: lastDraw.at };
  if (mode !== "both") return { crossed, ...next, changed: false };
  if (!grid || crossed.length === 0) return { crossed, ...next, changed: false };
  const target = findUncrossedCell(grid, crossed, lastDraw.num);
  if (!target) return { crossed, ...next, changed: false };
  const updated = crossed.map((row, ri) =>
    ri === target.row ? row.map((v, ci) => (ci === target.col ? true : v)) : row
  );
  return { crossed: updated, ...next, changed: true };
}
```

## Test cases (5)
1. NEW draw, mode=both, number on board → cell crossed, `changed: true`
2. Same `at` re-fire → no mutation, `changed: false`, `lastHandledAt` unchanged
3. Manual untick after auto-tick, then SAME number with NEW `at` → re-crosses
4. mode=player → no mutation regardless of draw
5. Number not on board → no mutation, `lastHandledAt` advances (we still consume the event)

## Steps
1. Create `auto-tick.js` with `processAutoTick` (above). Import `findUncrossedCell`.
2. Create `auto-tick.test.js` — mirror style of `call-bus.test.js`.
3. Refactor `PlayerBoard.svelte:157-171` effect:
   ```js
   $effect(() => {
     const drawn = bus.lastDrawn;
     const result = processAutoTick({
       grid, crossed, lastDraw: drawn,
       lastHandledAt: lastHandledDrawAt,
       mode: settings.mode,
     });
     lastHandledDrawAt = result.lastHandledAt;
     if (result.changed) crossed = result.crossed;
   });
   ```
4. `npm test` — all green.
5. Manual smoke: open app in mode=both, draw a number from master panel, confirm player cell crosses.

## Success
- 5 unit cases pass.
- `npm test` green (no regression in existing tests).
- Manual smoke confirms behavior unchanged.

## Risks
- The effect tracks `bus.lastDrawn` reactively — extracting body into a
  helper means the effect still needs to *read* `bus.lastDrawn` for
  reactivity. The wrapper above does that correctly (read inside the
  `$effect` body before passing to helper).
