---
slug: master-merge-theme-auto-mobile-fit
created: 2026-04-27
status: completed
completedAt: 2026-04-27
mode: auto
blockedBy: []
blocks: []
---

# Master mode merge + theme toggle + auto-call + mobile fit

Eight user-requested features bundled into one coordinated refactor.
Touches almost every component in the app but keeps the data model
(grid, crossed, called/remaining, storage keys) untouched.

## Decisions (locked)

- **Default color**: Excel "Standard Color: Purple" `#7030A0`. Most
  saturated of the 10 standard purples; reads well on both white and
  black grids; user picked "purple" as the default.
- **Color presets**: Office Standard Colors palette (10 swatches), see
  Phase 1 for hex values.
- **Theme**: 3-state — `auto` (default, follow OS), `light`, `dark`.
  Implemented by toggling `class="dark"` on `<html>` + Tailwind v4
  `@variant dark (.dark &)` declaration.
- **Master route**: kept as a deep-link that auto-enables master mode
  in settings on visit, then redirects (or just renders the same `/`).
  Recommend: keep `/master` rendering the same content as `/` but with
  master mode forced on for that view (no setting mutation — least
  surprise). Storage prefixes preserved.
- **Master logic refactor**: extract everything in `src/routes/master/+page.svelte`
  except the page-level header into `src/lib/MasterPanel.svelte`. Both
  `/` (when master mode on) and `/master` mount that component.
- **Auto-call lifecycle**: `setInterval` inside an `$effect` that
  depends on `(autoRunning, settings.autoCallSpeed)` so changing speed
  while running re-arms cleanly. Auto-stop when remaining hits 0.
- **Mobile fit**: cell aspect ratio responsive — `aspect-square` on
  mobile (default), `sm:aspect-[3/5]` on ≥640px. Number text
  `text-base sm:text-2xl md:text-3xl`. Page padding tightened on
  mobile.

## Phases

| # | Phase | Status | File |
|---|---|---|---|
| 1 | Settings expansion (theme, masterMode, autoCall*, Excel palette, purple default) + tests | DONE | [phase-01-settings-expansion.md](phase-01-settings-expansion.md) |
| 2 | Theme system: Tailwind v4 class-based dark + JS sync + settings UI | DONE | [phase-02-theme-system.md](phase-02-theme-system.md) |
| 3 | Mobile-fit + page header/footer refactor | DONE | [phase-03-mobile-and-header-footer.md](phase-03-mobile-and-header-footer.md) |
| 4 | Extract MasterPanel component from `/master` route | DONE | [phase-04-extract-master-panel.md](phase-04-extract-master-panel.md) |
| 5 | Master mode integration on `/` (inline render via setting) + `/master` deep-link | DONE | [phase-05-master-mode-integration.md](phase-05-master-mode-integration.md) |
| 6 | Auto-call toggle + interval lifecycle | DONE | [phase-06-auto-call.md](phase-06-auto-call.md) |

## Files Touched (summary)

| File | Phases |
|---|---|
| `src/lib/settings-store.svelte.js` | 1, 2, 5, 6 — new keys, theme apply logic |
| `src/lib/settings-store.test.js` | 1, 2, 6 — new tests |
| `src/lib/SettingsButton.svelte` | 1, 2, 5, 6 — Excel swatches, theme toggle, master mode toggle, auto-call speed |
| `src/lib/PlayerBoard.svelte` | 3 — responsive cells, drop "Made by" from bottom label, footer text simplification |
| `src/lib/MasterPanel.svelte` | 4 — NEW (extracted from master route) |
| `src/routes/+layout.svelte` | 2 — theme already loads on mount; ensure media-query listener if `auto` |
| `src/routes/+page.svelte` | 3, 5 — drop instructions, restructure header, mount MasterPanel conditionally, mount Footer |
| `src/routes/master/+page.svelte` | 4, 5 — collapse to a thin shell that mounts MasterPanel |
| `src/lib/PageFooter.svelte` | 3 — NEW small component (tagline + made-by) |
| `src/app.css` | 2, 3 — convert dark `@media` to class-based, add Tailwind dark variant declaration |
| `docs/project-overview-pdr.md` | post-impl — sync features |
| `docs/codebase-summary.md` | post-impl — new files |
| `docs/system-architecture.md` | post-impl — page-flow update |
| `docs/development-roadmap.md` | post-impl — move "Theme switcher" → Implemented |

## Out of Scope

- Per-card storage migrations
- E2E tests / component tests
- Sound effects, PWA, multiplayer sync
- Custom number range
- Card serial number badges
- Replacing PlayerBoard rendering for master grid
