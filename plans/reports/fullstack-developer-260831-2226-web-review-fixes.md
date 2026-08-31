# Fix Report — code review findings, `web/` (SvelteKit lô tô app)

Date: 2026-08-31 · Source review: `plans/reports/code-reviewer-260831-2213-web-review.md`

## Summary

Fixed C1, H1, H2, and mediums M1/M2/M3/M4/M6/M7 (partial — see checkJs below).
Skipped M5, M9, and the rest of Low as out of scope (product decisions or not
requested). 133/133 tests pass (121 original + 12 new), lint clean, svelte-check
clean, both `build`/`build:gh` profiles verified against real output.

## C1 — Service worker never registered

**Fix**: `src/routes/+layout.svelte` — added `registerServiceWorker()`, called
from the existing `onMount`. Guarded on `import.meta.env.PROD` and
`"serviceWorker" in navigator`; dynamically imports `virtual:pwa-register` (so
the module is never evaluated during SSR/prerender) and calls
`registerSW({ immediate: true })`. This reuses `@vite-pwa/sveltekit`'s own
base/scope resolution (`__SW__`/`__SCOPE__` placeholders), so it stays correct
for both the Firebase (`base ""`) and GitHub Pages (`base "/loto"`) builds
without duplicating that logic.

**Verified against real builds** (`BUILD_PROFILE=gh npx vite build` and
`npx vite build`): the compiled `_app/immutable/nodes/0.*.js` (the root
layout) contains the call site `import('../chunks/*.js')` →
`registerSW({immediate:!0})`; the resolved chunk constructs
`new Workbox('./sw.js', {scope: './', type: 'classic'})`. `build/index.html`
itself still shows no literal `sw.js`/`registerSW` string — expected, since
registration now lives in a JS chunk, not injected markup; grepping
`index.html` was never going to prove this either way (that's exactly C1's
root cause). Confirmed the actual reachability by inspecting the compiled
chunk content directly instead.

**Regression guard**: added `scripts/verify-pwa-build.mjs` (new
`npm run verify:pwa`, wired into the CI `build` job for both matrix legs)
that fails the build if no client JS chunk under `build/_app` contains both
`"serviceWorker"` and `"sw.js"`, so this can't silently regress again.

## H1 — Audio precache URLs ignored the base path

**Fix**: extracted `resolveBase()` out of `svelte.config.js` into a new
`web/base-path.js`, imported by both `svelte.config.js` and `vite.config.js`.
`vite.config.js`'s `defaultVoicePrecacheEntries` now prefix every URL with the
same resolved base SvelteKit itself uses, so the two configs can never drift
apart again.

**Verified**: `BUILD_PROFILE=gh npx vite build` →
`grep -o '/loto/' build/sw.js | wc -l` = 92 (all default-voice clips),
`grep -o '"/audio' build/sw.js | wc -l` = 0 (no more origin-rooted entries).
Root/Firebase build (`base ""`) still produces `"/audio/..."` entries
correctly (empty base + `/audio/...` = same as before).

**Regression guard**: `scripts/verify-pwa-build.mjs` also asserts every
precached `*.mp3` URL in `build/sw.js` starts with `${resolveBase()}/audio/`
for the build's actual `BUILD_PROFILE`.

## H2 — Reclaiming a frozen tab rolled back the live round

**Fix**, two parts:
1. `src/routes/+layout.svelte` — the "Nhấn để tiếp tục tại đây" button now
   calls a new `reclaimTab()` (`loadSettings(); loadMaster(); claimActiveTab();`)
   instead of `claimActiveTab` directly, so this tab re-reads
   localStorage — including whatever the peer tab persisted while this tab
   was frozen — BEFORE `activeTab.inactive` flips back to `false` and
   children remount.
2. Defense in depth: `master-store.svelte.js` — `masterState` gained a
   `hydrated` boolean, set `true` at the end of `loadMaster()` (via
   `finally`, so it's set on every path: success, corrupt JSON, missing
   data). `MasterPanel.svelte`'s save effect now only calls `saveMaster()`
   when `masterState.hydrated` is true, so a mount can never persist stale
   in-memory state before a load has actually happened in this tab's
   session.

**Tests added** (`master-store.test.js`, new `describe("hydrated flag …")`):
flag starts false and flips true after load (including the "nothing to
load" and "corrupt JSON" paths), and a scenario test simulating the exact
reclaim sequence (draw 2, persist, peer persists a 3rd draw, reload →
in-memory state matches the peer's newer data, not the stale copy).

## M1 — Invisible-but-clickable toast

**Fix**: `PlayerBoard.svelte` — added `toastId` state, incremented in
`showToast()`; wrapped the toast markup in `{#key toastId}` so every
`showToast()` call forces a full DOM remount instead of reusing the node,
which restarts the 5s CSS fade animation and guarantees the
`pointer-events-auto` button is torn down when its own timer expires, even
if a second "Chờ" arrived mid-animation.

## M2 — "Kinh!" cut off by "Chờ" in the same effect pass

**Fix**: `PlayerBoard.svelte`'s bingo/waiting `$effect` — added a local
`announcedBingo` flag set when pass 1 calls `playBingo()`; pass 2 now skips
the `playWaiting()` call (not the toast/state bookkeeping) when
`announcedBingo` is true, so a draw that completes one row and puts another
one number away no longer has the win clip silently cancelled a
millisecond after it starts.

## M3 — Theme flash on cold load

**Fix**: `src/app.html` — added a blocking inline `<script>` in `<head>`,
before `%sveltekit.head%` (so it runs before app.css's injected `<link>`),
that reads `localStorage.getItem("loto_settings")`, applies `.dark` and
`--empty-cell-bg` with the same validation semantics as
`settings-store.svelte.js` (defensive try/catch, hex6 regex), falling back
to light/default on any parse failure. `loadSettings()` in `+layout.svelte`
remains the authoritative pass.

## M4 — No focus management in the two `aria-modal` dialogs

**Fix**: new `src/lib/focus-trap.js` — a Svelte action (`use:focusTrap`)
that focuses the dialog container on mount (`tabindex="-1"` added if
missing), traps Tab/Shift+Tab among focusable descendants, restores focus to
whatever was focused before the dialog opened, and locks `document.body`
scroll for as long as any trap is active (reference-counted so the bingo
modal and settings sheet can't fight over the lock if both are somehow
open). Wired into `PlayerBoard.svelte`'s bingo dialog and
`SettingsButton.svelte`'s settings dialog. Existing window-level Escape
handling in both components is untouched.

**Tests added**: `focus-trap.test.js` (6 cases) — focuses container on
mount, Tab wraps last→first, Shift+Tab wraps first→last, body scroll
lock/unlock, focus restoration on destroy, and reference-counted lock across
two stacked traps.

## M6 — iOS auto-call voice silent (+ 92 retained `Audio` elements)

**Fix**: rewrote `voice.js`'s element management — one reusable
`HTMLAudioElement` (`.src` swapped per clip) instead of a `Map` growing to
one element per clip URL. New `unlockAudio()` export, called synchronously
from `MasterPanel.svelte`'s `toggleAuto()` ("Bắt đầu") and
`handleDrawNext()` ("Xổ số") — both real click handlers — primes the shared
element inside a genuine user gesture so it stays unlocked for later
`play()` calls from `setInterval` (auto-call), which iOS Safari does not
treat as a gesture. `clearAudioCache()` kept as an export (now just
`cancelPlayback()`) so callers that change `settings.voice` don't need to
change.

**Tests**: rewrote `voice.test.js` to match the new architecture (the old
tests asserted "one Audio instance per URL", now structurally false by
design) using `vi.resetModules()` + dynamic re-import per test for clean
module-singleton isolation, preserving every original assertion's intent,
plus two new cases for `unlockAudio()` (idempotent priming, later playback
reuses the primed element).

## M7 — Lint/typecheck never ran in CI

**Fix**: `.github/workflows/ci.yml` `test` job now runs `npm run lint` and
`npm run check` before `npm test`. Added `svelte-check` as a devDependency
and a `check` script (`svelte-check --tsconfig ./jsconfig.json`).

**checkJs — left off, per task's explicit escape hatch.** Tried enabling it:
`checkJs: true` produced 58 errors across 10 files. The large majority
cascade from one pre-existing, unrelated root cause — `DEFAULT_SETTINGS` is
`Object.freeze`d, so TS infers each field's literal type (`"#7030A0"`,
`false`, `5`, …) instead of its declared JSDoc type, and every subsequent
`settings.foo = <widened value>` assignment across `settings-store.svelte.js`,
`SettingsButton.svelte`, and their test files fails. The rest are similarly
pre-existing and unrelated to this task's fixes: `PlayerBoard.svelte`'s
nullable-grid call sites, `active-tab.test.js`'s Vitest cache-busting
`?query` dynamic imports, `overlay-history.test.js` implicit-any locals. None
of these were introduced by my changes — verified by re-running
`svelte-check` with `checkJs` back to `false`: 0 errors, 0 warnings. Fixing
the settings-store typing alone would mean re-typing every settings field
and touching ~5 files well outside this task's file list; that's the "large
refactor" the task told me to leave for later rather than attempt here.
`svelte-check` still runs in CI (and locally passes clean with `checkJs:
false`, catching real Svelte-specific issues like unused CSS/a11y/reactivity
mistakes independent of TS types) — only the JS-type-checking half of M7 is
deferred. `jsconfig.json` is unchanged (`checkJs: false`).

I did fix the two type errors my own new `focus-trap.js` produced under
`checkJs: true` (an untyped `filter()` losing its `HTMLElement` narrowing) —
cheap, and keeps that file honest even though `checkJs` ends up off.

## Skipped (per task instructions — product decisions / not requested)

- **M5** — two tabs opened simultaneously freeze each other (mutual claim).
  Explicitly excluded ("multi-tab contract" needs a product decision per the
  review's Unresolved Question #2).
- **M9** — docs/comment say auto-reload "must not happen"
  (`vite.config.js:40-41`'s comment), but `registerType: "autoUpdate"`
  auto-reloads on SW update. This was silently inert before C1 (nothing ever
  registered the SW, so it never fired); now that C1 is fixed, a deploy
  during a live round CAN trigger `window.location.reload()` mid-game — the
  exact scenario the comment warns against. Left `registerType: "autoUpdate"`
  unchanged since the review flags the resolution (implement a reload-prompt
  UI vs. accept auto-reload) as needing a product decision (Unresolved
  Question #4). **Flagging this explicitly since C1 makes the risk real
  where it was previously theoretical** — recommend deciding this soon.
- **L1–L10** — not in the requested fix list; untouched.
- **M8** (add broad component/effect test coverage) — not in the requested
  fix list either; added the specific unit tests called out above
  (hydration flag, focus-trap, voice unlock) that directly cover the fixed
  logic, but did not add `@testing-library/svelte`/component-mount
  infrastructure for M1/M2's Svelte-effect-level behavior — that's the
  larger, separately-scoped ask M8 describes, and doing it well means adding
  a new test-rendering dependency and harness, not a few lines.

## Files changed

- `web/src/routes/+layout.svelte` — SW registration, reclaim re-hydration.
- `web/src/lib/master-store.svelte.js` — `hydrated` flag.
- `web/src/lib/MasterPanel.svelte` — hydration-gated save effect, `unlockAudio()` wiring.
- `web/src/lib/PlayerBoard.svelte` — toast remount key, bingo/waiting priority, focus trap wiring.
- `web/src/lib/SettingsButton.svelte` — focus trap wiring.
- `web/src/lib/voice.js` — single shared `<audio>` element + `unlockAudio()`.
- `web/src/app.html` — pre-paint theme script.
- `web/vite.config.js`, `web/svelte.config.js` — shared base resolver.
- `web/base-path.js` (new) — shared `resolveBase()`.
- `web/src/lib/focus-trap.js` (new) — reusable dialog focus-trap action.
- `web/scripts/verify-pwa-build.mjs` (new) — C1/H1 build-time regression guard.
- `web/package.json`, `web/package-lock.json` — `check`/`verify:pwa` scripts, `svelte-check` devDependency.
- `.github/workflows/ci.yml` — lint/check/verify:pwa steps.
- Tests: `web/src/lib/master-store.test.js`, `web/src/lib/voice.test.js` (rewritten), `web/src/lib/focus-trap.test.js` (new).

## Verification

- `npx vitest run` → 133/133 pass (9 files; was 121/8).
- `npx eslint .` → 0 problems.
- `npx svelte-check --tsconfig ./jsconfig.json` → 0 errors, 0 warnings (418 files).
- `BUILD_PROFILE=gh npx vite build` then `BUILD_PROFILE=gh node scripts/verify-pwa-build.mjs` → pass (base `/loto`, 92 precached audio entries all prefixed, SW registration reachable in compiled JS).
- `npx vite build` (root/Firebase profile) then `node scripts/verify-pwa-build.mjs` → pass (base `""`).
- Manually inspected the compiled `_app/immutable/nodes/0.*.js` and its dynamically-imported chunk to confirm `registerSW({immediate:true})` → `Workbox('./sw.js', {scope: './'})` is really reachable at runtime, not just present in dead code.

## Unresolved questions

1. M9 is now a live risk, not a theoretical one (see above) — worth deciding
   soon whether to accept mid-round auto-reload or add a "Có bản mới, tải
   lại?" prompt banner.
2. `registerType: "autoUpdate"` was left as-is since changing it is the M9
   product decision; flagging in case the intent was actually to fix C1 *and*
   settle M9 together.

Status: DONE_WITH_CONCERNS
Summary: Fixed C1/H1/H2 and mediums M1/M2/M3/M4/M6/M7(CI half); checkJs left off after confirming the 58 errors are a pre-existing, unrelated settings-store typing refactor outside this task's scope; M5/M9/Lows skipped as instructed.
Concerns/Blockers: M9 (mid-round auto-reload) is now a real risk now that C1 registers the SW — was previously inert. Recommend a follow-up product decision soon.
