---
slug: ui-ux-improvements
created: 2026-04-27
status: completed
completedAt: 2026-04-27
mode: fast
blockedBy: []
blocks: []
---

# UI/UX improvements — mobile legibility, brand, master focus, dark mode

Synthesizes the 12 recommendations from the ui-ux-designer review (see
`/config/workspace/tiennm99/loto/plans/reports/` if archived; key
findings inline below). Six small phases, ~10–25 min each, no new
dependencies, no breaking changes to settings/storage.

## What this changes (in user terms)

- Numbers on the player card are bigger and easier to tap on phones.
- Cold first paint shows a friendly preview, not a gray placeholder.
- Brand wordmark feels like a fairground marquee, not a SaaS header.
- When the host enables master mode, the called number becomes a giant
  hero that auto-scrolls into view; new draws are announced for screen
  readers.
- Dark mode loses the "neon purple" punch on empty cells.
- Settings modal feels less cramped on tablet/desktop.
- Marking a cell has tactile feedback (haptic + crossout animation +
  active-press scale).

## Decisions (locked unless flagged)

- **Hard invariants stay**: 9×9 grid, 5-per-row + 5-per-col, ascending
  columns, no-3-consecutive soft rule, all storage keys, all settings
  keys.
- **No new components/library**: continue plain Svelte + Tailwind.
- **No new fonts in v1** — H1 marquee uses existing system stack +
  `italic tracking-tight` + warmer gradient. Adding a Google Font is
  P2/follow-up.
- **No illustration assets** in this plan — open question #2 below.
- **Section labels stay personal** ("TN1 (2014-2017)",
  "Độc-Đỉnh-Điên") — they're the original group's joke and part of the
  app's identity. Making them configurable is a separate plan.
- **Master mode keeps stacking below player**, not full-screen replace
  — open question #3 below; deferred until a host complains.

## Phases

| # | Phase | File |
|---|---|---|
| 1 | Mobile legibility — cell sizing, haptic, active state, animated cross-out | [phase-01-mobile-legibility.md](phase-01-mobile-legibility.md) |
| 2 | Brand H1 marquee + first-run empty preview | [phase-02-brand-and-empty-state.md](phase-02-brand-and-empty-state.md) |
| 3 | Master mode focal point — giant hero, auto-scroll, slide-in, aria-live | [phase-03-master-focal-point.md](phase-03-master-focal-point.md) |
| 4 | Settings modal width + dark-mode purple + switch-style toggles | [phase-04-settings-and-dark-polish.md](phase-04-settings-and-dark-polish.md) |
| 5 | Two-tier "Kinh!" celebration (first row vs nth row) | [phase-05-celebration-tiering.md](phase-05-celebration-tiering.md) |
| 6 | Tests pass + docs sync | [phase-06-tests-and-docs.md](phase-06-tests-and-docs.md) |

## Files Touched

| File | Phase(s) |
|---|---|
| `src/lib/PlayerBoard.svelte` | 1, 2, 5 |
| `src/lib/MasterPanel.svelte` | 3 |
| `src/lib/SettingsButton.svelte` | 4 |
| `src/routes/+page.svelte` | 2, 3 |
| `src/app.css` | 1, 4 |
| `docs/codebase-summary.md`, `docs/project-overview-pdr.md` | 6 |

## Open questions (deferred — answer at implementation time)

1. **Section labels** — keep "Lô tô / TN1 (2014-2017) / Độc-Đỉnh-Điên"
   hard-coded? **Default: yes, keep them.** Configurable labels = a
   future plan if multiple groups adopt the app.
2. **Illustration assets** — add a paper-lantern / dice motif to the
   header? **Default: skip in v1.** A typographic marquee carries
   enough personality; revisit if real users say it's bland.
3. **Master mode on phone** — should it replace the player view full-
   screen instead of stacking? **Default: keep stacking** + giant
   hero + auto-scroll. Revisit if a host complains they can't see the
   hero while holding their card.

## Out of Scope

- New fonts (Playfair / Bebas Neue / etc.)
- Illustration / icon assets
- Configurable section labels
- Full-screen master mode toggle for phones
- Confetti particle library (Phase 5 uses pure CSS)
- PWA, sound effects beyond TTS, multiplayer sync

## Acceptance summary (user-visible)

- [ ] On a 360px viewport, a player number is comfortably readable
  across a typical living room.
- [ ] First load shows a card preview + warm welcome line, not a gray
  placeholder.
- [ ] H1 looks like a vintage carnival marquee, not a SaaS header.
- [ ] In master mode, the most recently drawn number is the
  unambiguous focal point even with the player card on screen.
- [ ] Switching themes desaturates the purple empty cells in dark
  mode (no more neon punch).
- [ ] Tapping a cell vibrates briefly on mobile, presses inward, and
  draws the cross-out diagonal in ~200 ms.
- [ ] Bingo on row 1 ≠ bingo on row 5 visually (party gets louder).
- [ ] All 98 unit tests still pass; `npm run build` clean.
