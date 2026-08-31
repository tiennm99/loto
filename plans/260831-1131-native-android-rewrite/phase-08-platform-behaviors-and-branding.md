---
phase: 8
title: "Platform Behaviors and Branding"
status: done
priority: P1
effort: "1d"
dependencies: [5, 6, 7]
---

# Phase 8: Platform Behaviors and Branding

## Overview

Everything that made the wrapper feel like a real Android app, now
native-first: keep-screen-on, back handling, theming, splash, launcher
icons, edge-to-edge. The wrapper-era device-QA list in `plans/todo.md`
is the acceptance spec.

## Requirements

- **Keep screen on while a round is live** (auto-call runs untouched for
  15+ min): `FLAG_KEEP_SCREEN_ON` (or Modifier equivalent) held while
  `MasterPanelViewModel` reports a live round in master/both mode;
  released on round end. Player-only mode never holds it (web parity).
- **Back behavior:** back closes the top overlay first (Kinh dialog,
  settings sheet — Compose dialogs/sheets get this free; verify), and at
  root shows the Vietnamese exit confirm ("Ở lại" keeps state) via
  `OnBackPressedCallback`/`BackHandler`. Same copy as the wrapper's
  dialog — strings at wrapper `res/values/strings.xml:9-12`
  (`exit_title` "Thoát Lô tô?", `exit_message` "Ván đang chơi sẽ được
  lưu lại.", `exit_confirm` "Thoát", `exit_cancel` "Ở lại"); dialog
  wiring reference in `MainActivity.java:68-71`.
  <!-- Updated: Validation Session 1 - exit-string pointer corrected -->
- **Theme:** Material 3 color schemes for light + dark from the web's
  sky-blue brand (`#1565c0` light theme color, `#0a0f1f` dark — confirm
  against `web/src/app.html`/`app.css`); `settings.theme` auto follows
  system, light/dark force. `dynamicColor = false` (brand-fixed).
- **Splash:** `androidx.core:core-splashscreen`, brand mark + correct
  light/dark background — no white flash cold-starting in dark mode.
- **Launcher icons:** regenerate from `web/static/icons/source.svg` at
  the corrected glyph size (the wrapper's Android art was already fixed
  at 200/160 — reuse that render recipe from `android/README.md` §Icons):
  adaptive foreground/background, monochrome (themed icons, API 33+),
  legacy mipmaps. Label: `Lô tô`.
- **Edge-to-edge:** content and the gear/footer clear of status bar +
  gesture nav insets.
- **App metadata screens:** the web `PageFooter` content (version,
  links) gets a native equivalent — decide placement (footer of the
  scroll root, matching web).

## Related Code Files

- Create: `app/src/main/java/com/miti99/loto/ui/theme/{Color,Theme,Type}.kt`,
  `KeepScreenOn.kt` modifier/effect, exit-confirm dialog composable,
  `res/mipmap-*`, `res/drawable/` splash + icon vectors,
  `res/values*/themes.xml`, `values-night/`
- Modify: `MainActivity.kt` (back callback, edge-to-edge),
  `LotoAppRoot.kt` (footer, keep-screen-on binding)
- Spec: `plans/todo.md` device-QA list, wrapper `MainActivity.java` +
  `res/` in git history, `web/src/app.css` (colors), `web/src/lib/PageFooter.svelte`

## Implementation Steps

1. Theme + dark mode pass across all screens (Phase 5–7 components use
   theme tokens only — fix any hardcoded colors found).
2. Keep-screen-on effect bound to live-round state; unit-test the
   predicate (master/both + round live).
3. BackHandler chain: sheet > dialog > exit confirm; manual test script.
4. Splash + icons + edge-to-edge; verify on API 24 (drawable splash
   path) and 31+ (SplashScreen API path).
5. Footer/metadata.

## Success Criteria

- [ ] Wrapper-era QA items in `plans/todo.md` that are still applicable pass on an emulator (screen-awake, back chain, exit dialog copy, themed icon, dark splash)
- [ ] No hardcoded color literals outside `ui/theme` (lint check or grep)
- [ ] App label, icon, splash match the brand in light + dark

## Risk Assessment

- **Icon regeneration toolchain** (rsvg + Roboto Condensed ttf) is
  host-dependent. Signal: fonts render wrong in generated PNGs.
  Response: reuse the wrapper's committed PNGs from git history if the
  glyph is unchanged — they were already rendered correctly.
