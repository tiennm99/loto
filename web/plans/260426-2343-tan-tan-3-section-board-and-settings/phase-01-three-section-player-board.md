# Phase 1 — 3-Section Visual Split for PlayerBoard

## Context

- Plan: [plan.md](plan.md)
- Reference image: physical Minh Tân lô tô card (3 stacked 3×9 mini-cards on
  one sheet, brown empty cells, decorative cross-hatch separators).
- Touches: `src/lib/PlayerBoard.svelte`, `src/app.css`.

## Overview

- **Priority**: P1 (visual identity / authenticity)
- **Status**: TODO
- **Effort**: ~30 min
- **Description**: Render the existing 9×9 grid as 3 visually distinct 3-row
  sections. Data, generator, click handlers, win detection: unchanged.

## Key Insights

- The 9×9 → 3×(3×9) split is **purely visual**. Don't touch
  `src/lib/game-logic.js`. Don't change the grid shape, the storage shape,
  or `isRowComplete` — they all keep operating on a 9×9 array.
- Tân Tân section labels (top to bottom): **Minh Tân** / **Loại đặc biệt**
  / **Tấn tài tấn lộc**. Use these verbatim.
- The decorative cross-hatch border (✚✚✚✚) on the physical card is
  ornamental. CSS approximation: a thin orange/red dotted or repeating
  cross-pattern border between sections is enough — don't pixel-copy.
- Card serial number badge (e.g., "25" on the physical card) is **out of
  scope** per plan.

## Requirements

### Functional
- Player card renders as 3 stacked sub-grids: rows 0–2, rows 3–5, rows 6–8.
- A label sits between each pair of sub-grids (and above the first):
  `Minh Tân`, `Loại đặc biệt`, `Tấn tài tấn lộc`.
- All click / cross / waiting / Kinh behavior keeps working unchanged.
- Cell crossed style, row-complete style, the `Chờ N` toast, and the Kinh
  modal all behave exactly as before.

### Non-functional
- No layout shift or visual jitter.
- Mobile responsive (stays within `max-w-2xl` container).
- Dark mode parity: separator and labels visible on both themes.
- A11y: each sub-grid keeps `aria-label="Bảng lô tô"` (or section-specific
  label like `Bảng lô tô — phần 1`).

## Architecture

### Render strategy

Replace the single `<div class="loto-grid">` with three sibling
`<div class="loto-grid">` blocks, each iterating only its 3 rows. Use
`{#each [0, 3, 6] as startRow, sectionIdx}` and within each section iterate
`grid.slice(startRow, startRow + 3)`.

The `crossed` lookup must use the **absolute row index** (`startRow + r`),
not the slice-relative index. Same for click handlers and `rowCompleteness`.

### Section labels

Plain centered text (`text-center text-xs uppercase tracking-widest text-slate-500`)
between sections, sandwiched by horizontal cross-hatch lines. Use a
repeating linear-gradient or a `.section-divider` utility in `app.css`.

### Border continuity

Currently each cell has `border-r border-b`. Within a section this is fine.
Across sections we **don't** want the bottom row of section 1 to look
adjacent to the top row of section 2 — the separator label / divider sits
between them, so the visual gap naturally breaks the grid.

The outer wrapper (`rounded-2xl overflow-hidden shadow-xl border …`)
currently wraps the whole grid. Two choices:
- **A**: keep one outer wrapper, separators are inside it.
- **B**: each section has its own rounded card.

**Pick A** — one outer wrapper, internal separators. Closer to the
physical sheet which is one continuous piece of paper.

## Related Code Files

### Modify
- `src/lib/PlayerBoard.svelte`
  - Replace the single `{#each grid.flat() …}` loop with 3 sectioned loops
    (lines ~157–191 in current state).
  - Each section iterates `[0, 3, 6][i] .. + 3` and uses absolute row idx
    when reading `crossed[row]`, calling `handleCellClick(row, col)`,
    reading `rowCompleteness[row]`.
  - Add the section header text between sections.
- `src/app.css`
  - Add `.section-divider` utility (cross-hatch / dotted horizontal rule).
  - Optionally add `.section-label` if Tailwind utility classes don't
    cover the styling cleanly.

### Don't touch
- `src/lib/game-logic.js`
- `src/routes/master/+page.svelte` (master grid stays 11×9)
- `src/routes/+page.svelte`

## Implementation Steps

1. In `PlayerBoard.svelte`, extract a `SECTIONS = [0, 3, 6]` constant and a
   parallel `SECTION_LABELS = ['Minh Tân', 'Loại đặc biệt', 'Tấn tài tấn lộc']`.
2. Wrap the existing grid block with a flex column. Inside, loop sections:
   for each `startRow`, render the label, then a `loto-grid` containing
   that section's 3 rows × 9 cols.
3. Inside the inner loop, compute `row = startRow + r`. Pass `row` to
   `handleCellClick`, look up `crossed[row]?.[col]`, `rowCompleteness[row]`.
4. Add the `Chờ` toast container *outside* the sectioned grid (it
   currently uses `absolute inset-0` of the outer relative wrapper —
   keep that wrapper around all 3 sections).
5. In `app.css`, define `.section-divider` (e.g.
   `background: repeating-linear-gradient(90deg, transparent 0 8px, theme(colors.orange.400) 8px 10px); height: 6px;`).
6. Run `npx svelte-check`, then test in dev: generate card, mark cells,
   trigger Kinh, trigger Chờ. All should work.

## Todo

- [ ] Add `SECTIONS` + `SECTION_LABELS` constants to PlayerBoard.svelte
- [ ] Replace flat grid render with 3 sectioned `loto-grid` blocks
- [ ] Verify absolute-row-index math (click, crossed lookup, rowCompleteness)
- [ ] Add label markup between sections
- [ ] Add `.section-divider` style in `app.css`
- [ ] Verify Chờ toast still appears centered over the whole card
- [ ] Verify Kinh modal still triggers
- [ ] Verify dark-mode contrast for labels and dividers
- [ ] Mobile breakpoint check (sm + base)
- [ ] `npx svelte-check` clean

## Success Criteria

- Player page shows 3 visually separated 3×9 mini-cards with the 3 labels.
- Generating a new card, marking cells, and winning all behave identically
  to before this change.
- No svelte-check warnings.
- Dark mode looks intentional (not just light mode in the dark).

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Off-by-one in absolute row index | Medium | High (broken click handling) | Compute `row = startRow + r` once at the top of the inner each, reuse everywhere. |
| Toast positioning breaks | Low | Low | Toast container stays at the same wrapper level it's at today. |
| Border doubling at section seams | Low | Cosmetic | Last row of each section keeps its own `border-b`; separator visually masks it. |
| `aspect-square` cells distort with new container | Low | Cosmetic | grid-template-columns is still `repeat(9, 1fr)` per section, same width budget per section. |

## Security Considerations

None — view-only change.

## Next Steps

After this phase: Phase 2 (settings + color picker) plugs the
configurable color into `bg-slate-50 dark:bg-slate-900/60` empty cells
(replaced by a CSS variable).
