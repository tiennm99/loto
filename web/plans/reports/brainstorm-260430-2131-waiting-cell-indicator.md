# Brainstorm: "Chờ N" Waiting Cell Indicator

**Date:** 2026-04-30 21:31
**Context:** User wants the Chờ N indicator (a) inside the board, (b) translucent so cells stay readable, (c) with an animation on the actual cell holding number N.

---

## Current State Recap

- Toast `Chờ N` floats above grid (`-top-3 sm:-top-4`), amber-500/95, 5s `animate-toast`.
- Section label band already gets a persistent amber inset-ring + 2.4s `section-pulse` when any of its 3 rows is in waiting state.
- Voice "Chờ N" plays when announce flag is on.
- Grid cell holding N has **zero** visual treatment today. Section label only narrows the search to ~27 cells (3 rows × 9 cols).
- Multi-row Chờ is real: up to 9 rows could be in waiting state simultaneously (rare but possible). Toast currently single-slot, replaces previous.

---

## Design Space — 9 Options

### a. Cell-scoped pulse only (drop toast)
- **Clarity:** High — eye is drawn straight to the cell. No translation needed.
- **Read/write:** Zero — pulse is on the cell itself, doesn't cover anything.
- **Cost:** ~10 LoC. One `$derived` Set<`r,c`> of waiting cells, one `.cell-waiting` class with keyframe.
- **Mobile:** Excellent. Nothing to dismiss, nothing covered.
- **Reduced-motion:** Trivial — fall back to static amber ring (no animation).
- **Multi-row:** Scales linearly. 9 amber pulses on a 9×9 = noisy but informative.
- **Risk:** No textual cue → blind/low-vision/cognitive-load users lose Chờ N affordance. Voice + section ring partially mitigate. Toast users may miss the explicit number callout.

### b. Cell pulse + minimal centered chip
- **Clarity:** Good. Chip says number; pulse shows location.
- **Read/write:** Chip in dead-zone (gap between sections, top-right) is a pure overlay; ~30% opacity = readable beneath. Could still smudge cells if mispositioned.
- **Cost:** ~20 LoC. Cell pulse + small absolute-positioned chip with auto-fade.
- **Mobile:** Good. Chip takes ~50px square.
- **Reduced-motion:** Cell pulse → static ring; chip → no animation, just static.
- **Multi-row:** Chip can stack/queue; pulse scales fine.
- **Risk:** Two indicators = mild redundancy. Two CSS knobs to tune.

### c. Centered overlay banner (amber 70% opacity + arrow)
- **Clarity:** High initially, but the arrow is a layout nightmare across 9 possible cell positions and section boundaries.
- **Read/write:** Big overlay blocks taps unless `pointer-events: none`. Even translucent, it visually covers ~9–15 cells.
- **Cost:** ~50 LoC. Arrow geometry math, opacity tuning.
- **Mobile:** Banner crowds small screens.
- **Reduced-motion:** Banner static-only — fine.
- **Multi-row:** Multiple arrows = chaos. Single banner can't pluralize gracefully.
- **Verdict:** Over-engineered. Violates KISS.

### d. Big number ghost (huge faded digit centered over card)
- **Clarity:** Fast pattern-match for the number. No location info — user still has to scan.
- **Read/write:** A 8rem ghost @ 30% opacity over ~half the grid degrades cell legibility (especially for already-crossed red strokes).
- **Cost:** ~15 LoC. One absolute-positioned div with big text.
- **Mobile:** Same digit overlay = visually loud on small screens.
- **Reduced-motion:** No animation needed.
- **Multi-row:** Cannot show 3+ ghost numbers without becoming a soup.
- **Verdict:** Charming but flunks multi-row case + cell legibility.

### e. Cell pulse + smaller toast at bottom-center of card
- **Clarity:** Good. Two reinforcing cues.
- **Read/write:** Toast at bottom = below content, no obstruction.
- **Cost:** ~5 LoC change to existing toast position + add cell pulse.
- **Mobile:** Bottom-toast risks overlapping the next page section / footer.
- **Reduced-motion:** Existing fallback covers.
- **Multi-row:** Same single-toast limitation as today.
- **Verdict:** Closest to "minimum change". Reasonable B-option.

### f. Cell pulse + tooltip on hover
- **Mobile:** Hostile — taps cross cells, not show tooltips. Long-press conflicts with the swipe/tap UX.
- **Verdict:** Reject. Bad for primary use case.

### g. Sweep-light effect
- **Cost:** Heavy (~40 LoC + GPU motion).
- **Reduced-motion:** Must fully disable; users get no fallback indicator at all unless we layer pulse.
- **Multi-row:** 9 simultaneous sweeps = seizure territory.
- **Verdict:** Reject. Overkill, fails accessibility.

### h. Combo (a) + (d) — ghost number + cell pulse
- All of (d)'s cell-legibility issues persist.
- Two animations to coordinate.
- **Verdict:** Worse than either alone.

### i. Floating arrow bouncing at the cell
- Adds a 4th moving element (cells, slashes, section ring already animate).
- Arrow geometry per-cell again.
- **Verdict:** Reject. Visual noise budget exceeded.

---

## Comparison Matrix

| Option | Clarity | Obstruction | Cost | Mobile | RM-friendly | Multi-row |
|--------|---------|-------------|------|--------|-------------|-----------|
| **a. Cell pulse only** | High | None | Low | Best | Yes | Scales |
| b. Cell pulse + chip | High | Tiny | Low-Med | Good | Yes | Good |
| c. Banner + arrow | Med | High | High | Crowded | OK | Bad |
| d. Big ghost number | Med | High | Low | Crowded | OK | Bad |
| e. Pulse + bottom toast | High | None | Lowest | Med | Yes | Same as today |
| f. Hover tooltip | Low | None | Low | **Bad** | OK | OK |
| g. Sweep light | Med | Med | High | OK | **Bad** | **Bad** |
| h. a+d combo | Med | High | Med | Crowded | OK | Bad |
| i. Bouncing arrow | Med | Low | Med | OK | OK | Bad |

---

## Recommendation: **Option (a) — Cell-scoped pulse, drop the toast**

**Why this and not (b)/(e):**

- **YAGNI:** The toast text duplicates info already conveyed by (1) voice "Chờ N", (2) section ring narrowing region, (3) the pulse itself drawing eye to the cell. Three converging cues = textual chip is redundant.
- **KISS:** One mechanism, one CSS keyframe, one `$derived`. No positioning math, no z-stacking, no obstruction debate.
- **DRY:** Mirrors the existing `section-label-waiting` pattern — same amber, same `prefers-reduced-motion` opt-out, same cognitive model. Users already learn "amber = Chờ" from the section ring.
- **Multi-row friendly:** 9 amber pulses degrade gracefully; 9 toasts don't.
- **Brutal truth:** The user asked for 3 things (move inside, opacity, animation). Pulse satisfies all three without an overlay at all — the cell IS in the board, the pulse animates the cell, and there's nothing to fade because nothing covers anything.

**Acknowledged trade-off:** Drops the explicit textual "Chờ N" callout for sighted users who don't enable voice. Mitigation: voice already covers this; for the silent-mode minority, the section ring + cell pulse gives precise location. If user testing reveals the number itself is missed, fall back to **Option (b)** by adding a small chip — but ship (a) first, see if anyone complains. (Add chip later costs ~10 LoC.)

---

## Implementation Sketch (~12 LoC)

**`PlayerBoard.svelte`** — derive a Set of waiting cell coords:

```svelte
<script>
  // Set of "row,col" strings for cells holding a row's awaited number.
  const waitingCells = $derived.by(() => {
    const s = new Set();
    if (!grid || !crossed.length) return s;
    grid.forEach((row, r) => {
      if (rowCompleteness[r]) return;
      const n = getWaitingNumber(grid, crossed, r);
      if (n === null) return;
      const c = row.indexOf(n);
      if (c >= 0) s.add(`${r},${c}`);
    });
    return s;
  });
</script>
```

In the cell render, add `cell-waiting` class when `waitingCells.has(`${row},${col}`)`. Drop or keep the toast — recommendation: **delete the toast block** entirely (lines 466-484 + `toast` state + `showToast`/`dismissToast`/`toastTimer`).

**`app.css`** — pulse keyframe, mirrors `section-pulse`:

```css
.cell-waiting {
  animation: cell-waiting-pulse 1.6s ease-in-out infinite;
  box-shadow: inset 0 0 0 3px rgb(245 158 11 / 0.7);
}
@keyframes cell-waiting-pulse {
  0%, 100% { box-shadow: inset 0 0 0 3px rgb(245 158 11 / 0.45); }
  50%      { box-shadow: inset 0 0 0 3px rgb(245 158 11 / 0.95),
                         0 0 8px 2px rgb(245 158 11 / 0.5); }
}
@media (prefers-reduced-motion: reduce) {
  .cell-waiting { animation: none; box-shadow: inset 0 0 0 3px rgb(245 158 11 / 0.7); }
}
```

**Notes:**
- Inset ring keeps cell footprint stable (no layout shift).
- The outer amber glow at 50% adds extra "look here" without colliding with neighboring cells (8px halo dies at the cell border).
- Compatible with existing red-on-not-yet-crossed and crossed states — `box-shadow` layers above background, doesn't replace it.
- The section-label-waiting ring stays — it's the regional cue; cell pulse is the precise cue. Two-tier hierarchy mirrors how players actually scan: section first, then row, then cell.

---

## Cleanup To Do

If toast is dropped:
- Remove `toast`, `toastTimer`, `showToast`, `dismissToast` from `PlayerBoard.svelte`.
- Remove `showToast(...)` call at line 173.
- Remove `dismissToast()` from `handleGenerate`/`handleClear`/unmount.
- Remove toast HTML block (lines 466-484).
- Remove `@keyframes toast` and `.animate-toast` from `app.css`.

Net diff: ~+12 / −35 LoC. Codebase shrinks.

---

## Unresolved Questions

1. Should the cell pulse stop firing once user-tapped (false-positive scenario: user clicks the awaited number, it crosses, pulse vanishes — already handled by `waitingCells` recompute via `crossed` reactivity)?
2. Color contrast on dark-mode emerald-on-amber overlap if a row is mid-completion: the cell can be amber-pulsing AND already-crossed red (rare; only if we're waiting on a different cell same row — impossible by definition since `getWaitingNumber` returns null if all but one are crossed). Confirmed safe.
3. Should we keep the toast as a fallback behind a setting `settings.showWaitingToast`? Recommendation: **No** — YAGNI. Add it only if a user requests it.
