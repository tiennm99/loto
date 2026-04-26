# System Architecture

## Page Flow

```
Entry (+layout.svelte)
  ├─ onMount: loadSettings() — restore all 5 settings keys from loto_settings,
  │   apply CSS vars, toggle <html class="dark"> on theme/OS pref, setup auto-call effect
  │
  ├─ / (Player Page)
  │   ├─ Load loto_grid, loto_crossed from localStorage
  │   ├─ Display 9×9 PlayerBoard (empty cells use --empty-cell-bg from settings)
  │   ├─ Generate new grid on button click
  │   ├─ Mark/unmark cells on click
  │   ├─ Show bingo popup + "Chờ X" toasts
  │   ├─ [if settings.masterMode === true]
  │   │   └─ Mount MasterPanel (host controls + draw history)
  │   └─ PageFooter (tagline + miti99 link)
  │
  └─ /master (Host Page)
      ├─ MasterPanel (state, controls, draw history; same as above)
      ├─ Back link to /
      └─ PageFooter
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

### Production Mode
```
NODE_ENV=production
basePath="/loto"
Output: /loto/index.html, /loto/_next/...
GitHub Pages serves: https://user.github.io/loto
```

### Development Mode (Local)
```
NEXT_DEV_PROFILE not set
basePath="" (empty)
Dev server: http://localhost:3000
```

### Code-Server Mode
```
NEXT_DEV_PROFILE=codeserver
CODESERVER_HOST=<proxy-host>
CODESERVER_PORT=3000 (or env)
basePath="/absproxy/{PORT}"
HMR Origin: CODESERVER_HOST
Access: https://<proxy>/absproxy/3000/
```

**Note**: `/absproxy/{port}` preserves the basePath through the proxy. `/proxy/{port}` strips it before forwarding, breaking HMR.

## Client-Only Architecture

All pages are client-only (no SSR) because:
- `ssr: false` in `+layout.js` disables server-side rendering
- localStorage is unavailable on server
- State initialization (grid, crossed) must run in browser

Files that are client-only:
- `src/routes/+page.svelte` (player page)
- `src/routes/master/+page.svelte` (host page)
- `src/lib/PlayerBoard.svelte` (shared component)

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
