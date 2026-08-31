# Android review fixes (2026-08-31)

Source review: `plans/reports/code-reviewer-260831-2213-android-review.md`.
Scope: `android/` only (per delegation; `web/` read-only for M2 parity check;
`.github/workflows/` and `android/local.properties` untouched).

## Fixed

### H1 — restore/user-action race

- `state/PlayerBoardViewModel.kt`: `restore()` now only applies the saved
  round `if (saved != null && grid == null)` — a `generate()` that lands
  while the DataStore read is still in flight can no longer be clobbered.
  Added `loading: Boolean` to `PlayerUiState` (`true` until restore
  resolves); `PlayerBoardScreen.kt`'s "Tạo bảng mới" button is now
  `enabled = !state.loading`, closing the no-confirm branch at its root
  (previously `grid == null` meant both "no card" and "not loaded yet").
- `state/MasterStore.kt`: added `loading: StateFlow<Boolean>` and a
  `mutatedBeforeRestore` guard set by `startNewGame()`/`drawNext()`;
  `restore()` skips applying the saved snapshot once either has run.
  `state/MasterPanelViewModel.kt` exposes `loading`;
  `ui/master/MasterPanelScreen.kt`'s "Ván mới" button is now
  `enabled = !loading`, same shape as the player fix.
- Regression tests: added `app/src/test/java/com/miti99/loto/SlowDataStore.kt`
  (delays a wrapped `DataStore`'s first `.data` emission until a
  `CompletableDeferred` gate completes — `InMemoryDataStore` alone resolves
  synchronously so the race was invisible to the existing suite, per the
  report's test-quality note). Added
  `restore does not clobber a card generated while the load is still in
  flight` (PlayerBoardViewModelTest) and
  `restore does not clobber a new game started while the load is still in
  flight` (new `MasterStoreTest.kt`) — both fail without the fix, pass with it.

### H2 — tier-2 confetti invisible

- `ui/player/KinhDialog.kt`: `Dialog` now uses
  `DialogProperties(usePlatformDefaultWidth = false)` so the dialog window
  spans the full screen, and `ConfettiOverlay()` is composed *inside* that
  window (drawn after/above the card `Column` in a `Box`), gated by a new
  `showConfetti: Boolean` param. `PlayerBoardScreen.kt` no longer emits
  `ConfettiOverlay()` as a scroll-column sibling. This is option (b) from
  the report (embed in the dialog's own window) rather than a separate
  `Popup`, since it guarantees same-window z-order over the scrim without
  relying on cross-window layering behavior.

### M1 — RGB truncation

- `ui/settings/EmptyCellColorPicker.kt`: `r`/`g`/`b` now use `roundToInt()`
  instead of `toInt()`.

### M2 — force-scroll to hero in `both` mode

- Checked `web/src/lib/MasterPanel.svelte:86-158` (read-only): `handleDrawNext()`
  sets `scrollOnNextDraw = true` unconditionally, called by both the manual
  draw button and the auto-call `setInterval`, with no `settings.mode` gate.
  `MasterPanel.svelte` is rendered the same way in `master` and `both` mode
  (`+page.svelte:37`). **Web already force-scrolls on every draw in `both`
  mode** — Android's `LaunchedEffect(tickKey) { ... heroRequester.bringIntoView() }`
  in `MasterPanelScreen.kt` already matches. Left as-is; added a comment
  recording the parity check so this isn't re-litigated as a bug.

### M3 — hardcoded toast copy / unused strings

- `PlayerUiState.toast: String?` → `waitingNumber: Int?`. The ViewModel now
  stores the raw number; `PlayerBoardScreen.kt` formats it with
  `stringResource(R.string.toast_waiting, waitNum)`.
- Deleted 4 of the 5 unreferenced resources from `strings.xml`:
  `toast_dismiss`, `player_board_label`, `master_board_label`, `kinh_close`.
  `toast_waiting` is now wired (not deleted).
- Did **not** move `PlayerCell.kt`'s `stateDescription` fragments into
  `strings.xml` — the task scope named only the toast + the five unused
  resources; that's a separate, additive accessibility cleanup the report
  suggests but didn't require. Noted below as deferred.

### M4 — cold-start settings/master ordering

- `LotoApplication.kt`: split `onCreate`'s single coroutine into two
  `appScope.launch` blocks (`masterStore.restore()` and
  `settingsState.collect { ... }`) so a slow round-state restore can't stall
  voice-id sync.
- `PlayerBoardViewModel.kt`: `init` now does
  `combine(masterStore.state, settings) { master, _ -> master }.collect { onMasterState(it) }`
  instead of a plain `masterStore.state.collect`, so a settings arrival
  re-runs the replay against the current master snapshot.
- **Scope note (see Unresolved below)**: `PlayerAutoCross.applyMasterCalls`
  advances the cursor to `called.size` on every non-`BOTH` pass, even a
  no-op one. `combine()` closes the ordering where settings resolves to its
  real value no later than `masterStore.restore()`'s first real emission
  (verified by test). It does **not** close the reverse ordering — master's
  restore landing with full history before settings ever resolves — because
  by the time settings arrives, the cursor has already consumed that
  history under the wrong mode. Fully closing that needs a "settings
  loaded" signal threaded through `SettingsRepository`/`LotoApplication`,
  which is a larger change than this narrow, "usually invisible" (report's
  words) edge case warrants; documented as a deferred follow-up rather than
  silently left unfixed.

### M5 — `generateGrid()` off the main thread

- `PlayerBoardViewModel.generate()`: the pure `CardGenerator.generateGrid()`
  call now runs on an injectable `computeDispatcher` (defaults to
  `Dispatchers.Default`), then `withContext(Dispatchers.Main.immediate)`
  applies the result via a new private `applyGeneratedGrid()`. Only the
  computation is offloaded; all `ViewModel` field mutations still happen on
  Main, so there's no new cross-thread mutable-state hazard.
- Added the `computeDispatcher` constructor param (defaulted) specifically
  so unit tests can pass the same `StandardTestDispatcher` used for Main —
  otherwise `generate()` would race a real background thread against
  `runCurrent()`/`advanceUntilIdle()`, making the suite flaky.
  `LotoViewModelFactory.kt` needed no change (named-arg construction, new
  param is defaulted).

### M6 — master-panel recomposition

- **Deferred, not applied.** The report frames this as "not a blocker...
  worth measuring," conditional on API 24 device profiling that
  `plans/todo.md` shows as still unchecked. The suggested mitigations
  (`key(num)` blocks or lambda-based cell props) touch the hot
  `MasterBoard`/cell-render path with no compose UI test harness in this
  project to catch a visual regression — the risk/verification ratio didn't
  clear the bar for a code-review-driven change without a measured
  baseline. Flagging back to the user rather than guessing at scope.

### L1 — ExoPlayer never released

- `MainActivity.kt`: `onDestroy()` now calls
  `(application as LotoApplication).voicePlayer.release()` when
  `isFinishing` is true. Guarded by `isFinishing` (not fired on a
  rotation/config-change recreate) since `ExoPlayer.release()` is terminal
  — the single Activity finishing via the exit-confirm dialog's `finish()`
  is the one path where no further `speak()` call can follow.

### Manifest/CI hygiene (clear-cut only)

- `AndroidManifest.xml` (L8): dropped `android:launchMode="singleTask"`
  (default `standard`) — no deep links or external `startActivity` targets.
- `app/build.gradle.kts` (L11): moved the Vietnamese-only trim from the
  deprecated `defaultConfig.resourceConfigurations` to
  `androidResources.localeFilters` (AGP 8.13).
- **L10 (CI keystore cleanup) not applied**: `.github/workflows/android-release.yml`
  is outside `android/`, and the file-ownership instruction for this task is
  explicit ("Files you may modify: anything under .../android"). Flagged,
  not fixed — a one-line `if: always()` rm step for whoever owns that file.
- **L9 (`allowBackup`/`dataExtractionRules`) not applied**: the report
  itself frames this as conditional ("if a restored `loto_game_state` from
  another device is undesirable") — that's a product decision about
  cross-device transfer behavior, not a clear-cut technical fix. Left as-is
  pending a decision.
- **L7 (`Color.parseColor` → `toColorInt()`) not applied**: not in the
  task's enumerated item list (task named Manifest/CI hygiene specifically);
  `ColorParsing.kt` is core app code, out of the explicitly requested scope.

## Verification

- `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot" ./gradlew :app:testDebugUnitTest`
  → **BUILD SUCCESSFUL**. `PlayerBoardViewModelTest`: 17/17 (14 original +
  3 new). `MasterStoreTest` (new): 2/2. All other existing suites
  unaffected (no other test files touched).
- `./gradlew :app:lintDebug` → **BUILD SUCCESSFUL**, no findings in any file
  touched by this change (checked by grepping the HTML report for each
  modified filename).
- No gradle daemon left running beyond the two builds above (Gradle's
  default idle-timeout will reap it; not force-killed since this repo's
  standard flow doesn't request that and the daemon is reused by whoever
  runs the next Gradle command in this workspace).

## Deferred / out of scope

- M6 (master-panel recomposition) — needs a profiled baseline first.
- L9 (dataExtractionRules) — needs a product decision on cross-device
  round-state transfer.
- L10 (CI keystore cleanup) — file outside `android/`, blocked by
  file-ownership scope.
- L7 (`Color.parseColor` deprecation) — not in the delegated item list.
- `PlayerCell.kt`'s hardcoded `stateDescription` Vietnamese fragments — the
  report bundles this with M3 but the task scoped M3 to the toast + the
  five unused resources; left untouched.
- L2–L6 (shared voice player double-cancel, restore shape validation,
  swallowed `IOException` with no log signal, stale `autoRunning` after
  manual-draw exhaustion, `manualUnticks` surviving a non-BOTH reset) — not
  named in the task's item list; not touched.

## Unresolved questions

1. M4: is the residual race (master's restore landing with full history
   *before* settings ever resolves) worth a "settings loaded" signal, or is
   the report's own "usually invisible" framing enough to leave it as
   documented risk? I implemented the report's suggested `combine()` fix,
   which closes the more common ordering, but flagging the gap rather than
   silently claiming full closure.
2. L9: should `loto_game_state` be excluded from Android backup/device
   transfer via `dataExtractionRules`, or is cross-device restore of an
   in-progress round acceptable/desired? Needs a product call, not a code
   fix.
3. M6: is there an actual profiled slowdown on the API 24 device from
   `plans/todo.md`, or should this stay deferred until QA reports one?

Status: DONE_WITH_CONCERNS
Summary: Fixed H1, H2, M1, M3, M4 (partial, documented gap), M5, L1, and the two clear-cut manifest/gradle hygiene items (L8, L11); confirmed M2 already matches web and left as-is; deferred M6, L2-L7, L9, L10 with reasons. All unit tests (including 5 new race-regression tests) and lint pass.
Concerns/Blockers: M4's fix does not close every interleaving (see report); M6/L9/L10 deferred rather than guessed at; none are build-blocking.
