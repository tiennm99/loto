# Android regression fixes — H1, M2, M3, M4, backup .tmp, generate() double-tap guard

Source: `plans/reports/code-reviewer-260831-2314-precommit-review.md`
Scope: `android/` only. No commit made.

## H-1 — voicePlayer.release() kills audio on relaunch-in-process

Chose the **recreatable holder**, not the `cancel()` fallback: `cancel()` would
fully abandon L1's intent (never releasing the ExoPlayer on explicit exit,
reverting to pre-L1 behavior). The holder keeps L1's actual release on
"Thoát" while surviving a relaunch into the same cached process.

- New `android/app/src/main/java/com/miti99/loto/audio/VoicePlayerHolder.kt`:
  nullable-instance holder, `value` rebuilds via a factory after `release()`.
- `LotoApplication.kt`: `voicePlayer` now backed by a private
  `VoicePlayerHolder`; added `releaseVoicePlayer()`.
- `MainActivity.kt`: `onDestroy()` calls `releaseVoicePlayer()` instead of
  `voicePlayer.release()`.
- Testability: `LotoApplication`/`ExoVoicePlayer` need a real `Context`
  (no Robolectric in this project — `testOptions.unitTests` only sets
  `isReturnDefaultValues`), so the recreate-after-release contract is
  extracted into `VoicePlayerHolder` and unit-tested in isolation
  (`VoicePlayerHolderTest.kt`, 4 tests) against `FakeVoicePlayer`. The
  `LotoApplication`/`MainActivity` wiring itself stays unverified by a JVM
  test, same residual gap the reviewer already flagged — out of scope to
  close without adding Robolectric.

## M-2 — settings/loaded sibling-collector race

Replaced the two independent `stateIn` collectors PlayerBoardViewModel
depended on (`settingsState: StateFlow<Settings>` + a separate loaded
boolean) with one: `LotoApplication.settingsOrNull: StateFlow<Settings?>`
(null = not loaded), the sole collector over `settingsRepository.settingsFlow`.
`settingsState` (used by `MainActivity`, `MasterPanelViewModel`,
`SettingsViewModel`, `SettingsSheetTest`) is now *derived* from
`settingsOrNull` rather than an independent collection, removing the
top-level race for everyone, not just the reported call site.
`PlayerBoardViewModel` now takes `settingsOrNull` directly and reads
`settingsOrNull.value` for both "is it loaded" and "what is the mode" —
by construction the same snapshot, so they can never disagree.
`LotoViewModelFactory` updated to wire `app.settingsOrNull` (+
`app.settingsRepository.defaults` as the pre-load fallback for the rare
`onCellClick`-on-a-restored-card-before-settings-load path).

## M-3 — generate()/clearMarks() bypass the settings gate

- `PlayerUiState.loading` now folds in settings-loaded, not just restore:
  `publishLoading() = !restoreDone || settingsOrNull.value == null`,
  called from `restore()` and from the `init` combine's collector (which now
  combines `masterStore.state` with `settingsOrNull` directly — settings
  changes still re-trigger `onMasterState`, same as before). The existing UI
  gate (`state.loading` disables both buttons) now also covers the
  settings-unresolved window.
- Defense in depth, mirroring `restore()`'s own H1 guard: `generate()` and
  `clearMarks()` early-return (no-op) while `settingsOrNull.value == null`,
  so a caller that acts before the UI gate resolves can't corrupt the
  auto-cross cursor under a placeholder mode.
- Updated the M4-residual test in `PlayerBoardViewModelTest.kt` to actually
  exercise the ordering the review said it dodged: `masterStore.restore()`
  now resolves *before* `generate()` is tapped (previously the reverse),
  asserting the tap while gated is a no-op and the full replay lands once
  settings resolve and the user taps again. The other ordering test
  ("a settings arrival that lands before masterStore's restore...") is
  unchanged/kept.
- New: `clearMarks() is also a no-op while settings have not resolved yet`
  and `loading stays true while settings have not resolved... (M3)`.

## M-4 — non-IO settings read failure deadlocks the gate

`SettingsRepository.settingsFlow`'s `catch` now falls back to
`emptyPreferences()` (→ defaults) for **any** read failure, not only
`IOException` — the fail-open fix applies at the source now that
`LotoApplication.settingsOrNull` is the sole downstream collector (a fix
scoped only to the separate `loaded: Flow<Boolean>`, as the review's
snippet suggested, would no longer reach the actual production consumer
after the M2 unification). `loaded` is kept as a plain derived signal for
tests/other consumers; it no longer needs its own `.catch` since upstream
never throws.
`ThrowingDataStore` gained an optional `exception: Throwable` constructor
param (defaults to `IOException`, unchanged for existing callers) so tests
can exercise a non-IO failure. Added
`non-IO failure on read still falls back to defaults...` and
`loaded still flips true after a non-IO read failure` to
`SettingsRepositoryTest.kt`.

## Low — backup `.tmp` sibling

Added `datastore/loto_game_state.preferences_pb.tmp` exclusion alongside the
existing `.preferences_pb` one in both `data_extraction_rules.xml` and
`backup_rules.xml`.

## Low — generate() double-tap guard

Added `private var generating` to `PlayerBoardViewModel`: `generate()` no-ops
while a previous call's off-main compute hasn't resolved yet; also folded
into `restore()`'s clobber guard (`!generating`) per the review's "participates
in the restore guard" suggestion, since `grid` alone can't detect an
in-flight generation. Test: `a second rapid generate() tap while the first
is still computing is a no-op (L-a)`, asserting `FakeVoicePlayer.cancel()`
(new `cancelCount` counter) fires only once.

## Files Modified

- `android/app/src/main/java/com/miti99/loto/audio/VoicePlayerHolder.kt` (new)
- `android/app/src/main/java/com/miti99/loto/LotoApplication.kt`
- `android/app/src/main/java/com/miti99/loto/MainActivity.kt`
- `android/app/src/main/java/com/miti99/loto/settings/SettingsRepository.kt`
- `android/app/src/main/java/com/miti99/loto/state/LotoViewModelFactory.kt`
- `android/app/src/main/java/com/miti99/loto/state/PlayerBoardViewModel.kt`
- `android/app/src/main/res/xml/data_extraction_rules.xml`
- `android/app/src/main/res/xml/backup_rules.xml`
- `android/app/src/test/java/com/miti99/loto/audio/VoicePlayerHolderTest.kt` (new)
- `android/app/src/test/java/com/miti99/loto/audio/FakeVoicePlayer.kt`
- `android/app/src/test/java/com/miti99/loto/ThrowingDataStore.kt`
- `android/app/src/test/java/com/miti99/loto/settings/SettingsRepositoryTest.kt`
- `android/app/src/test/java/com/miti99/loto/state/PlayerBoardViewModelTest.kt`

Untouched by me but pre-existing modified/untracked in the working tree
(part of the earlier fix cycle under review, not this task): `build.gradle.kts`,
`AndroidManifest.xml`, `GameStateRepository.kt`, `GameStateRepositoryTest.kt`,
`MasterPanelViewModel.kt`, `MasterPanelViewModelTest.kt`, `MasterStore.kt`,
`MasterStoreTest.kt`, `SlowDataStore.kt`, various `ui/*` files, `strings.xml`.

## Tests Status

- `JAVA_HOME=jdk-21 ./gradlew :app:testDebugUnitTest` — **BUILD SUCCESSFUL**,
  129 tests, 0 failures, 0 errors (was 120; +9 new: 4 `VoicePlayerHolderTest`,
  3 `PlayerBoardViewModelTest`, 2 `SettingsRepositoryTest`).
- `JAVA_HOME=jdk-21 ./gradlew :app:lintDebug` — **BUILD SUCCESSFUL**, 0
  errors, 17 warnings (all pre-existing: dependency-version notices, one
  Vietnamese string typo heuristic, two plurals-candidate heuristics,
  `enableOnBackInvokedCallback` API-33 note — none touch the changed files).

## Issues Encountered

None. No file-ownership conflicts (single-agent task). `local.properties`
was not read.

## Unresolved Questions

1. H-1: the `LotoApplication`/`MainActivity` glue around `VoicePlayerHolder`
   (real `finish()` + relaunch through a real `Application`/`Context`) is
   still not exercised by any automated test — this project has no
   Robolectric dependency, and adding one was out of scope. The holder's
   own recreate-after-release contract is unit-tested; the wiring is
   equivalent to what the reviewer already called out as untested.
2. `fallbackSettings` for `PlayerBoardViewModel` (used only if `onCellClick`
   fires on a restored card before settings ever resolve) is wired to the
   real manifest-derived defaults in production
   (`app.settingsRepository.defaults`) and to a placeholder
   `Settings(voice = "")` in the constructor's own default parameter (never
   hit by existing tests, all of which pass a non-null-typed settings flow).
   Flagging this only so the placeholder default doesn't surprise a future
   reader who instantiates `PlayerBoardViewModel` with a null-capable flow
   without supplying `fallbackSettings`.

Status: DONE
Summary: Fixed H1 (recreatable voice player holder), M2 (single settingsOrNull source of truth), M3 (settings-loaded folded into loading + generate()/clearMarks() guards, test reordered to hit the previously-dodged case), M4 (fail-open on any settingsFlow read failure), plus the backup .tmp exclusion and a generate() double-tap/in-flight guard; all 129 unit tests and lint pass clean.
