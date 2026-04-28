---
name: Confetti polish
phase: 6
status: completed
priority: low
effort: 30m
completed: 2026-04-28
---

# Phase 6 — Confetti polish

## Context
- `src/lib/PlayerBoard.svelte:34-35` — confetti emoji set + count.
- `:113` — `celebrationTier = celebratedRows.size >= 3 ? 2 : 1`.
- `:381-390` — confetti rendering loop.
- TODO: tier-2 threshold 3+ rare on 9-row card; emoji pool too small.

## Decision

### 1. Threshold: 2nd bingo OR (1st bingo + active Chờ)
Lower threshold so tier-2 fires more often without devaluing it.
"Active Chờ" means at least one OTHER row is one cell away.

```js
// Before:
celebrationTier = celebratedRows.size >= 3 ? 2 : 1;

// After:
const hasActiveCho = grid.some((_, r) =>
  !celebratedRows.has(r) && getWaitingNumber(grid, crossed, r) !== null
);
celebrationTier =
  celebratedRows.size >= 2 || (celebratedRows.size >= 1 && hasActiveCho)
    ? 2
    : 1;
```

### 2. Emoji variety
Add festive Vietnamese-flavored 🥢 🎋 🏮 to the existing 🎊 ✨ 🎉 🥳.
7 emojis. 12 confetti pieces — `i % 7` distributes.

```js
const CONFETTI_EMOJI = ["🎊", "✨", "🎉", "🥳", "🥢", "🎋", "🏮"];
```

### 3. Random size 1.5–2.5rem
The existing `.confetti` CSS sets a fixed `font-size`. Override via
inline style with a stable random per piece:

```svelte
{#each CONFETTI as i (i)}
  <span
    class="confetti"
    style:--x="{(i * 8.3 + (i % 3) * 11) % 100}%"
    style:--delay="{(i * 37) % 400}ms"
    style:--rot="{(i * 67) % 360}deg"
    style:--size="{1.5 + ((i * 13) % 11) / 10}rem"
  >
    {CONFETTI_EMOJI[i % CONFETTI_EMOJI.length]}
  </span>
{/each}
```

CSS — read the var:

```css
.confetti { font-size: var(--size, 2rem); /* …existing… */ }
```

`(i * 13) % 11 / 10` → values in {0.0, 0.3, 0.6, 0.9, 0.2, 0.5, ...} → 1.5–2.4rem.

## Files
- Modify: `src/lib/PlayerBoard.svelte`
- Modify: `src/app.css` (confetti `font-size` rule)

## Steps
1. `grep -n "\.confetti" src/app.css` — confirm rule location.
2. Replace `CONFETTI_EMOJI` array.
3. Update `celebrationTier` assignment around line 113. Need to import
   `getWaitingNumber` (already imported).
4. Add `--size` inline style + read var in CSS.
5. Manual test:
   - Tick row 1 fully → tier 1 (no confetti). ✓
   - Tick row 2 fully → tier 2 (confetti). ✓ (was 3+ before)
   - Reset, tick most of row 1, get one Chờ on row 2, complete row 1
     → tier 2 confetti immediately on first bingo.
6. Visual check: confetti reads as varied sizes, lantern + chopsticks
   visible alongside party emojis.

## Success
- Tier-2 confetti triggers on 2nd bingo (or 1st bingo + active Chờ).
- Emoji set includes 🥢 🎋 🏮.
- Sizes vary 1.5–2.4rem.
- No regression on tier-1 silent celebration.

## Out of scope
- Adjusting confetti animation (drift speed, rotation curve).
- Reduced-motion gating — already handled by parent component or app.
  Confirm in test if `prefers-reduced-motion: reduce` skips the animation
  path; if not, that's a separate issue.
