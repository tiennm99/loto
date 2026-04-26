# Codebase Summary

## File Organization

### Routing & Layout
| File | Purpose |
|------|---------|
| `src/routes/+layout.svelte` | Root HTML layout. Sets Vietnamese lang, imports Geist font, applies global flex layout. |
| `src/routes/+page.svelte` | Player page (`/`). Instructions toggle, PlayerBoard component, indigo gradient branding. |
| `src/routes/master/+page.svelte` | Host page (`/master`). Controls (new game, draw number), 9×10 master board, host's player card. |

### Shared Components
| File | Purpose |
|------|---------|
| `src/lib/PlayerBoard.svelte` | Reusable player card (9×9 grid). Handles crossed state, bingo popup, "Chờ X" toast. Accepts `storagePrefix` prop for multi-card isolation. |

### Game Logic
| File | Purpose |
|------|---------|
| `src/lib/game-logic.js` | Stateless utilities: generateGrid (weighted column selection), saveGrid, loadGrid, saveCrossedState, loadCrossedState, isRowComplete, getWaitingNumber. |

### Styling
| File | Purpose |
|------|---------|
| `src/app.css` | Root styles: Tailwind @import, CSS variables (light/dark), `.loto-grid` & `.master-grid` (9-col), animations (fade-in, pop-in, bounce-slow, spin-slow, toast), `.cell-crossed` diagonal. |

### Configuration
| File | Purpose |
|------|---------|
| `svelte.config.js` | adapter-static (HTML export), dual basePath via BUILD_PROFILE env. |
| `vite.config.js` | Tailwind + SvelteKit plugins. codeserver HMR config (port, allowedHosts, hmr). |
| `package.json` | SvelteKit 2, Svelte 5 (runes), Tailwind 4, Vite. Scripts: dev, dev:codeserver, build, build:gh, lint. |
| `eslint.config.mjs` | ESLint 9 flat config (@eslint/js + eslint-plugin-svelte). |
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

## Component Hierarchy

```
RootLayout
├── HomePage (/)
│   ├── Instructions toggle
│   └── PlayerBoard (storagePrefix="loto")
└── MasterPage (/master)
    ├── Controls (new game, draw)
    ├── Master board (9×10)
    └── PlayerBoard (storagePrefix="loto_master_card")
```

## Key Functions

| Function | Location | Effect |
|----------|----------|--------|
| `generateGrid()` | game-logic.js:74 | Creates 9×9 with weighted column selection (5 nums/row). |
| `isRowComplete()` | game-logic.js:200 | Boolean: all non-zero cells in row crossed? |
| `getWaitingNumber()` | game-logic.js:218 | Returns the single uncrossed number in row, or null. |
| `handleCellClick()` | PlayerBoard.svelte:127 | Toggle crossed[row][col]. |
| `saveGrid()` / `loadGrid()` | game-logic.js:149–167 | localStorage with prefix-based keys. |

Last reviewed: 2026-04-26
