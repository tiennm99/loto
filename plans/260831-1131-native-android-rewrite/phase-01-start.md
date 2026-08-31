---
phase: 1
title: "Project Scaffold and Build Foundation"
status: done
priority: P1
effort: "1d"
dependencies: []
---

# Phase 1: Project Scaffold and Build Foundation

## Overview

Stand up a fresh, CI-buildable native Android project in a temporary
`android-native/` directory (renamed to `android/` in Phase 9, after the
wrapper is deleted). Everything downstream builds on this skeleton.

## Requirements

- Functional: `./gradlew :app:lint :app:test :app:assembleDebug` and
  `:app:bundleRelease` (with signing env set) succeed locally.
- Non-functional: no `INTERNET` permission; reproducible builds via
  version catalog; release build minified with R8.

## Architecture

- Single Gradle project, single `:app` module, Kotlin DSL
  (`settings.gradle.kts`, `app/build.gradle.kts`,
  `gradle/libs.versions.toml`).
- Kotlin 2.x, Compose BOM (latest stable), Material 3, activity-compose,
  lifecycle-viewmodel-compose, kotlinx-coroutines, DataStore Preferences,
  Media3 ExoPlayer. Test deps: JUnit4, kotlinx-coroutines-test, Turbine,
  compose-ui-test-junit4.
- Manual DI: an `Application` subclass owns singletons (settings repo,
  audio player); ViewModels built via a small factory. No Hilt.
- `applicationId com.miti99.loto`, `versionCode 7`, `versionName "0.2.0"`,
  minSdk 24, targetSdk 36, compileSdk 36, JDK 21 toolchain.
- Release signing config reads `LOTO_KEYSTORE_PATH`,
  `LOTO_KEYSTORE_PASSWORD`, `LOTO_KEY_ALIAS`, `LOTO_KEY_PASSWORD` env vars
  — same contract as the wrapper (`android/android/app/build.gradle` today)
  so CI secrets and local workflow are unchanged. Unsigned debug builds
  must not require them.
- Audio assets are NOT copied: `android { sourceSets["main"].assets.srcDir("../../web/static/audio") }`
  (exact relative path fixed when the dir moves in Phase 9) — mounts the
  184 MP3s + `manifest.json` under `assets/audio/` at build time.

## Related Code Files

- Create: `android-native/settings.gradle.kts`, `android-native/build.gradle.kts`,
  `android-native/gradle/libs.versions.toml`, `android-native/gradle.properties`,
  `android-native/gradlew*`, `android-native/app/build.gradle.kts`,
  `android-native/app/src/main/AndroidManifest.xml`,
  `android-native/app/src/main/java/com/miti99/loto/LotoApplication.kt`,
  `android-native/app/src/main/java/com/miti99/loto/MainActivity.kt` (empty Compose shell),
  `android-native/app/proguard-rules.pro`, `android-native/.gitignore`
- Reference (do not copy code): wrapper Gradle files under
  `android/android/`, old port `f7cbb6e:android/app/build.gradle.kts`

## Implementation Steps

1. Generate the Gradle skeleton (wrapper scripts pinned to a current
   Gradle 8.x; verify against Kotlin/AGP compatibility matrix).
2. Fill `libs.versions.toml`; wire Compose BOM; enable
   `kotlin.jvmToolchain(21)`.
3. Manifest: portrait-friendly single activity, `android:supportsRtl`
   left default-off unless spec says otherwise, `VIBRATE` permission,
   **no** `INTERNET`; declare `touchscreen`/`faketouch`/screen features
   optional (Play + emulator filtering, carried from wrapper manifest).
4. Signing config + release build type (minify on, shrinkResources on);
   assert debug build works without signing env.
5. Assets srcDir wired to `web/static/audio`; sanity-check an
   `assets.list("audio")` in a unit-less smoke check or manual apk
   inspection (`unzip -l`).
6. Placeholder `MainActivity` renders a "Lô tô" Compose text; smoke
   instrumentation test compiles (execution deferred to Phase 10).

## Success Criteria

- [ ] `assembleDebug` + `lint` + `test` pass locally without signing env
- [ ] `bundleRelease` signs with the miti99-apps.p12 keystore env contract
- [ ] `aapt dump permissions` shows only `VIBRATE`
- [ ] APK contains `assets/audio/hoai-my/1.mp3` … `assets/audio/manifest.json` (184 MP3s, 2 voices)

## Risk Assessment

- **AGP/Kotlin/Compose version drift** — pick the latest stable trio at
  implementation time; if the catalog resolution fails in CI, pin to the
  versions Android Studio's new-project template currently emits.
- **assets srcDir relative path** breaks when `android-native/` →
  `android/` (Phase 9). Signal: missing-asset crash on Phase 9 CI run.
  Response: path is defined in one place; Phase 9 checklist updates it.
