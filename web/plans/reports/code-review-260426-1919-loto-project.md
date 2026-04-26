# Code Review — loto (dev branch, 260426-1919)

Scope: `app/page.tsx`, `app/master/page.tsx`, `app/loto-player-board.tsx`, `app/loto-game-logic.ts`, `app/globals.css`, `app/layout.tsx`, `next.config.ts`, `package.json`, `.github/workflows/deploy.yml`, `.env.example`, `.gitignore`, `eslint.config.mjs`, `README.md`. ~1.2k LOC. Static-export Next.js 16 SPA, no backend, localStorage persistence only.

---

## Replace-or-keep verdict

**KEEP.** Architecture is sound for the scope: a static SPA with localStorage and no auth/network. No rewrite is justified. Top concrete improvements:

1. Fix the toast/race bug in `loto-player-board.tsx` (multiple eligible rows reset `notifiedWaitingRows` while the toast effect early-returns — see HIGH-1).
2. Validate `JSON.parse` outputs from localStorage (shape + dimension checks) — currently a single hand-edited key crashes the render.
3. Memoize `isRowComplete` per row — currently called 81× per render in `PlayerBoard`.
4. Add ARIA/keyboard support to grid cells (currently `<div onClick>` only — not focusable, not announced).
5. Split `master/page.tsx` (244 lines) into `use-master-state` hook + `<MasterBoard>` + `<CalledHistory>` components.

---

## CRITICAL

None. No data-loss path, no remote auth/security boundary (static export, no server). No dependency on user-supplied URLs.

---

## HIGH

### HIGH-1. Race-y toast / “Chờ X” suppression — `loto-player-board.tsx:72-97`
The detection effect uses `return;` after the *first* completed row or first new waiting row. Consequences:

- If two rows transition to "waiting" simultaneously (one click can do this only when grids overlap, but more importantly **on mount** — see HIGH-2 — multi-row recovery is fine because the seed loop in `useEffect` `46-65` populates the set first; but during *gameplay* with overlapping numbers, the second row never gets a "Chờ X" toast on the click that would have triggered it because `return` exits before the loop reaches it). Next state change re-runs the effect and surfaces it, so it self-heals — but in practice it means the second eligible row stays silent until *another* state change.
- Bigger issue: the “delete” branch at `89-95` runs only for indices the early-return loop *reaches*. Cross a row back from waiting → not-waiting → re-waiting in that order while a *lower-index* row is currently waiting, and the higher-index reset branch is never executed → second waiting toast is suppressed indefinitely.

Fix: split the loop into two passes — first recompute desired sets for all rows, then fire one notification.

### HIGH-2. `isRowComplete` returns `true` for empty/zero-cell row — `loto-game-logic.ts:108-117`
The "any cell with value > 0 must be crossed" check returns `true` if no positive cells exist. `generateGrid` always emits 5 numbers/row so the live path is safe, but:

- Master `BOARD` has rows where `col===0 && row===9` etc. set to 0; they aren't passed to `isRowComplete` today, but anything that imports the helper for a different grid shape could trip it.
- More immediate: if `loadGrid` returns a corrupted grid (e.g. user's localStorage edited to all zeros), the seed loop at `loto-player-board.tsx:56-59` would mark every row as celebrated, and the very first `crossed` change would skip the celebration animation for the real win.

Fix: add `let hasNumber = false;` guard, return `false` when no positive cells.

### HIGH-3. `loadGrid` / `loadCrossedState` accept any JSON shape — `loto-game-logic.ts:83-105`, `app/master/page.tsx:51-59`
`JSON.parse` is wrapped in try/catch, but the parsed value is returned as-is and trusted as `number[][]` / `boolean[][]` / `MasterState`. Hand-edited or stale-from-older-version localStorage will crash the render:

- `crossed[row]?.[col]` (`loto-game-logic.ts:114`) is defensive, but `grid[row][col]` (`isRowComplete` line 114) is not — `grid[row]` could be `undefined` (`grid` shorter than 9 rows), throwing inside render.
- `loadState()` in master could return `{}` and `state.called.length` (`app/master/page.tsx:69`) throws.
- Cross-version risk: if the grid algorithm changes, old saves become wrong-shape but still parse.

Fix: validate shape (`Array.isArray`, lengths, element type) before returning. Drop & log on mismatch.

### HIGH-4. `crossed[][]` / `grid[][]` dimension drift — `loto-player-board.tsx:46-65`
`saveGrid` and `saveCrossedState` are written separately. If `setGrid(newGrid)` succeeds but the next render throws before `saveCrossedState` runs (e.g. browser kills tab), the next session loads a new grid with stale crossed dimensions from a *prior* grid. `crossed[row]?.[col]` masks this for booleans, but a 9×9 grid paired with a 9×5 or 10×9 crossed array silently misreports row completion.

Fix: store both under one key as `{ grid, crossed, version }`, write atomically. Or always re-init `crossed` to all-false on grid load when shapes don't match.

### HIGH-5. `BOARD` mutation hazard via shared module-scope reference — `app/master/page.tsx:61`
`BOARD = buildBoard()` is a module-level mutable nested array. Today nothing mutates it. But any future "click-to-strike" UI that mutates `BOARD[r][c]` would persist across HMR and hot-reload between routes. Freeze with `Object.freeze` on rows or compute inside the component (cheap — 90 ints).

### HIGH-6. Performance: `isRowComplete` called 81× per render — `loto-player-board.tsx:144`
Inside `grid.flat().map(...)`, line 144 calls `isRowComplete(grid, crossed, row)` for every cell. That's 81 calls per render, each scanning 9 cells = 729 reads per render. Trivial today, but on every keystroke/click. Pre-compute `const completedRows = useMemo(() => grid.map((_, i) => isRowComplete(grid, crossed, i)), [grid, crossed])`.

### HIGH-7. `key={idx}` on history pills — `app/master/page.tsx:170`
Acceptable here (history is append-only). However `key={idx}` is also used on grid cells (`loto-player-board.tsx:148`, `master/page.tsx:190`) — for the player grid this *will* break React's reconciliation if `generateGrid` ever returns a different cell ordering between renders (it doesn't, but the contract isn't enforced). Use stable keys derived from `row*9+col` (which equals idx today, so functionally identical — but documents intent).

### HIGH-8. basePath/assetPrefix prod hardcode — `next.config.ts:23`
`basePath = isProd ? "/loto" : ""`. If the GH Pages repo is renamed, or someone deploys to a custom domain (apex), every asset 404s. Pull from `process.env.NEXT_PUBLIC_BASE_PATH` with `/loto` as fallback. Also: `output: "export"` is set unconditionally — `next start` (`package.json:9`) is meaningless for an exported build. Either remove the script or document.

---

## MEDIUM

### MED-1. `randomNumbersInCol` Fisher-Yates is biased — `loto-game-logic.ts:46-49`
`arr.sort(() => 0.5 - Math.random())` is a well-known biased shuffle (V8 sort is not guaranteed pairwise-symmetric). Fine for a casual game, but use a real shuffle (the same one in `master/page.tsx:39-43`) for fairness. DRY: extract one `shuffle<T>(arr: T[]): T[]`.

### MED-2. `confirm()` blocks during render — `loto-player-board.tsx:100`, `master/page.tsx:82`
Native `confirm()` is synchronous and blocked by some browsers (Safari iframe, in-app webviews). Replace with the existing modal pattern (already used for "Kinh!" popup) for consistency and reliability.

### MED-3. `randomARow` mutates caller's `baseWeight` — `loto-game-logic.ts:32-42`
`baseWeight[col]--` mutates the array passed by reference. The caller (`generateGrid:57`) creates a fresh array each call so it's safe today, but the function signature lies. Either rename to `randomARow(baseWeight, mutate=true)` or take/return a copy.

### MED-4. `state.remaining[0]` always drawn — `master/page.tsx:90`
The shuffle is done once at game start, then `remaining` is consumed FIFO. That's deterministic given the initial shuffle. Functionally fine, but "Xổ số" feels less random — consider `Math.floor(Math.random() * remaining.length)` per draw to make each draw visibly random (no algorithmic difference for fairness, just UX perception).

### MED-5. Toast effect dep on `showToast` causes re-runs — `loto-player-board.tsx:97`
`showToast` is `useCallback([dismissToast])` and `dismissToast` is `useCallback([])`, so identity is stable. OK, but adding any future dep would cause double-fires. Document or extract toast logic into a custom hook.

### MED-6. `master/page.tsx` hosts both a `loto_master` (called numbers) and `loto_master_card` (master's own card) key — naming collision risk
A user-side bug where the user navigates to `/master`, generates a master card, then clears storage by clicking "Tạo bảng mới" *only* clears `loto_master_card_*` not `loto_master`. That's correct, but the visual cue (orange palette) doesn't tell the master "the called-number state is independent of your card." Add a small UI hint or rename for clarity.

### MED-7. master/page.tsx is 244 lines — modularization candidate
Per project rules (>200 LOC). Suggested split: `app/master/use-master-state.ts` (state + persistence), `app/master/master-board.tsx` (the 9×10 tracking grid), `app/master/called-history.tsx` (chips). That brings each file under 100.

### MED-8. README is 14 lines, missing dev:codeserver instructions — `README.md`
The new codeserver profile is non-obvious. Add a section explaining `.env.local` setup, `CODESERVER_HOST/PORT`, and the `/absproxy/{port}` URL.

### MED-9. Accessibility — grid is unreachable by keyboard
- `<div onClick>` is not focusable, no `role="button"`, no `aria-pressed={isCrossed}`, no `aria-label="Số 42, đã đánh dấu"`, no `tabIndex={0}`, no Enter/Space handler.
- Congrats modal (`loto-player-board.tsx:192-232`) has no `role="dialog"`, no `aria-modal`, no focus trap, no Escape-to-close.
- Toast has no `aria-live="polite"`.
- Color contrast: `text-slate-400 dark:text-slate-500` (`master/page.tsx:154`, `211`) on `dark:bg-slate-900` likely fails WCAG AA. The diagonal-line cross-out (`globals.css:80-91`) is a single hue (`#ef4444`) — colorblind users may miss it; the bg-color change provides redundancy, OK.

### MED-10. No tests at all
There's no `__tests__/` or `*.test.ts`. `generateGrid`, `isRowComplete`, `getWaitingNumber`, `randomANumberInRow` are pure and trivially testable. Property-based test on `generateGrid`: every row has 5 numbers, every column count ≤ 6, all numbers in range, no duplicates.

### MED-11. CSP / iframe headers not set
Static export + GH Pages → no CSP. App is embedded-friendly which means clickjacking-friendly. Low impact (no auth, no money, no PII), but document or add `<meta http-equiv="Content-Security-Policy">` in `layout.tsx`.

### MED-12. `Math.random()` in `loto-game-logic.ts:24,47` — not cryptographic but called "random"
Fine for a game. Document so a future dev doesn't think it's secure.

---

## LOW

### LOW-1. Dead code: `notifiedWaitingRows` reset branch only triggers when `waitNum===null && notified && !celebrated` — `loto-player-board.tsx:89-95`
A row that completes will never hit this branch because `celebrated.has(i)` blocks it. Add a comment, or refactor: when a row becomes complete, remove from `notifiedWaitingRows` (already done at line 78) and rely on the check.

### LOW-2. `package.json` name is `nextjs-temp` — pre-rename leftover. Rename to `loto`.

### LOW-3. `master/page.tsx:184` `BOARD.flat()` allocates per render. Wrap in `useMemo(() => BOARD.flat(), [])` or compute once at module scope.

### LOW-4. `app/page.tsx:14` "TN1 (2014–2017)" hard-codes copy in the component. If localization is ever added, extract.

### LOW-5. `globals.css:67` typo-prone — `.animate-spin-slow-reverse` reuses `spin-slow` keyframe + `reverse` direction. Works but two classes named almost identically (`spin-slow` vs `spin-slow-reverse`) is brittle.

### LOW-6. `isRowComplete`/`getWaitingNumber` hardcode `col < 9` — `loto-game-logic.ts:113,126`. Use `NUM_COLS` constant for consistency.

### LOW-7. `app/master/page.tsx:79` `const calledSet = new Set(state?.called ?? []);` rebuilt every render. `useMemo` (cheap — 90 elements — so LOW).

### LOW-8. `app/page.tsx:65` `rel="noopener noreferrer"` is good. But `master/page.tsx:233` has the same external link duplicated — extract `<Footer />`.

### LOW-9. `next-env.d.ts` was changed in this branch. That file is auto-regenerated and shouldn't be hand-edited; ensure the change is benign (it imports `./.next/dev/types/routes.d.ts` which is the new Next 16 typegen). Verify the file isn't gitignored on other contributors' machines.

### LOW-10. `eslint.config.mjs:14` ignores `next-env.d.ts` but the file is now actually committed and modified — confirm intent.

---

## Edge cases (adversarial)

| Scenario | Behavior | Severity |
|---|---|---|
| `localStorage` is disabled / quota exceeded | `setItem` throws, uncaught → unhandled exception during state update. | HIGH |
| User edits `loto_grid` to `"hello"` | `JSON.parse` throws, caught, returns `null`. OK. | OK |
| User edits `loto_grid` to `[[1,2]]` (1 row, 2 cols) | `isRowComplete` reads `grid[1][0]` → `undefined.0` → TypeError on render. | HIGH-3 |
| User opens `/` and `/master` in two tabs of same browser | Both tabs write to same `loto_grid` key from `/`, but `/master` uses `loto_master_card_grid` for its own card, so they don't overlap. The user's main card on `/` is shared. NO `storage` event listener — second tab won't update. | MED |
| User opens `/` twice in two tabs | Both write to `loto_grid` — last writer wins, no sync. Mutation in tab A invisible in tab B until refresh. | MED |
| `generateGrid` called on a partially-completed board | `handleGenerate` always overwrites grid + resets crossed. `confirm()` guards. OK. | OK |
| `crossed[][]` shorter than `grid[][]` | `crossed[row]?.[col]` returns undefined → falsy → row never marked complete. Safe but wrong-state. | HIGH-4 |
| `crossed[][]` longer than `grid[][]` | Excess rows ignored. Safe. | OK |
| Click during congrats modal | Cell click works (grid is not aria-hidden, modal is a fixed overlay). User can keep marking. Minor UX: modal blocks clicks via backdrop, but `e.stopPropagation` on inner div allows close. OK. | OK |
| Codeserver host changes mid-session | basePath is baked at build/dev-start. `next dev` must restart. Document. | LOW |
| GH Pages deploy when repo renamed | `/loto` 404. | HIGH-8 |
| Two rows reach "waiting" on the same click | Only first row's "Chờ X" toast shown; second silent until next click. | HIGH-1 |

---

## Security

- **XSS via stored values:** All user-controlled data is rendered as `{num}` (numeric) or `{toast}` (template literal `Chờ ${num}`) — both pass through React's escaping. The "Hàng X đã đầy đủ" message uses `congratsRow` which is a number. No `dangerouslyInnerHTML` anywhere. **Safe.**
- **Prototype pollution via JSON.parse:** `JSON.parse` does not pollute Object.prototype by itself, but `loadCrossedState` returns the raw parsed value. If a malicious actor can control localStorage (i.e. user's own browser — not a real attacker model for a static SPA), they can set `{"__proto__": {...}}` but `JSON.parse` ignores `__proto__` as a data key in modern engines (V8 since long ago). **Safe by current engine behavior**, but a defensive `structuredClone` or shape validator (HIGH-3) makes it explicit.
- **Codeserver dev origin leakage:** `allowedDevOrigins: [host]` with `host` from `.env.local` — no validation. If `CODESERVER_HOST` is set to `*` or a malformed value, Next will accept it. Add a sanity check (must match `/^[a-z0-9.-]+$/i`). Low impact (dev only, never shipped to prod). **Low.**
- **Secrets:** `.env.local` is gitignored. `.env.example` only contains hostnames. `CODESERVER_HOST/PORT` are not `NEXT_PUBLIC_*` so they are server-side only — but `next.config.ts` consumes them at build time and bakes `basePath` into the static bundle. The basePath itself (`/absproxy/3000`) is visible in HTML. Hostname is NOT baked — only used for `allowedDevOrigins`. **Safe.**
- **GH Actions:** `permissions:` is correctly minimal. No third-party actions beyond `actions/*` v4. `npm ci` against committed `package-lock.json` (verify lockfile is committed — not visible in this review; check).
- **No CSRF / no auth / no PII** — N/A.

---

## Positive observations

- Clean separation of pure logic (`loto-game-logic.ts`) from React state.
- `useCallback` and `useRef` used appropriately for stable identity.
- Vietnamese-first UI is consistent.
- `prefix`-based localStorage namespacing is the right call for the new master-card feature — avoids the user-board / master-card collision cleanly.
- Tailwind + CSS keyframes is lighter than dragging in Framer Motion.
- TS strict (assumed from `eslint-config-next/typescript`) and no `any` in app code.
- `output: "export"` matches the deploy target (GH Pages static).
- The new codeserver profile is well-documented inline (`next.config.ts:6-10`) — comment explains *why* `/absproxy` not `/proxy`.

---

## Recommended actions (priority order)

1. **HIGH-1**: Rewrite the row-detection effect as two passes (compute → notify) to fix toast suppression.
2. **HIGH-3 + HIGH-4**: Add a `validateGrid(g): g is number[][]` and `validateCrossed(c, g): c is boolean[][]` helper. Use in `loadGrid`/`loadCrossedState`/`loadState`. On invalid: drop the key and return null.
3. **HIGH-2**: Add empty-row guard to `isRowComplete`.
4. **HIGH-6**: Memoize `completedRows` in `PlayerBoard`.
5. **HIGH-8**: Read `basePath` from `NEXT_PUBLIC_BASE_PATH` env with `/loto` fallback; document in README.
6. **MED-9**: Add ARIA + keyboard handlers to grid cells (role=button, tabIndex, aria-pressed, aria-label, Enter/Space). Add role=dialog + focus trap to congrats modal. Add aria-live=polite to toast.
7. **MED-7**: Split `master/page.tsx` into hook + 2-3 components.
8. **MED-1 + MED-3**: DRY up shuffle into one helper, use it everywhere.
9. **MED-10**: Add Vitest + a property test for `generateGrid` invariants.
10. **LOW-2 + MED-8**: Rename package, expand README.

---

## Metrics

- LOC reviewed: ~1.2k (app code only).
- Type coverage: 100% explicit (no `any`).
- Test coverage: 0% (no tests).
- Lint: not run in this review (ask `tester` agent if needed).
- File-size violations: 1 (`master/page.tsx` 244 LOC > 200 limit).

---

## Unresolved questions

1. Is `package-lock.json` committed? (Required for `npm ci` reproducibility — couldn't see it in the diff stat.)
2. Is the GH Pages deploy guaranteed to be at `/loto`, or is a custom domain planned? Determines whether HIGH-8 is real or theoretical.
3. Does the master "play along" feature (HIGH `loto_master_card`) need to interact with the called-numbers state (e.g. auto-cross master's card when a number is called)? Right now they're fully independent — verify this is intended.
4. Should the user's grid sync across tabs (`storage` event listener)?
5. Is i18n on the roadmap, or is Vietnamese-only acceptable long-term?
