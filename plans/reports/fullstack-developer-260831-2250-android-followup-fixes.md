# Android follow-up fixes (2026-08-31)

Source: `plans/reports/code-reviewer-260831-2213-android-review.md` (L2–L7,
L10) + the review's M3 note on `PlayerCell.kt`'s hardcoded `stateDescription`.
Prior pass: `plans/reports/fullstack-developer-260831-2226-android-fixes.md`
(H1/H2/M1/M3-partial/M4-partial/M5/L1/L8/L11; explicitly deferred L2–L7, L9,
L10, and the `PlayerCell.kt` cleanup — this task closes that deferred list
except L9, which stays a product decision, and M6, out of scope per the task).

Scope: `android/` + `.github/workflows/android-release.yml` (L10 only). `web/`
untouched. `android/local.properties` not read.

## Fixed

### L2 — shared voice player double-cancel

`state/MasterPanelViewModel.kt`, `state/PlayerBoardViewModel.kt`:
`onCleared()` no longer calls `voicePlayer.cancel()`, only `super.onCleared()`.
Root cause check (not just the described symptom): both ViewModels are
activity-scoped (`LotoViewModelFactory` via `by viewModels { factory }` in
`MainActivity`), so `onCleared()` only fires together, on a real finish — and
`MainActivity.onDestroy()` already calls the shared `voicePlayer.release()`
unconditionally on that same `isFinishing` path (the prior pass's L1 fix).
`release()` supersedes `cancel()` (it stops and tears the player down), so the
per-VM cancel calls were dead weight duplicating L1's fix, plus the fragility
the review flagged (two independent owners unilaterally acting on a shared
singleton). Removing them and adding the missing `super.onCleared()` closes
both parts of the finding without behavior change on any currently-possible
scoping.
Not unit-tested: `onCleared()` is `protected` on `ViewModel` and neither test
class subclasses it, so it isn't reachable from a JVM unit test without a
lifecycle test harness this project doesn't have (same gap as L1, which also
shipped without a dedicated test).

### L3 — restore payload shape validation

`state/GameStateRepository.kt`:
- `parseNumberList` (backs `master_called`, `master_remaining`,
  `player_manual_unticks`) now rejects duplicates (`toSet().size != size`),
  not just out-of-range values.
- `parseGrid` now rejects a duplicate non-zero cell value (a real card never
  repeats a number).
- `loadMasterState` adds `isCompleteDeckPartition`: `called` and `remaining`
  must be disjoint and together cover exactly the 90-number deck. Combined
  size ≠ 90 or an overlap (detected via combined-set size) both fail closed to
  "no saved round" — consistent with the class's existing "malformed payload
  shape-validates to empty" contract, so nothing new crashes or propagates.

Tests added (`state/GameStateRepositoryTest.kt`): duplicate grid cell,
duplicate manual unticks, three `isCompleteDeckPartition` violations
(duplicate-within-field, called/remaining overlap, doesn't sum to 90). Updated
`master state survives a repository recreation` to use a full valid 90-number
partition instead of 6 arbitrary numbers — it would otherwise now fail the new
invariant it wasn't designed to satisfy.

### L4 — swallowed IOException with no log signal

`state/GameStateRepository.kt`, `settings/SettingsRepository.kt`: every
`catch (_: IOException)` now does `Log.w(TAG, "...", e)` before falling back
(read → empty/defaults, write → silently keeps in-memory state, unchanged
behavior otherwise).
`app/build.gradle.kts`: added
`testOptions { unitTests { isReturnDefaultValues = true } }` — the stock
`android.jar` stub throws on any unmocked call (no Robolectric in this
project), so an untouched `Log.w` call would crash any unit test that
exercises the catch block. This is scoped to unit tests only, no production
behavior change.
Added `ThrowingDataStore` (`app/src/test/java/com/miti99/loto/`) — a
`DataStore<Preferences>` fake that throws `IOException` from both `data` and
`updateData`, per the task's note that file-backed DataStore can't be
exercised on Windows. Tests added in `GameStateRepositoryTest` and
`SettingsRepositoryTest` assert the read/write paths degrade instead of
throwing. (Log content itself isn't asserted — no mocking framework is set up
for `android.util.Log`; behavior is verified instead, matching this project's
existing test style.)

### L5 — stale autoRunning after a draw exhausts the deck

`state/MasterPanelViewModel.kt`: `drawNext()` now checks
`masterStore.state.value.remaining.isEmpty()` right after drawing and flips
`_autoRunning.value = false` immediately, instead of relying solely on the
ticker loop's next-tick top-of-loop check. Verified the `StateFlow` update
propagates through `combine(...).distinctUntilChanged().collectLatest{...}`
and cancels the in-flight `while(true)` loop within the same virtual-time
`advanceTimeBy` call (no extra tick needed) — confirmed by the updated test.
Also correct for a manual "Xổ số" draw that happens to be the one exhausting
the deck while auto-call was separately left on.
Left the ticker loop's own top-of-loop check in place as a harmless second
line of defense.

Tests: renamed/strengthened `auto-call stops when the deck is exhausted` →
`auto-call stops immediately when a draw exhausts the deck (L5)`, asserting
`autoRunning` is false right after the exhausting draw (previously only
asserted after a second tick, which is exactly the stale window L5 describes).
Added `manual drawNext exhausting the deck leaves autoRunning false (L5)` for
the literal "manual draw" wording.

### L6 — manualUnticks surviving a non-BOTH reset

`state/PlayerBoardViewModel.kt`: `onMasterState()`'s master-"Ván mới"
detection (`prev > 0 && len < prev`) now clears `manualUnticks` — and persists
that if it had entries — in every mode, not only inside the `BOTH`-gated
branch. `onCellClick` has no mode gate (any number in the master's `called[]`
can be manually unticked regardless of the currently displayed mode), so a
reset must invalidate that suppression state everywhere, not just when `BOTH`
happens to be selected at reset time. The `crossed`/`lastHandledIndex`/row-
tracker reset stays `BOTH`-only, unchanged — L6 only concerns
`manualUnticks`.

Test added: `manual unticks are cleared and persisted on a master Ván mới
outside both mode (L6)` — crosses/unticks a called number in `PLAYER` mode,
resets the master round, and reads `GameStateRepository.loadPlayerState()`
directly (sidesteps the unrelated, not-in-scope question of whether
`lastHandledIndex` also needs resetting on a non-`BOTH` shrink — L6 is
specifically about `manualUnticks`).

### L7 — deprecated Color.parseColor

`ui/ColorParsing.kt`: `android.graphics.Color.parseColor(this)` →
`this.toColorInt()` (androidx-core, already a dependency via
`androidx.core.ktx`). Same `IllegalArgumentException` catch/fallback
unchanged. Not unit-tested (no test existed before either — parsing depends
on the unmocked `android.graphics.Color` stub, which is untestable in a pure
JVM unit test regardless of which API calls it; the settings-layer regex is
what's actually covered, in `SettingsRepositoryTest`).

### PlayerCell.kt stateDescription → strings.xml

`res/values/strings.xml`: added `player_cell_number` ("Số %1$d"),
`player_cell_crossed_suffix` (", đã đánh dấu"),
`player_cell_waiting_suffix` (", đang chờ").
`ui/player/PlayerCell.kt`: resolves all three via `stringResource(...)`
unconditionally (composable-safe — no conditional `stringResource` calls) and
builds the same `stateDescription` string as before from those values. No
change in the rendered accessibility text.

### L10 — CI keystore cleanup

`.github/workflows/android-release.yml`: added a
`Remove decoded keystore` step (`if: always()`, `rm -f
"$GITHUB_WORKSPACE/keystore.p12"`) between the signed-build step and the
GitHub Release upload step, so the decoded `keystore.p12` is removed from the
runner workspace whether or not the build succeeds.

## Deferred / out of scope (unchanged from prior pass or explicitly excluded)

- L9 (`dataExtractionRules`) — product decision, not touched (task's NOT-IN-SCOPE list).
- M4's residual settings-loaded-signal race, M6 recomposition perf — explicitly out of scope per the task.

## Verification

- `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot" ./gradlew :app:testDebugUnitTest`
  → **BUILD SUCCESSFUL**. 118 tests across 13 suites, 0 failures/errors
  (includes all newly added L3/L4/L5/L6 regression tests).
- `JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot" ./gradlew :app:lintDebug`
  → **BUILD SUCCESSFUL**, 17 warnings — all pre-existing (AGP/Gradle
  dependency version notices, one unrelated string typo, two plurals
  candidates, one manifest `enableOnBackInvokedCallback` API-level notice).
  Verified none reference any file touched by this change and no
  `UnusedResources` finding for the three new strings.
- No gradle daemon force-killed; left to Gradle's own idle-timeout per the
  process-management rule.

## Files modified

- `android/app/src/main/java/com/miti99/loto/state/MasterPanelViewModel.kt`
- `android/app/src/main/java/com/miti99/loto/state/PlayerBoardViewModel.kt`
- `android/app/src/main/java/com/miti99/loto/state/GameStateRepository.kt`
- `android/app/src/main/java/com/miti99/loto/settings/SettingsRepository.kt`
- `android/app/src/main/java/com/miti99/loto/ui/ColorParsing.kt`
- `android/app/src/main/java/com/miti99/loto/ui/player/PlayerCell.kt`
- `android/app/src/main/res/values/strings.xml`
- `android/app/build.gradle.kts`
- `android/app/src/test/java/com/miti99/loto/ThrowingDataStore.kt` (new)
- `android/app/src/test/java/com/miti99/loto/state/GameStateRepositoryTest.kt`
- `android/app/src/test/java/com/miti99/loto/settings/SettingsRepositoryTest.kt`
- `android/app/src/test/java/com/miti99/loto/state/MasterPanelViewModelTest.kt`
- `android/app/src/test/java/com/miti99/loto/state/PlayerBoardViewModelTest.kt`
- `.github/workflows/android-release.yml`

## Unresolved questions

1. L2: is it worth adding a lifecycle-test harness (e.g. pulling in
   `androidx.lifecycle:lifecycle-viewmodel-testing` or Robolectric) so
   `onCleared()` behavior is directly testable, or does the code-level
   ownership argument above (both VMs share the exact same clearing
   condition as `MainActivity`'s existing guarded `release()`) suffice
   without one? Same gap already existed for L1.
2. L3: `isCompleteDeckPartition` treats *any* deviation from a full 1..90
   partition as corrupt (falls back to "no saved round"). This matches the
   review's literal ask, but is strict — e.g. it would also reject a
   hypothetical future variable-length deck. Flagging in case that's ever a
   real product direction, not just a theoretical one.

Status: DONE
Summary: Implemented and tested L2, L3, L4, L5, L6, L7, L10, and the
PlayerCell.kt strings.xml cleanup; all 118 unit tests and lint pass clean with
no new findings.
Concerns/Blockers: none blocking; two unresolved questions above are informational only.
