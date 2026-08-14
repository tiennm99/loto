---
status: complete
created: 2026-08-14
completed: 2026-08-14
branch: main
source: plans/reports/brainstorm-android-ux-260814-1004-android-experience-pass-report.md
report: plans/reports/from-cook-to-review-260814-1317-android-experience-pass-implementation-report.md
---

## Deviations from plan

1. **Icon scope grew.** Discovered mid-plan that the APK ships Capacitor's
   stock logo as launcher icon *and* splash. User approved regenerating from
   `source.svg`. Supersedes the originally-planned monochrome-layer and
   dark-splash items, which would have themed the wrong logo.
2. **`source.svg` is broken.** `font-size="240"` overflows the 512 canvas;
   `web/static/icons/*.png` are visibly clipped. Android art rendered at a
   corrected 200/160. Web PWA icons still carry the bug — not fixed, out of
   approved scope.
3. **Wake-lock condition tightened** from `autoRunning || hasGame` to
   `masterState.remaining.length > 0`. `hasGame` never returns to false once a
   round starts, which would have pinned the screen on for a finished board.
4. **Splash rebuilt rather than duplicated.** 11 density PNGs replaced by one
   layer-list + night variant + `windowSplashScreen*` for API 31+.

# Android experience pass

Close Android-only behaviour gaps in the Capacitor wrapper. Offline guarantee
(no INTERNET permission) untouched.

## Phases

| # | Phase | File | Depends on |
|---|-------|------|------------|
| 1 | Tier 1 defects | [phase-01-tier1-defects.md](phase-01-tier1-defects.md) | — |
| 2 | Tier 2 defensive | [phase-02-tier2-defensive.md](phase-02-tier2-defensive.md) | 1 (shares MainActivity) |
| 3 | Store polish | [phase-03-store-polish.md](phase-03-store-polish.md) | — |

## Resolved before planning

Brainstorm left W3 (back button) as a fork. Resolved by research:

- Android 16 / API 36 no longer calls `onBackPressed()` and no longer dispatches
  `KEYCODE_BACK`. Project targets 36.
- `OnBackPressedCallback` is the forward-compatible mechanism and keeps working
  under predictive back.
- Capacitor 8 `BridgeActivity`/`Bridge` show no back handling on `main`, so
  Capacitor's default "back navigates WebView history" cannot be assumed to
  survive targetSdk 36 either.

Consequence: back handling is implemented natively via `OnBackPressedCallback`.
This is not a fallback — it is the only mechanism that can be relied on. Kills
brainstorm risks 1 and 3 (`web/` never takes a Capacitor dependency).

## Acceptance criteria

1. Cell tap vibrates on device; `prefers-reduced-motion` still suppresses.
2. Auto-call runs 3+ min untouched without screen dimming; stop releases lock;
   background → foreground re-acquires.
3. Back closes an open overlay (bingo modal, settings sheet) instead of quitting;
   back at root shows a Vietnamese exit confirm.
4. Player grid legible at 200% system font; in-app board-size control resizes and
   persists.
5. Status bar / gesture nav do not overlap the settings gear or footer.
6. Volume rocker controls media stream during a call.
7. Launcher shows `Lô tô` with diacritics.
8. `pnpm test` green, `pnpm lint` clean, `pnpm build` succeeds.

## Out of scope

Multi-device sync, INTERNET permission, audio focus/MediaSession, APK size,
`allowBackup` changes, service-worker asset dedup.

## Verification limits

No Android device, emulator, or browser in this environment (headless ARM64, no
Chrome — see workspace CLAUDE.md). Gradle/Android SDK not installed. So:

- Verifiable here: unit tests, lint, web build, static review of native code.
- Device-only: items 1, 2 (real screen), 3, 4 (system font), 5, 6.

Phase files carry a manual QA checklist for the device-only items.
