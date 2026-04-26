# Development Roadmap

This document tracks **future work only**. Completed features live in git commit history, not here.

## Currently Implemented Features

The app is fully functional for core gameplay (Lô tô hội chợ Tân Tân variant):
- 9×9 player card with **exactly 5 per row and 5 per column**, columns
  ascending top-to-bottom
- Cell marking (toggle crossed state); player can keep playing after Kinh
- Bingo detection and "Kinh!" celebration popup
- "Chờ X" waiting notifications
- Host number drawing from 1–90 deck
- 11×9 last-digit-aligned master board with draw-order overlay for fast
  Kinh verification
- Player card rendered as 3 stacked Tân Tân mini-cards
  (Minh Tân / Loại đặc biệt / Tấn tài tấn lộc) with cross-hatch dividers
- Settings modal: 4 fieldsets (theme auto/light/dark, master mode toggle,
  auto-call speed 1–10s, 10 color presets), persisted to `loto_settings`
- Theme switcher (auto detects OS pref, explicit light/dark modes)
- Master mode toggle (shows MasterPanel on `/`)
- Auto-call timer with speed slider
- Mobile-responsive cells (aspect-ratio + text scaling)
- MasterPanel extracted component (reused on `/` and `/master`)
- PageFooter with tagline + creator link
- Host's own player card (isolated instance)
- localStorage persistence
- Dark mode
- Mobile responsive
- Offline capable
- Unit tests (Vitest: 26 game-logic tests + 27 settings-store tests, 53 total passing)
  covering constraint validation, persistence, theme/master-mode/auto-call logic, and error handling

## Idea Phase

### Sound Effects on Bingo
Play celebratory chime or "Kinh!" voice snippet when row completes. Could use Web Audio API or `<audio>` tag. **Status**: Idea (no demand yet)

### Undo Last Cell
Allow player to undo the most recent cross/uncross action. Requires change history or state snapshot. **Status**: Idea (low priority)


### PWA Install
Add service worker and manifest for "Install App" prompt on Android/iOS. **Status**: Idea (would require server-side components)

## Considered Phase

### Multiplayer Sync (Real-time)
Host and players connect via WebSocket to sync called numbers and state. Requires backend server. Would enable:
- Decoupled devices (players' phones, host's laptop on big screen)
- Remote tournaments
- Master board auto-updates all players in real-time

**Consideration**: Out of scope for static export. Requires major refactor (SvelteKit API routes + WebSocket server). Deferred indefinitely.

### i18n Beyond Vietnamese
Internationalization (English, Chinese, etc.). Requires extraction of all Vietnamese strings and i18n library (next-intl). **Status**: Considered (low demand for non-Vietnamese users)

### Undo/Redo System
Full undo/redo stack with history navigation. Adds complexity to state management. **Status**: Considered (YAGNI for now)

## Testing

### Component Tests (Planned)
- PlayerBoard with mocked localStorage, $state/$derived verification
- Master page with different game states

### E2E Tests
- Player flow: generate card → click cells → verify bingo popup
- Host flow: new game → draw numbers → verify board state

**Tech**: Playwright

## Future Enhancements (Speculative)

### Export/Import Card
Allow player to export their grid as image or JSON, import someone else's card. Use Canvas API or print-friendly CSS.

### Leaderboard / Stats
Track games won, time per bingo, etc. Requires server-side persistence. Out of scope for static export.

### Accessibility Improvements
- ARIA labels for grid cells
- Keyboard navigation (arrow keys to move, Enter to toggle)
- Screen reader support

### Custom Number Range
Host selects range (e.g., 1–75 for American bingo) instead of hardcoded 1–90. Requires config UI and refactor of game logic constants. **Out of scope** per Lô tô hội chợ Tân Tân focus.

### Different Grid Sizes
Support 8×8 or 10×10 grids, or 3×9 European Bingo 90 tickets. **Out of scope** — variant locked to 9×9 Tân Tân format.

### Two-line / Full-house Win Tiers
European Bingo 90 patterns. **Out of scope** — Tân Tân uses single-line "Kinh!" only.

---

## Decision Rationale

All decisions follow **YAGNI** (You Aren't Gonna Need It), **KISS** (Keep It Simple), **DRY** (Don't Repeat Yourself):
- No multiplayer sync → adds server dependency, breaks static export (Cloudflare Pages)
- No i18n → Vietnamese-only community, localizing adds complexity
- Unit tests implemented → critical paths (constraints, persistence, settings) now have automated coverage
- Component/E2E tests deferred → small codebase, manual testing sufficient for UI flows
- No undo/redo → simple game, mistakes are play experience
- Svelte 5 runes → cleaner reactivity than hooks, smaller mental model

Future work gates on **real user demand**, not speculation.

Last reviewed: 2026-04-27
Last synced: 2026-04-27 (6-phase refactor)
