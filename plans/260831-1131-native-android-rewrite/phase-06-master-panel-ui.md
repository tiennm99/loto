---
phase: 6
title: "Master Panel UI"
status: done
priority: P1
effort: "1.5d"
dependencies: [2, 3, 4]
---

# Phase 6: Master Panel UI

## Overview

The host (quản trò) surface: draw button / auto-call controls, the
current-number hero, called history, and the 11×9 ones-digit master
tracking board. Shown alone (`mode master`) or inline below the player
card (`mode both`), exactly as the web lays it out.

## Requirements

- Functional parity with `MasterPanel.svelte`, `MasterEmptyState.svelte`,
  `AutoCountdown.svelte`:
  - "Xổ số" draws one number; with `autoCallEnabled` the button becomes
    Bắt đầu/Dừng and a countdown ticks at `autoCallSpeed` seconds
    (visible countdown, matching `AutoCountdown.svelte`).
  - Current-number hero (large display), called history (most recent
    first or web order — copy the web), remaining count.
  - 11×9 master board, ones-digit-aligned (90 sits at row 10 / col 8 —
    the old port's `MasterBoardLayoutTest` documented these edge cells;
    re-derive from the web source, then keep that test idea).
  - Empty state before the first draw (ghost preview per
    `MasterEmptyState.svelte`).
  - Reset ("ván mới") with confirm; resets deck + hero + history and
    broadcasts to the player board per web behavior.
  - Voice announce on draw when `voiceEnabledMaster`; per-mode hint copy
    (superseded plan phase 04 documents the exact strings).
- Non-functional: auto-call keeps ticking with the app foregrounded and
  screen kept on (Phase 8 owns the keep-screen-on flag itself).

## Architecture

Package `com.miti99.loto.ui.master`: `MasterPanelScreen.kt`,
`CurrentNumberHero.kt`, `MasterBoard.kt`, `MasterControls.kt`,
`CalledHistory.kt`, `MasterEmptyState.kt`. Layout composes with the
player board in a shared scrollable root (`LotoAppRoot.kt` in
`com.miti99.loto.ui`) switching on `settings.mode`.

## Related Code Files

- Create: files above + Compose test `MasterPanelScreenTest.kt`
  (draw updates hero + history; auto-call toggle relabels button),
  `MasterBoardLayoutTest.kt` (unit: ones-digit cell mapping incl. 90)
- Spec: `web/src/lib/MasterPanel.svelte`, `MasterEmptyState.svelte`,
  `AutoCountdown.svelte`, `master-store.svelte.js`, superseded plan
  phase 04 (hint copy), `web/src/app.css`

## Implementation Steps

1. Read the three Svelte components; note layout, copy, and state
   transitions (start/stop/reset edge cases: stop on exhaustion, confirm
   on reset mid-round).
2. Build `MasterBoard` mapping + unit test first (pure function).
3. Hero, history, controls wired to `MasterPanelViewModel`.
4. Auto-call UI: countdown display, start/stop, disabled states.
5. Root layout switching on mode (player / master / both).
6. Compose tests for draw + auto-call toggle.

## Success Criteria

- [ ] Ones-digit mapping unit test green (incl. the 90 edge cell)
- [ ] Compose tests: draw updates hero/history; toggle relabels; reset confirm
- [ ] Mode both shows player card above master panel, single scroll — matches web

## Risk Assessment

- **Auto-call drift vs Phase 4 ticker.** UI must render the ViewModel's
  countdown, never run its own timer. Signal: two counters visible or
  countdown jumps on recomposition. Response: ViewModel exposes
  `secondsUntilNextCall` in UI state; UI is render-only.
