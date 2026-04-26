# System Architecture

## Page Flow

```
Entry
  ├─ / (Player Page)
  │   ├─ Load loto_grid, loto_crossed from localStorage
  │   ├─ Display 9×9 grid
  │   ├─ Generate new grid on button click
  │   ├─ Mark/unmark cells on click
  │   └─ Show bingo popup + "Chờ X" toasts
  │
  └─ /master (Host Page)
      ├─ Load loto_master (called/remaining)
      ├─ Display 9×10 master board (numbers 1–90)
      ├─ Draw button shows next called number
      ├─ Display host's own card (loto_master_card prefix)
      └─ New Game button resets called/remaining
```

## State Model

### Player Card (`storagePrefix="loto"`)
```
grid: number[][]          // 9×9 numbers (0 = empty)
crossed: boolean[][]      // 9×9 marked state
```

Each row has exactly 5 non-zero numbers (distributed across columns via weighted random).

### Host State (`storagePrefix="loto_master"`)
```
called: number[]          // [5, 23, 67, ...] — drawn in order
remaining: number[]       // [1, 2, 3, ...] minus called — shuffled initially
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

Last reviewed: 2026-04-26
