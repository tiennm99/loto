# Android experience pass — implementation report

- Date: 2026-08-14
- Branch: `main` (uncommitted)
- Plan: [plans/260814-1317-android-experience-pass/plan.md](../260814-1317-android-experience-pass/plan.md)
- Brainstorm: [brainstorm-android-ux-260814-1004](brainstorm-android-ux-260814-1004-android-experience-pass-report.md)

## Verified here

| Gate | Result |
|------|--------|
| `pnpm test` | 8 files, 121 tests pass (was 6 files / 105) |
| `pnpm lint` | clean |
| `pnpm build` | succeeds; `viewport-fit`, safe-area CSS, `.board-num`/`.master-num`, `--board-text-scale` confirmed present in output |

## NOT verified — no device, emulator, browser, or Android SDK

Gradle cannot run here (`ANDROID_HOME` unset, no SDK, no `adb`). **The Java and
all Android resources are uncompiled.** Static review only.

Device QA required for: haptics, wake lock, back button + exit dialog, safe-area
insets, system font scale, volume rocker, launcher icon, splash.

## Delivered

### Phase 1 — Tier 1 defects

- **W1 VIBRATE** — `AndroidManifest.xml`. Normal permission; no runtime prompt,
  no data-safety change.
- **W2 wake lock** — new `web/src/lib/wake-lock.js` + 8 tests. Re-acquires on
  `visibilitychange` (Android drops the lock on hide). Generation token stops a
  late-resolving `request()` leaking a lock after the caller turned it off.
  Consumed by a `$effect` in `MasterPanel.svelte`.
- **W3 back button** — native `OnBackPressedCallback` in `MainActivity.java` +
  new `web/src/lib/overlay-history.js` (8 tests). Overlays push a history
  sentinel; `canGoBack()` therefore means "an overlay is open". Root back shows
  a Vietnamese confirm dialog.

### Phase 2 — Tier 2, defensive

- **W4 safe areas** — `viewport-fit=cover` + `env(safe-area-inset-*)` padding on
  `body`. Also fixes the iOS PWA notch.
- **W5 font scale** — `textZoom = 100` pinned natively; new `boardTextScale`
  setting (0.9/1/1.15/1.3) with validator, CSS var, and a "Cỡ chữ bảng" picker
  in the settings sheet. 7 new tests.
- **W6 volume rocker** — `setVolumeControlStream(STREAM_MUSIC)`.

### Phase 3 — Store polish

- `app_name` → `Lô tô` (was ASCII `Lo To`).
- **Launcher icon and splash rebuilt from the brand mark** (see finding below).
  Legacy + round + adaptive foreground + monochrome at 5 densities; adaptive
  background as a gradient vector; splash as a themed layer-list with a night
  variant plus `windowSplashScreen*` for API 31+.
- 11 stock splash PNGs and the template robot vector deleted.
- `versionCode 4`, `versionName 0.1.0`.
- `android/README.md`: permissions, Android-specific behaviour, icon pipeline.

## Findings

### 1. The APK was shipping Capacitor's logo (fixed)

Launcher icon, round icon, adaptive foreground and splash were all the stock
Capacitor template — the blue "X" mark on a teal grid plate. The brand icon
existed only in `web/static/icons/`. A published Play Store app was showing
another project's logo in the launcher and on cold start.

### 2. `source.svg` overflows its canvas (NOT fixed — web scope)

`font-size="240"` renders "Lô tô" at ~437px against a 400px inner panel, and the
original PNGs were rendered without Roboto Condensed so a wider fallback pushed
it past the 512px canvas entirely. **`web/static/icons/icon-192.png`,
`icon-512.png` and `icon-maskable-512.png` are visibly clipped** — the `L` and
the trailing `ô` are cut off. These are what the PWA, GH Pages install prompt,
and the OG/Twitter card images use.

Android art was rendered at a corrected size (200 legacy / 160 adaptive) with
the real font. The web PNGs were left alone — outside approved scope.

### 3. Predictive back changes the mechanism (resolved during planning)

Android 16 stops calling `onBackPressed()` and stops dispatching `KEYCODE_BACK`
at targetSdk 36. Capacitor 8 shows no back handling in `BridgeActivity`/`Bridge`
on `main`, so its default "back navigates WebView history" cannot be assumed to
survive either — meaning a pure-web History-API approach could not have worked
alone. `OnBackPressedCallback` is the only reliable mechanism.

### 4. `allowBackup` reassessed — no change

Flagged as a gap in the brainstorm. Stored data is grid, crossed cells, and UI
settings. Nothing sensitive; backup restores a player's card on a new phone.
Correct as-is.

## Risks carried into device QA

1. **Exit dialog theme.** Activity theme is `AppTheme.NoActionBarLaunch`
   (parent `Theme.SplashScreen`). Mitigated two ways: explicit
   `AppTheme.ExitDialog` (`Theme.AppCompat.DayNight.Dialog.Alert`) passed to the
   builder, and `postSplashScreenTheme` added. If the dialog still throws, the
   activity theme is the cause.
2. **`canGoBack()` baseline.** Assumes the WebView has no history at rest.
   SvelteKit hydrates with `replaceState`, so it should be false — verify back
   at the root confirms exit rather than navigating.
3. **`textZoom` pin is an accessibility override.** Only defensible alongside
   the in-app size control. If that setting is ever cut, cut the pin too.
4. **Uncompiled Java.** `OnBackPressedCallback` and `Bridge` resolve
   transitively through `appcompat` / `capacitor-android`; not proven.

## Device QA checklist

- [ ] Tap a cell → vibrates; enable reduced-motion → silent.
- [ ] Auto-call 3+ min untouched → screen stays lit. Stop → sleeps normally.
- [ ] Background during auto-call, return → still lit.
- [ ] Finish a round (remaining 0) → lock releases.
- [ ] Back with bingo modal open → modal closes, app stays.
- [ ] Back with settings open → sheet closes.
- [ ] Back at root → Vietnamese exit dialog; "Ở lại" keeps state.
- [ ] System font 200% → grid intact; Settings → Cỡ chữ bảng resizes; survives reload.
- [ ] Gesture nav + status bar clear of the settings gear and footer.
- [ ] Volume rocker during a call → media slider.
- [ ] Launcher shows `Lô tô` with the brand icon; themed-icon mode tints it.
- [ ] Cold start in dark mode → dark splash, no white flash.

## Unresolved questions

1. Fix the clipped web PWA icons (finding 2)? Same root cause, different scope —
   changes GH Pages / PWA / OG card images.
2. `boardTextScale` rungs 0.9/1/1.15/1.3 — enough range for users who ran
   system font at 200%?
3. Exit confirm currently fires at root regardless of round state. Worth
   plumbing "is a round live" to native so an idle app exits without a prompt?
