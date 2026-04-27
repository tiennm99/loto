# Phase 6 — Tests pass + docs sync

## Context
- [plan.md](plan.md). Final phase.

## Overview
- Priority: P2
- Status: TODO
- Effort: ~10 min

## Verification

```bash
npm test          # expect 98/98 still pass — no new tests required
npm run build     # clean
```

This plan ships no new tests because the changes are visual /
interaction polish; existing settings + game-logic + number-words
tests remain authoritative.

## Manual smoke (browser, mobile + desktop)

| Step | Expected |
|---|---|
| Cold reload (clear localStorage) | H1 marquee + subtitle visible; preview card faded; warm welcome line |
| Click "Tạo bảng mới" | grid renders; cells comfortably tappable on phone |
| Tap a cell on Android | 10ms vibration, scale-down press, cross-out diagonal animates in |
| Mark a row to bingo | popup + audio (existing behavior) |
| Mark second & third row to bingo | popup + confetti rain on 3rd |
| Open Settings → toggle master mode | section slides in, no jump |
| Master "Xổ số" | giant hero appears, auto-scrolls into view, screen reader announces number |
| Toggle dark mode → look at empty cells | purple is muted, not neon |
| Settings on tablet (≥640px) | modal `max-w-md`, swatches breathe |
| Switch toggles in settings | feel like switches, not buttons |

## Docs to sync

| File | Change |
|---|---|
| `docs/codebase-summary.md` | Update PlayerBoard description (haptic, animated cross-out, tiered celebration). Update SettingsButton (switch UI). Update +page.svelte (marquee H1 + subtitle). Add `@keyframes confetti-fall`, `cross-draw` to app.css description. |
| `docs/project-overview-pdr.md` | "Visual Language" section: add wordmark gradient (rose→amber→rose); confetti tier note; haptic feedback. Add 1-2 acceptance items (mobile cell legibility, tiered celebration). |
| `docs/development-roadmap.md` | No change (no new ideas, no new completed items belong here). |

Skip `system-architecture.md`, `code-standards.md`, `deployment-guide.md`.

## Out of scope (intentionally)

- New tests for visual changes (Vitest doesn't render, Playwright not
  set up; visual regression deferred to a future PWA/E2E plan).
- Reduced-motion media queries for animations (one-line fix; defer
  until a real accessibility complaint).
- Configurable section labels.
- Custom font for the wordmark.

## Success criteria

- All 98 unit tests still pass.
- `npm run build` clean.
- Manual smoke checklist all green.
- Docs reflect new visual behavior.
- Plan marked completed; tasks #5–#11 from voice plan stay closed,
  new task chain marked done.
