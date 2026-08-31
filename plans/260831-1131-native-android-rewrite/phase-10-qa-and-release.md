---
phase: 10
title: "QA and Release"
status: done
priority: P1
effort: "1d"
dependencies: [9]
---

# Phase 10: QA and Release

## Overview

Emulator/device QA against the carried checklist, then the first native
release: `v0.2.0`, versionCode 7, to the Play `alpha` closed track —
without breaking the running 14-day tester window.

## Requirements

- Functional: golden path on emulators API 24, 31, 36 — generate card →
  manual draws → enable auto-call 3+ min (screen stays lit) → chờ →
  complete row → Kinh modal + confetti + audio → reset → back-chain →
  exit confirm.
- Audio QA: both voices; rapid draws cancel cleanly; chờ+N gapless;
  volume rocker controls media stream; behavior on interruption
  (the Phase-3 audio-attributes decision) observed and documented.
- Persistence QA: kill app mid-round → relaunch → identical state;
  airplane mode cold start (offline hard guarantee).
- Accessibility spot-check: TalkBack reads cells and controls; system
  font 200% doesn't break the grid (app-controlled `boardTextScale`
  covers user needs); contrast in both themes.
- Instrumentation: run `connectedDebugAndroidTest` (Compose smoke +
  VoicePlayer instrumentation) on at least one API level.
- Release: bump check (versionCode 7 already set in Phase 1 — verify),
  tag `v0.2.0`, confirm GH Release assets + Play upload log
  (`Validating tracks: 'alpha'`), then install the Play build via a
  tester account and re-run the golden path once.
- Release notes flag: full native rewrite; settings and any in-progress
  round from the previous version are not carried over.

## Related Code Files

- Modify: `android/app/build.gradle.kts` (only if versionCode/Name
  need correction), `plans/todo.md` (retire completed QA items, carry
  genuinely open ones — e.g. store-listing assets)
- No feature code — QA findings each become a fix commit or a tracked
  todo item; do not batch silent fixes into the release commit.

## Implementation Steps

<!-- Updated: Validation Session 1 - SDK/emulator setup added; ship-when-ready timing -->
0. **Environment setup (none exists yet):** install Android
   Studio/SDK command-line tools on this machine, create AVDs for
   API 24, 31, 36 (or attach a physical device via ADB). Budget ~0.5d
   extra the first time.
1. Build QA matrix from `plans/todo.md` device-QA list + the checklist
   above; run on API 24 / 31 / 36 emulators (BlueStacks optional).
2. File and fix blockers; re-run the affected checks.
3. Verify versionCode/Name; tag `v0.2.0`; watch `android-release` run.
4. Confirm Play console shows the build on `alpha`; monitor tester
   opt-in count for the next days (window must not dip below 12).
5. Sweep `plans/todo.md`: mark superseded wrapper items, record the
   decision reversal, keep the production-access log going (tester
   feedback + exercised features — Play asks for both).

## Success Criteria

- [x] QA matrix executed — API 36 only, per user decision 2026-08-31
      ("just api 36 also ok"); API 24/31 images not installed
- [x] `v0.2.0` on GH Releases with signed AAB + APK (guard, lint, tests
      all green in the tag run)
- [x] Play `alpha` accepted the upload (`Validating tracks: 'alpha'`);
      tester opt-in count to be monitored over the coming days
- [x] `plans/todo.md` reconciled with the new reality

## QA log — 2026-08-31, API 36 emulator (Medium_Phone_API_36.1)

Executed by the implementation session; screenshots in the session
scratchpad. API 24/31 images are not installed locally — that part of the
matrix plus tagging remain open.

- [x] `connectedDebugAndroidTest`: 9/9 green (player golden path, master
      draw/reset/auto relabel, settings mode switch, smoke)
- [x] Golden path manual: generate → cross 4 → "Chờ 70" toast + amber
      cell/section rings → cross 5th → Kinh modal, exact web copy
- [x] Kill mid-round + airplane-mode cold start → exact state restored,
      completed row keeps win styling, no re-announcement, fully offline
- [x] Master: Ván mới → 11×9 board (90 at row 10/col 8) → Xổ số → hero,
      stats, history token, board mark; MediaCodec audio track confirmed
      in logcat (voice announced)
- [x] Dark mode cold start: dark splash color, dark theme, state intact
- [x] Back at root → exit dialog with wrapper copy; "Ở lại" keeps state
- [x] `FLAG_KEEP_SCREEN_ON` held on the app window while remaining > 0
- [ ] API 24 / API 31 runs (system images not installed)
- [ ] TalkBack + font-200% spot-check (manual)
- [ ] Tag `v0.2.0` → GH Release + Play alpha (user decision: ship when ready)

## Risk Assessment

- **Release timing (user decision, Validation Session 1):** tag
  `v0.2.0` as soon as QA passes — do NOT hold for the closed-test
  window to complete. The reset risk below was presented and accepted.
- **A tester-visible regression resets the 14-day window.** Signal:
  opt-in count drops after rollout. Response: halt further releases,
  fix-forward within days, keep spare testers engaged. If the rewrite
  cannot stabilize quickly, the previous wrapper AAB (versionCode 6) can
  be re-promoted from the Play console as an emergency rollback — verify
  this path exists in the console before tagging.
