# Codebase Summary

## File Organization

### Routing & Layout
| File | Purpose |
|------|---------|
| `app/layout.jsx` | Root HTML layout. Sets Vietnamese lang, imports Geist font, applies global flex layout. |
| `app/page.jsx` | Player page (`/`). Instructions toggle, PlayerBoard component, indigo gradient branding. |
| `app/master/page.jsx` | Host page (`/master`). Controls (new game, draw number), 9×10 master board, host's player card. |

### Shared Components
| File | Purpose |
|------|---------|
| `components/player-board.jsx` | Reusable player card (9×9 grid). Handles crossed state, bingo popup, "Chờ X" toast. Accepts `storagePrefix` prop for multi-card isolation. |

### Game Logic
| File | Purpose |
|------|---------|
| `lib/game-logic.js` | Stateless utilities: generateGrid (weighted column selection), saveGrid, loadGrid, saveCrossedState, loadCrossedState, isRowComplete, getWaitingNumber. |

### Styling
| File | Purpose |
|------|---------|
| `app/globals.css` | Root styles: Tailwind @import, CSS variables (light/dark), `.loto-grid` & `.master-grid` (9-col), animations (fade-in, pop-in, bounce-slow, spin-slow, toast), `.cell-crossed` diagonal. |

### Configuration
| File | Purpose |
|------|---------|
| `next.config.mjs` | Dual basePath: prod `/loto`, codeserver `/absproxy/{PORT}`. Exports static HTML. HMR-aware. |
| `package.json` | Next 16.2.2, React 19.2.4, Tailwind 4. Scripts: dev, dev:codeserver, build, start, lint. |
| `eslint.config.mjs` | ESLint 9 config (Next.js preset). |
| `.gitignore` | Excludes node_modules, .next, .env.local, etc. |
| `.env.example` | Template for env vars (currently none required for runtime; codeserver profile reads CODESERVER_HOST/PORT). |

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
| `generateGrid()` | game-logic.js:52 | Creates 9×9 with weighted column selection (5 nums/row). |
| `isRowComplete()` | game-logic.js:108 | Boolean: all non-zero cells in row crossed? |
| `getWaitingNumber()` | game-logic.js:120 | Returns the single uncrossed number in row, or null. |
| `handleCellClick()` | player-board.jsx:112 | Toggle crossed[row][col]. |
| `handleDrawNext()` | master/page.jsx:88 | Pop first number from remaining, add to called. |

Last reviewed: 2026-04-26
