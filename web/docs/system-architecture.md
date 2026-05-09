# System Architecture

## Page Flow

```
Entry (+layout.svelte)
  ├─ onMount: loadSettings() — restore all 9 settings keys from loto_settings,
  │   apply CSS vars, toggle <html class="dark"> on theme/OS pref, setup auto-call effect
  │
  └─ / (single page)
      ├─ [if settings.mode !== "master"]
      │   ├─ Load loto_grid, loto_crossed from localStorage
      │   ├─ Display 9×9 PlayerBoard (empty cells use --empty-cell-bg from settings)
      │   ├─ Listen to call-bus for auto-tick when master draws
      │   ├─ Generate new grid on button click
      │   ├─ Mark/unmark cells on click
      │   └─ Show bingo popup + "Chờ X" toasts
      ├─ [if settings.mode !== "player"]
      │   └─ Mount MasterPanel (controls + draw history, publishes to call-bus)
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
theme: "auto" | "light" | "dark"    // Display mode
mode: "player" | "master" | "both"  // Panel visibility (replaces masterMode)
autoCallEnabled: boolean            // Enable auto-call timer
autoCallSpeed: number (1–10)        // Speed in seconds
emptyCellColor: "#rrggbb"           // Hex color (default #7030A0 Excel Purple)
voiceEnabledMaster: boolean         // Speak called numbers (master view)
voiceEnabledPlayer: boolean         // Speak "Chờ"/"Kinh" (player events)
voiceWaitingNumber: boolean         // Include number after "Chờ"
voice: string                       // Voice ID from audio manifest
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

### Production (GitHub Pages, canonical)
```
npm run build:gh
basePath="/loto"
Output: build/index.html with /loto prefix
Deploy: https://tiennm99.github.io/loto/
Workflow: .github/workflows/deploy-github-pages.yml
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

## Data Flow: Master Draw (mode: "both")

```
1. Master taps "Xổ số" button in MasterPanel
2. Draw logic removes number from remaining
3. broadcastDraw(num) publishes to call-bus
4. PlayerBoard's $effect listens to bus.lastDrawn
5. Auto-marks cell in player's grid (if it exists)
6. If row completes → bingo popup + auto "Kinh" voice
7. MasterPanel displays hero token, appends to history
```

## Data Flow: Mark a Cell (Player)

```
1. User clicks button in PlayerBoard
2. handleCellClick(row, col) fires
3. crossed = crossed.map(...) toggled state
4. $effect listens to crossed → saveCrossedState()
5. localStorage updated with new crossed state
6. $derived updates rowCompleteness matrix
7. If row complete → bingo popup; if waiting → "Chờ X" toast
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

## Call Bus (Master ↔ Player Wiring)

In `mode: "both"`, MasterPanel and PlayerBoard communicate via `call-bus.svelte.js`:

```
call-bus.svelte.js:
├─ bus.lastDrawn = { num, at }     // Published by master draw
├─ broadcastDraw(num)              // Called by MasterPanel on each draw
└─ resetBus()                       // Called on new game

PlayerBoard:
└─ $effect(() => { bus.lastDrawn })  // Listens for draw broadcast
   └─ Auto-marks cell if number exists on board
   └─ Triggers bingo popup if row completes
```

Each draw creates a new object (even repeat numbers) to ensure reactive re-fire.

## Offline Capability & PWA

**Static Storage**: All game state in localStorage. No API calls. Fully functional offline.

**Service Worker**: @vite-pwa/sveltekit auto-generates `/sw.js` with:
- App shell precache (~353 KB): HTML, CSS, JS bundles for instant load
- Audio runtime caching: Voice clips cached on-demand (CacheFirst strategy)
- Navigation: NetworkFirst (try network, fallback to cache)
- Auto-update: Detects new versions, shows reload toast, skipWaiting on user action

**Manifest**: `/manifest.webmanifest` enables install prompt on Android/iOS:
- Display: `standalone` (fullscreen app-like mode, no browser chrome)
- Theme colors: Indigo primary, white background
- Icons: 192px + 512px PNG (auto-generated)
- Start URL: `/` with basePath awareness

**Installation Flow**:
1. App loads, registers SW (production only, not dev)
2. Browser detects manifest → shows install prompt (native, no custom banner)
3. User installs → app appears on home screen, launches in standalone mode
4. Updates: SW checks for new version; if found, toast appears; user can reload

Last reviewed: 2026-05-09
Last synced: 2026-05-09 (Cloudflare Pages removed; GitHub Pages is the sole production target)
