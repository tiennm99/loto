---
name: Per-row "Chờ" indicator
phase: 5
status: todo
priority: medium
effort: 1h
---

# Phase 5 — Per-row "Chờ" visual indicator

## Context
- `src/lib/PlayerBoard.svelte` — current "Chờ" notification is a
  toast-only signal (`showToast` in the waiting-row $effect at lines
  120-130). When the toast dismisses (5s), no persistent visual cue.
- TODO: subtle ring/glow on the `section-label` band when a Chờ row
  exists in that section, so user retains awareness without re-reading
  the toast.
- 9 rows split into 3 sections (`SECTIONS = [0, 3, 6]`). Section labels
  rendered at `PlayerBoard.svelte:266`.

## Decision
Track waiting state reactively per row, derive per-section flag, apply
a Tailwind ring + soft pulse to that section's `section-label` band.

Already have `notifiedWaitingRows` (a plain Set, not reactive). Need a
reactive mirror. Cheapest: derive from `grid` + `crossed` directly.

## Files
- Modify: `src/lib/PlayerBoard.svelte`

## Approach

### 1. Derive waiting flag per row
Reuse `getWaitingNumber` from `game-logic.js`:

```js
const waitingRows = $derived(
  grid && crossed.length
    ? grid.map((_, r) => getWaitingNumber(grid, crossed, r) !== null && !celebratedRows.has(r))
    : []
);
```

⚠ `celebratedRows` is a Set (not reactive). For the derived flag to
update when a row completes, switch `celebratedRows` to `$state(new Set())`
or a derived `completedRows` array. Simpler: just derive from
`isRowComplete` directly:

```js
const waitingRows = $derived(
  grid && crossed.length
    ? grid.map((_, r) =>
        !isRowComplete(grid, crossed, r) &&
        getWaitingNumber(grid, crossed, r) !== null
      )
    : []
);
```

### 2. Per-section flag
```js
const sectionHasWaiting = $derived(
  SECTIONS.map((startRow) =>
    waitingRows.slice(startRow, startRow + 3).some(Boolean)
  )
);
```

### 3. Apply visual to section label
At line 266:
```html
<div
  class="section-label {sectionHasWaiting[sectionIdx] ? 'section-label-waiting' : ''}"
>
  {SECTION_LABELS[sectionIdx]}
</div>
```

### 4. CSS
Add to `src/app.css` (where `section-label` is already defined):

```css
.section-label-waiting {
  box-shadow: inset 0 0 0 2px rgb(245 158 11 / 0.6);
  animation: section-pulse 2.4s ease-in-out infinite;
}

@keyframes section-pulse {
  0%, 100% { box-shadow: inset 0 0 0 2px rgb(245 158 11 / 0.45); }
  50%      { box-shadow: inset 0 0 0 2px rgb(245 158 11 / 0.85); }
}

@media (prefers-reduced-motion: reduce) {
  .section-label-waiting { animation: none; }
}
```

Amber-500 chosen to match the existing toast color (`bg-amber-500/95`
at PlayerBoard.svelte:336). Reduces context-switching; user already
associates amber with "Chờ".

## Steps
1. Verify `section-label` is defined in `src/app.css` (grep first).
2. Add `.section-label-waiting` + `@keyframes` to `src/app.css`.
3. Add the two `$derived` blocks to `PlayerBoard.svelte` script.
4. Add conditional class on the `section-label` div.
5. Test: tick 8/9 cells in row 1 (section 0). Section 0 label glows.
   Tick the 9th cell. Glow stops, "Kinh!" fires.
6. Test: untick a cell to break the Chờ. Glow stops.
7. Test reduced-motion: animation off, ring still present.

## Success
- Section label band has visible ring when any of its 3 rows is in Chờ.
- Glow stops on row complete or Chờ broken.
- Ring color matches amber toast.
- Reduced-motion users see static ring (no pulse).

## Risks
- Computing `getWaitingNumber` 9× per render is fine (called per cell
  click only; <1ms).
- `isRowComplete` already memoized via `rowCompleteness` derived —
  consider reusing it for the `!isRowComplete` half of the check:

  ```js
  const waitingRows = $derived(
    grid && crossed.length
      ? grid.map((_, r) => !rowCompleteness[r] && getWaitingNumber(grid, crossed, r) !== null)
      : []
  );
  ```
