---
title: "Native Android Rewrite"
description: "Rewrite android/ as a fresh native Kotlin + Jetpack Compose app, replacing the Capacitor WebView wrapper entirely"
status: in-progress
priority: P1
effort: "6-9d"
tags: [android, kotlin, compose, rewrite]
created: 2026-08-31
supersedes: [260429-1511-loto-upstream-sync]
---

# Native Android Rewrite

## Overview

Replace the Capacitor WebView wrapper in `android/` with a **fresh** native
Kotlin + Jetpack Compose app at full feature parity with the current
`web/` SvelteKit app. The web app's source under `web/src/lib/` is the
behavioral spec; the retired 2026-04 native port (git `f7cbb6e`) is
**reference reading only** — no code is resurrected (user decision,
2026-08-31). The wrapper is deleted in the same effort; the native app
takes over the package id, signing key, and tag-driven release pipeline.

**Decision reversal on record:** `plans/todo.md` records "Native
Kotlin/Compose port retired (2026-05-10) in favour of the wrapper, for
maintenance parity." The user explicitly reversed this on 2026-08-31.
Trade-off accepted: every future `web/` feature must now be hand-ported
to Kotlin; the two apps no longer ship from one codebase.

## Constraints (non-negotiable)

- **Play continuity:** `applicationId com.miti99.loto`, same keystore
  (env `LOTO_KEYSTORE_*`, CI secrets unchanged), `versionCode >= 7`
  (wrapper shipped 6), releases keep flowing to the Play closed track
  (`alpha`) via `v*.*.*` tags.
- **Fully offline, hard guarantee:** no `INTERNET` permission. `VIBRATE`
  only.
- **Toolchain:** Kotlin 2.x + Jetpack Compose + Material 3, single `:app`
  module, Gradle Kotlin DSL + version catalog, minSdk 24, targetSdk 36,
  JDK 21. Manual DI (no Hilt). Media3 ExoPlayer for voice clips.
  DataStore Preferences for settings.
- **Vietnamese-first UI**, matching current web copy and the sky-blue
  brand (`d2c79d6` replaced the old green/rose accents).

## Goals

| # | Goal | Priority |
|---|------|----------|
| 1 | Native app at feature parity with `web/` (board, master panel, audio, settings, persistence) | P1 |
| 2 | Android platform behaviors kept from the wrapper era (keep-screen-on, back/overlay/exit-confirm, haptics, splash, adaptive icon, dark mode) | P1 |
| 3 | Capacitor wrapper deleted; CI + release pipeline rewired to Gradle-only Android jobs | P1 |
| 4 | Signed 0.2.0 (versionCode 7) AAB reaches the Play `alpha` closed track via the existing tag flow | P1 |

## Non-goals

- iOS target, multiplayer, i18n, background audio, share sheet — all stay
  in the `plans/todo.md` parking lot.
- Migrating WebView `localStorage` (wrapper users' settings/round state)
  into the native app — loss accepted (Validation Session 1); release
  notes must state it.
- Web app changes of any kind (`web/` is untouched except docs links).

## Phases

| # | Phase | Status | Depends on |
|---|-------|--------|------------|
| 1 | [Project Scaffold and Build Foundation](./phase-01-start.md) | Done | — |
| 2 | [Game Logic Core](./phase-02-game-logic-core.md) | Done | 1 |
| 3 | [Audio Subsystem](./phase-03-audio-subsystem.md) | Done | 1 |
| 4 | [Settings and Game State Persistence](./phase-04-settings-and-game-state-persistence.md) | Done | 1, 2 |
| 5 | [Player Board UI](./phase-05-player-board-ui.md) | Done | 2, 3, 4 |
| 6 | [Master Panel UI](./phase-06-master-panel-ui.md) | Done | 2, 3, 4 |
| 7 | [Settings UI](./phase-07-settings-ui.md) | Done | 4, 5, 6 |
| 8 | [Platform Behaviors and Branding](./phase-08-platform-behaviors-and-branding.md) | Done | 5, 6, 7 |
| 9 | [Wrapper Removal and CI Rewire](./phase-09-wrapper-removal-and-ci-rewire.md) | Done | 8 |
| 10 | [QA and Release](./phase-10-qa-and-release.md) | In progress (API 36 QA done; API 24/31 + tag open) | 9 |

Phases 2 and 3 are independent after 1. Phases 5 and 6 are independent
after 4. Everything else is sequential.

## Behavioral spec sources (read before each phase)

| Area | Authoritative source |
|------|----------------------|
| Card generation, chờ/kinh row logic | `web/src/lib/game-logic.js` + `game-logic.test.js` |
| Draw deck, master state | `web/src/lib/master-store.svelte.js` + test |
| Settings contract | `web/src/lib/settings-store.svelte.js` (DEFAULT_SETTINGS) |
| Voice semantics (cancellation, chờ+N sequence) | `web/src/lib/voice.js` + test |
| Auto-cross, auto-countdown | `web/src/lib/player-auto-cross.js`, `AutoCountdown.svelte` |
| Wrapper-era platform behaviors | `plans/todo.md` (device QA list), old `android/.../MainActivity.java`, `web/src/lib/wake-lock.js`, `overlay-history.js` |
| Visual parity details (confetti tiers, chờ ring, palette, hint copy) | superseded plan `plans/260429-1511-loto-upstream-sync/` phase files |
| Old port architecture (reference only, no code reuse) | git `f7cbb6e` tree, commit message of `90a9dd1` |

## Success Criteria

- [ ] `./gradlew :app:lint :app:test :app:assembleDebug` green in CI with no web-build dependency
- [ ] All game-logic unit tests ported from `web/src/lib/*.test.js` pass in Kotlin
- [ ] Golden path on emulator: generate card → draw numbers (manual + auto) → voice announces → chờ ring/toast → complete row → Kinh modal + confetti + audio
- [ ] Settings round-trip: every `DEFAULT_SETTINGS` field editable, persisted, restored after process death
- [ ] APK has no `INTERNET` permission; airplane-mode cold start fully functional
- [ ] `android/` contains no Capacitor/Node artifacts; root README + android README describe the native app
- [ ] Tag `v0.2.0` produces signed AAB+APK on the GH Release and uploads to Play track `alpha` (versionCode 7)

## Risks

| Risk | Impact | Mitigation |
|------|--------|------------|
| Play closed-test window (12 testers, 14-day continuous) breaks if the rewrite crashes for testers | Production-access clock resets | Full Phase-10 emulator QA before tagging; keep spare testers; release notes flag the rewrite |
| Wrapper→native update wipes WebView localStorage (settings + in-progress round) | Testers lose settings once | Accepted (Validation Session 1); state it in release notes |
| Maintenance parity regression (the original retirement reason) | Future web features need manual Kotlin ports | Accepted by user decision 2026-08-31; recorded above |
| versionCode footgun (manual bump before tag) | Play rejects upload | Phase 9 adds the long-parked CI guard: release workflow fails if tag's versionCode ≤ last released |
| 184 MP3s duplicated between web and android | Asset drift | Gradle `srcDir` points at `../web/static/audio` — single source of truth, no copy step |

## Open questions

None — both resolved in Validation Session 1 (see log).

## Validation Log

### Session 1 — 2026-08-31
**Trigger:** `/ak:plan validate` after initial plan creation
**Questions asked:** 4

#### Verification Results
- **Tier:** Full (10 phases)
- **Claims checked:** 20
- **Verified:** 18 | **Failed:** 1 | **Refined:** 1

Verified highlights: `web/static/audio/manifest.json` shape + 92×2 MP3s
incl. cho/kinh; theme colors `#1565c0`/`#0a0f1f` (`web/src/app.html:21-22`);
wrapper `versionCode 6` / `LOTO_KEYSTORE_*` env contract
(`android/android/app/build.gradle:10-29`); `tracks: alpha` + secrets
names (`.github/workflows/android-release.yml:47-93`); superseded plan
phase files; chờ = exactly-one-remaining-in-row
(`web/src/lib/game-logic.js:354`); all cited `web/src/lib/*` spec files.

Failures/refinements (both propagated):
1. [Fact Checker] `plans/reports/researcher-260427-*` — not found;
   phase 3 now cites only the `f7cbb6e:android/plans/todo.md` decision
   record for the ExoPlayer pick.
2. [Fact Checker] Exit-dialog Vietnamese strings live in wrapper
   `res/values/strings.xml:9-12`, not `MainActivity.java` — phase 8
   pointer corrected.

#### Questions & Answers

1. **[Assumptions]** Wrapper→native update wipes WebView localStorage —
   accept, or one-time migration?
   - Options: Accept the loss (Recommended) | One-time WebView migration
   - **Answer:** Accept the loss
   - **Rationale:** Rounds are minutes-long; avoids a WebView dependency
     in a WebView-free app. Release notes must state it (Phase 10).
2. **[Risks]** First native release timing vs the running 14-day Play
   closed-test window (completes ~2026-09-02)?
   - Options: After production access (Recommended) | Ship when ready
   - **Answer:** Ship when ready — **user overrode the recommendation**;
     tag `v0.2.0` as soon as Phase 10 QA passes, window running or not.
3. **[Scope]** First native version identifier?
   - Options: 0.2.0 / code 7 (Recommended) | 1.0.0 | 0.1.3
   - **Answer:** 0.2.0 / versionCode 7 (as planned).
4. **[Assumptions]** Android SDK/emulator availability for Phase 10 QA?
   - Options: Emulator available | Physical device only | Neither yet
   - **Answer:** Neither yet — Phase 10 gains a step 0: install Android
     Studio/SDK + create API 24/31/36 AVDs (or attach a device via ADB).

#### Confirmed Decisions
- localStorage migration: none — accept one-time loss, note in release notes
- Release timing: ship when Phase 10 QA passes (user decision, risk accepted)
- Version: `0.2.0` / versionCode 7
- QA environment: SDK/emulator setup is now part of Phase 10

#### Impact on Phases
- Phase 3: dropped stale researcher-report citation
- Phase 8: exit-string spec pointer → `res/values/strings.xml`
- Phase 10: added SDK/emulator setup step; release-timing decision recorded

### Whole-Plan Consistency Sweep
- Files reread: plan.md, phase-01 … phase-10 (all current in session)
- Decision deltas checked: 4 (migration, timing, version, QA env)
- Reconciled stale references: 2 (Non-goals + Risks table both said the
  migration question was open; now marked resolved)
- Unresolved contradictions: 0

<!-- slug: native-android-rewrite -->
