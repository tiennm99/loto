---
slug: tan-tan-3-section-board-and-settings
created: 2026-04-26
status: completed
completedAt: 2026-04-26
mode: fast
blockedBy: []
blocks: []
---

# Tân Tân 3-Section Player Board + Settings

Two cosmetic features that make the app look like a real Minh Tân lô tô sheet:

1. **Visual split** — render the 9×9 player card as 3 stacked 3×9 mini-cards
   with traditional Tân Tân separator labels. Data and generator unchanged.
2. **Settings** — gear icon → modal with a color picker for empty cells.
   Applies to all boards (player + master tracking grid). Default: brown.

Reference image: physical "Minh Tân" card from dochoicholon.com — 3 mini-cards
stacked on one sheet, brown empty cells, decorative cross-hatch separators
labeled "Minh Tân" / "Loại đặc biệt" / "Tấn tài tấn lộc".

## Decisions (locked, do not re-ask)

- **Layout**: stacked on one page, not paginated.
- **Generator**: unchanged — keeps exact 5/row + 5/col on the full 9×9.
- **Color scope**: setting applies to player card *and* master tracking grid.

## Phases

| # | Phase | Status | File |
|---|---|---|---|
| 1 | 3-section visual split for PlayerBoard | DONE | [phase-01-three-section-player-board.md](phase-01-three-section-player-board.md) |
| 2 | Settings modal + empty-cell color picker | DONE | [phase-02-settings-color-picker.md](phase-02-settings-color-picker.md) |

## Files Touched

| File | Phase | Why |
|---|---|---|
| `src/lib/PlayerBoard.svelte` | 1, 2 | render 3 sections; consume color setting |
| `src/routes/master/+page.svelte` | 2 | consume color setting for empty cells |
| `src/routes/+page.svelte` | 2 | mount settings button (player page header) |
| `src/lib/settings-store.svelte.js` | 2 | new — rune-based settings store + persistence |
| `src/lib/SettingsButton.svelte` | 2 | new — gear button + modal |
| `src/app.css` | 1, 2 | section separator styles, CSS var for empty color |
| `docs/project-overview-pdr.md` | 1, 2 | document new visual + settings |
| `docs/codebase-summary.md` | 1, 2 | new files in component table |

## Key Dependencies

- Phase 2 depends on Phase 1 (so the new section background uses the
  configurable color from day one). They can also ship independently —
  Phase 2 will simply apply to both flat and section layouts.

## Out of Scope

- Per-card color override (one global color for all cards).
- Theme preset bundles.
- Other settings (font, sound, etc.) — settings store will be extensible
  but only "empty-cell color" lands now.
- Decorative bg images / paper texture.
- Card serial number badge (the "25" badge in the physical photo).
