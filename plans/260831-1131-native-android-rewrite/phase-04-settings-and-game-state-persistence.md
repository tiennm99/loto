---
phase: 4
title: "Settings and Game State Persistence"
status: done
priority: P1
effort: "1d"
dependencies: [1, 2]
---

# Phase 4: Settings and Game State Persistence

## Overview

Implement the settings contract (DataStore) and round-state persistence
(card, crossed cells, manual unticks, master deck) so a process-death or
device reboot restores the exact game, matching what the web persists in
`localStorage` (`loto*` / `loto_master` keys).

## Requirements

- Functional: every field of the web `DEFAULT_SETTINGS` exists with the
  same defaults and validation:
  `emptyCellColor "#7030A0"` (hex6-validated), `theme auto|light|dark`,
  `mode player|master|both` (default player), `autoCallEnabled false`,
  `autoCallSpeed 5` (int 1..10), `voiceEnabledMaster true`,
  `voiceEnabledPlayer false`, `voiceWaitingNumber false`,
  `voice <default from manifest>`, `boardTextScale 1`
  (∈ {0.9, 1, 1.15, 1.3}).
  Invalid stored values fall back per-field to defaults (the web's
  per-field `valid*` ?? default pattern), never wholesale reset.
  The web's legacy `masterMode` migration is web-only — do NOT port it;
  the native store starts clean.
- Functional: round state survives process death — player grid, crossed
  set, manual unticks, master called-list/deck order, and whether a round
  is live.
- Non-functional: main-thread-safe (DataStore is async); ViewModels read
  settings as `StateFlow`.

## Architecture

Package `com.miti99.loto.settings` + `com.miti99.loto.state`:

- `SettingsRepository.kt` — DataStore Preferences; typed accessors +
  per-field validation on read; `settingsFlow: Flow<Settings>`;
  `reset()`.
- `Settings.kt` — immutable data class mirroring DEFAULT_SETTINGS.
- `GameStateRepository.kt` — second DataStore (or the same with
  namespaced keys) persisting the Phase-2 models; serialize grid/crossed
  as compact strings (e.g. CSV of 45 numbers + bitmask) — no kotlinx
  serialization dependency unless it pays for itself.
- ViewModels (`PlayerBoardViewModel`, `MasterPanelViewModel`,
  `SettingsViewModel`) in `com.miti99.loto.state`, constructed by the
  Phase-1 factory; expose `StateFlow` UI state consumed by Phases 5–7.
  Master→player call propagation via a shared flow owned by the
  Application scope (the web's store coupling, the old port called it
  CallBus — same idea, fresh code).

## Related Code Files

- Create: `app/src/main/java/com/miti99/loto/settings/Settings.kt`,
  `SettingsRepository.kt`;
  `app/src/main/java/com/miti99/loto/state/GameStateRepository.kt`,
  `PlayerBoardViewModel.kt`, `MasterPanelViewModel.kt`,
  `SettingsViewModel.kt`
- Create tests: repository round-trip + validation tests (Robolectric or
  in-memory DataStore), ViewModel tests with `runTest` +
  `advanceTimeBy` for auto-call cadence, Turbine for flows
- Spec: `web/src/lib/settings-store.svelte.js`, `settings-store.test.js`,
  `game-logic.js` save/load functions, `master-store.svelte.js`

## Implementation Steps

1. Read the spec files; table every persisted key and its validation.
2. Implement `Settings` + `SettingsRepository` with per-field fallback;
   tests for each invalid-value case in `settings-store.test.js`.
3. Implement `GameStateRepository` round-trip; test corrupt-payload
   fallback (fresh round, never crash).
4. Implement the three ViewModels against fakes (deck, voice player,
   repos). Auto-call: coroutine ticker honoring `autoCallSpeed`,
   cancel/restart on speed change without double-ticking (test with
   virtual time), stop when remaining == 0.
5. Wire everything in `LotoApplication` + factory.

## Success Criteria

- [ ] Settings round-trip test: write all fields, recreate repo, read back identical
- [ ] Every invalid-value case from `settings-store.test.js` has a Kotlin twin
- [ ] Kill-and-restore test (Robolectric): live round state fully restored
- [ ] Auto-call virtual-time tests: cadence, mid-run speed change, exhaustion stop

## Risk Assessment

- **State model churn** once UI phases start. Signal: Phase 5/6 needs a
  field the UI state doesn't carry. Response: add it here first with a
  test — UI phases never reach into repositories directly.
