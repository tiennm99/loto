# Codebase Summary

## File Organization

### Routing & Layout
| File | Purpose |
|------|---------|
| `src/routes/+layout.svelte` | Root HTML layout. Sets Vietnamese lang, imports Geist font, applies global flex layout. |
| `src/routes/+page.svelte` | Player page (`/`). Header + SettingsButton, PlayerBoard, conditional MasterPanel (when `settings.masterMode`), PageFooter. Indigo→purple gradient branding. |
| `src/routes/master/+page.svelte` | Host page (`/master`). Thin shell: header + back link + MasterPanel + PageFooter. All host logic lives in `MasterPanel.svelte`. |

### Shared Components
| File | Purpose |
|------|---------|
| `src/lib/PlayerBoard.svelte` | Reusable player card (9×9 grid rendered as 3 stacked 3×9 mini-cards: Tân Tân / An khang thịnh vượng / Tân Tân tốt nhất). Tall (3:5 on mobile; wider on sm+) cells with condensed bold black numbers (`tan-tan-num` font stack), white number cells, purple empty cells by default. Handles crossed state, bingo popup, "Chờ X" toast. Accepts `storagePrefix` prop for multi-card isolation. Empty cells use `--empty-cell-bg` CSS var from settings store. |
| `src/lib/SettingsButton.svelte` | Gear icon + modal. 4 fieldsets: Giao diện (theme auto/light/dark), Chế độ quản trò (master mode toggle), Tự động xổ (auto-call + speed 1–10s), Màu ô trống (10 Excel color swatches). Reset-to-default button. Mounted on both `/` and `/master` headers. |
| `src/lib/MasterPanel.svelte` | Host controls. New game / draw, "Số vừa xổ" hero token, "Thứ tự đã xổ" history list, host's own player card. No 11×9 tracking board (player card is enough). "Xổ số" / "Bắt đầu / Dừng" button bound to auto-call. Mounted conditionally on `/` when `settings.masterMode === true`, or directly on `/master`. |
| `src/lib/PageFooter.svelte` | Footer with tagline ("Made by miti99 with ❤️ SVG icon") + link. Mounted on `/` and `/master`. |

### Game Logic
| File | Purpose |
|------|---------|
| `src/lib/game-logic.js` | Stateless utilities: generateGrid (constraint-aware picker — exact 5 per row & per col, ascending-sorted columns), saveGrid, loadGrid, saveCrossedState, loadCrossedState, isRowComplete, getWaitingNumber. |
| `src/lib/settings-store.svelte.js` | Reactive global UI settings via Svelte 5 runes. Stores 5 keys: `theme` (enum: "auto" / "light" / "dark"), `masterMode` (bool), `autoCallEnabled` (bool), `autoCallSpeed` (1–10), `emptyCellColor` (hex). Persisted to localStorage `loto_settings`. Pushes values to CSS vars and `<html class="dark">` on `:root`. Per-key validators preserve old data. |

### Styling
| File | Purpose |
|------|---------|
| `src/app.css` | Root styles: Tailwind @import, CSS variables (light/dark), Tailwind v4 `@variant dark (.dark *)` for explicit dark-mode class selector, `.loto-grid` & `.master-grid` (9-col), animations (fade-in, pop-in, bounce-slow, spin-slow, toast), `.cell-crossed` diagonal. |

### Tests
| File | Purpose |
|------|---------|
| `src/lib/game-logic.test.js` | 26 unit tests: generateGrid shape (9×9, 5 per row/col, no duplicates), column ranges & ascending sort, row completion, waiting number detection, persistence (saveGrid/loadGrid/saveCrossedState/loadCrossedState with validators). |
| `src/lib/settings-store.test.js` | 27 unit tests: defaults, loadSettings (restore 5 keys from localStorage, apply CSS vars, toggle dark class, handle empty/corrupt), saveSettings, resetSettings, theme toggle (auto → OS pref detection), master mode, auto-call + speed, color validation. |

### Configuration
| File | Purpose |
|------|---------|
| `svelte.config.js` | adapter-static (HTML export), dual basePath via BUILD_PROFILE env. |
| `vite.config.js` | Tailwind + SvelteKit plugins. codeserver HMR config (port, allowedHosts, hmr). |
| `package.json` | SvelteKit 2, Svelte 5 (runes), Tailwind 4, Vite. Scripts: dev, dev:codeserver, build, build:gh, lint, test, test:watch. |
| `eslint.config.mjs` | ESLint 9 flat config (@eslint/js + eslint-plugin-svelte). Declares Svelte 5 rune globals (lines 16–22). |
| `jsconfig.json` | Path alias `$lib`, no checkJs. |
| `.gitignore` | Excludes node_modules, build, .env.local, etc. |
| `.env.example` | codeserver profile vars (CODESERVER_HOST, CODESERVER_PORT). |

## Key Data Structures

**Grid**: 9×9 2D array of numbers (1–90). Empty cells are 0.  
**Crossed**: 9×9 2D array of booleans indicating marked cells.  
**Master State**: `{ called: number[], remaining: number[] }` — drawn and undrawn numbers.

## Storage Keys (localStorage)

| Key | Use Case |
|-----|----------|
| `loto_grid` | Player's card numbers. |
| `loto_crossed` | Player's marked cells. |
| `loto_master` | Host's drawn/remaining numbers. |
| `loto_master_card_grid` | Host's player card numbers. |
| `loto_master_card_crossed` | Host's marked cells. |
| `loto_settings` | Global UI settings: `{ theme, masterMode, autoCallEnabled, autoCallSpeed, emptyCellColor }`. |

## Component Hierarchy

```
RootLayout
├── HomePage (/)
│   ├── PlayerBoard (storagePrefix="loto")
│   ├── [if settings.masterMode]
│   │   └── MasterPanel (state, controls, draw history)
│   └── PageFooter
└── MasterPage (/master)
    ├── MasterPanel (state, controls, draw history)
    ├── Back link
    └── PageFooter
```

## Key Functions

| Function | Location | Effect |
|----------|----------|--------|
| `pickFilledCols()` | game-logic.js | Per-row column selection that guarantees exact 5 per col (forces any col whose remaining quota equals rowsLeft, random-fills the rest). |
| `generateGrid()` | game-logic.js | Builds 9×9; ascending-sorted numbers per column. |
| `isRowComplete()` | game-logic.js | Boolean: all non-zero cells in row crossed? |
| `getWaitingNumber()` | game-logic.js | Returns the single uncrossed number in row, or null. |
| `handleCellClick()` | PlayerBoard.svelte | Toggle crossed[row][col]. |
| `saveGrid()` / `loadGrid()` | game-logic.js | localStorage with prefix-based keys. |

Last reviewed: 2026-04-27
Last synced: 2026-04-27 (6-phase refactor)
