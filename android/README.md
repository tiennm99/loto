# Lô tô — Android

![build-debug](https://github.com/tiennm99/loto-android/actions/workflows/build-debug.yml/badge.svg)

Native Android port of [tiennm99/loto](https://github.com/tiennm99/loto) (SvelteKit web app).

Lô tô hội chợ TN1 — Vietnamese fairground bingo with host (quản trò) + player modes,
called-number speech in Vietnamese (Hoài My / Nam Minh voices).

## Stack

- Kotlin 2.1 + Jetpack Compose + Material 3
- Single Activity, single Gradle module (`:app`)
- Audio: AndroidX Media3 ExoPlayer (bundled MP3s in `assets/audio/`)
- Settings persistence: DataStore Preferences
- minSdk 24 · targetSdk 35 · JDK 17

## Build

### Debug APK (no signing required)

```bash
./gradlew :app:assembleDebug
# Output: app/build/outputs/apk/debug/app-debug.apk
```

### Install on connected device/emulator

```bash
./gradlew :app:installDebug
```

### Open in Android Studio

Open the repo root in Android Studio Ladybug (2024.2) or newer. The IDE picks
up the Gradle wrapper automatically. Run the `app` configuration on any API 24+
device or emulator.

## Tests

### Unit tests (JVM, no device needed)

```bash
./gradlew :app:test
```

Covers: `GameLogicTest`, `VietnameseNumberTest`, `VoiceManifestTest`,
`SettingsRepositoryTest`, `CallBusTest`, `PlayerBoardViewModelTest`,
`MasterPanelViewModelTest`, `MasterBoardLayoutTest`.

### Instrumentation tests (requires connected device or emulator)

```bash
./gradlew :app:connectedDebugAndroidTest
```

Includes `MainActivityComposeSmokeTest` — cold-start → generate grid → assert 81 cells.

## Release signing

Release builds read signing credentials from environment variables so the
keystore is never committed to the repository.

```bash
export LOTO_KEYSTORE_PATH=/path/to/loto-release.jks
export LOTO_KEYSTORE_PASSWORD=<store-password>
export LOTO_KEY_ALIAS=<key-alias>
export LOTO_KEY_PASSWORD=<key-password>

./gradlew :app:assembleRelease   # signed APK
./gradlew :app:bundleRelease     # signed AAB for Play Store
```

Local convention: store the keystore at `~/.android/loto-release.jks`.

### GitHub Secrets (for CI)

Set the following secrets in the repository settings under **Settings → Secrets → Actions**:

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | `base64 -w0 loto-release.jks` output |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias inside the keystore |
| `KEY_PASSWORD` | Key password |

**Never commit `keystore.jks`, `.env`, or any file containing these values.**

## CI / CD

| Workflow | Trigger | Tasks |
|----------|---------|-------|
| `build-debug` | push to `main`, any PR | `lint test assembleDebug` |
| `release` | tag `v*.*.*` | `bundleRelease assembleRelease` + sign + upload to GH Release |

Tag a release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

The `release` workflow will sign and attach the APK + AAB to the GitHub Release page.

## Version bump

1. Increment `versionCode` and `versionName` in `app/build.gradle.kts`.
2. Commit, tag, push tag.

## App ID

`com.miti99.loto` — registered in `AndroidManifest.xml` and `app/build.gradle.kts`.

## Audio

Bundled MP3 voice clips under `assets/audio/{hoai-my,nam-minh}/{1..90,cho,kinh}.mp3`.
Generated from the upstream repo's `scripts/generate-audio.py` using Microsoft Edge TTS.
Re-generate by running that script against a Python 3.10+ environment with `edge-tts` installed.

## License

Same license as upstream [`tiennm99/loto`](https://github.com/tiennm99/loto).
