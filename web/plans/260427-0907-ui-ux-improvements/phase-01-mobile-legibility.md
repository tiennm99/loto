# Phase 1 — Mobile legibility (cell sizing, haptic, active, cross-out)

## Context
- [plan.md](plan.md). No deps. Highest user-facing impact.
- Reviewer P0: cell text under ~14px on 360px viewport, tap target
  ~37px (under WCAG 44px), no active-press feedback, no haptic, static
  cross-out line.

## Overview
- Priority: P0
- Status: TODO
- Effort: ~20 min

## Files

| File | Change |
|---|---|
| `src/lib/PlayerBoard.svelte` | Cell button class — taller aspect on mobile, larger text, `active:scale-90`, `navigator.vibrate(10)` in `handleCellClick` |
| `src/app.css` | `@keyframes cross-draw` + apply to `.cell-crossed::after` |

## Diffs (illustrative)

```diff
  /** @param {number} row, @param {number} col */
  function handleCellClick(row, col) {
+   if (typeof navigator !== "undefined" && navigator.vibrate) {
+     navigator.vibrate(10);
+   }
    crossed = crossed.map((r, ri) =>
      ri === row ? r.map((v, ci) => (ci === col ? !v : v)) : r,
    );
  }
```

```diff
  <button
    type="button"
    aria-label="Số {num}{isCrossed ? ', đã đánh dấu' : ''}"
    aria-pressed={isCrossed}
    onclick={() => handleCellClick(row, col)}
    class="tan-tan-num relative flex items-center justify-center
-          aspect-square sm:aspect-[3/5]
-          text-base sm:text-2xl md:text-3xl
+          aspect-[3/4] sm:aspect-[3/5]
+          text-lg sm:text-2xl md:text-3xl
           border border-slate-400/50 dark:border-slate-600/40
-          transition-all select-none cursor-pointer
+          transition-all select-none cursor-pointer active:scale-90
           focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-400
           {isCrossed ...}"
```

```css
/* src/app.css — add near .cell-crossed */
@keyframes cross-draw {
  from { clip-path: inset(0 100% 0 0); }
  to   { clip-path: inset(0 0 0 0); }
}

.cell-crossed::after {
  /* …existing rules… */
  animation: cross-draw 200ms ease-out;
}
```

## Edge cases

| Case | Handling |
|---|---|
| iOS Safari ignores `navigator.vibrate` | Guarded `typeof navigator !== "undefined" && navigator.vibrate` — silent no-op |
| User uncrosses a cell | `cell-crossed::after` simply unmounts; no inverse animation needed |
| Reduced-motion preference | Optional follow-up: wrap `cross-draw` in `@media (prefers-reduced-motion: no-preference)`. Skip in v1 — animation is 200ms and non-essential. |
| `aspect-[3/4]` on very small screens | Cells grow ~10px taller; layout still fits because parent caps width |

## Success criteria

- On a 360px viewport, a number reads at ≥18px effective.
- Tap target ≥40px on mobile (cells are 40×53 px in 9-col grid at 360px).
- Tapping a cell on Android Chrome vibrates ~10ms.
- Press scale-down is visible on touchstart.
- Cross-out diagonal animates in over ~200ms when toggled on.
- Existing 98 unit tests still pass.

## Risks

| Risk | Mitigation |
|---|---|
| Taller cells push the bingo popup off-screen on tiny viewports | Already a `fixed inset-0` modal with internal scroll — unaffected |
| Haptic abused as a cell-bouncer | One 10ms pulse per click; user can hold-press without retrigger because Svelte uses click events, not touchstart |

## Next
- Phase 2 brand + first-run state.
