---
phase: 1
title: "Lift master state to shared store"
status: completed
priority: P1
effort: "1h"
dependencies: []
---

# Phase 1: Lift master state to shared store

## Overview

Extract the `{called, remaining}` state from `MasterPanel.svelte` into a
new reactive module `master-store.svelte.js`. MasterPanel becomes a
view over the store; nothing else changes UX-wise. Foundation for
phase 2's player auto-cross.

## Requirements

**Functional**
- Same persistence semantics: localStorage `loto_master`, same shape
- Same load-on-mount, save-on-change behavior
- Existing master grid / "Số vừa xổ" / history list unchanged

**Non-functional**
- File ≤ 200 lines
- Validators preserved (16 KB cap, `__proto__` stripping, shape check)
- No behavior change observable from user — pure refactor

## Architecture

**`src/lib/master-store.svelte.js`** (new)

```js
const STORAGE_KEY = "loto_master";
const MAX_STORAGE_BYTES = 16_384;

export const masterState = $state({
  /** @type {number[]} */
  called: [],
  /** @type {number[]} */
  remaining: [],
});

export function loadMaster() { /* parse + validate, write into masterState */ }
export function saveMaster() { /* serialize from masterState */ }
export function startNewGame() { /* fill remaining with shuffled 1..90, clear called */ }
export function drawNext() {
  /** @returns {number | null} */
  if (masterState.remaining.length === 0) return null;
  const next = masterState.remaining[0];
  masterState.called = [...masterState.called, next];
  masterState.remaining = masterState.remaining.slice(1);
  return next;
}
export function resetMaster() {
  /** Clears both arrays — used by "Ván mới" */
  masterState.called = [];
  masterState.remaining = [];
}
```

The `lastCalled` derived value lives in `MasterPanel` since it's
display-only:
```js
const lastCalled = $derived(
  masterState.called.length ? masterState.called.at(-1) : null,
);
```

**Persistence pattern**: a single $effect in `MasterPanel` (or in the
store module if cleaner) calls `saveMaster()` on `masterState.called`
or `masterState.remaining` change. Keep load gated on first mount so
SSR doesn't try to touch localStorage.

## Related Code Files

- Create: `src/lib/master-store.svelte.js`
- Modify: `src/lib/MasterPanel.svelte`
  - Remove `state`, `loadState`, `saveState`, `createFreshState` from
    component-level — move to `master-store.svelte.js`
  - `handleNewGame` calls `startNewGame()`
  - `handleDrawNext` calls `drawNext()`, then `broadcastDraw(next)`
    (still using bus until phase 2)
  - `lastCalled` becomes `$derived(masterState.called.at(-1))`
  - The 11×9 board's `callOrder` map derived from `masterState.called`

## Implementation Steps

1. Create `master-store.svelte.js` with the 5 exports above
2. Move `STORAGE_KEY`, `MAX_STORAGE_BYTES`, `loadState`, `saveState`,
   and the shuffle helper from `MasterPanel.svelte` into the store
3. Replace `MasterPanel`'s `let state = $state(...)` with reads against
   `masterState` (all six mutations: load, draw, new, called list,
   remaining list, lastCalled derive)
4. Wire load on mount: `$effect(() => { loadMaster(); });`
5. Wire save on change: `$effect(() => { saveMaster(); });` —
   reading `masterState.called` + `masterState.remaining` inside
6. Smoke-test: refresh tab, master state persists; click "Ván mới",
   reset works; click "Xổ số", draws + broadcasts as before

## Success Criteria

- [ ] `master-store.svelte.js` exists, ≤ 100 lines
- [ ] `MasterPanel.svelte` shrinks (no state/storage code inside)
- [ ] Master flow unchanged: load on mount, draw, new game, reload-restore
- [ ] All 123 existing vitest tests still pass
- [ ] `npx svelte-check` clean
- [ ] `npm run build` succeeds

## Risk Assessment

- **Reactivity break**: rune state inside a `.svelte.js` module re-exports
  fine via destructuring? Yes — `settings-store.svelte.js` proves the
  pattern. Make sure consumers import `masterState` (not destructure
  fields) so reactivity threads through.
- **Migration of saved data**: localStorage shape unchanged, no migration
  needed. Validators carried over verbatim.
- **Multiple MasterPanel mounts**: not exercised today (mode toggle
  unmounts). Store is module-singleton, so two mounts would share — fine.
