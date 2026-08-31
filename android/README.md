# Lô tô — Android

Native Kotlin + Jetpack Compose app for *lô tô hội chợ*, at feature parity
with the [`web/`](../web) SvelteKit app. Fully offline: the APK declares no
`INTERNET` permission and bundles everything it needs, including the
Vietnamese voice clips.

The web app is the behavioral spec — game rules, settings contract, and voice
semantics are ported one-to-one from `web/src/lib/` and its test suites. A
behavior change on the web side needs a matching Kotlin change here.

## Stack

- Kotlin 2.x, Jetpack Compose, Material 3, single `:app` module
- Gradle Kotlin DSL + version catalog (`gradle/libs.versions.toml`), JDK 21
- `applicationId com.miti99.loto`, minSdk 24, targetSdk 36
- Manual DI: `LotoApplication` owns the singletons; ViewModels come from
  `LotoViewModelFactory`. No Hilt.
- Media3 ExoPlayer for voice clips, DataStore Preferences for settings and
  round state

## Layout

| Package | What it is |
|---------|------------|
| `game` | Pure game rules ported from `web/src/lib/game-logic.js` and friends: card generator, row state, draw deck, auto-cross |
| `audio` | Voice catalog (parses `assets/audio/manifest.json`) + ExoPlayer wrapper with the web's cancellation semantics |
| `settings` | `Settings` model + DataStore repository — same fields, defaults, and per-field validation as the web store |
| `state` | Round persistence, the shared master store, and the three ViewModels |
| `ui` | Compose screens: player board, master panel, settings sheet, theme |

## Audio assets

The 184 MP3s (2 voices × 92 clips) and their manifest are **not copied** into
this project. `app/build.gradle.kts` mounts `../web/static/audio/` as
`assets/audio/` at build time, so web and Android always ship the same files.
Regenerate them with the web app's `scripts/generate-audio.py`.

## Audio focus

The player requests **no audio focus**: clips are ~1s speech cues layered
over whatever the table is already listening to, so music keeps playing and
incoming calls behave normally. Playback still pauses when audio would become
noisy (headphones unplugged). See `ExoVoicePlayer`.

## Permissions

`VIBRATE` only (cell-tap haptics; normal permission, no runtime prompt).
Media3 would merge `ACCESS_NETWORK_STATE` in; the manifest strips it with
`tools:node="remove"` to keep the hard offline guarantee.

## Build

Requires JDK 21 and an Android SDK (compileSdk 36).

```bash
./gradlew :app:lint :app:test :app:assembleDebug   # checks + debug APK
./gradlew :app:bundleRelease                       # release AAB
```

Debug builds need no signing setup. Release builds sign with a PKCS#12
keystore read from env vars — the same contract every previous release used:

| Env var | Meaning |
|---------|---------|
| `LOTO_KEYSTORE_PATH` | absolute path to the `.p12` keystore |
| `LOTO_KEYSTORE_PASSWORD` | keystore password |
| `LOTO_KEY_ALIAS` | key alias |
| `LOTO_KEY_PASSWORD` | key password |

Without `LOTO_KEYSTORE_PATH`, release artifacts build unsigned.

## Release

Releases are tag-driven: bump `versionCode` + `versionName` in
`app/build.gradle.kts`, tag `v*.*.*`, push. `.github/workflows/android-release.yml`
guards against a forgotten versionCode bump, runs lint + tests, builds the
signed AAB + APK, attaches them to the GitHub Release, and uploads the AAB to
the Play closed-testing track (`alpha`). Secrets and Play setup are documented
in [`docs/play-store-publishing.md`](../docs/play-store-publishing.md).

## Icons and splash

Launcher art (adaptive foreground/background, monochrome for Android 13+
themed icons, legacy mipmaps) was rendered from `web/static/icons/source.svg`
and is committed under `app/src/main/res/mipmap-*`. The splash uses
`androidx.core:core-splashscreen` on API 31+ and a layer-list drawable below,
with light/dark backgrounds matching the web (`#FFFBEB` / `#050813`).
