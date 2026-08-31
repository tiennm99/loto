# Fix report: Android backup rules (L9) + settings-loaded signal (M4 residual)

Phase 2 of `plans/260831-2314-review-followups/plan.md`. Scope: two decisions
delegated by the user — exclude game-state DataStore from backup/transfer,
keep settings backed up; close the M4 residual race (master restore landing
before settings resolves) with an explicit settings-loaded signal.

## Item A — backup rules

Verified on-disk DataStore names from `LotoApplication.kt`'s
`preferencesDataStore(name = ...)` delegates: `loto_settings` and
`loto_game_state`. `androidx.datastore.preferences.preferencesDataStore`'s
default file location is `files/datastore/<name>.preferences_pb`, so the
excluded path is `datastore/loto_game_state.preferences_pb`.

- New `android/app/src/main/res/xml/data_extraction_rules.xml` — API 31+,
  excludes that path from both `<cloud-backup>` and `<device-transfer>`.
- New `android/app/src/main/res/xml/backup_rules.xml` — `full-backup-content`
  counterpart for API 24-30 (minSdk 24, so this path is live).
- `AndroidManifest.xml`: added `android:dataExtractionRules` and
  `android:fullBackupContent` to `<application>`; `android:allowBackup="true"`
  left unchanged. Settings DataStore is not mentioned anywhere in either XML,
  so it stays backed up/transferred by default.

## Item B — M4 residual: settings-loaded signal

Root cause (confirmed by reading `PlayerAutoCross.applyMasterCalls`): the
cursor advances to `called.size` on every non-`BOTH` pass even when nothing
changes. If `masterStore.restore()` lands with the full history before the
settings DataStore read resolves, the prior `combine()` fix still evaluates
that first master emission with `settings.value.mode` at its `PLAYER`
default, consumes the cursor to `called.size`, and the later real-mode
arrival finds `cursor >= called.size` — replay lost, permanently, since
`masterStore.state` doesn't re-emit until the next draw.

Fix — a loaded signal threaded end to end:

- `settings/SettingsRepository.kt`: new `val loaded: Flow<Boolean> =
  settingsFlow.map { true }.onStart { emit(false) }` — cold, derived purely
  from `settingsFlow`, mirrors the existing `settingsFlow`/`settingsState`
  pairing rather than adding a scope to the repository.
- `LotoApplication.kt`: new `val settingsLoaded: StateFlow<Boolean> =
  settingsRepository.loaded.stateIn(appScope, SharingStarted.Eagerly, false)`,
  next to `settingsState`.
- `state/PlayerBoardViewModel.kt`: new constructor param `settingsLoaded:
  StateFlow<Boolean> = MutableStateFlow(true)` (defaults to "already loaded"
  so every existing call site, including all prior tests, is unaffected).
  `init`'s collector changed from
  `combine(masterStore.state, settings) { master, _ -> master }.collect { onMasterState(it) }`
  to `combine(masterStore.state, settings, settingsLoaded) { master, _, loaded -> master to loaded }.collect { (master, loaded) -> if (loaded) onMasterState(master) }`.
  While `!loaded`, `onMasterState` is skipped entirely — `lastHandledIndex`
  stays untouched — so once `settingsLoaded` flips true, `combine`'s cached
  master snapshot is re-delivered and replayed under the real mode.
- `state/LotoViewModelFactory.kt`: passes `settingsLoaded = app.settingsLoaded`
  to the production `PlayerBoardViewModel`. This file isn't in the plan's
  Phase 2 file-ownership list but is load-bearing — without it the fix would
  only exist in test wiring, not in the shipped app. The direct task
  instructions ("Files you may modify: anything under android/") permit it.
- `state/MasterPanelViewModel.kt` / `state/MasterStore.kt`: unchanged. Neither
  consumes master emissions through the mode-gated cursor the task scoped
  this fix to; `MasterPanelViewModel` reads `settings.value` synchronously
  per-draw with no persistent cursor to corrupt.
- H1/loading behavior (restore vs. in-flight user action, from the prior fix
  pass) is untouched — `restore()`/`publish(loading = ...)` logic wasn't
  touched, and the H1 regression tests in `PlayerBoardViewModelTest` still
  pass unmodified.

## Tests

- `settings/SettingsRepositoryTest.kt`: new `loaded stays false until the
  first DataStore read resolves` — `SlowDataStore` + `CompletableDeferred`
  gate, collects `loaded` into a list via `backgroundScope`, asserts
  `[false]` before the gate opens and `[false, true]` after.
- `state/PlayerBoardViewModelTest.kt`: new `master restore landing with full
  history before settings resolves still replays under the real mode (M4
  residual)` — builds a real `SettingsRepository` over a gated
  `SlowDataStore` (mode persisted as `BOTH` ahead of time on the ungated
  backing store), constructs the `ViewModel` with `settingsFlow`/`loaded`
  `stateIn`'d the same way `LotoApplication` does, resolves `masterStore`
  with the full 90-number history *before* completing the settings gate,
  asserts nothing crossed yet, then completes the gate and asserts the full
  board replays (`rowComplete` all true).
- **Verified fail-without-fix**: temporarily reverted the `init` collector to
  the pre-fix `combine(...) { master, _, _ -> master }.collect { onMasterState(it) }`
  (ignoring `loaded`) and reran `:app:testDebugUnitTest --tests
  "...PlayerBoardViewModelTest"` — exactly the new regression test failed (18
  others passed), confirming it actually exercises the closed gap. Reverted
  back to the fix immediately after.
- Fixed a pre-existing opt-in warning while touching `SettingsRepositoryTest.kt`
  (`@OptIn(ExperimentalCoroutinesApi::class)` on the class, matching the
  pattern already used in `PlayerBoardViewModelTest.kt`) since the new test
  needed `runCurrent`.

## Verification

```
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.10.7-hotspot" ./gradlew :app:testDebugUnitTest :app:lintDebug
```
`BUILD SUCCESSFUL`, all unit tests pass (including the two new ones), lint
clean. Not committed, per instructions.

## Files changed

- `android/app/src/main/AndroidManifest.xml`
- `android/app/src/main/res/xml/data_extraction_rules.xml` (new)
- `android/app/src/main/res/xml/backup_rules.xml` (new)
- `android/app/src/main/java/com/miti99/loto/settings/SettingsRepository.kt`
- `android/app/src/main/java/com/miti99/loto/LotoApplication.kt`
- `android/app/src/main/java/com/miti99/loto/state/PlayerBoardViewModel.kt`
- `android/app/src/main/java/com/miti99/loto/state/LotoViewModelFactory.kt`
- `android/app/src/test/java/com/miti99/loto/settings/SettingsRepositoryTest.kt`
- `android/app/src/test/java/com/miti99/loto/state/PlayerBoardViewModelTest.kt`

Note: `git status` also shows other android files modified/new
(`build.gradle.kts`, `MainActivity.kt`, `MasterStore.kt`,
`GameStateRepository.kt`, `MasterPanelViewModel.kt`, `ColorParsing.kt`, UI
files, `MasterStoreTest.kt`, `ThrowingDataStore.kt`, etc.) — these are
pre-existing uncommitted changes from the earlier `fullstack-developer-260831-2226`
fix pass, not touched by this task.

## Unresolved questions

None.

Status: DONE
Summary: Added API-31+ and pre-31 backup-exclusion rules for the game-state DataStore only (settings stay backed up), and closed the M4 residual race with a `SettingsRepository.loaded`/`LotoApplication.settingsLoaded` signal that gates `PlayerBoardViewModel`'s master-emission consumption until settings have really resolved; new regression test verified to fail without the fix. `:app:testDebugUnitTest` and `:app:lintDebug` both green.
Concerns/Blockers: none.
