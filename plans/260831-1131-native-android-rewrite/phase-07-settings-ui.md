---
phase: 7
title: "Settings UI"
status: done
priority: P1
effort: "1d"
dependencies: [4, 5, 6]
---

# Phase 7: Settings UI

## Overview

The settings sheet (gear button, top-right as on the web): every field of
the Phase-4 `Settings` model gets a control, matching the web's
`SettingsButton.svelte` sheet content and Vietnamese copy.

## Requirements

- Functional — controls for:
  - Chế độ (mode): player / master / both — segmented or radio.
  - Giọng đọc (voice): picker over `VoiceCatalog.VOICES` labels; master
    voice toggle, player voice toggle, chờ-number sub-toggle (enabled
    only when player voice is on — copy the web's gating).
  - Tự động gạch (auto-cross) toggle.
  - Auto-call: enable toggle + speed slider/stepper 1..10s.
  - Giao diện (theme): auto / light / dark.
  - Màu ô trống (empty cell color): preset swatches + custom picker
    (the web ships Excel-preset swatches and RGB input — mirror the
    presets; a full RGB slider is in-scope only if the web sheet has it
    today: verify `SettingsButton.svelte` first).
  - Cỡ chữ bảng (board text scale): 0.9 / 1 / 1.15 / 1.3 rungs.
  - Đặt lại (reset settings) with confirm.
- Every change applies immediately (live recomposition) and persists via
  `SettingsViewModel` — no save button, same as the web.
- Non-functional: sheet is dismissible by back gesture (handled in
  Phase 8's back stack) and scrollable on small screens.

## Architecture

Package `com.miti99.loto.ui.settings`: `SettingsSheet.kt` (ModalBottomSheet)
+ small control composables (`VoicePicker.kt`, `EmptyCellColorPicker.kt`,
`BoardTextScalePicker.kt`, …). Gear entry point lives in the Phase-6 root
top bar.

## Related Code Files

- Create: files above + Compose test `SettingsSheetTest.kt` (change mode
  → root layout switches; change scale → grid text scales)
- Spec: `web/src/lib/SettingsButton.svelte`,
  `settings-store.svelte.js`, superseded plan phase 03 (solid-color
  palette), `web/src/app.css`

## Implementation Steps

1. Read `SettingsButton.svelte` end to end; inventory every control,
   its copy, and its gating (write the inventory into the PR).
2. Build the sheet skeleton + wire `SettingsViewModel`.
3. Implement controls in the web's order; reuse Material 3 components,
   no custom widgets unless the web design demands it.
4. Compose tests for the two cross-cutting settings (mode, text scale).

## Success Criteria

- [ ] Every `Settings` field reachable and live-applied from the sheet
- [ ] Copy matches the web sheet (Vietnamese, exact strings)
- [ ] Compose tests green

## Risk Assessment

- **Scope creep in the color picker.** Signal: building a color wheel.
  Response: ship exactly what the web sheet has — presets (+ RGB rows if
  present on the web today), nothing more.
