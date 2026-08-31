# Fix Report — M9 (update-prompt UX) + checkJs, `web/` (SvelteKit lô tô app)

Date: 2026-08-31 · Source: `plans/reports/code-reviewer-260831-2213-web-review.md` (M9, M7)
and `plans/reports/fullstack-developer-260831-2226-web-review-fixes.md` (checkJs left off)

## Summary

Both items done in full, no escape hatch needed. `registerType` is now
`"prompt"` with a real reload-prompt banner; `checkJs: true` with all 56
pre-existing errors fixed via type-only changes (no runtime behavior
changed). 138/138 tests pass (133 + 5 new), lint clean, svelte-check clean,
both build profiles + `verify-pwa-build.mjs` pass.

## Item 1 — M9: mid-round auto-reload

**`vite.config.js`**: `registerType: "autoUpdate"` → `"prompt"`. Comment
above it rewritten to describe the new flow instead of just warning against
the old one.

**`src/lib/update-prompt.svelte.js`** (new): small `.svelte.js` store module,
same shape as `active-tab.svelte.js`/`master-store.svelte.js`:
- `updatePrompt = $state({ visible: false })`
- `setUpdateSW(fn)` — stores the function `registerSW()` returns
- `showUpdatePrompt()` — wired to `onNeedRefresh`, flips `visible = true`
- `applyUpdate()` — hides the banner, calls `updateSW(true)` (reload)
- `dismissUpdate()` — hides the banner, does NOT call `updateSW` (old
  version keeps running this visit)
- `_resetUpdatePromptForTest()` — test-only reset, matches
  `_resetOverlayHistoryForTest()`'s convention

**`src/routes/+layout.svelte`**: `registerServiceWorker()` now calls
`registerSW({ onNeedRefresh: showUpdatePrompt })` (was
`registerSW({ immediate: true })`) and stores the returned `updateSW` via
`setUpdateSW()`. Template: banner renders as a sibling of `{@render
children()}` inside the active-tab branch only (hidden while the "other tab
active" full-screen overlay is showing, so the two never fight over
z-index). Markup: `fixed`, bottom-4/corner-anchored, `max-w-xs` on ≥sm,
`z-40` (below the app's `z-50` dialogs, so a modal correctly covers it) —
deliberately NOT full-screen and NOT near the board or MasterPanel's
`heroEl` "Số vừa xổ" element, both of which live in normal document flow
mid-page. Vietnamese copy: "Có bản mới. Tải lại?" + a rose-600 "Tải lại"
button (matches the bingo modal's primary-button styling) + a plain-text
"Để sau" dismiss button. Both light and dark variants via the existing
`bg-white dark:bg-slate-800` / `border-slate-200 dark:border-slate-600`
pattern used elsewhere (`SettingsButton.svelte`'s sticky header/footer).

**Verified against real builds**: compiled `_app/immutable/nodes/0.*.js` +
its `virtual:pwa-register` chunk contain `onNeedRefresh` and no longer
contain `immediate:!0`. `build/sw.js`'s only `self.skipWaiting()` call is
gated behind `self.addEventListener("message", i => i.data?.type ===
"SKIP_WAITING" && self.skipWaiting())` — i.e. the new SW never activates
itself; it only does so on an explicit `postMessage` that workbox-window's
`updateSW(true)` sends, which only fires from the banner's "Tải lại" tap.

**`scripts/verify-pwa-build.mjs`**: unchanged — its C1/H1 assertions only
check for `"serviceWorker"` + `"sw.js"` reachability and the base-prefixed
audio precache list, neither of which depends on `registerType`. Re-ran it
against both profiles; both pass.

**Test added**: `src/lib/update-prompt.test.js` (5 cases) — starts hidden;
`showUpdatePrompt` reveals it; `dismissUpdate` hides without calling
`updateSW`; `applyUpdate` hides and calls `updateSW(true)` exactly once;
`applyUpdate` before `registerSW` resolves is a safe no-op.

## Item 2 — `checkJs: true`

`jsconfig.json`: `checkJs: false` → `true`. Fixed all 56 errors
(`npx svelte-kit sync && npx svelte-check --tsconfig ./jsconfig.json` → the
list matched the prior report's count almost exactly — 56 not 58, small
line-number drift from the Item-1 edits). All fixes are type-only; no
runtime behavior changed anywhere.

**Root cause 1 — `Object.freeze(DEFAULT_SETTINGS)` literal narrowing**
(~25 of the 56 errors, across `settings-store.svelte.js`,
`settings-store.test.js`, `SettingsButton.svelte`, `voice.test.js`): fresh
object-literal fields like `autoCallEnabled: false` were inferred as the
literal type `false`, not `boolean`, and that leaked into
`settings = $state({ ...DEFAULT_SETTINGS })`. **Fix**: added an explicit
`@typedef {Object} Settings` (one property per field, general types) in
`settings-store.svelte.js` and cast the object literal passed to
`Object.freeze()` — `Object.freeze(/** @type {Settings} */ ({ ... }))` —
so every field keeps its widened type instead of narrowing to the default's
literal value. Removed the two now-redundant per-field inline casts
(`theme`, `mode`) since the typedef covers them. Zero other files touched
for this cause — it single-handedly cleared the settings-store,
SettingsButton, and voice/settings test errors.

**Root cause 2 — `voice.js`'s `new Promise` executor**: `playClip()` had no
return-type annotation, so TS couldn't infer `T` for `new Promise<T>` and
fell back to a `resolve` that requires an argument (can't be called as
`resolve()`). **Fix**: added `@returns {Promise<void>}` to `playClip`'s
JSDoc — `void` gets TS's special "callable with zero args" treatment for
`resolve`. No behavior change (the function always resolved with no value).

**Root cause 3 — `PlayerBoard.svelte` nullable-grid narrowing lost inside
closures**: `grid` is `$state(number[][] | null)`; three call sites read it
inside a `.map()`/`.some()` callback after an outer null check, and TS
doesn't carry narrowing of a mutable `let` binding into a nested function
expression. **Fix**: capture `grid` into a local `const g = grid` right
after each null check, then reference `g` (not `grid`) inside the closures
— `rowCompleteness` and `waitingRows` changed from `$derived(expr)` to
`$derived.by(() => { const g = grid; return … })` (same dependency-tracking
semantics, since the same reactive reads happen either way); the
bingo/waiting `$effect` got a `const g = grid;` right after its early
return and had its `grid` references swapped to `g`. Purely a type-narrowing
refactor — no logic changed.

**Remaining one-off fixes** (implicit-`any`s, one unresolvable-module class,
all pre-existing per the prior report):
- `SettingsButton.svelte`'s `{#snippet switchRow(label, isOn, onToggle)}` —
  added inline JSDoc casts on the three params (`string`, `boolean`,
  `() => void`), same style already used elsewhere in the file for event
  handler params.
- `settings-store.test.js`'s `mockMatchMedia()` stub — added
  `(e: MediaQueryListEvent) => void` JSDoc to the `fn` params of
  `addEventListener`/`removeEventListener`.
- `overlay-history.test.js`'s `back`/`pushState` — typed as
  `ReturnType<typeof vi.spyOn>` instead of left to bare `let` inference.
- `active-tab.test.js`'s `FakeBC.onmessage` — added
  `{((e: {data: any}) => void) | null}` JSDoc to the constructor
  assignment.
- `active-tab.test.js`'s six `import("./active-tab.svelte.js?<tag>")` —
  these are real relative imports at runtime (Vitest's cache-busting query
  suffix loads a fresh module instance per test for isolation) but not a
  specifier TS can resolve. Tried an ambient wildcard module declaration
  (`declare module "./active-tab.svelte.js?*" { export * from "..." }`)
  first; it resolved but produced a wrapped `{ default: typeof import(...) }`
  shape instead of the real named exports (wildcard ambient modules don't
  behave like `export *` re-exports for query-string specifiers). Dropped
  that file and used `@ts-expect-error` with an explanatory comment on each
  import line instead — simpler, guaranteed correct, and each error was a
  genuine one-to-one match for `@ts-expect-error` to consume.
- `+layout.svelte`'s `import("virtual:pwa-register")` — no `app.d.ts`
  existed in this project (SvelteKit's usual scaffold file was never
  created). Added `src/app.d.ts` with the standard SvelteKit template plus
  `/// <reference types="vite-plugin-pwa/client" />`, which supplies
  `vite-plugin-pwa`'s own ambient types for the virtual module (already a
  transitive dependency; no new package added).

**Verified `checkJs` didn't just move the goalposts**: after all fixes,
`npx svelte-kit sync && npx svelte-check --tsconfig ./jsconfig.json` →
`0 ERRORS, 0 WARNINGS` (429 files). No runtime test assertions changed —
`vitest run` stayed at the same pass/fail shape (133 → 138, the +5 being the
new Item-1 test file only).

## Files changed

- `web/vite.config.js` — `registerType: "prompt"`, comment rewritten.
- `web/src/routes/+layout.svelte` — `onNeedRefresh` wiring, update banner.
- `web/src/lib/update-prompt.svelte.js` (new) — update-prompt store.
- `web/src/lib/update-prompt.test.js` (new) — 5 tests.
- `web/jsconfig.json` — `checkJs: true`.
- `web/src/lib/settings-store.svelte.js` — `Settings` typedef, cast on
  `DEFAULT_SETTINGS`'s object literal.
- `web/src/lib/voice.js` — `@returns {Promise<void>}` on `playClip`.
- `web/src/lib/PlayerBoard.svelte` — local `const g = grid` narrowing at 3
  call sites (2 `$derived` → `$derived.by`, 1 `$effect`).
- `web/src/lib/SettingsButton.svelte` — JSDoc param casts on the
  `switchRow` snippet.
- `web/src/lib/settings-store.test.js` — JSDoc param casts on
  `mockMatchMedia`'s stub.
- `web/src/lib/overlay-history.test.js` — typed `back`/`pushState`.
- `web/src/lib/active-tab.test.js` — typed `FakeBC.onmessage`,
  `@ts-expect-error` on the 6 query-string dynamic imports.
- `web/src/app.d.ts` (new) — standard SvelteKit ambient types file +
  `vite-plugin-pwa/client` reference.

## Verification

- `npx vitest run` → 138/138 pass (10 files; was 133/9).
- `npx eslint .` → 0 problems.
- `npx svelte-kit sync && npx svelte-check --tsconfig ./jsconfig.json` →
  0 errors, 0 warnings, `checkJs: true` (429 files).
- `BUILD_PROFILE=gh npx vite build` + `BUILD_PROFILE=gh node
  scripts/verify-pwa-build.mjs` → pass (base `/loto`, 92 audio entries,
  registerSW/sw.js reachable).
- `npx vite build` + `node scripts/verify-pwa-build.mjs` → pass (base `""`).
- Manually inspected compiled `_app/immutable/nodes/0.*.js` + its
  `virtual:pwa-register` chunk: `onNeedRefresh` present, `immediate:!0`
  gone. Manually inspected `build/sw.js`: `self.skipWaiting()` only fires
  on an incoming `SKIP_WAITING` postMessage, never unconditionally.

## Unresolved questions

None — both items were resolvable in full within the stated scope; no
escape hatch was needed for `checkJs`.
