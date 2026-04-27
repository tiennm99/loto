# Security & Reliability Audit — Lô tô (Pass 2)

Date: 2026-04-27 | Scope: SvelteKit static export + new PWA layer (since `f28279b`).
Method: STRIDE + OWASP Top 10, manual.
Files scanned: `src/**`, `static/_headers`, `static/manifest.webmanifest`, `vite.config.js`, `package.json`, `npm audit`.

## Summary
- 0 Critical, 0 High, **2 Medium, 4 Low, 5 Informational**.
- All 4 pass-1 mediums (M1 CSP, M2 proto-pollution, M3 payload cap, L1 encodeURIComponent) **resolved**.
- `npm audit` clean (overrides verified: `cookie@0.7.2`, `serialize-javascript@7.0.5` — no API breakage; both transitive-only at build time).
- No outbound network calls; no `{@html}`, `eval`, `fetch`, `XHR`, `WebSocket` anywhere in `src/`.

## Findings

### Medium

**M1 — CSP gap: SW-update / workbox / manifest fetches not explicitly covered, and `style-src 'unsafe-inline'` likely still required.**
`static/_headers:2`. `connect-src 'self'` covers SW `update()` GETs, OK. But: (a) workbox's generated `sw.js` registers via `navigator.serviceWorker.register('/sw.js')` — covered by `worker-src 'self'`. (b) The plugin emits an inline `<script>` registration block in `app.html` injected at build; check post-build `index.html` for inline `<script>` — if present, current `script-src 'self'` will block it. `script-src` lacks `'unsafe-inline'` AND no nonce/hash declared (header docstring says "nonce for scripts" but no `'nonce-…'` token in the policy string). Verify built output. (c) `'unsafe-inline'` for style: confirmed needed — `style:` directives at `MasterPanel.svelte:309`, `PlayerBoard.svelte:282,356,384-386`, `SettingsButton.svelte:418`, `MasterEmptyState.svelte:18` all compile to inline `style=` attributes. Nonces don't help inline attributes; CSP3 `'unsafe-hashes'` does but isn't widely supported. Keep `'unsafe-inline'` for style. (file: `static/_headers:2`)

**M2 — Service-worker precache integrity not protected by SRI; CacheFirst stores opaque (status 0) responses.**
`vite.config.js:54-67`. `cacheableResponse: { statuses: [0, 200] }` is **required** for CDN-served audio if range requests strip CORS, but it also means a poisoned CDN response is cached as opaque and served forever (CacheFirst). Risk vector: hijacked Cloudflare edge OR rogue CF Pages preview deploy serves a malicious mp3; SW caches and replays for the 30-day TTL. Mitigations available: (a) drop status `0` (audio is same-origin via `${base}/audio/...`, so 200 is sufficient — no opaque needed); (b) bump cache name on each redeploy via revision (already done for precache via `audio-v1-...`, but **not for runtime cache**). Recommend `cacheableResponse: { statuses: [200] }`. Same-origin `<audio>` does not need opaque mode. (file: `vite.config.js:65`)

### Low

**L1 — Manifest `start_url: "."` resolves relative; subpath deploys could mislead PWA install scope.**
`static/manifest.webmanifest:5-6`. Both GH-Pages (`/loto/`) and CF root use `"."` which resolves at the manifest URL. Acceptable, but if `_headers` ever serves `manifest.webmanifest` from a non-root path with redirect, scope can drift. Defense-in-depth: pin `start_url` to absolute path per build profile, or `"./"` (trailing slash) for clarity. Cache-Control `no-cache` (line 13 of `_headers`) is correct. (file: `static/manifest.webmanifest:5`)

**L2 — Audio runtime cache `maxAgeSeconds: 30 days` with no integrity revision.**
`vite.config.js:60-64`. Precache entries get `revision: audio-v1-...`. Runtime cache (alternate voices) has no revision → if a voice clip is regenerated, clients hold the stale 30-day copy until natural eviction. Functional issue (stale audio), low security impact. Bump prefix → also bump runtime `cacheName` in same release to force purge. (file: `vite.config.js:59`)

**L3 — Reactive bus is module-level singleton — fine for same-tab single user, but BroadcastChannel-style leakage if SW ever cross-posts.**
`src/lib/call-bus.svelte.js:10-13`. `bus` lives in JS module memory, scoped per tab/window — no cross-tab leak. SW does not import it (SW runs in separate context). PASS for current arch. Risk would only appear if a future feature uses `BroadcastChannel`/`postMessage` to mirror draws; document that bus is intentionally tab-local. (file: `src/lib/call-bus.svelte.js`)

**L4 — `MasterPanel.loadState` minimal validator accepts any array shape.**
`MasterPanel.svelte:55-75`. Reviver strips `__proto__`/`constructor`, length cap 16 KB, `Array.isArray` on both halves — but elements are not type-checked. Poisoned origin could store `{called: ["💀"], remaining: [{}]}` → `state.called[i]` rendered into DOM at line 288 as `{num}` (Svelte text-interpolation auto-escapes, so no XSS), but `callOrder.get(num)` and number comparisons silently produce NaN/`undefined`. Ugly UI, no security breach. Add `n => typeof n === "number" && n >= 1 && n <= 90` per element if hardening further. (file: `src/lib/MasterPanel.svelte:64-69`)

### Informational

**I1 — `'unsafe-inline'` for style is unavoidable today.** Svelte's `style:` compiles to inline attributes. Hash-based or nonce-based style CSP would require Svelte-side opt-out + extracting all dynamic styles to CSS variables (already done for `--empty-cell-bg`; not done for confetti, master cell bg toggle).

**I2 — `frame-ancestors 'none' + X-Frame-Options: DENY`**: belt-and-braces, OK.

**I3 — `manifest-src 'self'` correctly added.** Required by Chrome since 2020. PASS.

**I4 — Self-hosted font (`@fontsource/roboto-condensed`)** removes Google Fonts CDN dependency → `font-src 'self' data:` is sufficient and tight. PASS.

**I5 — npm overrides verified non-breaking.** `npm ls cookie serialize-javascript` resolves cleanly to `0.7.2` and `7.0.5`; no peer-dep warnings; build artifacts unchanged. Both packages are build-time only (kit dev internals + workbox-build via @rollup/plugin-terser) — runtime never executes them. PASS.

## Trust-boundary verification (re-confirmed)
- localStorage payload caps applied to **all 4 keys**: `loto_settings` (8KB, `settings-store.svelte.js:17,127`), `loto_grid` + `loto_crossed` (32KB, `game-logic.js:169,180`), `loto_master` (16KB, `MasterPanel.svelte:53,58`). PASS.
- `__proto__`/`constructor` reviver applied in **all 3 parse sites**: `settings-store.svelte.js:130`, `game-logic.js:182`, `MasterPanel.svelte:59`. PASS.
- `clipUrl` `encodeURIComponent` belt-and-braces on both `voice` + `name` (`voice.js:41`). PASS.
- Voice allowlist `VOICE_IDS.has(v)` (`settings-store.svelte.js:71`). PASS.
- CSP `frame-ancestors 'none'` blocks clickjacking. PASS.
- SW: `registerType: "autoUpdate"` without `skipWaiting` (`vite.config.js:40`) — explicit comment confirms intent. PASS.
- `noopener noreferrer` on all external `<a target="_blank">`. PASS.

## Pass-1 follow-ups status
| ID | Item | Status |
|----|------|--------|
| M1 (pass1) | CSP added | RESOLVED |
| M2 (pass1) | proto/constructor stripping | RESOLVED (3 sites) |
| M3 (pass1) | localStorage payload caps | RESOLVED (4 keys) |
| L1 (pass1) | encodeURIComponent on clipUrl | RESOLVED |
| L2 (pass1) | Audio cache LRU | DEFERRED — acceptable while voice count ≤ ~5; SW cache now also caps at 400 entries |
| L3 (pass1) | crypto.getRandomValues | DEFERRED — not security-relevant |
| L4 (pass1) | cookie <0.7.0 | RESOLVED via override |

## Unresolved questions
1. Inspect post-build `build/index.html` — does workbox/SvelteKit-PWA inject any inline `<script>` for SW registration? If yes, `script-src 'self'` blocks it (M1c). Run `npm run build && grep -c "<script>" build/index.html`.
2. Is `cacheableResponse.statuses: [0]` actually needed for same-origin `/audio/*.mp3`, or can it be tightened to `[200]` only? (M2)
3. GitHub Pages mirror — does it serve `_headers`? (GH Pages ignores `_headers`; CSP only enforced on CF.) Acceptable since GH is mirror-only?
4. Should runtime `cacheName: "loto-audio"` be versioned (e.g. `loto-audio-v1`) so a future audio regen forces purge? (L2)
