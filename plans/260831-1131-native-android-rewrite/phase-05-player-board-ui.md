---
phase: 5
title: "Player Board UI"
status: done
priority: P1
effort: "1.5d"
dependencies: [2, 3, 4]
---

# Phase 5: Player Board UI

## Overview

The player-facing screen: the 9×9 card, tap-to-cross with haptics,
auto-cross, the chờ (waiting) presentation, and the Kinh (bingo)
celebration. Compose throughout, driven by `PlayerBoardViewModel`.

## Requirements

- Functional parity with `PlayerBoard.svelte` + the superseded upstream-
  sync plan's specs (read `plans/260429-1511-loto-upstream-sync/`
  phases 01–02 for the confetti tier rule and the chờ ring on the label
  band before styling):
  - 9×9 grid, empty cells tinted with the user's `emptyCellColor`,
    numbers scaled by `boardTextScale`.
  - Tap toggles cross; un-crossing a called number records a manual
    untick (auto-cross must not re-cross it — Phase 2 logic).
  - Auto-cross setting crosses the drawn number automatically.
  - Chờ: ring/highlight on the waiting row's label band + toast;
    optional voice "Chờ" (+ awaited number when `voiceWaitingNumber`).
  - Kinh: modal with celebration copy, confetti (tiered per the
    upstream-sync spec), voice "Kinh"; dismissible; idempotent (no
    re-trigger on recomposition).
  - "Tạo bảng mới" (new card) flow with confirm when a round is live —
    match web copy exactly.
- Non-functional: haptics via `VIBRATE` on cell tap, suppressed when the
  system reduced-motion/haptics-off signals say so (mirror wrapper
  behavior recorded in `plans/todo.md` QA list); 60fps grid on API 24
  hardware (no per-cell recomposition storms — stable keys, immutable
  cell state).

## Architecture

Package `com.miti99.loto.ui.player`: `PlayerBoardScreen.kt` (state
hookup), `PlayerCardGrid.kt`, `PlayerCell.kt`, `ChoIndicator.kt`,
`KinhDialog.kt`, `Confetti.kt` (Canvas-based; keep it small). Vietnamese
strings in `res/values/strings.xml` (default locale = vi copy, no
translation indirection yet — i18n is a non-goal).

## Related Code Files

- Create: files above + `app/src/test/.../PlayerBoardViewModelTest.kt`
  additions, Compose UI test `PlayerBoardScreenTest.kt` (grid renders,
  tap crosses, kinh dialog appears on completed row)
- Spec: `web/src/lib/PlayerBoard.svelte`, `player-auto-cross.js` + test,
  `web/src/app.css` (cell colors/dims), superseded plan phases 01–03

## Implementation Steps

1. Read the Svelte component + CSS; write a short visual-parity note
   (colors, spacing, states) at the top of `PlayerCardGrid.kt`.
2. Build the static grid from ViewModel state; wire tap/haptics.
3. Chờ presentation (ring + toast) from `getWaitingNumber`.
4. Kinh dialog + confetti + voice trigger; idempotency guard tested.
5. New-card flow with live-round confirm.
6. Compose UI tests for the three critical interactions.

## Success Criteria

- [ ] Compose tests: tap-cross, chờ indicator, kinh dialog
- [ ] Manual untick never re-crossed by auto-cross (ViewModel test)
- [ ] Visual spot-check vs the web app side by side (screenshot pair in PR)

## Risk Assessment

- **Confetti performance on low-end devices.** Signal: jank on the API 24
  emulator. Response: cap particle count / drop confetti below API 26 —
  cosmetic, cut before optimizing.
