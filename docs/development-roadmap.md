# Development Roadmap

This document tracks **future work only**. Completed features live in git commit history, not here.

## Currently Implemented Features

The app is fully functional for core gameplay:
- 9×9 player card generation with weighted number distribution
- Cell marking (toggle crossed state)
- Bingo detection and celebration popup
- "Chờ X" waiting notifications
- Host number drawing from 1–90 deck
- 9×10 master board tracking called numbers
- Host's own player card (isolated instance)
- localStorage persistence
- Dark mode
- Mobile responsive
- Offline capable

## Idea Phase

### Sound Effects on Bingo
Play celebratory chime or "Kinh!" voice snippet when row completes. Could use Web Audio API or `<audio>` tag. **Status**: Idea (no demand yet)

### Undo Last Cell
Allow player to undo the most recent cross/uncross action. Requires change history or state snapshot. **Status**: Idea (low priority)

### Theme Switcher
Explicit light/dark toggle button instead of relying on OS preference. **Status**: Idea (Tailwind already supports OS toggle)

### PWA Install
Add service worker and manifest for "Install App" prompt on Android/iOS. **Status**: Idea (would require server-side components)

## Considered Phase

### Multiplayer Sync (Real-time)
Host and players connect via WebSocket to sync called numbers and state. Requires backend server. Would enable:
- Decoupled devices (players' phones, host's laptop on big screen)
- Remote tournaments
- Master board auto-updates all players in real-time

**Consideration**: Out of scope for static export. Requires major refactor (Next.js API routes + WebSocket server). Deferred indefinitely.

### i18n Beyond Vietnamese
Internationalization (English, Chinese, etc.). Requires extraction of all Vietnamese strings and i18n library (next-intl). **Status**: Considered (low demand for non-Vietnamese users)

### Undo/Redo System
Full undo/redo stack with history navigation. Adds complexity to state management. **Status**: Considered (YAGNI for now)

## Testing (Planned but Unstarted)

### Unit Tests
- Game logic: `generateGrid()`, `isRowComplete()`, `getWaitingNumber()`
- localStorage helpers: `saveGrid()`, `loadGrid()`, etc.

**Tech**: Jest + @testing-library/react

### Component Tests
- PlayerBoard with mocked localStorage
- Master page with different game states

### E2E Tests
- Player flow: generate card → click cells → verify bingo popup
- Host flow: new game → draw numbers → verify board state

**Tech**: Playwright or Cypress

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
Host selects range (e.g., 1–75 for American bingo) instead of hardcoded 1–90. Requires config UI and refactor of game logic constants.

### Different Grid Sizes
Support 8×8 or 10×10 grids. Major refactor (NUM_ROWS, NUM_COLS constants, weighted selection algorithm).

---

## Decision Rationale

All decisions follow **YAGNI** (You Aren't Gonna Need It):
- No multiplayer sync → adds server dependency, breaks static export model
- No i18n → Vietnamese-only community, localizing adds complexity without demand
- No testing → small codebase, manual testing covers critical paths; add tests when code grows or bugs surface
- No undo/redo → simple game, mistakes are part of play experience

Future work gates on **real user demand**, not speculation.

Last reviewed: 2026-04-26
