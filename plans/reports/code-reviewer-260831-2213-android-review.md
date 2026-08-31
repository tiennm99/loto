# Android native app — code review (2026-08-31)

Scope: `android/app/src/**`, `android/*.gradle.kts`, `gradle.properties`,
`gradle/libs.versions.toml`, `AndroidManifest.xml`, `res/**`,
`.github/workflows/android-release.yml`. Read-only review; no source changed.

Baseline: ~2.9k LOC main + ~1.5k LOC unit tests + 4 instrumented tests.
Overall the port is disciplined — pure game logic is isolated and well
covered, DataStore validation is per-field, and the web-parity notes are
specific rather than decorative. Findings below are the gaps.

---

## Critical

None. No trust-boundary, data-loss-at-rest, or crash-on-launch defect found.
The app takes no network input, declares no `INTERNET` permission, exports
only the LAUNCHER activity, and the signing config reads env vars only.

---

## High

### H1 — Restore races a user action and silently clobbers a fresh card/round

`android/app/src/main/java/com/miti99/loto/state/PlayerBoardViewModel.kt:76-101`
`android/app/src/main/java/com/miti99/loto/state/MasterStore.kt:25-29`

`restore()` suspends on `dataStore.data.first()` while the UI is already
composed and interactive. During that window `uiState.grid == null`, so
`PlayerBoardScreen.kt:70-73` takes the **no-confirm** branch:
`if (state.grid != null) confirmGenerate = true else viewModel.generate()`.
A single tap on "Tạo bảng mới" during the load therefore generates + persists
a new card; `restore()` then resumes and unconditionally overwrites
`grid`/`crossed`/`manualUnticks` with the stale saved round, without
re-persisting. Result: the user's new card vanishes, and in-memory state now
disagrees with what is on disk until the next `persist()`.

Identical shape on the master side: `MasterPanelScreen.kt:86` calls
`viewModel.newGame()` with no confirmation while `hasGame == false` (which is
exactly the pre-restore state), and `MasterStore.restore()` then replaces the
deck with the persisted one.

Fix: make restore a no-op once the user has acted, and/or gate the buttons on
a load-complete flag.

```kotlin
// PlayerBoardViewModel
private var restored = false
private suspend fun restore() {
    val saved = repository.loadPlayerState()
    if (grid != null) { restored = true; return }   // user already generated
    ...
}
// MasterStore
suspend fun restore() {
    if (deck.remaining.isNotEmpty() || deck.called.isNotEmpty()) return
    ...
}
```

Cleaner alternative: add `val loading: Boolean` to `PlayerUiState` /
`MasterRoundState` exposure and render the empty state as non-interactive
until both restores settle.

### H2 — Tier-2 confetti is emitted inside the scrolling column, behind the dialog

`android/app/src/main/java/com/miti99/loto/ui/player/PlayerBoardScreen.kt:154-159`
`android/app/src/main/java/com/miti99/loto/ui/player/Confetti.kt:615-624`

`ConfettiOverlay` uses `Modifier.fillMaxSize()`, but it is emitted as a
sibling in `LotoAppRoot`'s `Column(Modifier.verticalScroll(...))`
(`LotoAppRoot.kt:64-100`). Under an unbounded max height, Compose's fill
modifier falls back to `constraints.minHeight`, so the Box wraps its content
instead of filling the screen — `size.height` used for `translationY`
(`Confetti.kt:640`) is the height of a single emoji, not the viewport.

Worse, `KinhDialog` is a real `Dialog` in its own window with the default dim
scrim, so the confetti is drawn *behind* the dim layer in the activity
window. The tier-2 celebration is effectively invisible and also injects
layout into the scroll content.

Fix: host the confetti in the same window as the dialog, or in a `Popup`:

```kotlin
if (state.showCongrats) {
    KinhDialog(congratsRow = state.congratsRow, onDismiss = { viewModel.dismissCongrats() })
    if (state.celebrationTier >= 2 && !reducedMotion) {
        Popup(properties = PopupProperties(clippingEnabled = false)) {
            Box(Modifier.fillMaxSize()) { ConfettiOverlay() }   // bounded by the popup window
        }
    }
}
```

(Or move `ConfettiOverlay()` inside `KinhDialog`'s `Dialog { }` content with
`usePlatformDefaultWidth = false` + `decorFitsSystemWindows = false`.)

---

## Medium

### M1 — RGB channel truncation drifts the empty-cell colour on every drag

`android/app/src/main/java/com/miti99/loto/ui/settings/EmptyCellColorPicker.kt:369-403`

```kotlin
val r = (color.red * 255).toInt()   // truncation, not rounding
```

`Color.red/green/blue` are Float. `toInt()` truncates, so a float round-trip
that lands at `126.9999` yields 126 for a stored `0x7F`. Because dragging the
R slider rebuilds the hex from the *derived* `g` and `b`
(`draft = hex(it, g, b)`), any truncation error on the untouched channels is
committed too — repeated drags walk the colour downward.

Fix: `val r = (color.red * 255).roundToInt()` (same for g/b).

### M2 — Every draw force-scrolls the page to the master hero, even in "both" mode

`android/app/src/main/java/com/miti99/loto/ui/master/MasterPanelScreen.kt:73-75`

```kotlin
LaunchedEffect(tickKey) { if (tickKey > 0 && lastCalled != null) heroRequester.bringIntoView() }
```

In `AppMode.BOTH` with auto-call at 1–10 s, the shared scroll is yanked away
from the player card on every draw — the player physically cannot keep their
board on screen. Suggest gating on mode:

```kotlin
LaunchedEffect(tickKey) {
    if (tickKey > 0 && lastCalled != null && settings.mode == AppMode.MASTER) {
        heroRequester.bringIntoView()
    }
}
```

Confirm against the web behaviour before changing (see Unresolved Q1).

### M3 — Toast copy and cell accessibility text are hardcoded Vietnamese in non-UI layers

`android/app/src/main/java/com/miti99/loto/state/PlayerBoardViewModel.kt:282` — `showToast("Chờ $waitNum")`
`android/app/src/main/java/com/miti99/loto/ui/player/PlayerCell.kt:563-569` — `"Số $num", ", đã đánh dấu", ", đang chờ"`

`res/values/strings.xml` already ships `toast_waiting` (`Chờ %1$d`),
`toast_dismiss`, `player_board_label`, `master_board_label` and `kinh_close`
— **all five are unreferenced** (verified by grep). The ViewModel emits
presentation copy instead, which also makes the string un-translatable and
couples the VM to display text.

Fix: put a `waitingNumber: Int?` in `PlayerUiState` and format with
`stringResource(R.string.toast_waiting, n)` at the call site; move the cell
`stateDescription` fragments into `strings.xml`. Delete whichever resources
stay genuinely unused (lint `UnusedResources` will otherwise keep flagging
them; `isShrinkResources` hides it only in release).

### M4 — Cold-start ordering can leave `settings.mode` at its default during the first master emission

`android/app/src/main/java/com/miti99/loto/LotoApplication.kt:58-61, 76-83`
`android/app/src/main/java/com/miti99/loto/state/PlayerBoardViewModel.kt:103-135`

`settingsState` is `stateIn(..., SharingStarted.Eagerly, defaults)` — its
initial value is `mode = PLAYER` until the settings DataStore read lands.
`onMasterState` reads `settings.value.mode` synchronously. Two independent
DataStore reads (settings vs. game state) race, so on a cold start with a
persisted `mode = BOTH`, the first master emission can be evaluated as
`PLAYER`: `PlayerAutoCross` then advances `lastHandledIndex` to
`called.size` with `changed = false` and no replay ever happens for that
history (`masterStore.state` will not re-emit until the next draw).

In practice the persisted `crossed` matrix already reflects the earlier
auto-crosses, so this is usually invisible — but it is load-bearing on the
"restore replays idempotently" claim in the code comment
(`PlayerBoardViewModel.kt:65-67`), which does not hold under this ordering.

Fix: make the ordering explicit rather than accidental — e.g. `settings.first
{ it !== defaults }`-style gating, a `Settings?` (null = not loaded) flow, or
`combine(masterStore.state, settings)` so a mode arrival re-triggers the
replay pass.

Also: `LotoApplication.onCreate` serialises two unrelated jobs in one
coroutine (`masterStore.restore()` then `settingsState.collect`). Split them
into two `launch` blocks so a slow/failed restore cannot stall voice-id sync.

### M5 — Card generation runs on the main thread with a 200-attempt rejection loop

`android/app/src/main/java/com/miti99/loto/game/CardGenerator.kt:147-155`
called synchronously from `PlayerBoardViewModel.generate()` (`:140`).

`pickFilledCols` can run `pickFilledColsOnce` up to 201 times; each pass
enumerates up to `C(9,5) = 126` combinations per row across 9 rows, with a
recursive `combinations()` that allocates a fresh `List` per node. Typical
runtime is fine, but the worst case is an unbounded-ish main-thread stall on
a low-end device. The column-quota invariant is correct (verified: `forced.size
<= 5` and `candidates.size > need` hold for every `rowsLeft`), so this is a
latency risk, not a correctness one.

Fix: `viewModelScope.launch(Dispatchers.Default) { val g = CardGenerator.generateGrid(); withContext(Dispatchers.Main) { ... } }`,
or memoise `combinations()` per `(candidates.size, need)` shape.

### M6 — Master panel recomposes ~200 non-lazy nodes on every draw

`android/app/src/main/java/com/miti99/loto/ui/master/MasterPanelScreen.kt:187-209, 270-325`

The called-history `FlowRow` (up to 90 tokens) plus the 11×9 `MasterBoard`
(99 cells, each a Box + Box + up to 2 Texts) are plain `Column`/`Row`
inside a `verticalScroll` — everything is composed and measured. Each draw
mutates `master` and recomposes the whole subtree. Combined with the
per-frame `withFrameNanos` loop in `AutoCountdown.kt:143-153`, auto-call at
1 s/number keeps the main thread busy.

Not a blocker for a fairground app on modern hardware, but worth measuring
on the API 24 target listed in `plans/todo.md`. Cheap mitigations: hoist
`MasterBoardLayout.BOARD` rows into `key(num)` blocks, or pass `isCalled` /
`isLast` as lambdas so the token colour changes without recomposing the cell.

---

## Low

### L1 — `ExoPlayer` is never released

`LotoApplication.kt:72-74`, `VoicePlayer.kt:98-100`. `VoicePlayerApi.release()`
has no call site anywhere in `main/`. The player is app-scoped over an
Application context, so no Activity leak — but audio renderers/codecs stay
resident for the process lifetime. Either wire it to a
`ProcessLifecycleOwner` `ON_STOP`+timeout, or drop the unused `release()` from
the interface so it does not read as a contract that is being honoured.

### L2 — Shared voice player + per-ViewModel `onCleared` cancel

`MasterPanelViewModel.kt:105-107`, `PlayerBoardViewModel.kt:238-240`. Both
VMs `cancel()` the *same* singleton on clear, and neither calls
`super.onCleared()`. Harmless today (both clear together), fragile if
scoping ever changes.

### L3 — Restore payload is range-validated but not shape-validated

`GameStateRepository.kt:107-113`. `parseNumberList` accepts duplicates and
does not check `called ∪ remaining == 1..90` or disjointness. A
partially-written/tampered `loto_game_state` file can restore a deck with a
repeated number, which `PlayerAutoCross` will then cross twice on different
cells. Local-only file, low impact; a `toSet().size == size` check plus a
disjointness assert would close it cheaply.

### L4 — `write()` swallows every `IOException` with no signal

`SettingsRepository.kt:107-113`, `GameStateRepository.kt:85-90`. Documented as
intentional ("web parity"), and correct for this product, but a persistently
failing store now degrades to "settings silently never save" with zero
telemetry. Consider at least a `Log.w` so device QA can spot it.

### L5 — `MasterPanelViewModel` leaves `autoRunning = true` after a manual draw exhausts the deck

`MasterPanelViewModel.kt:65-72, 85-91`. `_autoRunning` only flips false at the
*next* tick. Between exhaustion and that tick the UI shows "Dừng" while
`MasterPanelScreen.kt:88` has already hidden the button (guarded on
`remaining.isNotEmpty()`), so it is invisible — but `toggleAuto()` can no
longer reset it either. Add the `remaining.isEmpty()` check inside
`drawNext()`.

### L6 — `manualUnticks` survives a master "Ván mới" outside `BOTH` mode

`PlayerBoardViewModel.kt:107-119`. The reset branch is gated on
`mode == AppMode.BOTH`, so unticks recorded in a previous round persist. A
later switch to BOTH + "Xoá đánh dấu" replays with stale suppressions. Narrow
edge; clear `manualUnticks` on any shrink regardless of mode.

### L7 — `Color.parseColor` is deprecated

`ColorParsing.kt:9-13`. Works on all supported API levels; `androidx.core`
ships `String.toColorInt()` which is the non-deprecated equivalent and keeps
the same `IllegalArgumentException` contract.

### L8 — `launchMode="singleTask"` on the LAUNCHER activity

`AndroidManifest.xml:44`. No deep links or external `startActivity` targets
exist, so nothing needs it, and `singleTask` changes task-reparenting and
`onNewIntent` semantics. `standard` is the safer default unless the wrapper
era needed it for a reason worth recording.

### L9 — `allowBackup="true"` with no backup rules

`AndroidManifest.xml:36`. Both DataStores (settings + round state) are
included in cloud backup / D2D transfer by default. Nothing sensitive is
stored, so this is a data-hygiene note rather than a security finding — but
if a restored `loto_game_state` from another device is undesirable, exclude
it via `android:dataExtractionRules`.

### L10 — CI leaves the decoded keystore in the workspace

`.github/workflows/android-release.yml:66-70`. `keystore.p12` is written to
`$GITHUB_WORKSPACE` and never removed. The runner is ephemeral and the
release upload globs only `*.apk`/`*.aab`, so there is no actual exposure —
but an `if: always()` cleanup step is one line and removes the class of
mistake where a future step archives the workspace.

### L11 — `resourceConfigurations` is deprecated in AGP 8.13

`app/build.gradle.kts:22`. Replaced by `androidResources.localeFilters`.
Functional today; will warn/break on an AGP major bump.

---

## Edge cases checked and found sound

Recorded so a future audit does not re-litigate them:

- `CardGenerator` column-quota invariant holds for every `rowsLeft`
  (`forced.size <= 5`, `candidates.size > need`), so
  `picked.removeFirstOrNull() ?: 0` never silently drops a number.
- `PlayerAutoCross` cursor advances strictly in non-`BOTH` modes, so a
  `player → both` toggle does not dump back-history (`PlayerAutoCross.kt:40-43`).
- `MasterStore.publishAndSave` launches saves on `Dispatchers.Main.immediate`
  and each coroutine reaches `DataStore.edit` in dispatch order, so snapshots
  cannot be persisted out of order.
- `parseCrossed` returns null for a 0-length string, so an empty `crossed`
  matrix falls back to all-false rather than corrupting the grid.
- `AutoCountdown` deliberately uses `withFrameNanos` instead of a Compose
  animation so the digit still ticks under "remove animations"
  (`AutoCountdown.kt:139-153`) — correct, and the comment explains why.
- `KeepScreenOn` releases the flag in `onDispose` and is keyed on `remaining`,
  matching the `plans/todo.md` QA items.
- Manifest surface is minimal: `VIBRATE` only, `ACCESS_NETWORK_STATE`
  stripped, one exported activity with a LAUNCHER filter.

## Test-quality note

`InMemoryDataStore` (`app/src/test/java/com/miti99/loto/InMemoryDataStore.kt`)
is a `MutableStateFlow`, so `dataStore.data.first()` resolves *synchronously*
in unit tests. That is why H1 and M4 — both of which are purely about the
async gap between construction and the first DataStore emission — are
invisible to the existing suite despite `PlayerBoardViewModelTest` and
`GameStateRepositoryTest` looking thorough. The Windows rename limitation
that motivated the fake is real; the fix is a delay-injecting fake, not a
file-backed one:

```kotlin
class SlowDataStore(private val gate: CompletableDeferred<Unit>) : DataStore<Preferences> {
    override val data = flow { gate.await(); emitAll(state) }
    ...
}
```

Then assert that `generate()` before `gate.complete(Unit)` wins.

---

## Recommended actions (priority order)

1. Guard `PlayerBoardViewModel.restore()` and `MasterStore.restore()` against
   a concurrent user action, or disable the action buttons until restore
   settles (H1).
2. Re-host `ConfettiOverlay` in a `Popup`/dialog window so tier-2
   celebrations are actually visible and stop injecting scroll layout (H2).
3. `roundToInt()` in `EmptyCellColorPicker` (M1) — one-line, prevents
   user-visible colour drift.
4. Decide and document the `bringIntoView` policy for `BOTH` mode (M2).
5. Move the toast/`stateDescription` copy into `strings.xml` and delete or
   wire the five orphaned resources (M3).
6. Make the settings-vs-master-restore ordering explicit and split
   `LotoApplication.onCreate` into two launches (M4).
7. Move `generateGrid()` off the main thread (M5).
8. Add a delay-injecting DataStore fake and regression tests for 1 and 6.
9. Sweep the Low list opportunistically; L1/L5/L6 are each a few lines.

## Metrics

- Type coverage: 100% Kotlin, no `Any`/unchecked casts outside the one
  documented `@Suppress("UNCHECKED_CAST")` in `LotoViewModelFactory.kt:13`
  (standard `ViewModelProvider.Factory` idiom).
- Unit tests: 13 files / ~1.5k LOC over `game`, `audio`, `settings`, `state`,
  `ui`. Instrumented: 4 files. Pure game logic is genuinely covered (real
  assertions on invariants, not smoke calls). Gap is the async-boundary
  behaviour described above.
- Lint/typecheck/build: **not run** — no Android SDK configured in this
  review environment (`local.properties` was out of scope by instruction).
  Findings are static-analysis + code-reading only.

## Unresolved questions

1. **M2** — does `web/src/lib/MasterPanel.svelte` scroll the hero into view
   in `both` mode, or only in master-only mode? The web app is the stated
   behavioural spec, so the Kotlin side should match whatever it does. I did
   not read `web/` (out of the delegated file scope).
2. **H1** — is the pre-restore no-confirm branch intentional (an empty board
   genuinely has nothing to confirm), or was the `grid == null` check meant
   to be a "not loaded yet" check? The fix differs: guard the restore vs.
   gate the button.
3. **L8** — was `launchMode="singleTask"` carried over deliberately from the
   Capacitor wrapper manifest, or copied without a reason?
4. **M6** — has the master panel been profiled on the API 24 device named in
   `plans/todo.md`, or is that QA item still open? The `todo.md` list shows
   all device-QA boxes unchecked.
