# Code review — full project (260427-1151)

Scope: `src/` SvelteKit + Svelte 5. Skip `node_modules`/`.svelte-kit`/`build`.
Focus: runes correctness, bus/state coordination, voice cancellation, edge cases, JSDoc, tests, anti-patterns.
Overall: code is clean, well-commented, JSDoc-typed throughout. One real auto-tick re-mark bug; rest are minor.

## P0 — blocking

**1. Auto-tick re-marks cells the user cleared / unticked / regenerated** — `src/lib/PlayerBoard.svelte:136-151`.
The auto-tick `$effect` reads `bus.lastDrawn` AND `crossed` AND `grid` reactively. So it re-fires when `crossed` changes (manual untick, "Xoá đánh dấu", "Tạo bảng mới") even though no new draw happened. With `bus.lastDrawn` still set, it scans for that number and re-marks it. Reproductions in "both" mode:
- Master draws 42. User taps 42 to untoggle → effect re-runs, sees `!crossed[r][c]`, re-marks. User cannot ever untick the latest draw.
- User taps "Xoá đánh dấu" → all marks clear → effect re-fires → 42 re-marked instantly.
- User taps "Tạo bảng mới" → new `crossed` matrix → effect re-fires → 42 marked on the new card.
Fixes: track `lastDrawn.at` (or the bus reference) in a non-reactive ref and early-exit when unchanged; OR call `resetBus()` from `handleClear`/`handleGenerate` AND skip the effect on the very first run after generate. Same hazard exists conceptually with the comment "auto-tick can't fight manual taps" — comment is wrong.

## P1 — high

**2. `handleClear` / `handleGenerate` don't reset `bus.lastDrawn`** — same file, lines 153-177. Even after fixing #1, the bus stays sticky across player-card resets. Master `handleNewGame` does reset it (`MasterPanel:139`); player resets should too in "both" mode (or at minimum, snapshot `lastDrawn.at` so post-clear effect knows to ignore stale draws).

**3. `handleClear` doesn't reset `congratsRow`/`celebrationTier` and leaves `showCongrats`** — `PlayerBoard.svelte:167-177` sets `showCongrats = false` but if a celebration was mid-animation the state is fine. However `congratsRow`/`celebrationTier` aren't reset; harmless until next bingo overwrites. P2.

**4. Multiple-row simultaneous bingo silently swallowed** — `PlayerBoard.svelte:102-112`. Pass 1 `break`s after first new completion, but pass 2 won't surface another. Not reachable today (one cell-toggle = one row at most). But auto-tick + cross-row coincidence + future logic changes could expose it. Add a short queue or track "pending celebrations" if you ever batch-mark.

**5. Bingo modal Escape only works when backdrop has focus** — `PlayerBoard.svelte:393-402`. `onkeydown={onModalKeydown}` is on the backdrop `<button>`; opening the modal doesn't auto-focus it, so Escape often doesn't fire. Settings modal solved this with a window listener (`SettingsButton.svelte:107-115`); replicate.

**6. MasterPanel auto-stop branch sets state during effect synchronously** — `MasterPanel.svelte:99-103`. `if (!autoRunning || !settings.autoCallEnabled) { autoRunning = false; return; }` writes the same value when `autoRunning` already false; harmless but produces a redundant effect re-run. Minor correctness — early-return without write when already false.

**7. `loadSettings` is not idempotent for theme listener if called twice without DOM teardown** — `settings-store.svelte.js:88-110`. `applyTheme` defensively clears prior listener via module-scoped `mql/mqlListener`, but module is shared across HMR boundaries. In production fine; in dev hot-reload the previous module's listener can leak. Not a prod concern. P2.

## P2 — medium

- **Smooth-scroll-on-draw ignores `prefers-reduced-motion`** — `MasterPanel.svelte:127-128`. Auto mode at 1s/draw forces page jumps. Gate on `matchMedia('(prefers-reduced-motion: reduce)')`.
- **`navigator.vibrate(10)` on every cell click** — `PlayerBoard.svelte:184-186`. No reduced-motion gate; minor.
- **`toastTimer` not cleared on component unmount** — leaks if PlayerBoard unmounts while toast pending. Wrap in `$effect(() => () => dismissToast())`.
- **`onModalKeydown` only handles Escape** — settings modal uses window listener pattern; bingo modal should match for consistency.
- **`CONFETTI` placement is deterministic** — `(i*8.3 + (i%3)*11) % 100` — fine, but visually clusters. Cosmetic.
- **`generateGrid` uses `Math.random()` based shuffle** — `game-logic.js:31-34` does `arr.sort(() => 0.5 - Math.random())` which is biased. Replace with Fisher-Yates (already used in `pickFilledColsOnce:106-109` and `MasterPanel.svelte:35-38`). Bias is small but real and breaks the "uniform pick" assumption.
- **`scrollOnNextDraw` is a plain `let` in a Svelte 5 component** — works because the gating effect is triggered by `lastCalled`, but reads non-reactively. Document or move into ref-style helper.
- **Settings modal lacks focus trap and focus restoration** — clicking gear, then Escape, returns focus to body, not the gear button. Minor a11y.
- **`storagePrefix` prop on PlayerBoard is unused (always default)** — `PlayerBoard.svelte:22`. YAGNI hook. If keeping, document it; otherwise drop.

## P3 — low / nits

- `celebrationTier` typed `1 | 2` but only `>= 3` switches to 2; type and threshold name mismatch — fine, but could be `'normal' | 'big'`.
- `BOARD_FLAT` is computed at module load in `<script module>` — good, just note it persists across HMR.
- `audio-manifest.js:23` `DEFAULT_VOICE = VOICES[0]?.id ?? "hoai-my"` — fallback string isn't validated against `VOICE_IDS` if manifest is empty; impossible today but fragile.
- `cancelPlayback` resets `currentTime` after `pause()` — order is fine, but the cached element keeps `onended`/`onerror` nulled; next `playClip` reattaches. OK.

## Tests
Coverage strong on `game-logic`, `settings-store`, `vietnamese-number`, `call-bus`. No vacuous assertions seen. Gaps:
- No integration test for the auto-tick bus → PlayerBoard `crossed` flow (would have caught P0 #1).
- No test for `MasterPanel` auto-call interval lifecycle (start/stop/speed-change re-arm).
- No test for `voice.js` token cancellation (pure logic — easy to add).
- `generateGrid` rejection sampling tested for "no triple" but not for the fallback path (return `last` after 200 attempts).

## Positives
- Clean module boundaries; `game-logic` pure.
- `safeParse` pattern for localStorage is solid.
- `playClip` token/resolver dance handles cancellation correctly.
- Theme `auto` listener cleanup is proper.
- JSDoc coverage near 100%; types tight.
- Migration path (`masterMode` → `mode: "both"`) preserved with tests.

## Recommended fix order
1. P0 #1 — track `lastDrawn.at` non-reactively in PlayerBoard; only auto-tick when it changes.
2. P1 #2 — call `resetBus()` from player resets (or rely on #1 fix making it moot).
3. P1 #5 — window-level Escape on bingo modal.
4. P2 — Fisher-Yates in `randomNumbersInCol`; reduced-motion gates.
5. Add integration test covering auto-tick + manual-untick interaction.

## Unresolved questions
- Is `storagePrefix` prop intended for a future feature (e.g. master also tracks their own card) or YAGNI?
- For the auto-tick ergonomics: is "user untoggles latest draw" actually a desired flow, or is the auto-tick supposed to be authoritative? Affects the chosen fix for P0 #1.
- Should `voiceEnabledMaster` driving the player Chờ/Kinh in "both" mode be made a separate setting? Current double-meaning (read in `PlayerBoard.svelte:97-99`) is non-obvious.

**Status:** DONE
**Summary:** One P0 (auto-tick re-marks cleared cells), 6 P1, several P2 cosmetic/a11y. Code quality high overall.
