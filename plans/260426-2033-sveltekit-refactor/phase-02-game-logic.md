---
phase: 2
title: Port game logic to src/lib
priority: high
effort: S
status: planned
---

# Phase 2 — Port game logic

Move the pure functions verbatim. They have no React dependency, so this is
a near-copy with one path move.

## Source

Existing: `lib/game-logic.js` (~205 lines, JSDoc). Already React-free.

## Target

`src/lib/game-logic.js` — same content, no edits beyond removing dead
markers if any.

## Steps

1. Create `src/lib/`.

2. Copy file:
   ```bash
   cp lib/game-logic.js src/lib/game-logic.js  # or git mv after Phase 1 deletes
   ```

   If Phase 1 already deleted `lib/`, restore from `git show HEAD:lib/game-logic.js > src/lib/game-logic.js`.

3. **Verify exports unchanged.** Public API consumed by Phase 3:
   - `generateGrid()`
   - `saveGrid(grid, prefix?)`
   - `loadGrid(prefix?)`
   - `saveCrossedState(crossed, prefix?)`
   - `loadCrossedState(prefix?)`
   - `isRowComplete(grid, crossed, row)`
   - `getWaitingNumber(grid, crossed, row)`

4. **Imports** — file uses no imports. Pure JS. Done.

5. **JSDoc** — already present, vanilla JSDoc style. No changes.

6. **localStorage guard** — file already wraps `localStorage.setItem` /
   `getItem` in try/catch for quota and disabled-storage cases. SvelteKit
   SSR check needed? `adapter-static` prerenders at build time and the
   game logic is only called from browser-mounted components, so SSR
   doesn't reach it. But add a `typeof window !== 'undefined'` guard inside
   each storage function to be safe — it's a one-line cheap defense:

   ```js
   export function saveGrid(grid, prefix = 'loto') {
     if (typeof localStorage === 'undefined') return;
     try { localStorage.setItem(`${prefix}_grid`, JSON.stringify(grid)); }
     catch { /* see notes */ }
   }
   ```

   Apply to all four storage functions.

## Files affected

- create: `src/lib/game-logic.js`
- delete (in Phase 1, if not yet): `lib/game-logic.js`

## Verify

- `node --input-type=module -e "import('./src/lib/game-logic.js').then(m => { const g = m.generateGrid(); console.log('rows:', g.length, 'cols:', g[0].length); })"`
- Output: `rows: 9 cols: 9`

## Out of scope

UI components, persistence wiring (Phase 3).

## Status: planned
