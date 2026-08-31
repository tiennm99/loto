---
phase: 9
title: "Wrapper Removal and CI Rewire"
status: done
priority: P1
effort: "0.5d"
dependencies: [8]
---

# Phase 9: Wrapper Removal and CI Rewire

## Overview

Delete the Capacitor wrapper, move the native project into `android/`,
and rewire CI + release workflows to Gradle-only Android jobs. Web
pipeline stays untouched.

## Requirements

- Functional: `android/` = the native Gradle project root (no nested
  `android/android/`, no `package.json`, no Capacitor config).
- CI `ci.yml`: the Android job no longer consumes the web build
  artifact — it runs `lint`, `test`, `assembleDebug` and uploads the
  debug APK artifact. Web jobs (test/build/deploy-firebase/deploy-pages/
  preview) unchanged.
- `android-release.yml` on `v*.*.*`: gradle test → signed AAB + APK →
  GH Release assets → Play upload to track `alpha` (same action + same
  secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`,
  `KEY_PASSWORD`, `PLAY_SERVICE_ACCOUNT_JSON`). No web build, no
  `cap sync`.
- **versionCode CI guard (closes the long-parked footgun):** the release
  workflow fails fast if the tag's `versionCode` in
  `android/app/build.gradle.kts` is not greater than the versionCode of
  the previous release tag (git-diff based check step).
- `.github/actions/setup-android` keeps JDK setup; drop Node from the
  Android path (`setup-web` remains for web jobs). Update the composite
  action only where the Android side used it.
- Docs: root `README.md` (layout table, CI graph, quick start),
  `android/README.md` full rewrite (native stack, build, release,
  permissions rationale kept — no INTERNET), `docs/play-store-publishing.md`
  spot-check (paths to build.gradle change),
  `plans/todo.md`: drop wrapper-only items, keep still-valid ones.

## Architecture

Move, don't rewrite: `git rm -r` the Capacitor tree, `git mv
android-native android`, fix the audio-assets relative path
(`../web/static/audio` from `android/app/`), fix workflow paths.
History preserves the wrapper (like the old native port before it) —
note the swap in the commit message and `plans/todo.md` decisions record.

## Related Code Files

- Delete: `android/{package.json,package-lock.json,capacitor.config.json,assets,README.md}`,
  `android/android/**` (entire Capacitor Gradle tree)
- Move: `android-native/**` → `android/**`
- Modify: `.github/workflows/ci.yml`, `.github/workflows/android-release.yml`,
  `.github/actions/setup-android/action.yml`, root `README.md`,
  `docs/play-store-publishing.md`, `plans/todo.md`
- Create: `android/README.md` (rewritten)

## Implementation Steps

1. Delete wrapper, move project, fix assets srcDir path; local
   `assembleDebug` green.
2. Rewire `ci.yml` Android job (no web artifact dependency).
3. Rewire `android-release.yml`; add the versionCode guard step.
4. Update docs (README pair, publishing doc, todo.md decisions record:
   "wrapper retired 2026-08-31 in favour of fresh native rewrite,
   reversing the 2026-05-10 decision").
5. Push branch; verify `ci` run green end-to-end including APK artifact.

## Success Criteria

- [ ] CI green on the PR: web jobs untouched and passing, Android job builds debug APK without web artifact
- [ ] `git grep -i capacitor` returns only historical docs/plans mentions
- [ ] versionCode guard demonstrably fails on an unbumped code (test with a scratch tag or workflow_dispatch dry run)
- [ ] Docs accurate against the new tree (link check)

## Risk Assessment

- **Play closed-test continuity:** nothing in this phase touches Play
  until a tag is pushed (Phase 10 owns the release). Keep `main`
  releasable: merge this PR only when Phase 10's QA is about to follow.
