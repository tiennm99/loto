---
title: "Plan: native Android rewrite of loto"
date: 2026-08-31
summary: Planned and validated the fresh Kotlin/Compose rewrite replacing the Capacitor wrapper
---

# Plan: native Android rewrite of loto

## What happened

Planned a full native Android rewrite of loto (`plans/260831-1131-native-android-rewrite/`, 10 phases, ~6-9d). The user reversed the 2026-05-10 "wrapper for maintenance parity" decision. Notably a complete Kotlin/Compose port already exists at git `f7cbb6e` — the user chose **fresh rewrite** over resurrecting it, and full wrapper replacement over dual-track transition.

Scouted: web drift since the old port's retirement is only 3 commits (sky-blue theme, wrapper behavior gaps, deps). Verification pass (Full tier, 20 claims): 18 verified against source; 2 corrections propagated (missing researcher-260427 reports; exit-dialog strings live in `res/values/strings.xml:9-12`, not MainActivity.java).

Marked `plans/260429-1511-loto-upstream-sync` superseded (it targeted the retired old port); its phases 01-04 remain referenced as behavior specs (confetti tiers, chờ ring, palette, hint copy).

## Decisions (validation interview, 4 questions)

- WebView localStorage migration: **accept the one-time loss**, state in release notes.
- Release timing: **ship when QA passes** — user overrode the "hold until Play production access" recommendation despite the 14-day closed-test window completing ~2026-09-02.
- Version: 0.2.0 / versionCode 7 (wrapper shipped 6).
- QA env: no Android SDK/emulator on this machine yet — Phase 10 gained a setup step 0.

## Constraints locked

`com.miti99.loto`, same `LOTO_KEYSTORE_*` signing contract, no INTERNET permission, minSdk 24 / targetSdk 36, audio assets mounted from `web/static/audio` via Gradle srcDir (no copy), Play `alpha` track via existing tag flow. Phase 9 adds the long-parked versionCode CI guard.

## Next steps

`/ak:cook D:/tiennm99/loto/plans/260831-1131-native-android-rewrite/plan.md` (whole-plan consistency sweep: 0 unresolved contradictions). Note: `.claude/scripts/set-active-plan.cjs` does not exist in this repo; `ak plan use` holds the active-plan pointer.

> Historical work record — not durable authority. Prefer docs/specs/ADRs for current decisions.
