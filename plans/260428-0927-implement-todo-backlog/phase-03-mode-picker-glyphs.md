---
name: Mode picker glyph redesign
phase: 3
status: todo
priority: medium
effort: 45m
---

# Phase 3 — Mode picker glyph redesign

## Context
- `src/lib/SettingsButton.svelte:259-278` — three SVG glyphs for
  player / master / both modes.
- TODO complaint: "Both" two-stacked-rectangles reads as "windows", not
  the intended "player + master" combination. Player rect is clear,
  megaphone is abstract but acceptable.

## Decision
Two paths considered, picking **Path B**:
- ❌ Path A: redesign the "Both" glyph to be more obvious. Risk: any
  abstract glyph in a 24×20 box has the same readability ceiling.
- ✅ Path B: keep glyphs as visual anchors, add an icon-pair for "Both"
  (mini-grid + mini-megaphone composed), and the existing button text
  label below the glyph already says "Cả hai" — that text is the real
  affordance. Tighten the SVG.

## Files
- Modify: `src/lib/SettingsButton.svelte:269-275` — replace "both" SVG only

## New "Both" glyph

Instead of two stacked rectangles, show a small grid (player) AND a
small megaphone shape side-by-side:

```html
{:else}
  <svg viewBox="0 0 28 16" class="w-7 h-5" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
    <!-- Player grid (left) -->
    <rect x="1" y="2" width="11" height="12" rx="1" />
    <path d="M1 6h11M1 10h11M5 2v12M9 2v12" />
    <!-- Megaphone (right) -->
    <path d="M16 6l9-3v10l-9-3z" stroke-linejoin="round" />
    <path d="M16 6v4" />
  </svg>
{/if}
```

Outcome: glyph reads as "grid + megaphone" — concrete representation of
the two roles being combined. Same 1.5px stroke as siblings, fits in
existing button height.

## Steps
1. Replace the `{:else}` SVG block (lines 270-274 region).
2. Visual check: open settings modal, all three buttons aligned, glyphs
   feel proportional.
3. Dark mode: confirm `currentColor` strokes contrast properly in both themes.
4. iPhone SE width (375px) — buttons stay in `grid-cols-3` row without
   wrapping.

## Success
- "Both" glyph visually combines a grid + megaphone.
- All three buttons align (same row, equal heights).
- Touch targets unchanged (button padding intact).

## Out of scope
- Adding labels under glyphs — buttons already render `{label}` from
  `MODES`. The TODO author proposed "tiny role labels under each glyph"
  but the existing label already serves that role.
