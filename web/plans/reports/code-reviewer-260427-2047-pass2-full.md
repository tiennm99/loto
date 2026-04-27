# Code Review — Pass 2 (full project, post-PWA)

**Scope:** `src/**`, `vite.config.js`, `static/_headers`, `static/manifest.webmanifest`, `package.json`.
**Baseline:** prior pass at `f28279b`. Reviewed commits: `ad6291e`, `f7db20c`, `d94294d`.
**Verdict:** clean. Prior P0/P1 findings landed correctly. No new P0. Two P1 around PWA/CSP. A few P2 nits.

---

## Prior fixes — landing check

- Auto-tick re-mark guard: `PlayerBoard.svelte:45,157-171` — `lastHandledDrawAt` compared against `bus.lastDrawn.at`. Manual untick / clear / regen no longer re-fires (re-runs read same `at`, early return). Correct.
- Toast positioning: `PlayerBoard.svelte:329-340` — anchored to grid container with `-top-3 sm:-top-4`, `pointer-events-none` wrapper, button-only `pointer-events-auto`. Correct.
- Modal Escape: window-level `keydown` in `PlayerBoard.svelte:142-150` and `SettingsButton.svelte:113-121`. Correct.
- Master empty state: extracted `MasterEmptyState.svelte`, used at `MasterPanel.svelte:348`. Correct.
- Biased shuffle replaced by Fisher-Yates: `game-logic.js:32-35`, `MasterPanel.svelte:35-38`. Correct.
- Reduced-motion gates: `MasterPanel.svelte:145-153` (scroll), `PlayerBoard.svelte:18-23` (vibrate), `app.css:219-232` (animations). Correct.
- Storage payload caps + `__proto__`/`constructor` reviver: `game-logic.js:166-189`, `MasterPanel.svelte:51-75`, `settings-store.svelte.js:127-132`. Correct.
- `encodeURIComponent` on voice URL: `voice.js:41`. Correct.
- Security headers: `static/_headers` present with strict CSP, COOP-equivalent (`frame-ancestors 'none'`), nosniff, etc.

---

## P1 (action recommended pre-merge)

**1. CSP ↔ inline SW registration race / failure mode** — `static/_headers:2`
CSP has `script-src 'self'` (no `'unsafe-inline'`, no nonce). `@vite-pwa/sveltekit` with `registerType: "autoUpdate"` injects an inline `<script>` registering `/sw.js` into the prerendered `index.html`. On a strict CSP host (Cloudflare Pages honors `_headers`), that inline registration block will be blocked → no PWA install. Two fixes: (a) switch to virtual `import { registerSW } from 'virtual:pwa-register'` from a real module, OR (b) configure plugin `injectRegister: 'script-defer'` with a hashed/external file. Verify post-build that `build/index.html` does NOT contain inline registration; if it does, this is silently broken in production.

**2. `static/_headers` does not cover `/sw.js` MIME** — `static/_headers:9-10`
Only `Cache-Control: no-cache` is set on `/sw.js`. Cloudflare Pages will infer `application/javascript` from extension, but spec-strict registrars reject if `Content-Type` isn't `text/javascript`/`application/javascript`. Low-risk on CF, but add explicit `Content-Type: application/javascript` to be safe alongside the manifest entry already doing so.

---

## P2 (nits / tech debt)

- `vite.config.js:13` — `defaultVoiceId = audioManifest.voices[0]?.id ?? "hoai-my"`. Hardcoded fallback drifts from `audio-manifest.js:23`. Either move to a shared `scripts/audio-default.js` import or assert at config-eval time. Low likelihood of mismatch but easy to lose on a manifest rewrite.
- `vite.config.js:23` — `revision: 'audio-v1-{voice}-{n}'` is a manual cache-buster. Comment ("Bump the prefix when audio is regenerated") relies on humans. Consider hashing file content (`createHash('sha1', readFileSync(path))`) so audio regen invalidates automatically.
- `vite.config.js:46` — `includeAssets: ["icons/*.png", "audio/**/*.mp3"]` lists ALL voice mp3s, but `globPatterns` (line 48) does NOT include `mp3`. Result: `includeAssets` only copies them into the build (already happens via `static/`); the actual precache list is `globPatterns ∪ additionalManifestEntries`. Default voice is precached via `additionalManifestEntries`; alt voices fall through runtime CacheFirst as documented. Behavior is correct — but `includeAssets` is dead config noise, drop it or document its no-op role.
- `manifest.webmanifest:5-6` — `start_url: "."`, `scope: "."`. Under base path `/loto/`, browsers resolve relative to manifest URL, so this works on GH Pages. But Cloudflare and GH share the same file. If you ever add a non-root deploy without rewriting the manifest, scope drift will silently break PWA scope detection. Consider `%sveltekit.assets%`-templated manifest emitted at build time, or explicit `start_url: "/loto/"` + a CF-only override.
- `MasterPanel.svelte:107-109` — `callOrder` rebuilds the whole `Map` on every state change; fine at 90 entries. No action.
- `MasterPanel.svelte:90` — `heroEl` typed `HTMLDivElement | null` but `bind:this` runs at every render. Using `$state` here is correct in Svelte 5; OK.
- `PlayerBoard.svelte:48-52` — `rowCompleteness` derived; uses `grid.map((_, r) => isRowComplete(grid, crossed, r))`. Reads both reactively — fires on every cell toggle (9 calls). Acceptable.
- `MasterPanel.svelte:166-177` `handleDrawNext` does not check `settings.mode` before `broadcastDraw`; player auto-tick effect already gates on `settings.mode === "both"` (`PlayerBoard.svelte:162`), so harmless. But broadcasting in `master`-only mode is wasted work and pollutes the bus across mode flips. Suggest gating, OR documenting why it's intentional (so resuming from "both" → "master" → "both" mid-game works).
- `call-bus.svelte.js:1-22` — JSDoc accurate. `broadcastDraw` lacks `@returns`, `resetBus` lacks docstring; trivial.
- `game-logic.js:277-288` — `findUncrossedCell` JSDoc accurate. Top-down/left-right scan order matches test expectation.
- Stale comment risk: `MasterPanel.svelte:51-53` says "16 KB has 30× headroom" — true after the cap landed; keep.
- Dead/duplicate `isBrowser` check in `voice.js:54` (`cancelPlayback`) — fine, but `if (!isBrowser()) return;` is unreachable in test path; cosmetic.

---

## Test-coverage gaps

- No test for the `PlayerBoard` auto-tick effect (mode flip, dedup-by-`at`, manual-untick re-mark prevention). The behavior is the highest-risk new code path. Add one component test: drive `bus.lastDrawn`, assert `crossed[r][c]` flips once, untick by hand, broadcast same `at` → does NOT re-mark. Recommended.
- No test for `MasterPanel.handleDrawNext` → `broadcastDraw` linkage; bus contract tested in isolation only.
- No SW/Workbox integration test — out of scope for vitest, but a `npm run build:gh && grep -r 'sw.js' build/index.html` smoke check in CI would catch P1 #1.
- `voice.test.js` does not cover the `cho → number` cancel-mid-chain case (cancel between the two awaited `playClip` calls). The token mismatch path resolves cleanly; one test would lock it in.

---

## Security

- CSP unchanged, still strict. PWA SW served same-origin; runtime caching only matches `/audio/*.mp3` regex, no third-party cacheing. `manifest.webmanifest` is plain JSON, no scripts. Icons are local PNGs. New attack surface: SW lifecycle. `registerType: "autoUpdate"` + comment on line 38-39 ("Do NOT add `skipWaiting`") is correct — stale clients keep working until tab close.
- `npm overrides` for `serialize-javascript@^7.0.5` and `cookie@^0.7.2` are dev-only build-chain transitive vulns. Lockfile is the source of truth — confirm `npm ls serialize-javascript cookie` shows resolved 7.x/0.7.x post-`npm install`.
- No PII, no telemetry, no remote endpoints.

---

## Positive

- `lastHandledDrawAt` design (closed-over plain ref, not `$state`) is exactly right — avoids the auto-tick effect re-triggering itself.
- Two-pass `$effect` on bingo + waiting in `PlayerBoard.svelte:97-131` reads cleanly; comment on "at most one bingo popup per render" matches code.
- JSDoc + JSDoc-via-`/** @type */` casts give meaningful type narrowing without TS toolchain.
- Test file naming and `// @vitest-environment happy-dom` annotation per file is consistent.

---

## Unresolved questions

1. Is the PWA install actually working in production (Cloudflare Pages) under the strict CSP, or has nobody tested install + reload offline? See P1 #1.
2. Is `BUILD_PROFILE=gh` (`/loto/` base) deployed anywhere live? If not, `manifest.webmanifest`'s relative `start_url` is untested at non-root scope.
3. Should `MasterPanel.handleDrawNext` skip `broadcastDraw` when `settings.mode !== "both"`, or is the cross-mode bus intentional for future "Cả hai" toggling mid-game?

**Status:** DONE
**Summary:** Prior P0/P1 fixes landed cleanly. Two P1 items around PWA SW registration vs strict CSP need a build-output check before next deploy. P2s are minor.
