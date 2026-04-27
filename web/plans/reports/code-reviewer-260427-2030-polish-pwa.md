# Code review — UI polish v2 + PWA (260427-2030)

**Scope:** uncommitted changes (9 modified, 5 new). LOC delta ~280 (excluding lockfile). All 115 tests pass; build clean.
**Verdict:** ship-ready with minor follow-ups. No P0 blockers. A couple of P1s worth fixing pre-deploy.

## P0 (blocking)
None.

## P1 (fix before deploy)

**1. Audio cache `maxEntries: 200` — no headroom for a 3rd voice.**
`vite.config.js:38`. Today: 2 voices × 92 = 184 clips. Cap = 200, only 16-clip headroom. If a 3rd voice ships (`hoai-my`/`nam-minh` already exist; future voice would push to 276) Workbox LRU will evict mid-game. Bump to 400 or `2 * voices.length * 92`. Zero runtime cost — `maxEntries` only affects eviction policy, not memory.

**2. Plan vs. impl divergence — audio strategy silently changed.**
Plan (phase-03 §workbox) called for `precache` of all 184 mp3s. Implementation chose runtime `CacheFirst` (`vite.config.js:31-44`). This is actually a better call (saves ~1.87 MB from initial precache, app-shell-only is 213 entries / ~353 KB now). But: **first offline play of any uncached voice will fail silently** — fairground use case (no signal at venue) won't work for clips the user hasn't played yet. Plan's "killer feature: works offline" assumes precache. Two options: (a) document the "warm-up: play once on Wi-Fi" requirement in UI; (b) precache the default voice only (~92 × 10 KB = 920 KB) and runtime-cache alternates. (b) is the right answer.

**3. CSP — no `connect-src` allowance for SW update fetches.**
`static/_headers:2`. `connect-src 'self'` is present — fine. But Workbox `autoUpdate` fetches `/sw.js` periodically; with the new `Cache-Control: no-cache` header on `/sw.js` (good!) the SW will revalidate. No CSP gap detected. `worker-src 'self'` and `manifest-src 'self'` are correctly added. **No action required** — flagging because the question was asked.

**4. `autoUpdate` mid-game stale-content trap is real.**
`vite.config.js:18`. `registerType: "autoUpdate"` + Workbox default `skipWaiting: false` means the new SW activates only after **all tabs close**. For a fairground host running the app for an hour straight, this is fine. BUT if anyone bumps `skipWaiting` later, mid-game asset reload could swap the JS bundle while audio is buffering → broken playback. Suggest a code comment in `vite.config.js` documenting "do NOT add skipWaiting without a reload-prompt UI" so future-you doesn't silently break it.

## P2 (cleanup, non-blocking)

**5. DRY gap — `MasterEmptyState` and `PlayerBoard` empty branch are near-duplicates.**
`MasterEmptyState.svelte:6-43` vs `PlayerBoard.svelte:343-371`. Both: ghost grid (99 vs 27 cells) → italic prompt → emoji subline. Differences: grid size, role pill (master only), accent color (orange vs rose). Worth extracting shared `<EmptyStateHero {gridCells} {gridCols} {rolePill} {prompt} {subline} {accent} />` if a 3rd empty state ever lands. Today: not worth the indirection. **Skip.**

**6. Maskable icon — content ratio 70% is below Android's recommended 80%.**
`source.svg:14` shows 400/512 = 78% inner content area in the standard icon, but `phase-03` plan §icons used `-resize 70%x70%` for maskable padding (= 70% safe zone). Android adaptive icons crop to a circle of diameter ~80% of the container. 70% is fine — slightly conservative — but the wordmark "Lô tô" with stroke at `font-size:240` may still graze edges after the 70% scale. Risk: text clipping on aggressive shape masks (squircle/teardrop). Quick fix: re-export maskable at 65%. Verify in Chrome DevTools → Application → Manifest → "Show maskable preview".

**7. iOS Safari — `apple-touch-icon` only ships 192px.**
`app.html:11`. Apple recommends 180×180 specifically; 192×192 works (Safari scales) but a dedicated 180px is the convention. Also missing: `apple-touch-startup-image` for splash. Low priority — standalone launch will still work, splash is a minor polish.

**8. `MasterEmptyState.svelte:13` unused destructured var pattern.**
`{#each Array(99) as _, i (i)}` — works but `Array(99)` creates a sparse array; some bundler/linter combos warn on iterating sparse arrays. Use `{#each {length: 99}, i}` if Svelte 5 supports it, else `Array.from({length:99})`. Cosmetic; current code works.

**9. Font swap — confirmed safe.**
`@fontsource/roboto-condensed/700.css` ships `font-display: swap` by default per Fontsource docs. FOUT risk: minimal (28 KB woff2, served from same origin under `font-src 'self' data:`). Fallback stack at `app.css:140-146` (Arial Narrow, Avenir Next Condensed, Liberation Sans Narrow, system-ui) ensures Vietnamese diacritics render before Roboto Condensed loads. **No action.**

## Plan compliance check

| Plan item | Shipped? |
|---|---|
| Phase 1: Vietnamese font + master empty state | ✅ (font weight 900→700 documented in CSS comment) |
| Phase 2: mode picker icons + color picker card + brand subline | ✅ |
| Phase 3: PWA + offline | ⚠️ audio precache → runtime-only (P1 §2 above) |
| Section-label Chờ ring (plan §decisions, marked optional) | ❌ skipped (was flagged optional, fine) |
| `npm test` still passing | ✅ 115/115 |
| Lighthouse PWA audit run | ❓ not in evidence — recommend before deploy |

## Positive observations

- `MasterPanel.svelte:135` correctly cancels playback on unmount — no audio leak when switching modes mid-clip.
- `static/_headers:9-13` adds `no-cache` on both `/sw.js` AND `/manifest.webmanifest` — Cloudflare-aware, correct.
- `manifest.webmanifest` uses relative `start_url: "."` and `scope: "."` — survives the `BUILD_PROFILE=gh` `/loto/` base path automatically. Good call vs. hardcoding `/`.
- Dual `theme-color` meta with media queries (`app.html:9-10`) — proper light/dark splash on iOS 15+.
- Scope freeze on `BOARD` + `BOARD_FLAT` (`MasterPanel.svelte:77-78`) — correct from prior review.

## Unresolved questions

1. Was the audio strategy change (precache → runtime CacheFirst) intentional, or did it slip during impl? If intentional, plan should be updated retroactively. If not, P1 §2 should be applied.
2. Has the GitHub Pages mirror (`BUILD_PROFILE=gh`) been smoke-tested? `@vite-pwa/sveltekit` with `paths.base = "/loto"` usually rewrites SW scope correctly, but the hand-written manifest icon paths (`icons/icon-192.png` relative) need verification under `/loto/`.
3. Lighthouse PWA score — was 100/100 confirmed?
