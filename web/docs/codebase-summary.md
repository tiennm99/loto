# Codebase Summary

## File Organization

### Routing & Layout
| File | Purpose |
|------|---------|
| `src/routes/+layout.svelte` | Root HTML layout. Sets Vietnamese lang, imports Geist font, applies global flex layout. |
| `src/routes/+page.svelte` | Single page (`/`). Header + SettingsButton, renders player/master/both via `settings.mode`. Indigo→purple gradient branding. |

### Shared Components
| File | Purpose |
|------|---------|
| `src/lib/PlayerBoard.svelte` | Reusable player card (9×9 grid rendered as 3 stacked 3×9 mini-cards: Tân Tân / An khang thịnh vượng / Tân Tân tốt nhất). Tall (3:4 on mobile; 3:5 on sm+) cells with condensed bold black numbers (`tan-tan-num` font stack w/ self-hosted Roboto Condensed), white number cells, purple empty cells (dark mode dims via `filter:brightness(0.85)`). Handles crossed state, animated cross-out (200 ms `cross-draw` keyframe), `active:scale-90` press, 10 ms haptic on tap. Two header actions: "Tạo bảng mới" / "Xoá đánh dấu". First-run state shows a faded preview card. Bingo popup tiers: row 1 = standard celebration; row 3+ = falling-emoji confetti rain via CSS `confetti-fall`. Toast "Chờ N" + audio. Accepts `storagePrefix` prop for multi-card isolation. |
| `src/lib/SettingsButton.svelte` | Gear icon + modal (responsive `max-w-sm sm:max-w-md`). 6 fieldsets: Giao diện (theme pills), Chế độ (3-way mode picker w/ SVG glyphs: player/master/both), Chế độ quản trò (switch row), Tự động xổ (switch + speed slider), Âm thanh (two switches + voice picker), Màu ô trống (10 Excel swatches + custom input in bordered card w/ "Tuỳ chỉnh"/"Mẫu" sub-headers). Boolean toggles use a shared `switchRow` snippet (`role="switch"` + keyboard support). Reset-to-default button. Mounted on `/`. |
| `src/lib/MasterEmptyState.svelte` | Empty board placeholder for first-run master (mirrors PlayerBoard's preview UX). Displays faded 11×9 grid with "Ấn để bắt đầu ván mới" hint. |
| `src/lib/MasterPanel.svelte` | Host controls. New game / draw, large "Số vừa xổ" hero token (160 px mobile, 224 px sm+) with `aria-live="assertive"` + auto `scrollIntoView` on each new draw, "Thứ tự đã xổ" history list, 11×9 last-digit-aligned tracking grid (with circular tokens + draw-order overlay). Calls `drawNext()` / `startNewGame()` from `master-store`; player side reads the same store directly (no bus). "Xổ số" / "Bắt đầu / Dừng" button bound to auto-call. While auto-call runs, mounts `<AutoCountdown>` above the hero (driven by `tickCount` $state, bumped on draw and on every (re-)arm of the auto-call $effect). Mounted conditionally on `/` when `settings.mode !== "player"`; the wrapping section uses `transition:slide` for smooth toggle-in. |
| `src/lib/AutoCountdown.svelte` | Visual countdown for auto-call. Props-driven (`running`, `duration`, `tickKey`) — parent owns the `setInterval`, this component just renders. SVG ring with `stroke-dashoffset` controlled by elapsed-time progress (rAF loop while running) plus centered seconds-remaining number. `prefers-reduced-motion` clamps `dashOffset = 0` (static full ring). `role="timer"` + `aria-live="off"` so screen readers don't announce every second. |
| `src/lib/PageFooter.svelte` | Footer with tagline ("Made by miti99 with ❤️ SVG icon") + link. Mounted on `/`. |

### Game Logic & Coordination
| File | Purpose |
|------|---------|
| `src/lib/game-logic.js` | Stateless utilities: generateGrid (constraint-aware picker — exact 5 per row & per col, ascending-sorted columns, soft "no 3 consecutive filled cols per row" via rejection sampling), saveGrid, loadGrid, saveCrossedState, loadCrossedState, isRowComplete, getWaitingNumber. |
| `src/lib/master-store.svelte.js` | Shared reactive `{called, remaining}` $state for the master deck, persisted to `loto_master`. Exports `masterState`, `loadMaster`, `saveMaster`, `startNewGame`, `drawNext`, `resetMaster`. Hydrated once via `+layout.svelte`'s `onMount` so player-side reads see the full history regardless of mount order. Replaced the single-slot `call-bus` to fix history-loss bugs (regen, reload, multi-tab) — see `plans/reports/code-reviewer-260430-2024-both-mode-consistency.md`. |
| `src/lib/active-tab.svelte.js` | Single-active-tab coordinator via `BroadcastChannel`. New tab broadcasts `claim`; old tab sets `activeTab.inactive = true`. `+layout.svelte` mounts `watchActiveTab()` on boot and renders a fullscreen overlay banner ("Phiên Lô tô đang chạy ở tab khác. Nhấn để tiếp tục tại đây.") when inactive — nhấn calls `claimActiveTab()` which broadcasts back, inactivating the other tab in turn. Soft coordination only (cooperating tabs); no-op in browsers without `BroadcastChannel` (legacy iOS Safari ≤15.4). Prevents double auto-call intervals, double localStorage writers, and overlapping audio across tabs. |
| `src/lib/player-auto-cross.js` | Pure `applyMasterCalls({grid, crossed, called, lastHandledIndex, manualUnticks, mode})`. Cursor-by-index dedup (vs the retired `at`-timestamp model), so any caller can pass `lastHandledIndex: 0` to replay master's full history (used by player regen + "Xoá đánh dấu" in both mode). Manual unticks suppress re-cross on replay. |
| `src/lib/vietnamese-number.js` | `numberToVietnamese(n)` — pure utility mapping 0..90 to spoken Vietnamese, with tonal exceptions (15 → "mười lăm", 21 → "hai mươi mốt", 25 → "hai mươi lăm"). Out-of-range falls back to `String(n)`. |
| `src/lib/voice.js` | Bundled-MP3 playback. Exports `playNumber(n)`, `playWaiting(n)` (sequences cho + N), `playBingo()`, `cancelPlayback()`. Lazy `<audio>` cache, cancel-then-play, token-based cancel ensures stale promises can't resume after a new event. Reads active voice from `settings.voice`; URLs go through `import { base } from "$app/paths"` for basePath safety. |
| `src/lib/audio-manifest.js` | Re-exports `static/audio/manifest.json` as `VOICES` (array) + `VOICE_IDS` (Set) + `DEFAULT_VOICE`. Manifest is generated by `scripts/generate-audio.py`. |
| `src/lib/settings-store.svelte.js` | Reactive global UI settings via Svelte 5 runes. Stores 9 keys: `theme` (enum: "auto" / "light" / "dark"), `mode` (enum: "player" / "master" / "both"; replaces legacy `masterMode`), `autoCallEnabled` (bool), `autoCallSpeed` (1–10), `emptyCellColor` (hex), `voiceEnabledMaster` (bool), `voiceEnabledPlayer` (bool), `voiceWaitingNumber` (bool), `voice` (string id matching audio manifest). Persisted to localStorage `loto_settings`. Pushes values to CSS vars and `<html class="dark">` on `:root`. Per-key validators preserve old data. One-shot migration: `masterMode: true` → `mode: "both"`. |

### Styling
| File | Purpose |
|------|---------|
| `src/app.css` | Root styles: Tailwind @import, CSS variables (light/dark), Tailwind v4 `@variant dark (.dark *)` for explicit dark-mode class selector, `.loto-grid` & `.master-grid` (9-col), animations (fade-in, pop-in, bounce-slow, spin-slow, toast, cross-draw 200 ms cell mark, confetti-fall 1.6 s), `.cell-crossed` diagonal, `.confetti` falling-emoji helper. |

### Tests
| File | Purpose |
|------|---------|
| `src/lib/game-logic.test.js` | 33 unit tests: generateGrid shape (9×9, 5 per row/col, no duplicates), column ranges & ascending sort, no-3-consecutive soft constraint, row completion, waiting number detection, persistence (saveGrid/loadGrid/saveCrossedState/loadCrossedState/saveManualUnticks/loadManualUnticks with validators). |
| `src/lib/settings-store.test.js` | 31 unit tests: defaults (incl. voice keys), loadSettings (restore 8 keys, apply CSS vars, toggle dark class, handle empty/corrupt), saveSettings, resetSettings, theme toggle (auto → OS pref detection), master mode, auto-call + speed, color validation, voice round-trip + invalid-id fallback. |
| `src/lib/vietnamese-number.test.js` | 40 unit tests: ones (0–9), teens (10–19 incl. mười lăm), 20–90 incl. mốt and lăm exceptions, out-of-range fall-through. |
| `src/lib/master-store.test.js` | 9 unit tests for shared master state: starts empty, `startNewGame` fills 1..90 unique, `drawNext` appends called and shifts remaining (returns null when exhausted), `resetMaster` clears, save+load round-trip, rejects corrupt JSON / out-of-range / oversize payloads. |
| `src/lib/player-auto-cross.test.js` | 10 unit tests for `applyMasterCalls`: empty called no-op, cursor-at-length no-op, mode=player advances cursor without flipping, mode=both crosses uncrossed cell, full-history replay when cursor=0, manualUnticks skipped, grid=null no-ops with cursor advance, returns same crossed reference on no-flip. |

### Configuration & PWA
| File | Purpose |
|------|---------|
| `svelte.config.js` | adapter-static (HTML export), dual basePath via BUILD_PROFILE env, SvelteKit PWA plugin config. |
| `vite.config.js` | Tailwind + SvelteKit + PWA plugins. codeserver HMR config (port, allowedHosts, hmr). |
| `package.json` | SvelteKit 2, Svelte 5 (runes), Tailwind 4, Vite, @vite-pwa/sveltekit. Scripts: dev, dev:codeserver, build, build:gh, lint, test, test:watch. |
| `.github/workflows/ci.yml` | Repo-root pipeline for push/PR. `test` (`pnpm test`) gates a two-profile `build` matrix; the artifacts feed `deploy-pages` (canonical, `https://tiennm99.github.io/loto/`), `deploy-firebase`, `preview-firebase`, and `android-debug`. |
| `.github/actions/setup-web/` | Composite action: pnpm + Node 24 + `pnpm install --frozen-lockfile` in `web/`. Shared by `ci.yml` and `android-release.yml`. |
| `eslint.config.mjs` | ESLint 9 flat config (@eslint/js + eslint-plugin-svelte). Declares Svelte 5 rune globals. |
| `jsconfig.json` | Path alias `$lib`, no checkJs. |
| `.gitignore` | Excludes node_modules, build, .env.local, etc. |
| `.env.example` | codeserver profile vars (CODESERVER_HOST, CODESERVER_PORT). |
| `static/manifest.webmanifest` | PWA manifest: name, short_name, icons (192/512px), theme colors, display: standalone, scope, start_url. |
| `static/icons/{192,512}.png` | PWA icons (auto-generated by @vite-pwa/sveltekit from source). |
| `static/audio/{voiceId}/*.mp3` | Pre-generated Vietnamese voice clips (1–90 + cho + kinh per voice). Generated by `scripts/generate-audio.py`. SW caches at runtime (CacheFirst). |
| `static/audio/manifest.json` | Voice list `{ id, edgeName, label, gender }[]`. |
| `scripts/generate-audio.py` | One-shot Python script: generates 92 clips per voice, writes manifest. |
| Service Worker (auto) | @vite-pwa/sveltekit generates `/pwa-manifest.json`, `/sw.js`. App shell precache ~353 KB. Auto-update mode with reload toast. |

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
| `loto_settings` | Global UI settings: `{ theme, mode, autoCallEnabled, autoCallSpeed, emptyCellColor, voiceEnabledMaster, voiceEnabledPlayer, voiceWaitingNumber, voice }`. |

Old `masterMode` bool is migrated on load: `masterMode: true` → `mode: "both"`. `loto_master_card_*` keys no longer written but old saved data untouched.

## Component Hierarchy

```
RootLayout
└── HomePage (/)   ← single page; any other URL redirects to /
    ├── [if settings.mode !== "master"]
    │   └── PlayerBoard (storagePrefix="loto")
    ├── [if settings.mode !== "player"]
    │   └── MasterPanel (mutates master-store; player auto-cross derives from it)
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

Last reviewed: 2026-05-09
Last synced: 2026-05-09 (deploy target switched from Cloudflare Pages to GitHub Pages; CSP-hash machinery and `_headers` / `_redirects` removed)
