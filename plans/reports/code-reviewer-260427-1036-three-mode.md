# Code Review — three-mode + auto-tick + master Chờ/Kinh

## Scope
- Files: settings-store(.svelte.js + .test.js), SettingsButton.svelte, +page.svelte, call-bus(.svelte.js + .test.js), MasterPanel.svelte, PlayerBoard.svelte, voice.js
- Focus: three-mode picker, legacy migration, master→player draw bus, auto-tick gating, master voice for Chờ/Kinh in `both`
- Test/typecheck: 106/106 passing; `svelte-check` 0 errors / 0 warnings

## Overall Assessment
Clean, small, idiomatic Svelte 5. Bus-as-rune is the right primitive — fresh-object trick (`{num,at}`) handles repeats correctly. Set-only auto-tick eliminates manual-vs-auto fights by construction. Migration is pure and one-shot via `saveSettings`. One real test bug found; everything else is minor.

## Critical Issues
None.

## High Priority

### 1. Stale `masterMode` assertion in `settings-store.test.js:189` (vacuous test)
```js
expect(settings.masterMode).toBe(DEFAULT_SETTINGS.masterMode);
```
Both sides are `undefined` after the `mode` rename — the assertion passes for the wrong reason and gives false coverage. Replace with `expect(settings.mode).toBe(DEFAULT_SETTINGS.mode);` or delete (the per-key fallback is already covered elsewhere).

## Medium Priority

### 2. Auto-tick effect early-return collapses dependency tracking
`PlayerBoard.svelte:134` reads `bus.lastDrawn` first (good) but the subsequent `settings.mode !== "both"` early-return means Svelte stops tracking on that path. In practice this works because the *next* draw retriggers via `bus.lastDrawn` and re-reads `settings.mode`. The user-stated intent ("changing mode mid-game tears down/re-arms cleanly") holds for *future* draws, **not retroactively** — a number drawn while in `master` mode and then switching to `both` will not back-fill ticks. If retroactive backfill is desired, tick from `state.called` on mode change. If not (likely correct UX — auto-tick is forward-only), add a one-line comment so a future reader doesn't think it's a bug.

### 3. Master voice cancellation collides with player voice in `both` mode
Both panels share the same `voice.js` singleton (`activeClip`/`activeToken`). Sequence in `both`+both-voices-on: master draws → `playNumber(n)` starts → player auto-tick fires the same render → if the auto-tick completes a row, `playBingo()` cancels the in-flight number clip. End-user effect: "Bingo!" wins, number announcement gets cut off. This is arguably correct (Kinh is more important), but the tradeoff is undocumented. Consider: (a) leave + comment, or (b) queue Kinh after current clip via a small chain. KISS says leave it — but flag it in CHANGELOG so the "voice cuts off sometimes" report doesn't surprise you.

### 4. Voice flag composition repeated as inline expression
`PlayerBoard.svelte:98-99` recomputes `voiceEnabledPlayer || (voiceEnabledMaster && mode === "both")` inline. If a third caller appears, factor a `$derived` on `settings` (stays one place). Skip for YAGNI.

## Low Priority
- `+page.svelte:42` — `transition:slide` on the master section is fine but plays on every mount including initial page load. Acceptable; minor.
- `call-bus.svelte.js` — module-level `$state` is fine for SvelteKit SSR (no client state leak across requests since it's reset on each app instance), but a comment noting this would help future readers who worry about it.

## Edge Cases (scout)
- **Repeat draw**: bus publishes new object → effect re-runs → set-only guard finds cell already true → no-op. Correct.
- **`Ván mới` mid-auto**: `MasterPanel.handleNewGame` calls `cancelPlayback()` + `autoRunning=false` + `resetBus()`. Bus reset to `null`, player effect early-returns. Correct.
- **Mode flip mid-game from `both`→`master`**: PlayerBoard unmounts → `cancelPlayback()` cleanup runs → master clip stops. Master's `playNumber` next draw re-acquires audio. Correct.
- **Migration idempotency**: `saveSettings` writes `mode:"both"` and drops `masterMode` key. Subsequent loads take the `validMode` branch. Correct.
- **localStorage QuotaExceeded mid-save**: silently swallowed (existing behavior). OK.

## Test Coverage Gaps
- No test asserts `bus.lastDrawn` integration with PlayerBoard auto-tick (would require a Svelte component test harness — currently the suite is unit-only). Given KISS, a single component-level integration test is worth it: render PlayerBoard with a fixed grid, set `settings.mode="both"`, call `broadcastDraw(n)`, assert `crossed[r][c]===true`. This is the only un-tested branch of the new feature.
- No regression test for "auto-tick does NOT fire in `master` or `player` mode" — easy to add alongside the above.

## Positive Observations
- Per-key validation pattern preserved on the new `mode` field — adding it didn't break the existing per-key fallback contract.
- Fresh-object publish pattern (`{num, at: Date.now()}`) is the right primitive for "fire on every draw including repeats."
- Set-only auto-tick eliminates an entire class of state-fight bugs without locking.
- `voice.js` token + `activeResolver` cancellation is correct under interleaved cancel/play and was not regressed.
- Migration is one-shot via natural `saveSettings` write — no extra "version" plumbing. KISS.

## Recommended Actions
1. Fix vacuous `masterMode` assertion (test line 189) — high.
2. Add one component-level integration test for the auto-tick path — medium.
3. Add 1-line comment in PlayerBoard auto-tick `$effect` clarifying "forward-only, no backfill" — medium.
4. CHANGELOG note: in `both` mode, completing a row may interrupt the number callout — low.

## Metrics
- Tests: 106/106 pass
- svelte-check: 0 errors, 0 warnings
- New LOC: ~+50 net (call-bus + tests + PlayerBoard effect)

## Unresolved Questions
- Is master-clip-cut-by-bingo intentional or worth a queue? (Q for product owner — UX call, not a bug.)
- Should mode-flip retroactively auto-tick already-called numbers? (Current: no. If "no" is intentional, add comment; otherwise small change to seed from `state.called` once on mode→both.)
