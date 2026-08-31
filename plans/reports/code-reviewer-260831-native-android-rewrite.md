# Code Review — Native Android Rewrite (Kotlin + Compose)

Reviewer pass over `android/app/src/**` (~5,570 LOC Kotlin, 38 main + 15 test files),
`android/app/build.gradle.kts`, `android/gradle/libs.versions.toml`, `AndroidManifest.xml`,
and the CI rewire in `.github/`. Behavioral spec: `web/src/lib/`.

Advisory only — no code or plan files were modified.

## Scope

- Game logic parity: `CardGenerator` / `PlayerCard` / `DrawDeck` / `PlayerAutoCross` vs
  `game-logic.js`, `master-store.svelte.js`, `player-auto-cross.js`
- Business logic: `PlayerBoardViewModel` / `MasterPanelViewModel` vs
  `PlayerBoard.svelte` / `MasterPanel.svelte`
- Public contracts: applicationId, versionCode/Name, signing env, release secrets, Play track, assets
- Concurrency/lifecycle: DataStore, `viewModelScope` collectors, StateFlow conflation, ExoPlayer
- Release-build risk: R8 / `bundleRelease`

## Overall Assessment

The pure game-logic port is genuinely faithful — I walked
`generateGrid`/`pickFilledCols`/`pickFilledColsOnce`/`randomNumbersInCol`/`combinations`/
`hasThreeInARow`, `findUncrossedCell`/`isRowComplete`/`getWaitingNumber`, `DrawDeck`, and
`applyMasterCalls` statement-by-statement against the JS and found no algorithmic divergence.
The settings contract (10 fields, defaults, per-field validation with independent fallback)
matches `DEFAULT_SETTINGS` exactly. The manifest permission posture is correct. The test suite
is behavioral, not phantom.

The defects cluster in the **layer the JS spec expressed as reactive `$effect`s** and in
**platform error/lifecycle boundaries the web got for free**. The ViewModels replaced Svelte's
automatic dependency tracking with explicit call sites, and two call sites were missed. Separately,
the persistence layer has no error handling at all, despite KDoc claiming otherwise.

## Critical Issues

None. No trust-boundary defect, no data exposure, no auth surface, no breaking public-contract change.

## High Priority

### H1 — No error handling on any DataStore read or write (major)

`GameStateRepository.kt:41,58,64,70`, `SettingsRepository.kt:47,68-100`,
`LotoApplication.kt:70-75`

Every DataStore call is unguarded. `dataStore.data` throws `CorruptionException` (an `IOException`)
when the preferences file is unreadable, and `edit {}` throws on write failure. There is no
`corruptionHandler` on either `preferencesDataStore` delegate and no `.catch {}` on either flow.

Propagation paths, all of which terminate in an uncaught exception:

- `LotoApplication.onCreate` → `appScope.launch { masterStore.restore() }` →
  `dataStore.data.first()`. `appScope` uses a bare `SupervisorJob` with no
  `CoroutineExceptionHandler`, so the throw reaches the thread's default handler.
- `settingsState = settingsFlow.stateIn(appScope, Eagerly, defaults)` — an upstream throw
  fails the sharing coroutine identically.
- `persist()` in `PlayerBoardViewModel.kt:288` and `publishAndSave()` in `MasterStore.kt:52`
  launch bare `edit {}` calls.

Failure scenario: a corrupt `loto_game_state.preferences_pb` (interrupted write, filesystem
damage, restored-from-backup mismatch) crashes the app on every launch. The user cannot recover
without clearing app data.

This is also a **false doc claim**. `GameStateRepository`'s KDoc states: *"every payload is
shape-validated on load and falls back to 'no saved round' rather than crashing (the web's
safeParse posture)."* That is true for shape problems only. The web's actual posture wraps
**every** localStorage touch in `try/catch` — `saveGrid`, `loadGrid`, `loadMaster`,
`saveMaster`, `loadSettings`, `saveSettings` all swallow and fall back. The port kept the
comment and dropped the guarantee.

Fix: pass `ReplaceFileCorruptionHandler { emptyPreferences() }` to both delegates, add
`.catch { if (it is IOException) emit(emptyPreferences()) else throw it }` upstream of the
`.map`, and wrap the write paths.

### H2 — `generate()` and `clearMarks()` never run chờ/kinh detection (major)

`PlayerBoardViewModel.kt:141-176` (`generate`), `:178-208` (`clearMarks`)

Both methods clear `celebratedRows` / `notifiedWaitingRows`, replay the master's `called[]` onto
the board (in both mode), then call `persist()` and `publish()` — but never `detectRowEvents()`.

In the web this is not a decision, it is a consequence: the row-detection `$effect` in
`PlayerBoard.svelte` depends on `crossed`, `celebratedRows`, and `notifiedWaitingRows`, so
`handleGenerate` / `handleClear` mutating all three re-runs it automatically, after the
`showCongrats = false` assignment at the end of the handler.

Failure scenario: both mode, master has called 40 numbers, player taps "Xoá đánh dấu". The
replay re-crosses everything and completes a row. Web: Kinh modal + `kinh` clip. Android:
silent — no modal, no voice, no toast. Because the trackers were just cleared, the row is not
in `celebratedRows`, so the announcement fires **late**, on the next master draw. If the deck
is exhausted or the host stops calling, it never fires at all. The same applies to "Tạo bảng
mới" mid-game, where the replay can land a row into chờ with no toast and no `cho` clip.

The existing test `clearMarks in both mode re-crosses all called numbers (unticks wiped)`
(`PlayerBoardViewModelTest.kt:247`) asserts only `crossed[0][col]` and therefore does not
catch this.

Fix: call `detectRowEvents()` before `publish(showCongrats = false)` in both methods — or,
better, fold detection into `publish()` so no future call site can forget it.

### H3 — Auto-call ticker has no lifecycle gating (major)

`MasterPanelViewModel.kt:43-67`

The ticker runs in `viewModelScope`, which is alive for the ViewModel's whole lifetime and
independent of `Lifecycle.State`. Backgrounding the app during auto-call keeps drawing numbers
and playing MP3s through ExoPlayer.

The web behaves differently and this is load-bearing, not incidental: a hidden tab's
`setInterval` is throttled (and a backgrounded Capacitor WebView is paused outright), and
`MasterPanel.svelte`'s wake-lock effect explicitly notes *"Android drops the lock whenever the
page hides, so a backgrounded app holds nothing."* The Compose `KeepScreenOn` helper
(`KeepScreenOn.kt:22`) only releases on composition dispose, not on `onStop`.

Failure scenario: host backgrounds the app to answer a message; the deck keeps draining and
Vietnamese number clips keep playing over their phone call / music. On return, 15 numbers have
been called that nobody at the table heard.

Fix: gate the ticker with `repeatOnLifecycle(Lifecycle.State.STARTED)` at the collection site,
or expose a paused flag the Activity drives from `onStop`/`onStart`.

### H4 — Per-frame DataStore commits from the colour-picker sliders (major)

`EmptyCellColorPicker.kt:126-129`, `SettingsViewModel.kt:34-36`

The three RGB `Slider`s use `valueRange = 0f..255f` with **no `steps`**, so `onValueChange`
fires continuously during a drag. Each call goes
`onPick → setEmptyCellColor → viewModelScope.launch { dataStore.edit {} }` — a full
preferences-file rewrite plus fsync, per touch frame. A single full-range drag can issue
~250 commits; three channels compound it. Each commit also re-emits `settingsState`,
recomposing every settings consumer.

The web does the same thing against `localStorage`, where a synchronous in-memory write is
cheap. Ported verbatim to DataStore it becomes real disk I/O and write amplification.

The `autoCallSpeed` slider (`SettingsSheet.kt:165-171`) is fine — `steps = 8` snaps
`onValueChange` to 10 tick values.

Fix: hold the colour in local Compose state during the drag and commit on
`onValueChangeFinished`.

### H5 — R8 is never exercised before the release tag (major)

`.github/workflows/ci.yml:129-133`, `.github/workflows/android-release.yml:57-70`,
`android/app/build.gradle.kts:64-75`

`isMinifyEnabled = true` and `isShrinkResources = true` are new in this rewrite, and
`proguard-rules.pro` is empty (comments only). CI's `android-debug` job runs
`:app:lint :app:test :app:assembleDebug` — no minified variant. The first R8 run in the
project's life happens in `android-release.yml`, on a tag, after the keystore has been decoded.

I found no reflection in the app itself: `LotoViewModelFactory` uses `isAssignableFrom` for
dispatch but constructs each ViewModel explicitly, `org.json` parsing in `VoiceCatalog` is
non-reflective, and Compose/Media3/DataStore ship consumer rules. So `bundleRelease` will
probably pass. "Probably" is the problem — a failure here is discovered at tag time, and the
tag has already been pushed.

Note `isShrinkResources` only touches `res/`, not `assets/`, so the 184 MP3s are safe.

Fix: add `:app:assembleRelease` to the `android-debug` CI job. Without `LOTO_KEYSTORE_PATH`
the `signingConfig` is skipped and AGP produces an unsigned release APK — full R8 coverage,
no secrets needed.

## Medium Priority

### M1 — `lastHandledIndex` is never clamped when `called[]` shrinks

`PlayerAutoCross.kt:38-40`, `PlayerBoardViewModel.kt:113-125`

The ván-mới reset in `onMasterState` triggers only on the exact transition
`prev > 0 && len == 0`. If the observed `called` size drops from N to a non-zero value,
neither branch recovers: the reset is skipped, and `applyMasterCalls` short-circuits on
`lastHandledIndex >= called.size` **without resetting the cursor**. Auto-cross is then dead
until the new round's `called` exceeds N, and the previous round's marks are never cleared.

I could not construct a UI-reachable trigger. `StateFlow` + `Dispatchers.Main.immediate`
resumes the collector inline from the emitting main-thread call, and the two mutation sites
(`newGame` and `drawNext`) are always separated by a touch event or a `delay`. But the code
path is demonstrably live: `PlayerBoardViewModelTest.kt:161-173` conflates 31 emissions into
one under `StandardTestDispatcher` and only passes because the cursor happens to be 0. The
`prevCalledLen`-based reset is a strictly weaker signal than the web's, where Svelte effects
re-read `masterState.called` on every flush.

Fix: treat any `called.size < lastHandledIndex` as a round reset (`lastHandledIndex = 0`)
inside `applyMasterCalls`, and key the ván-mới clear on `len < prev` rather than `len == 0`.

Related inherited limitation (present in the web too, so not a regression): across a process
restart, `prevCalledLen` starts at 0, so a ván-mới that happened while the player VM was dead
is never detected and stale marks survive into the new round.

### M2 — ExoPlayer is never released

`LotoApplication.kt:63-65`, `VoicePlayer.kt:97-99`

`VoicePlayerApi.release()` has zero call sites (grep-verified across `app/src/`). The
Application-scoped `ExoVoicePlayer` holds its playback thread, renderers, and — once prepared —
a `MediaCodec`/`AudioTrack` for the entire process lifetime. Both `onCleared()` overrides call
`cancel()`, not `release()`, which is correct for a shared instance but leaves nothing that
ever releases it.

Bounded by process death, so not a growing leak, but `release()` is dead API surface that reads
as if cleanup were handled.

### M3 — `AutoCountdown` freezes at "1" when system animations are off

`AutoCountdown.kt:49-63`

`progress.animateTo(tween(...))` is a Compose animation, and Compose scales animation durations
by the system `MotionDurationScale` sourced from `ANIMATOR_DURATION_SCALE`. With "Remove
animations" enabled — the exact condition `rememberReducedMotion()` detects — the tween
completes instantly, `progress.value` jumps to 1f, and `secondsRemaining` clamps to 1 for the
whole interval.

The inline comment claims the opposite: *"Reduced motion keeps a static full ring; the number
still ticks."* It does not tick. Fix: drive `secondsRemaining` from a `withFrameNanos`/wall-clock
countdown independent of the animation clock, or from the ViewModel.

### M4 — `rememberReducedMotion()` reads the setting once and never re-reads

`ReducedMotion.kt:15-24`

`remember {}` with no key caches for the composition's lifetime. Toggling "Remove animations"
in system settings and returning to the app leaves stale behaviour until the process restarts.
The web calls `matchMedia(...).matches` freshly at each use site. Low impact; worth a
`LocalLifecycleOwner`-keyed re-read.

### M5 — Manifest-parse asset I/O on the main thread at startup

`LotoApplication.kt:39` → `VoiceCatalog.load()` (`VoiceCatalog.kt:60-70`)

`voices` is resolved through the `settingsRepository` → `settingsState` lazy chain, which is
first touched from `appScope` (`Dispatchers.Main.immediate`). `assets.open(...).readText()` plus
`JSONObject` parsing runs on the main thread during `onCreate`. The manifest is tiny so this is
sub-millisecond, but it is a StrictMode violation and an easy thing to regress if the manifest
grows.

## Low Priority

### L1 — `ignoreAssetsPattern` is a denylist over an externally-owned directory

`app/build.gradle.kts:36-45`

`assets.srcDir("../../web/static")` mounts the whole web static tree; the exclusion pattern
names `icons` and `manifest.webmanifest` explicitly. I verified `web/static/` currently holds
exactly `audio/`, `icons/`, and `manifest.webmanifest`, so today the APK gets audio only —
matching the stated contract. But any file added to `web/static` by web-side work ships into
the APK silently. A comment on the web side (or an allowlist-shaped guard) would make the
coupling visible to whoever adds the next file.

### L2 — `grep -vx` treats the tag name as a regex

`.github/workflows/android-release.yml:33`

`grep -vx "${GITHUB_REF_NAME}"` — the dots in `v0.2.0` are regex wildcards, so `v0x2y0` would
also be excluded. Harmless with the current tag set. Use `grep -Fvx`.

### L3 — Empty `KEYSTORE_BASE64` produces a confusing failure

`.github/workflows/android-release.yml:61-64`

If the secret is unset, `printf '%s' "" | base64 --decode` writes a zero-byte `keystore.p12`
and exits 0. `LOTO_KEYSTORE_PATH` is still set, so `signingConfig` is applied and Gradle fails
deep in signing with a keystore-format error. A `[ -n "$KEYSTORE_BASE64" ] || { echo "::error::..."; exit 1; }`
guard makes the diagnosis immediate. Also, `keystore.p12` is left in the workspace with no
cleanup step — no leak today (the release upload globs only `android/app/build/outputs/**`),
but worth a `rm -f` in an `always()` step.

### L4 — `allowBackup="true"` with no backup rules

`AndroidManifest.xml:39`

Both DataStore files are included in Google auto-backup. Contents are game state and UI
preferences — no PII, no secrets — so this is fine as a product decision. Flagging only because
it is implicit; a `dataExtractionRules` entry would make it deliberate.

### L5 — `LaunchedEffect(tickKey)` re-scrolls the hero after a config change

`MasterPanelScreen.kt:71-73`

`tickKey` lives in the Activity-scoped ViewModel and survives rotation, so the effect re-fires
on recomposition and yanks the hero into view. The web guards this with a `scrollOnNextDraw`
user-interaction flag. The `tickKey > 0` guard correctly handles the restore-on-launch case
(cold start has `tickKey == 0`), so this is rotation-only and cosmetic.

## Verified Contracts (no action)

- `applicationId = "com.miti99.loto"`, `versionCode = 7`, `versionName = "0.2.0"` — correct bump from 6 / 0.1.2
- Signing env: `LOTO_KEYSTORE_PATH` / `LOTO_KEYSTORE_PASSWORD` / `LOTO_KEY_ALIAS` / `LOTO_KEY_PASSWORD`, consistent between `build.gradle.kts` and the workflow
- Release secrets unchanged: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`, `PLAY_SERVICE_ACCOUNT_JSON`
- Play: `packageName: com.miti99.loto`, `tracks: alpha`, `status: completed`
- Assets: `web/static/audio/**` only; `icons` and `manifest.webmanifest` excluded — verified against the actual directory listing
- Permissions: `VIBRATE` only; no `INTERNET`; `ACCESS_NETWORK_STATE` stripped via `tools:node="remove"`
- Settings contract: all 10 fields, defaults, and validation rules match `DEFAULT_SETTINGS`. The
  web-only `masterMode` → `mode: "both"` migration is correctly documented as out of scope.
- Voice semantics: `speak()` replaces the playlist (cancellation), and
  `waitingClips(n, voiceWaitingNumber, modeIsBoth)` reproduces
  `voiceWaitingNumber && mode !== "both"` exactly.
- Announce gating: `voiceEnabledPlayer || (voiceEnabledMaster && mode == BOTH)` matches
  `PlayerBoard.svelte` verbatim.
- `celebrationTier`: Kotlin's `celebratedRows.size >= 2 || hasActiveCho` is equivalent to the
  web's `size >= 2 || (size >= 1 && hasActiveCho)` — `add(i)` precedes the check in both, so
  `size >= 1` is always true there.
- Auto-call teardown/re-arm: `combine(...).distinctUntilChanged().collectLatest` reproduces the
  web's single `$effect` over `(autoRunning, autoCallEnabled, autoCallSpeed)`, including the
  disable-mid-run auto-stop and the deck-exhausted stop.
- Wake lock: `shouldKeepScreenOn(mode, remaining) = mode != PLAYER && remaining > 0` matches the
  web's `setWakeLock(remaining.length > 0)` inside a panel that only mounts when `mode != player`.
- `MasterBoardLayout.BOARD` matches `buildBoard()` cell-for-cell.
- Input validation is correctly two-layered: bounded at the UI (`coerceIn`, `valueRange`) **and**
  re-validated per-field on read in `SettingsRepository`.

## Positive Observations (risk calibration)

Recorded because they narrow where remaining risk can hide:

- The game-logic port is line-for-line faithful, including non-obvious details: the Fisher-Yates
  loop bounds, `sorted()` after `take(num)`, the `combinations` ordering, and the 200-attempt
  rejection-sampling structure. RNG differs by necessity but is injectable and seeded in tests.
- Tests assert behaviour, not execution: seeded determinism, 1,000-card invariant sweeps,
  process-death round-trips, corrupt-payload fallbacks, and cross-restart manual-untick
  suppression. No phantom tests found.
- No parallel reimplementation of existing utilities, no `any`-equivalent widening, no lint
  suppression beyond the one necessary `@Suppress("UNCHECKED_CAST")` in the ViewModel factory,
  and no scope drift. Manual DI is proportionate to the app's size.

## Recommended Actions

1. **H1** — add corruption handlers and I/O guards to both DataStore surfaces; correct the
   `GameStateRepository` KDoc.
2. **H2** — call `detectRowEvents()` from `generate()` and `clearMarks()`, and extend
   `PlayerBoardViewModelTest` to assert the announcement, not just the crossed matrix.
3. **H5** — add `:app:assembleRelease` to the CI Android job (unsigned, no secrets) so R8 is
   proven before tagging. Cheapest fix on this list; do it first.
4. **H3** — gate the auto-call ticker on `Lifecycle.State.STARTED`.
5. **H4** — commit the colour picker on `onValueChangeFinished`.
6. **M1** — clamp `lastHandledIndex` on shrink and key the ván-mới clear on `len < prev`.
7. **M3** — decouple the countdown number from the animation clock; fix the contradicted comment.
8. **M2, M4, M5, L1–L5** — batch as follow-up.

## Metrics

- Type coverage: 100% (Kotlin, no platform-type leakage in reviewed code; one justified
  `@Suppress("UNCHECKED_CAST")` at `LotoViewModelFactory.kt:16`)
- Test coverage: 101 unit + 9 instrumentation tests, reported green (not re-run in this pass)
- Lint: reported green (not re-run); no new suppressions introduced
- Release-build coverage: **0%** — `bundleRelease` / R8 unexercised in CI (H5)

## Unresolved Questions

1. Is H3 intended? A caller app that keeps announcing while backgrounded is arguably a feature,
   but it diverges from the web and from the wrapper's behaviour. If intentional, it needs a
   foreground service (Play policy) and a comment; if not, it needs lifecycle gating.
2. Was `isMinifyEnabled = true` a deliberate change from the Capacitor-era config, or an AGP
   template default that came along with the rewrite? It shifts release-build risk without a
   corresponding CI gate.
3. H2 and M1 both stem from replacing Svelte's automatic dependency tracking with explicit
   call sites. Is it worth folding detection into `publish()` so the class has one funnel
   instead of five call sites to keep in sync?
