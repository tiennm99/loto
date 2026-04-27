# Phase 5 — Two-tier "Kinh!" celebration

## Context
- [plan.md](plan.md). Independent of earlier phases.
- Reviewer P2: bingo on row 1 looks identical to bingo on row 5. The
  party should escalate.

## Overview
- Priority: P2
- Status: TODO
- Effort: ~15 min

## Goal

First completed row: current celebration (gradient text, 🎉 bounce, ✨🎊 spinners).
Third+ completed row: same modal **plus** a CSS confetti burst (8–12
emoji spans falling from the top with random `--x` and `--delay`).

No JS particle library — pure CSS keyframes.

## Files

| File | Change |
|---|---|
| `src/lib/PlayerBoard.svelte` | Track `celebratedRows.size`; pass `tier` to popup; render confetti layer when tier ≥ 2 |
| `src/app.css` | `@keyframes confetti-fall` |

## Diff (illustrative)

```diff
  $effect(() => {
    if (!grid || crossed.length === 0) return;
    for (let i = 0; i < grid.length; i++) {
      if (!celebratedRows.has(i) && isRowComplete(grid, crossed, i)) {
        celebratedRows.add(i);
        notifiedWaitingRows.add(i);
        congratsRow = i + 1;
        showCongrats = true;
+       celebrationTier = celebratedRows.size >= 3 ? 2 : 1;
        if (settings.voiceEnabledPlayer) playBingo();
        break;
      }
    }
    // …pass 2 unchanged…
  });
```

```svelte
  let celebrationTier = $state(/** @type {1 | 2} */ (1));
```

In the popup template, after the existing modal block:

```svelte
{#if showCongrats && celebrationTier >= 2}
  <div aria-hidden="true" class="fixed inset-0 z-40 pointer-events-none overflow-hidden">
    {#each Array(12) as _, i (i)}
      <span
        class="confetti"
        style:--x="{Math.random() * 100}%"
        style:--delay="{Math.random() * 400}ms"
        style:--rot="{Math.random() * 360}deg"
      >
        {["🎊", "✨", "🎉", "🥳"][i % 4]}
      </span>
    {/each}
  </div>
{/if}
```

```css
/* src/app.css */
@keyframes confetti-fall {
  0%   { transform: translate(0, -10vh) rotate(0); opacity: 0; }
  10%  { opacity: 1; }
  100% { transform: translate(0, 110vh) rotate(var(--rot)); opacity: 0.8; }
}

.confetti {
  position: absolute;
  top: 0;
  left: var(--x);
  font-size: 2rem;
  animation: confetti-fall 1.6s ease-in var(--delay) forwards;
  will-change: transform, opacity;
}
```

## Edge cases

| Case | Handling |
|---|---|
| User dismisses popup before confetti finishes | `showCongrats = false` unmounts the confetti layer too — clean |
| Player generates a new card mid-celebration | `handleGenerate` already clears `celebratedRows` — tier resets to 1 next bingo |
| Reduced motion users | Wrap `confetti` block in `@media (prefers-reduced-motion: no-preference) { … }` (skip in v1, follow-up) |
| 12 emoji × random pos = browser perf | 1.6s, 12 spans, GPU layer — trivial. No issue. |

## Success criteria

- First bingo: same modal as today, no confetti.
- Third bingo (and beyond): same modal **plus** ~12 falling confetti
  emoji that complete in ~1.6 s.
- Tier resets when the player generates a new card.

## Risks

| Risk | Mitigation |
|---|---|
| Confetti behind the modal looks weird | `z-40` for confetti vs `z-50` for modal — modal floats above |
| Animation lingers on slow phones | `forwards` keeps final state; opacity 0.8→0 fade still hides it |

## Next
- Phase 6 tests + docs.
