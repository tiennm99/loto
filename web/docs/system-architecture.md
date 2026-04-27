# System Architecture

## Page Flow

```
Entry (+layout.svelte)
  ├─ onMount: loadSettings() — restore all 5 settings keys from loto_settings,
  │   apply CSS vars, toggle <html class="dark"> on theme/OS pref, setup auto-call effect
  │
  └─ / (single page)
      ├─ Load loto_grid, loto_crossed from localStorage
      ├─ Display 9×9 PlayerBoard (empty cells use --empty-cell-bg from settings)
      ├─ Generate new grid on button click
      ├─ Mark/unmark cells on click
      ├─ Show bingo popup + "Chờ X" toasts
      ├─ [if settings.masterMode === true]
      │   └─ Mount MasterPanel (host controls + draw history)
      └─ PageFooter (tagline + miti99 link)
```

## State Model

### Player Card (`storagePrefix="loto"`)
```
grid: number[][]          // 9×9 numbers (0 = empty)
crossed: boolean[][]      // 9×9 marked state
```

Each row has exactly 5 non-zero numbers AND each column has exactly 5
(constraint-aware picker — no slack). Numbers within a column are sorted
ascending top-to-bottom (lô tô hội chợ Tân Tân convention).

### Settings (`loto_settings`)
```
theme: "auto" | "light" | "dark"  // Display mode
masterMode: boolean               // Show MasterPanel on /
autoCallEnabled: boolean          // Enable auto-call timer
autoCallSpeed: number (1–10)      // Speed in seconds
emptyCellColor: "#rrggbb"         // Hex color (default #7030A0 Excel Purple)
```

### Host State (`storagePrefix="loto_master"`)
```
called: number[]          // [5, 23, 67, ...] — drawn in order
remaining: number[]       // [1, 2, 3, ...] minus called — shuffled initially
autoRunning: boolean      // Auto-call timer active
```

### Host's Card (`storagePrefix="loto_master_card"`)
Same as player card; isolated by prefix to allow host to play.

## localStorage Keys

| Prefix | Grid Key | Crossed Key |
|--------|----------|-------------|
| `"loto"` | `loto_grid` | `loto_crossed` |
| `"loto_master_card"` | `loto_master_card_grid` | `loto_master_card_crossed` |

(Special) `loto_master` stores `{ called, remaining }` for the master state.

All keys are JSON stringified. Corruption is silent (returns null).

## basePath & Asset Resolution

### Production (Cloudflare Pages, default)
```
npm run build
basePath="" (root)
Output: build/index.html
Deploy: loto.miti99.com (root domain)
```

### Production (GitHub Pages, manual)
```
npm run build:gh
basePath="/loto"
Output: build/index.html with /loto prefix
Deploy: https://tiennm99.github.io/loto
```

### Development (Local)
```
npm run dev
basePath="" (empty)
Dev server: http://localhost:5173
```

### Development (Code-Server)
```
npm run dev:codeserver
.env.local: CODESERVER_HOST + CODESERVER_PORT
basePath="/absproxy/{PORT}"
Access: https://<proxy>/absproxy/{PORT}/
```

**Note**: Use `/absproxy/{port}` — `/proxy/{port}` strips the path prefix and breaks the SvelteKit base path.

## Client-Only Architecture

All pages are client-only (no SSR) because:
- `ssr: false` in `+layout.js` disables server-side rendering
- localStorage is unavailable on server
- State initialization (grid, crossed) must run in browser

Files that are client-only:
- `src/routes/+page.svelte` (single page)
- `src/lib/PlayerBoard.svelte` (player card component)
- `src/lib/MasterPanel.svelte` (host panel, mounted when `settings.masterMode`)

## Data Flow: Mark a Cell

```
1. User clicks button in PlayerBoard
2. handleCellClick(row, col) fires
3. crossed = crossed.map(...) toggled state
4. $effect listens to crossed → saveCrossedState()
5. localStorage updated with new crossed state
6. $derived updates rowCompleteness matrix
7. If row complete → bingo popup; if waiting → toast
```

## Data Flow: Initial Load

```
1. Component mounts
2. $effect runs initial loadGrid(storagePrefix)
3. If found, load crossed state (or initialize as all false)
4. Pre-populate celebratedRows + notifiedWaitingRows sets
5. Pass grid + crossed to UI render
```

## Animations

| Name | Duration | Use |
|------|----------|-----|
| fade-in | 0.2s | Modal background entry |
| pop-in | 0.4s | Bingo popup scale + scale-back |
| bounce-slow | 1.5s infinite | Emoji on bingo popup |
| spin-slow | 3s infinite | ✨ on bingo popup |
| spin-slow-reverse | 3s infinite reverse | 🎊 on bingo popup |
| toast | 5s forwards | "Chờ X" notification fade in/build |
| cell-crossed::after | instant | Red diagonal line in marked cells |

## Offline Capability

All state is localStorage. No API calls. Fully functional offline after initial load.

Last reviewed: 2026-04-27
Last synced: 2026-04-27 (6-phase refactor)
