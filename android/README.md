# Lô tô — Android (Capacitor wrapper)

![build-debug](https://github.com/tiennm99/loto-android/actions/workflows/build-debug.yml/badge.svg)

Fully-offline Android wrapper around [tiennm99/loto](https://github.com/tiennm99/loto)
(SvelteKit PWA). All assets — HTML, JS, CSS, and 184 voice MP3s — are bundled
into the APK at build time. No network is required at runtime.

> **Note:** the previous native Kotlin/Compose port lives in git history at
> commit `9a35686` and earlier. This repo is now a thin wrapper; loto evolves
> upstream and we just rebuild + ship.

## How it works

```
loto/ (git submodule, pinned SHA)
  └── npm run build  →  loto/build/  (SvelteKit static output)
                                ↓
                         npx cap sync
                                ↓
android/app/src/main/assets/public/  (bundled into APK)
                                ↓
            WebView serves https://localhost/* off-disk
```

Capacitor's bridge serves the bundled site from `https://localhost`, which is
loopback only (no INTERNET permission requested). Workbox precache, IndexedDB,
and the `<audio>` element all work offline.

## Stack

- Capacitor 8 (Android wrapper)
- Loto upstream: SvelteKit 2 + Vite 7 + `@sveltejs/adapter-static` + `@vite-pwa/sveltekit`
- minSdk 24 · targetSdk 36 · JDK 21 · Node 22 (Capacitor 8 requires both)

## Setup

```bash
git clone --recurse-submodules https://github.com/tiennm99/loto-android.git
cd loto-android
npm ci
npm run build      # builds loto + cap sync into android/
```

If you cloned without `--recurse-submodules`:

```bash
git submodule update --init --recursive
```

## Build

### Debug APK

```bash
npm run build              # build loto + cap sync (must run after any loto change)
npm run assemble:debug     # → android/app/build/outputs/apk/debug/app-debug.apk
```

### Release AAB + APK (signed)

```bash
export LOTO_KEYSTORE_PATH=$HOME/.android/loto-release.jks
export LOTO_KEYSTORE_PASSWORD=<store-password>
export LOTO_KEY_ALIAS=<key-alias>
export LOTO_KEY_PASSWORD=<key-password>

npm run build
npm run assemble:release
# → android/app/build/outputs/{apk/release/*.apk, bundle/release/*.aab}
```

### Open in Android Studio

```bash
npx cap open android
```

## Updating loto

```bash
cd loto
git fetch origin
git checkout <new-sha>
cd ..
git add loto
npm run build              # rebuild + re-sync
git commit -m "chore: bump loto submodule to <sha>"
```

## Why no INTERNET permission?

The whole loto build (HTML, JS, CSS, fonts, manifest, icons, all 184 MP3s)
ships inside the APK. The WebView loads from `https://localhost`, which is
loopback. No remote fetches happen at runtime, so the permission is omitted —
this makes "fully offline" a hard guarantee, not a convention.

If you ever add a remote feature (analytics, sync, etc.), add this back to
`android/app/src/main/AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
```

## Running on BlueStacks / NoxPlayer / Android emulators

The APK has no native libraries (`lib/` is empty), so it's architecture-
independent — same APK installs on x86_64 emulators and ARM phones.

1. Download the APK from the [Actions artifact](https://github.com/tiennm99/loto-android/actions) (debug)
   or [Releases](https://github.com/tiennm99/loto-android/releases) (signed).
2. Drag-drop the APK onto the BlueStacks window, or use **Install APK**
   from the sidebar.
3. Launch "Lo To" from the BlueStacks home screen.

If the app shows a blank white screen on first launch, open chrome://inspect
on the host machine while BlueStacks is running, click **Inspect** on the
WebView, and check the console — the WebView debugging is enabled in debug
builds (Capacitor default behavior, no INTERNET permission needed because
chrome://inspect uses ADB).

Manifest declares `touchscreen`, `faketouch`, `screen.portrait`, and
`screen.landscape` as **optional** so the Play Store and emulators don't
filter the app out.

## CI / CD

| Workflow | Trigger | Result |
|----------|---------|--------|
| `build-debug` | push to `main`, any PR | unsigned debug APK uploaded as artifact |
| `release` | tag `v*.*.*` | signed AAB + APK attached to GH Release |

Both workflows checkout the loto submodule, install npm deps, build loto,
`cap sync`, then run Gradle inside `android/`.

### GitHub Secrets (release only)

| Secret | Description |
|--------|-------------|
| `KEYSTORE_BASE64` | `base64 -w0 loto-release.jks` |
| `KEYSTORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Key alias |
| `KEY_PASSWORD` | Key password |

**Never commit `*.jks`, `*.keystore`, or `.env`.**

Tag a release:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Version bump

1. Edit `versionCode` and `versionName` in `android/app/build.gradle`.
2. Commit, tag, push.

## App ID

`com.miti99.loto` — set in `capacitor.config.json` and `android/app/build.gradle`.

## Audio

Bundled by upstream loto under `loto/static/audio/{hoai-my,nam-minh}/{1..90,cho,kinh}.mp3`,
served by the wrapper from `https://localhost/audio/...`. No audio post-processing
on the Android side.

## License

Same license as upstream [`tiennm99/loto`](https://github.com/tiennm99/loto).
