# Phase 3 — Mobile fit + page header/footer refactor

## Context
- [plan.md](plan.md). Independent of Phases 2/4/5/6 — can land any time.
- Touches PlayerBoard CSS (responsive cells), `/` page header, and adds a footer.

## Overview
- Priority: P1
- Status: TODO
- Effort: ~30 min

## Mobile fit

### Goal
The 9-col card with `aspect-[3/5]` cells × 3 sections × 3 rows currently produces
a tall card that overflows portrait viewport on small phones. Want the player
board to **fit a typical mobile portrait viewport** (no horizontal scroll, fits
within 1 viewport height where reasonable).

### Approach (numbers worked from a 360 px portrait baseline)
- 360 px viewport − 16 px page padding (`px-2`) = 344 px card width.
- 9 cols → 38 px wide per cell.
- On mobile, switch cells from `aspect-[3/5]` (63 px tall) → `aspect-square`
  (38 px tall). 3 rows × 38 = 114 px per section + 30 px label = 144 px × 3
  sections + 16 px frames = ~448 px. Plus header (~120 px) + buttons + footer
  ≈ 700 px total — fits portrait on most phones (Pixel 7 = 915 px).
- On `sm:` breakpoint (≥640 px) cells return to the prettier `aspect-[3/5]`.

### Number text size
- Smaller cells need smaller font: `text-base` mobile, `text-2xl sm:text-3xl`
  on larger.

### Page padding
- Player page wrapper: `px-3 py-8 sm:py-12` → `px-2 py-4 sm:px-3 sm:py-12`.
  Tighten on mobile so the card breathes less.

## Header/footer refactor

### Current header (`src/routes/+page.svelte`)
- H1 "Lô tô"
- Tagline: "Lấy cảm hứng từ những buổi họp lớp thiếu giấy chơi lô tô / của TN1 (2014–2017)"
- "Hướng dẫn / Trang quản trò" links
- Settings gear

### After
- Settings gear (same place)
- H1 "Lô tô"
- (no tagline — moved to footer)
- (no instructions toggle — removed)
- (no "Trang quản trò" link — gated by master-mode setting in Phase 5)
- Subhead removed

### Footer (NEW component `src/lib/PageFooter.svelte`)
```
[centered tagline: "Lấy cảm hứng từ những buổi họp lớp thiếu bộ lô tô của TN1 (2014–2017)"]
[centered: Made by <a>miti99</a> with <heart-svg>]
```
Tagline corrected from "thiếu giấy chơi lô tô" → "thiếu bộ lô tô" per user spec.

Heart icon: small inline SVG (Phosphor / Lucide-style heart, 14px), filled red
(`fill-red-500`). Use the same simple SVG pattern as the SettingsButton gear.

### Bottom-section-label cleanup in PlayerBoard
The current PlayerBoard has a final `<div class="section-label">Made by miti99
with ❤️ ...</div>`. Remove that block — the attribution lives in PageFooter
now. Keep the rest of the labels above each section.

## Files

| File | Change |
|---|---|
| `src/lib/PlayerBoard.svelte` | Cells responsive `aspect-square sm:aspect-[3/5]`. Number text `text-base sm:text-2xl md:text-3xl`. Drop the bottom "Made by" section-label. |
| `src/lib/PageFooter.svelte` | NEW — tagline + made-by-with-heart-svg. ~30 LOC. |
| `src/routes/+page.svelte` | Drop tagline `<p>`, instructions toggle/panel, "Trang quản trò" link. Mount `<PageFooter />` after `<PlayerBoard />`. Tighten page padding. |
| `src/routes/master/+page.svelte` | Mount `<PageFooter />` too (Phase 5 collapses this whole route into a thin shell, so this footer mount becomes near-trivial then). |

## Implementation Steps

1. **PageFooter.svelte (new)** — write the small component:
   ```svelte
   <footer class="mt-8 text-center text-xs text-slate-500 dark:text-slate-400 space-y-2">
     <p>Lấy cảm hứng từ những buổi họp lớp thiếu bộ lô tô của TN1 (2014–2017)</p>
     <p class="flex items-center justify-center gap-1">
       Made by
       <a href="https://miti99.com" target="_blank" rel="noopener noreferrer"
          class="text-indigo-500 hover:underline">miti99</a>
       with
       <svg viewBox="0 0 24 24" fill="currentColor" aria-label="trái tim"
            class="inline w-3.5 h-3.5 text-red-500">
         <path d="M12 21s-7-4.35-9.5-8.5C.5 8.5 3 4 7 4c2 0 3.5 1 5 3 1.5-2 3-3 5-3 4 0 6.5 4.5 4.5 8.5C19 16.65 12 21 12 21z"/>
       </svg>
     </p>
   </footer>
   ```
2. **PlayerBoard.svelte**:
   - Cells: replace `aspect-[3/5]` → `aspect-square sm:aspect-[3/5]` on both empty-cell div and button cell.
   - Number text size: replace `text-2xl sm:text-3xl` → `text-base sm:text-2xl md:text-3xl`.
   - Delete the final bottom `<div class="section-label">…Made by miti99…</div>` block.
3. **/+page.svelte**:
   - Remove `let showInstructions = $state(false)`, the tagline `<p>`, the instructions toggle button, the instructions panel block, and the "Trang quản trò" link (Phase 5 will gate this differently).
   - Add `import PageFooter from "$lib/PageFooter.svelte";` and mount `<PageFooter />` after `<PlayerBoard />`.
   - Adjust wrapper to `px-2 py-4 sm:px-3 sm:py-12`.
4. **/master/+page.svelte**: import + mount `<PageFooter />`. (Will be massively simplified in Phase 5.)

## Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Mobile cells too small to read at `text-base` | Med | UX | Manually verify with browser devtools 360px width; bump to `text-lg` if needed |
| Heart SVG render width drift across browsers | Low | Cosmetic | Fixed `w-3.5 h-3.5` via Tailwind |
| Aspect-square mobile + tan-tan-num font feels squat | Low | Cosmetic | Acceptable trade for fit |

## Success criteria

- iPhone-width devtools (375 px): card fits without horizontal scroll, total page height ≤ ~750 px.
- Desktop (≥640 px): card looks the same as before this phase (tall cells).
- Footer shows tagline above made-by-with-heart-svg-link.
- No instructions toggle anywhere.
- All tests still pass.

## Next
- Phase 4 (extract MasterPanel) is independent.
- Phase 5 wires master mode + the missing "Trang quản trò" hook (now lives in Settings).
