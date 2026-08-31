# Code Review — `web/` (SvelteKit lô tô app)

Date: 2026-08-31 · Reviewer: code-reviewer · Mode: report-only (no source edits)

## Scope

- Files: all of `web/src` (23 files, ~4.7k LOC incl. tests), `package.json`,
  `svelte.config.js`, `vite.config.js`, `firebase.json`, `eslint.config.mjs`,
  `jsconfig.json`, `static/manifest.webmanifest`, `scripts/generate-audio.py`,
  `.github/workflows/ci.yml`, `README.md`, `docs/`.
- Verification run locally: `npm ci`, `npx vitest run` (121 pass / 8 files),
  `npx eslint .` (0 problems), `BUILD_PROFILE=gh npx vite build` + inspection of
  `build/sw.js`, `build/index.html`, `.svelte-kit/output/prerendered/pages/index.html`.

## Overall Assessment

Game logic is solid and matches `docs/project-overview-pdr.md` (5-per-row and
5-per-column invariants, ascending column order, unbiased Fisher–Yates, soft
"no 3 consecutive" via rejection sampling). Storage parsing has size caps and
per-key validators; there are no XSS sinks (`@html`/`innerHTML` absent), no
client-side Firebase SDK, no secrets, and CI uses `pull_request` (not
`pull_request_target`) with a SHA-pinned deploy action.

The real risk sits at the edges the unit tests don't reach: the PWA layer is
built but never wired up, and the multi-tab coordinator can roll back a live
round. Component/effect code (PlayerBoard, MasterPanel) has zero test coverage
and carries several timing/UX defects.

---

## Critical

### C1. Service worker is generated but never registered — offline mode does not exist
`web/vite.config.js:39-81` (SvelteKitPWA config), `web/src/routes/+layout.svelte:19-23`

Empirically verified on a real build (`BUILD_PROFILE=gh npx vite build`):

- `build/sw.js`, `build/workbox-*.js`, `build/registerSW.js` are emitted.
- `build/index.html` contains **zero** occurrences of `registerSW`, `sw.js`, or
  `serviceWorker` (`grep -oi 'sw\.js\|registerSW\|serviceWorker' build/index.html`
  → empty). Same for `.svelte-kit/output/prerendered/pages/index.html`.
- The only `serviceWorker` reference in `_app/immutable/**` is SvelteKit's own
  `updated.check()` helper, not vite-pwa's registration.

Cause: with `adapter-static`, page HTML is produced during `adapt()`, which runs
after the PWA plugin's `closeBundle`; `@vite-pwa/sveltekit` therefore never
injects the tag, and nothing in `src/` imports `virtual:pwa-register`.

Impact: no precache, no runtime audio cache, no offline start. `README.md:27`
("works offline after first load"), `docs/development-roadmap.md:21` ("Works
offline at fairground (no signal venues)") and `docs/system-architecture.md:173-178`
are all false for both the GitHub Pages and Firebase deployments. This is the
app's stated core venue requirement and it fails silently — no test or CI step
catches it. (Android is unaffected: the APK mounts the assets locally.)

Fix (in `+layout.svelte`, alongside the existing `onMount`):

```js
import { base } from "$app/paths";
onMount(() => {
  if (import.meta.env.PROD && "serviceWorker" in navigator) {
    navigator.serviceWorker.register(`${base}/sw.js`, { scope: `${base}/` });
  }
});
```

or use the plugin's own entry point (`const { registerSW } = await import("virtual:pwa-register"); registerSW({ immediate: true })`).
Then re-verify with the greps above, and add a build-time assertion (a tiny node
script in CI: fail if `build/index.html` lacks a SW registration) so this cannot
silently regress again.

---

## High

### H1. Audio precache URLs ignore the SvelteKit base path — SW install will 404 on GitHub Pages
`web/vite.config.js:21-26`, base resolution at `web/svelte.config.js:17-25`

`additionalManifestEntries` are passed verbatim to workbox-build; the plugin only
rewrites entries it generates from globs. Verified in the `gh` build:

```
$ grep -o '"[^"]*audio[^"]*mp3"' build/sw.js | head -1
"/audio/hoai-my/1.mp3"          # absolute, origin-rooted
$ grep -oE '"[^"]*\.(js|css)"' build/sw.js | head -2
"registerSW.js" "_app/immutable/nodes/2.QP53jZoe.js"   # relative → correct
$ grep -c '/loto/' build/sw.js
0
```

On `tiennm99.github.io/loto/` those 92 entries resolve to
`https://tiennm99.github.io/audio/hoai-my/N.mp3` → 404. Workbox rejects the
`install` event when any precache request fails, so once C1 is fixed the SW will
install-fail on every load of the Pages deployment and never activate — offline
stays broken there while appearing fixed on Firebase (base `""`).

Fix: share one base resolver between the two configs and prefix the entries.

```js
// web/base-path.js  (imported by svelte.config.js and vite.config.js)
export function resolveBase() { /* moved verbatim from svelte.config.js */ }
```
```js
const base = resolveBase();
const defaultVoicePrecacheEntries = clipNames.map((n) => ({
  url: `${base}/audio/${defaultVoiceId}/${n}.mp3`,
  revision: `audio-v1-${defaultVoiceId}-${n}`,
}));
```

Verify: `BUILD_PROFILE=gh npm run build:gh && grep -o '"[^"]*audio[^"]*mp3"' build/sw.js | head -1`
must print `"/loto/audio/hoai-my/1.mp3"`.

### H2. Reclaiming a frozen tab overwrites the live round with stale state (data loss)
`web/src/lib/active-tab.svelte.js:58-61`, `web/src/lib/MasterPanel.svelte:69-74`,
`web/src/routes/+layout.svelte:26-44`

Sequence: tab A hosts a round → user opens tab B → B broadcasts `claim` → A sets
`activeTab.inactive = true` → the layout `{#if}` unmounts all children of A, but
the `masterState` module singleton in A keeps its snapshot. B draws 20 more
numbers and persists them. User returns to A and taps "Nhấn để tiếp tục tại đây"
→ `claimActiveTab()` only flips the flag → children remount → `MasterPanel`'s
save effect (`MasterPanel.svelte:69-74`) runs unconditionally on mount and calls
`saveMaster()` with A's stale `called`/`remaining` → `loto_master` in
localStorage is rolled back and B's 20 draws are gone.

There is no re-hydration on unfreeze and no `storage`-event listener anywhere.

Fix: re-hydrate before handing control back.

```js
// active-tab.svelte.js — keep the store dumb, or do it in +layout.svelte
export function claimActiveTab() {
  loadSettings();
  loadMaster();          // re-read authoritative state written by the peer
  activeTab.inactive = false;
  if (bc) bc.postMessage({ type: "claim", id: TAB_ID });
}
```

Defence in depth: gate the `saveMaster()` effect on a "hydrated" flag so a mount
never writes before a load has happened in this tab's current active session.

---

## Medium

### M1. Second "Chờ" toast within 5 s is invisible but still swallows taps
`web/src/lib/PlayerBoard.svelte:117-123` and `:493-512`, `web/src/app.css:194-207`

`showToast()` sets `toast = null` then `toast = msg` in the same synchronous
tick, so the `{#if toast}` block never tears down — the DOM node is reused and
the `animate-toast` 5 s animation is **not** restarted. Consequences when a
second row enters "Chờ" 3 s after the first:

1. The chip shows the new text but keeps the old animation's progress; with
   `forwards` fill it reaches `opacity: 0` at t=5 s while the JS timer
   (`:120`) keeps the node mounted until t=8 s.
2. That node is `pointer-events-auto` and sits centred over the card, so for
   ~3 s an invisible ~150×60 px button eats taps on the middle cells — the user
   taps a number and nothing happens.

Fix: force a remount and let one owner control the lifetime.

```svelte
{#key toastId}
  <div role="status" aria-live="polite" …>…</div>
{/key}
```
with `let toastId = $state(0)` incremented in `showToast`, or drop the CSS
`forwards` fade and remove the node from JS only.

### M2. "Kinh!" is cut off by "Chờ" in the same effect pass
`web/src/lib/PlayerBoard.svelte:191` (pass 1 `playBingo()`), `:203` (pass 2
`playWaiting()`), `web/src/lib/voice.js:107,123` (every entry point starts with
`cancelPlayback()`)

A single draw that completes one row and puts another row one number away runs
both passes in one effect run: `playBingo()` starts, then `playWaiting()`
cancels it a fraction of a millisecond later. The win announcement — the whole
point of the feature — is never heard in exactly the most exciting case.

Fix: give the two events a priority or a queue. Cheapest correct version: in
pass 2, skip `playWaiting()` when pass 1 fired this run
(`let announcedBingo = false` local flag), or add a small FIFO in `voice.js`
(`enqueue(clip)`) instead of unconditional `cancelPlayback()`.

### M3. Theme flash on every cold load
`web/src/routes/+layout.svelte:19-23`, `web/src/app.html` (no inline script)

`loadSettings()` runs in `onMount`, so the prerendered shell always paints with
`:root` light tokens (`app.css:14-25`) before `documentElement.classList.toggle("dark")`
is applied. Dark-theme users get a full-page white flash on every launch,
including the Android WebView.

Fix: add a blocking inline script in `app.html` head that reads
`localStorage.getItem("loto_settings")`, applies the `dark` class and
`--empty-cell-bg`, before the first paint. Keep `loadSettings()` as the
authoritative pass.

### M4. Both modal dialogs have no focus management
`web/src/lib/PlayerBoard.svelte:566-618` (bingo), `web/src/lib/SettingsButton.svelte:175-190` (settings)

Both use `role="dialog" aria-modal="true"` but never move focus into the dialog,
never trap Tab, never restore focus on close, and leave background content
tabbable and scrollable. Screen-reader users get no announcement (focus stays on
the trigger, outside the dialog), and keyboard users tab straight into the
board behind the overlay. `aria-modal` claims a containment that does not exist.

Fix: on open, focus the dialog container (`tabindex="-1"` + `element.focus()`);
cycle Tab between the first/last focusable descendants; restore
`document.activeElement` on close; add `overflow: hidden` on `<body>` while open
(or `inert` on the page wrapper). Escape handling already exists.

### M5. Two tabs opened at the same time freeze each other
`web/src/lib/active-tab.svelte.js:35-51`

Both tabs `postMessage({type:"claim"})` on mount; each receives the other's claim
and sets `inactive = true`. Result: both windows show the "đang chạy ở tab khác"
banner and the app is unusable until the user manually clicks one. Not
hypothetical — restoring a browser session with two pinned tabs does exactly this.

Fix: include a monotonic claim timestamp and ignore a peer claim that is older
than this tab's own claim (`if (e.data.ts < myClaimTs) return;`), tie-break on id.

### M6. Auto-call voice is silent on iOS Safari (and 92 `Audio` objects are retained)
`web/src/lib/voice.js:27-35`, driven from `web/src/lib/MasterPanel.svelte:97-103`

`getAudio()` constructs a **new** `Audio` per clip URL, lazily, at play time.
Under auto-call, every `play()` happens inside a `setInterval` callback, i.e.
outside a user gesture, on an element that was never unlocked. iOS Safari
rejects it; `playClip` swallows the rejection via `a.play().catch(done)`
(`voice.js:93`) so the host gets silence with no diagnostic. Secondary cost: up
to 92 `preload="auto"` elements per voice held in the `cache` Map.

Fix: use a single reusable `HTMLAudioElement` whose `src` is swapped per clip and
which is `play()`/`pause()`-unlocked during the first user gesture (the
"Bắt đầu"/"Xổ số" click). Same for `clearAudioCache`. Keep the token/cancel
bookkeeping as-is.

### M7. Lint and type checking never run in CI; JSDoc types are decorative
`.github/workflows/ci.yml:42-50`, `web/jsconfig.json:1-6`, `web/package.json:11-16`

The `test` job runs `npm test` only. `npm run lint` exists but is not wired into
any job (the Android job does run `:app:lint`). `jsconfig.json` sets
`"checkJs": false` and there is no `svelte-check` dependency or script, so none
of the extensive `@type`/`@param` annotations are ever verified — they can drift
arbitrarily and CI stays green.

Fix: add `- run: npm run lint` to the `test` job, add
`svelte-check --tsconfig ./jsconfig.json` as a `check` script (with
`"checkJs": true`), and run it in the same job. Expect an initial batch of
findings; fix or explicitly ignore them once.

### M8. Riskiest code has no tests
`web/src/lib/*.test.js` (8 files, all module-level)

Coverage is good for pure/logic modules but there is not a single component or
effect test. Every defect above (C1, H2, M1, M2, M5) lives in component effects
or build config — exactly the untested surface. The passing 121 tests give a
false sense of safety.

Fix: add `@testing-library/svelte`/`vitest-browser-svelte` cases for at least:
master draw → player auto-cross in `both` mode, "Ván mới" clearing player marks,
consecutive-toast behaviour, and the bingo+waiting audio ordering.

### M9. Docs describe an update UX that does not exist
`web/docs/codebase-summary.md:64`, `web/docs/development-roadmap.md:21`,
`web/vite.config.js:40-42`

Docs say "Auto-update w/ reload toast"; there is no reload-prompt component
anywhere in `src/`. `registerType: "autoUpdate"` makes vite-plugin-pwa emit
`skipWaiting`/`clientsClaim` and reload on controller change — the exact
mid-game swap the comment at `vite.config.js:40-41` says must not happen.
Once C1 is fixed, a deploy during a fairground round can reload the page and
drop in-memory state (`autoRunning`, `showCongrats`, countdown).

Fix: pick one and make code, comment, and docs agree — either implement the
prompt (`registerType: "prompt"` + a small "Có bản mới, tải lại?" banner) or
document that auto-reload is accepted and delete the contradicting comment.

---

## Low

- **L1** `web/static/manifest.webmanifest:2` — `"id": "/loto/"` is hardcoded, but
  the same file is deployed to the Firebase root (base `""`), where that path
  does not exist. Use `"id": "."` to match `start_url`/`scope`, or template the
  file per build profile.
- **L2** `web/src/lib/PlayerBoard.svelte:197-207` — when several rows enter "Chờ"
  on the same draw, each iteration overwrites the previous toast and cancels the
  previous clip; only the last row is ever announced. Aggregate
  (`Chờ 12, 47`) or queue.
- **L3** `web/src/lib/settings-store.svelte.js:204-216` — `resetSettings()` can
  change `settings.voice` without calling `clearAudioCache()` (unlike
  `SettingsButton.svelte:116-120`), leaving the old voice's elements cached.
- **L4** `web/src/lib/game-logic.js:192-217` and
  `web/src/lib/master-store.svelte.js:34-56` — validators check shape and range
  but not game semantics: a grid may contain duplicates/values outside a
  column's range, and `called`/`remaining` may overlap, contain duplicates, or
  not sum to 90. Self-inflicted only (localStorage), but a duplicate in `called`
  silently collapses in the `callOrder` Map (`MasterPanel.svelte:77-79`). Add
  `called.length + remaining.length === 90` and a disjointness check; fall back
  to defaults otherwise.
- **L5** `web/src/routes/+layout.svelte:27-41` — `<button>` wrapping `<p>`
  elements is an invalid content model (phrasing content only). Use a
  `<div role="button" tabindex="0">` or move the text out and keep a labelled
  button.
- **L6** `web/src/lib/AutoCountdown.svelte:41-48` — the rAF loop runs at display
  refresh rate for the whole round even when `reduceMotion` is true and
  `dashOffset` is pinned to 0 (`:63-65`); only the integer second changes. Gate
  the loop on `!reduceMotion` and drive the digit from a 1 Hz timer.
- **L7** `web/vite.config.js:23-25` — `revision: "audio-v1-…"` is a manual
  version string; regenerating clips without editing it serves stale audio from
  precache forever. Derive it from a file hash or the manifest mtime.
- **L8** `web/firebase.json` — no `headers` block. Add explicit `Cache-Control`
  (`no-cache` for `index.html`/`sw.js`, `immutable` for `_app/immutable/**`)
  rather than relying on Hosting defaults, especially once the SW ships.
- **L9** `web/scripts/generate-audio.py:44-49` — `voice_id()` slugs from the last
  hyphen segment only; two voices whose names slug identically would silently
  overwrite each other's folder. Assert uniqueness before writing.
- **L10** `web/src/lib/PlayerBoard.svelte:176` — the modal says "Hàng {1..9}" but
  the card is presented as three labelled 3-row sections
  (`:374-379`); users must count across sections to find the row. Consider
  "Hàng 1 — TN1 (2014-2017)".

---

## Edge Cases Checked and Found Sound

Recorded so they are not re-litigated:

- `pickFilledColsOnce` (`game-logic.js:80-121`) cannot produce `need < 0` (that
  would recurse `combinations` with a negative `k`): more than 5 columns with
  `quota === rowsLeft` would imply `sum(quota) > 5·rowsLeft`, contradicting the
  invariant that each row removes exactly 5. Column quotas hold on the fallback
  path too, so `picked.shift() ?? 0` (`:160`) never silently blanks a cell.
- The `crossed`/`lastHandledIndex` self-writing effect
  (`PlayerBoard.svelte:245-258`) converges: `applyMasterCalls` short-circuits at
  `lastHandledIndex >= called.length` (`player-auto-cross.js:33-35`).
- Store hydration order is safe either way: `MasterPanel` cannot mount before
  `loadSettings()` (default `mode` is `"player"`), and `PlayerBoard`'s replay
  from cursor 0 is idempotent via `findUncrossedCell`.
- Unticking a called number does not get re-crossed on the next effect run
  (cursor already at `called.length`).
- Freezing an inactive tab unmounts the page, which clears the auto-call
  interval, releases the wake lock, and cancels audio.
- Shuffle bias, column ranges, ascending-within-column, and 5/5 quotas match
  `docs/project-overview-pdr.md:16-27` exactly.

## Recommended Actions (priority order)

1. Register the service worker and add a CI assertion that the built HTML
   contains the registration (C1).
2. Prefix `additionalManifestEntries` with the resolved base path; verify
   `build/sw.js` for the `gh` profile (H1).
3. Re-hydrate stores in `claimActiveTab()` and gate `saveMaster()` on hydration (H2).
4. Fix the toast remount/pointer-events defect (M1) and the bingo/waiting audio
   collision (M2).
5. Add the inline pre-paint theme script (M3).
6. Add focus management to both dialogs (M4).
7. Wire `npm run lint` + `svelte-check` into CI and turn on `checkJs` (M7).
8. Resolve the double-claim freeze (M5) and iOS audio unlock (M6).
9. Add component/effect tests covering the paths above (M8); reconcile the
   update-UX docs (M9).

## Metrics

- Unit tests: 121 passed / 8 files (`vitest run`), all module-level; 0 component tests.
- Lint: `eslint .` → 0 problems.
- Type coverage: not enforced — `checkJs: false`, no `svelte-check` in the repo.
- Build: `BUILD_PROFILE=gh vite build` succeeds; precache manifest 115 entries
  (92 of them unreachable on that base path, see H1).

## Unresolved Questions

1. Was the PWA ever verified in a deployed browser (Application → Service Workers)?
   If it worked previously, something in the `@vite-pwa/sveltekit@1` /
   `@sveltejs/kit@2` / `vite@8` upgrade path dropped the injection, and the fix
   should be pinned by a regression check rather than a one-off patch.
2. Multi-tab policy: is the intended contract "last claimant wins and adopts the
   stored round" (implies re-hydrate, H2) or "each tab owns its own session"?
   The fix differs.
3. `manifest.webmanifest` ships identically to two origins with different base
   paths — is the GitHub Pages deployment still a supported target, or is
   Firebase now canonical? That decides L1 and the H1 fix's urgency.
4. Update policy for mid-round deploys (M9): silent auto-reload, or prompt?
