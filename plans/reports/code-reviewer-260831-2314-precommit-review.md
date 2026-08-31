# Pre-commit review — accumulated review-fix cycle (web + android + CI)

Reviewer: code-reviewer | Date: 2026-08-31 | Mode: static reading only (no builds/tests run)
Scope: full uncommitted working tree (42 modified + 14 new files), plan
`plans/260831-2314-review-followups/plan.md` and the six fix reports.

## Verdict

Plan acceptance criteria are substantially met, but three of them are met only
under an ordering assumption or only on the path that was tested. One **High**
regression unrelated to this plan (introduced by the earlier L1/L2 audio fix) is
a launch-blocker for the announcer.

| Criterion | Verdict |
|---|---|
| Multi-tab newest-claim-wins, exactly one winner | Partially met — symmetric cases correct; a losing claimer is never told it lost (M-1) |
| Game-state-only backup exclusion, settings kept | Met — path verified against DataStore's default location |
| Settings-loaded gate never advances the cursor early | Met for `onMasterState`; not for `generate()`/`clearMarks()` (M-3) |
| Gate cannot deadlock the ViewModel | Met for the `IOException` path; not for a non-IO failure (M-4) |

---

## High

### H-1 — `voicePlayer.release()` permanently kills audio after "Thoát" + relaunch in the same process

`android/app/src/main/java/com/miti99/loto/MainActivity.kt:58-59`
`android/app/src/main/java/com/miti99/loto/LotoApplication.kt:85-87`

`onDestroy()` releases the **app-scoped** ExoPlayer when `isFinishing`. The
"Thoát" dialog calls `finish()` (MainActivity.kt:104), but `finish()` does not
end the process — Android keeps it cached. On relaunch from the launcher/Recents
the same `LotoApplication` instance is reused, and `voicePlayer` is a `by lazy`
singleton that is never rebuilt, so `LotoViewModelFactory` hands both ViewModels
the already-released `ExoVoicePlayer` (`VoicePlayer.kt:98-100` → `player.release()`
is terminal, per the interface's own doc: "the instance is unusable afterwards").
Every subsequent `speak()` either throws or silently drops — the announcer is dead
until the OS reclaims the process. This is a new failure mode: before this change
the VMs only called `cancel()`.

Fix (pick one):

```kotlin
// LotoApplication.kt — make it recreatable instead of a one-shot lazy
private var _voicePlayer: VoicePlayerApi? = null
val voicePlayer: VoicePlayerApi
    get() = _voicePlayer ?: ExoVoicePlayer(this)
        .also { it.voiceId = VoiceCatalog.defaultVoiceId(voices); _voicePlayer = it }
fun releaseVoicePlayer() { _voicePlayer?.release(); _voicePlayer = null }
```

then call `releaseVoicePlayer()` from `MainActivity.onDestroy()`. Alternatively
drop `release()` entirely and call `voicePlayer.cancel()` there — the player is a
tiny resource and the process teardown reclaims it.

Note the release path is untested: no unit test covers `onDestroy`, and
`FakeVoicePlayer` has no "released" state to assert against.

---

## Medium

### M-1 — Newest-claim-wins can now leave **two** tabs active (a losing claimer is never corrected)

`web/src/lib/active-tab.svelte.js:57-66`

The protocol is one-way: a tab that receives a *losing* peer claim just ignores
it. The peer therefore never learns it lost and stays active locally
(`claimActiveTab()` sets `inactive = false` before broadcasting, line 83). The
old bug (both tabs frozen) has been traded for both tabs *active* — double
auto-call intervals, double `localStorage` writers, overlapping audio, i.e. the
exact thing the module exists to prevent.

Reachable paths:

1. The repo's own new test demonstrates it —
   `web/src/lib/active-tab.test.js` "reclaim updates myClaimTs so a stale peer
   claim can't re-freeze the reclaimed tab": after `tabB.claimActiveTab()` with a
   stale ts, `tabA.inactive === false` **and** `tabB.inactive === false`. The test
   only asserts A.
2. Wall-clock stepping backwards (NTP correction / user changing device time on a
   fairground tablet) makes any new claim "older" than the incumbent's.
3. Equal-millisecond opens where delivery is asymmetric: a tab created *after*
   the peer's `postMessage` never receives that claim (real `BroadcastChannel`
   does not replay), so only one side evaluates the tie-break; if the surviving
   evaluator holds the greater id it ignores the peer and both stay active.
4. Mixed-version tabs (now more likely, since `registerType: "prompt"` keeps old
   tabs alive): a pre-fix tab broadcasts `{type, id}` with no `ts`, so
   `peerTs > myClaimTs` and `peerTs === myClaimTs` are both `false` → the new tab
   never freezes.

Fix — make the winner echo, and treat a missing `ts` as winning:

```js
bc.onmessage = (e) => {
  if (e.data?.type !== "claim" || e.data.id === TAB_ID) return;
  const peerTs = typeof e.data.ts === "number" ? e.data.ts : Infinity; // legacy peer wins
  const peerWins =
    peerTs > myClaimTs || (peerTs === myClaimTs && e.data.id > TAB_ID);
  if (peerWins) {
    activeTab.inactive = true;
  } else if (!activeTab.inactive && bc) {
    // Tell the loser it lost — same (ts,id), no refresh, so this terminates:
    // the peer freezes and a frozen tab never echoes.
    bc.postMessage({ type: "claim", id: TAB_ID, ts: myClaimTs });
  }
};
```

Add a test for the asymmetric case (only one channel open at post time) asserting
exactly one `inactive === false`.

### M-2 — The settings-loaded gate relies on undocumented emission ordering between two independent collectors

`android/app/src/main/java/com/miti99/loto/LotoApplication.kt:58-74`
`android/app/src/main/java/com/miti99/loto/state/PlayerBoardViewModel.kt:126-127, 192`

`settingsState` and `settingsLoaded` are two separate `stateIn` collectors over
two separate `settingsRepository.settingsFlow` chains. `onMasterState` reads
`settings.value` directly (line 192), not the combine snapshot. Nothing guarantees
that the `settingsState` collector applies the loaded value *before* the
`settingsLoaded` collector flips to `true` — both are resumed as independent tasks
on `Dispatchers.Main.immediate`. If `loaded` wins the race, the gate opens while
`settings.value` still returns the PLAYER default and the M4 bug reappears exactly
as before, silently. It currently works only because `settingsState` happens to be
subscribed first (`onCreate` line 100, and factory arg order).

Fix — single source of truth, so "loaded" and "value" cannot disagree:

```kotlin
// LotoApplication
val settingsOrNull: StateFlow<Settings?> by lazy {
    settingsRepository.settingsFlow.stateIn(appScope, SharingStarted.Eagerly, null)
}
```

and in `PlayerBoardViewModel` take `settings: StateFlow<Settings?>` (or keep both
but pass the settings snapshot *into* `onMasterState(master, settings)` from the
combine transform, and derive `loaded` as `snapshot != null`). Reading
`settings.value` inside a gated handler is the defect shape to remove.

### M-3 — The gate covers `onMasterState` only; `generate()` / `clearMarks()` still consume the master history under the placeholder mode

`android/app/src/main/java/com/miti99/loto/state/PlayerBoardViewModel.kt:224-237, 258-271`
`android/app/src/main/java/com/miti99/loto/ui/player/PlayerBoardScreen.kt:78`

Both methods read `settings.value.mode` unguarded and, in the non-BOTH branch, do
`lastHandledIndex = masterStore.state.value.called.size`. The generate button is
gated on `state.loading` (player restore only), not on settings having resolved.
So a tap in the pre-settings window — the most likely moment to tap, since the
empty board's only action is "Tạo bảng mới" — with a persisted `mode = BOTH` and a
restored master history advances the cursor to `called.size` with no crossing.
`onMasterState` later runs under the real BOTH mode but `cursor >= called.size`
returns `changed = false` (`PlayerAutoCross.kt:34-36`), so the replay is
permanently lost. This is the same class of bug M4 was filed for; the new test
`master restore landing with full history before settings resolves...` avoids it
only because it calls `generate()` *before* `masterStore.restore()`.

Fix: fold the settings gate into the UI loading flag, e.g. publish
`loading = !restoreDone || !settingsLoaded.value` (collect `settingsLoaded` in the
existing `init` combine and republish), so both buttons are disabled until the
mode is real. Add a test that restores the master history *before* `generate()`
while settings is still gated.

### M-4 — `settingsLoaded` can stick at `false` forever, permanently disabling auto-cross

`android/app/src/main/java/com/miti99/loto/settings/SettingsRepository.kt:76`

`loaded` is derived from `settingsFlow`, whose `catch` re-throws anything that is
not an `IOException` (line 52-60). A non-IO failure therefore cancels the
`settingsLoaded` collector (`SupervisorJob` keeps the app alive), leaving
`loaded == false` for the process lifetime — `onMasterState` is then never invoked
and the player board silently stops following the master. The plan explicitly
required the gate not to deadlock the ViewModel on a settings-load failure.

Fix (one line, fail-open):

```kotlin
val loaded: Flow<Boolean> =
    settingsFlow.map { true }.onStart { emit(false) }.catch { emit(true) }
```

Fail-open is correct here: a broken settings store should degrade to defaults, not
to a dead board. Add a `ThrowingDataStore`-with-`RuntimeException` test.

---

## Low

- **L-a `generate()` is now asynchronous** (`PlayerBoardViewModel.kt:210-216`):
  two fast taps enqueue two generations (the confirm dialog is skipped while
  `grid` is still `null`), and the H1 guard `saved != null && grid == null`
  (line 137) is weaker than its comment claims, because `grid` is only assigned at
  the end of `applyGeneratedGrid`. Consider a `generating` flag that both
  short-circuits a second `generate()` and participates in the restore guard.
- **L-b iOS unlock is master-only** (`web/src/lib/voice.js:56-72`,
  `MasterPanel.svelte:156, 170`): `unlockAudio()` is called only from the master's
  draw/auto buttons. In player-only mode with `voiceEnabledPlayer` on, the shared
  element is never primed inside a gesture, so Chờ/Kinh may stay silent on iOS.
  Call `unlockAudio()` from the player cell-tap handler too. Also: the unlock's
  `play().then(() => a.pause())` would cut a live clip if `unlockAudio()` were ever
  called while something is playing — safe today only because the `src` swap in
  `playClip` happens synchronously in the same click task.
- **L-c web/android validation parity**: Android now rejects a restored round that
  is not a complete 90-number partition (`GameStateRepository.kt:70-73`), while
  `web/src/lib/master-store.svelte.js:56-62` still accepts any two in-range arrays
  (duplicates, overlap, short deck). Either port the check or record the
  divergence — a shared localStorage origin can produce a double-crossed cell.
- **L-d dead overrides**: `PlayerBoardViewModel.onCleared()` (line 316-323) and
  `MasterPanelViewModel.onCleared()` now only call `super`. Delete the overrides and
  move the rationale to the class doc; an empty override invites someone to
  "fix" it later.
- **L-e loading flags never time out**: `MasterStore.loading` /
  `PlayerUiState.loading` gate the only two primary buttons and only clear when the
  DataStore read resolves. An `IOException` resolves (handled), but a hung read
  leaves the app unusable with no escape hatch. Optional: `withTimeoutOrNull` around
  the restore read.
- **L-f brittle CI gate**: `web/scripts/verify-pwa-build.mjs:52-56` requires one
  built chunk to contain both `"serviceWorker"` and `"sw.js"`. A future Rollup
  chunking change can split them and fail CI with no real regression. Prefer
  asserting on the presence of the `virtual:pwa-register` output chunk plus a
  `sw.js` reference anywhere under `_app`.
- **L-g backup exclusion misses the DataStore temp sibling**:
  `android/app/src/main/res/xml/{data_extraction_rules,backup_rules}.xml` exclude
  `datastore/loto_game_state.preferences_pb` only. DataStore writes through
  `<name>.preferences_pb.tmp`; a backup taken mid-write can capture it. Add the
  `.tmp` path (or exclude by directory if you ever move the round store to its own
  subdirectory).

---

## Verified as intentional / no action

- `android:launchMode="singleTask"` removal is finding L8 from
  `plans/reports/code-reviewer-260831-2213-android-review.md:276` — deliberate, and
  the launcher intent already brings the existing task to front, so no behavioral
  regression.
- Backup path is correct: `preferencesDataStore(name = "loto_game_state")`
  (`LotoApplication.kt:33-36`) → `files/datastore/loto_game_state.preferences_pb`;
  `loto_settings` is not excluded, as the decision requires. `allowBackup="true"`
  unchanged.
- Storage keys/file names unchanged on both platforms (`loto_settings`,
  `loto_master`, `loto_game_state`); `masterState.hydrated` is *not* serialized
  (`master-store.svelte.js:72-84`), so no stored-shape change.
- Removed strings (`player_board_label`, `toast_dismiss`, `kinh_close`,
  `master_board_label`) have no remaining references.
- H1 loading-gate behavior survives the M4 gate: `restore()` still publishes
  `loading = false` on both branches (`PlayerBoardViewModel.kt:153`), the early-return
  branch of `publish()` now carries `loading` through (line 404), and `prevCalledLen`
  stays `0` while the gate is closed, so no false "Ván mới" shrink is detected when
  it opens.
- Web reclaim/hydration flow is coherent: `reclaimTab()` re-reads storage *before*
  flipping `inactive` (`+layout.svelte:64-68`), children are destroyed while frozen
  so `PlayerBoard` re-initializes from storage, and `MasterPanel`'s save effect is
  gated on `masterState.hydrated`.
- Update banner vs other-tab overlay cannot collide: they live in mutually
  exclusive branches of `{#if activeTab.inactive}`; a prompt raised while frozen
  simply appears after reclaim.
- `PlayerAutoCross.applyMasterCalls` already resets a stale cursor
  (`PlayerAutoCross.kt:33`), so the L6 change (clearing `manualUnticks` on a shrink
  in every mode without resetting `lastHandledIndex` outside BOTH) does not strand
  the cursor.
- New tests are behavioral, not phantom: `SlowDataStore`/`ThrowingDataStore` drive
  real suspension/failure paths, and the M4-residual and H1 tests would fail without
  their respective fixes.
- CI additions (`npm run lint`, `npm run check`, `verify:pwa` with `BUILD_PROFILE`)
  and the keystore cleanup step (`if: always()`) are contract-safe additions.

## Unresolved questions

1. H-1 — is exiting via "Thoát" and immediately reopening in-scope for QA? If the
   answer is "the process always dies", H-1 drops to Low; nothing in the code
   guarantees that.
2. M-1 — is "two active tabs" acceptable in any scenario, or should a losing
   claimer always freeze itself? The echo fix assumes the latter.
3. Is `unlockAudio()`'s src-less priming `play()` actually sufficient to unlock iOS
   Safari? It has not been verified on a device, and the comment asserts it as
   fact. A silent one-sample data-URI source would be the safer primer.
4. Scope: the diff carries a large amount of work beyond this plan's three items
   (PWA prompt + registration, voice single-element rewrite, `checkJs`, focus trap,
   pre-paint theme script). Confirm this is intended to land as one commit, or split
   the PWA/audio work out — it has a different blast radius than the three plan items
   and (H-1, L-b) is where the residual risk sits.
