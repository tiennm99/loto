# Security & Reliability Audit — Lô tô

Date: 2026-04-27 | Scope: SvelteKit static export, no backend.
Method: STRIDE + OWASP Top 10, manual review.
Files scanned: 11 (`src/**`), build/deploy configs, `npm audit`.

## Summary
- 0 Critical, 0 High, 3 Medium, 4 Low, 5 Informational.
- No outbound network calls; no `{@html}`, `innerHTML`, `eval`, `fetch`, `XHR`, `WebSocket`, `sendBeacon` anywhere in `src/`. Privacy-clean.
- All user input flows through validators; static export served with redirects only.

## Findings

### Medium

**M1 — No CSP / security headers configured.**
`static/_redirects` only handles SPA fallback; no `_headers` file for Cloudflare Pages, no headers in GitHub Pages action. Missing `Content-Security-Policy`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, `Permissions-Policy`, `X-Frame-Options`. With no CSP, defense-in-depth against future XSS or third-party-origin embedding is absent. Recommend adding `static/_headers` with restrictive CSP (`default-src 'self'; img-src 'self' data:; media-src 'self'; style-src 'self' 'unsafe-inline'; script-src 'self'; connect-src 'self'; frame-ancestors 'none'`). `'unsafe-inline'` for style is needed because Svelte emits inline `style:` attributes. (file: `static/_redirects`)

**M2 — `loadSettings` does not reject `__proto__` / `constructor` keys before destructuring.**
`settings-store.svelte.js:125` does `JSON.parse(raw) ?? {}` then reads named properties. JSON.parse of `{"__proto__":{"polluted":1}}` does NOT pollute `Object.prototype` (modern JS treats `__proto__` from JSON as an own property), so this is theoretically safe today, but the validators read named props directly off `parsed` without `Object.hasOwn` checks. If future code ever uses spread or `Object.assign(target, parsed)` the surface grows. Same pattern in `game-logic.js:171` (`safeParse`). Defense-in-depth: parse with `JSON.parse(raw, (k,v) => k === "__proto__" ? undefined : v)` or check `Object.hasOwn`. (files: `src/lib/settings-store.svelte.js:121-153`, `src/lib/game-logic.js:168-176`)

**M3 — Auto-call interval lower bound borderline; no oversize-payload guard on localStorage reads.**
`autoCallSpeed` validated 1-10s — fine. But `loadGrid` / `loadCrossedState` accept arbitrarily large strings up to localStorage's per-origin quota (~5-10MB) before `JSON.parse`. A poisoned origin (browser extension, shared device) could store a megabyte-scale string; parse stalls UI on mount. Add a length cap (`raw.length > 50_000` reject) before parse. Same applies to `loto_settings` and `loto_master`. (files: `src/lib/game-logic.js:222-228, 246-252`, `src/lib/settings-store.svelte.js:123`, `src/lib/MasterPanel.svelte:51-59`)

### Low

**L1 — `clipUrl` builds path from `settings.voice` only; relies entirely on `validVoiceId` allowlist.**
`voice.js:38-40` interpolates `settings.voice` into a URL. `validVoiceId` (settings-store.svelte.js:67) checks `VOICE_IDS.has(v)` — Set of strings from the bundled manifest. Path-traversal blocked because allowlist values are static slugs. If `settings.voice` is ever set without going through validation (e.g. direct mutation in a future feature), `..%2F` could escape. Add a defensive `encodeURIComponent` on `name` and `settings.voice` in `clipUrl` for resilience. Clip `name` is currently always internal (`String(n)`, `"cho"`, `"kinh"`) — safe. (file: `src/lib/voice.js:38-40`)

**L2 — Audio cache unbounded growth.**
`voice.js:11` keeps every constructed `<audio>` in a `Map` keyed by URL. Per voice = 92 entries (1-90 + cho + kinh). `clearAudioCache` only fires on voice change. With 2 voices today this is fine; if voices grow past ~10 the cache holds ~1000 audio elements. Add LRU cap or drop cache when document hides for >N min. (file: `src/lib/voice.js:11-49`)

**L3 — `crypto.getRandomValues` not used for grid / call shuffles.**
`game-logic.js:31, 104, 107` and `MasterPanel.svelte:35-37` use `Math.random` for shuffle and combination selection. Game has no stake / no fairness guarantees so practical impact is zero, but `Math.random()` is V8's xorshift128+, not cryptographically random; `arr.sort(() => 0.5 - Math.random())` in `randomNumbersInCol` is also a known-biased shuffle (use Fisher-Yates throughout). Information only — replace with Fisher-Yates for bias correctness, not security. (files: `src/lib/game-logic.js:31`, `src/lib/MasterPanel.svelte:35-39`)

**L4 — `npm audit` reports 3 Low vulns (transitive `cookie` <0.7.0 via @sveltejs/kit).**
Static export — runtime never parses cookies, so the OOB-character issue (CWE-74) is not exploitable here. Bump SvelteKit to a release shipping `cookie` ≥0.7.0 next dependency sweep.

### Informational

**I1 — `confirm()` race vs auto-call.**
`MasterPanel.svelte:134` shows native `confirm()` for "Ván mới". `setInterval` callback runs while modal is open (browser-dependent — Chrome pauses, Safari may not). On rapid double-click of "Ván mới" while autoRunning, state could mutate mid-confirmation. Low practical impact. (file: `src/lib/MasterPanel.svelte:99-113, 133-140`)

**I2 — `bus.lastDrawn` never cleared after `bus.lastDrawn` set, only on `resetBus()`.**
If master mode toggles off/on with `mode === "both"` after a draw, `PlayerBoard`'s auto-tick effect re-fires on remount with the stale draw and may double-mark. Effect already has the `!crossed[r][c]` guard so it's idempotent — but a stricter design clears `bus` on mode flip. (file: `src/lib/call-bus.svelte.js`, `src/lib/PlayerBoard.svelte:136-151`)

**I3 — `<a href="https://miti99.com" target="_blank" rel="noopener noreferrer">` is correctly hardened.** Both occurrences (`PlayerBoard.svelte:295`, `PageFooter.svelte`) include `noopener noreferrer`. No reverse-tabnabbing risk.

**I4 — `.env.local` committed to repo via tracked file but only contains non-secret hostnames.**
`.env.local` (in working tree, excluded by `.gitignore` for future) holds `CODESERVER_HOST=codeserver.sg.miti99.com`. Not a credential, but reveals dev host. Confirm gitignore excludes it (it does — `.env*.local`). No remediation needed if not in git history. Verify with `git log --all -- .env.local`.

**I5 — GitHub Pages action interpolates `DEST` into HTML via shell heredoc.**
`.github/workflows/deploy-github-pages.yml:24-50` builds redirect HTML. `DEST` is hardcoded; if it ever becomes a workflow input, the heredoc would shell-interpolate without escaping. Currently safe.

## Trust-boundary verification
- `--empty-cell-bg` injection (M2 from spec): regex `/^#[0-9a-fA-F]{6}$/` is **tight** — anchored, length-fixed, no whitespace. Cannot escape into `;color:red;` or url() injection. PASS.
- localStorage poisoning: type validators present for every key; falls back to default on mismatch. The remaining gap is M2 above (proto keys / payload size). PASS with caveat.
- Voice path traversal: allowlist enforced; clip names internal-only. PASS.
- Resource exhaustion: auto-call clamped 1-10s, `setInterval` cleared on dependency change. Audio cache addressed in L2.
- Privacy: `grep -rn "fetch\|XMLHttpRequest\|sendBeacon\|WebSocket\|EventSource" src/` returns 0 hits. PASS.
- Build/deploy secrets: none in `wrangler.toml`, `svelte.config.js`, `.env.example`. `.env.local` in tree but only non-secret hostnames.

## Unresolved questions
1. Is `'unsafe-inline'` style acceptable for the M1 CSP, or should Svelte be configured to extract `style:` directives into hashed inline blocks?
2. Will the voice list grow beyond 2 entries (affects L2 priority)?
3. Is `.env.local` in any historical git commit, or only working tree? (run `git log --all --oneline -- .env.local`)
4. Cloudflare Pages serves `_headers` — is GitHub Pages mirror also expected to enforce the same headers, or is the GitHub deploy redirect-only (so headers don't matter for that origin)?
