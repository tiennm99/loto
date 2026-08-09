# Lô tô — Android (Capacitor wrapper)

![ci](https://github.com/tiennm99/loto/actions/workflows/ci.yml/badge.svg)

Fully-offline Android wrapper around the [`web/`](../web) SvelteKit PWA. All
assets — HTML, JS, CSS, and 184 voice MP3s — are bundled into the APK at build
time. No network is required at runtime.

> **Note:** the previous native Kotlin/Compose port lives in git history at
> the commit titled `docs: add post-implementation todo list` and earlier. This
> is now a thin wrapper over `web/`; the web app evolves and we rebuild + ship.

## How it works

```
../web/
  └── pnpm run build →  ../web/build/  (SvelteKit static output)
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
- Web app in `web/`: SvelteKit 2 + Vite 8 + `@sveltejs/adapter-static` + `@vite-pwa/sveltekit`
- minSdk 24 · targetSdk 36 · JDK 21 · Node 24 (Capacitor 8 requires the first two)

## Setup

```bash
git clone https://github.com/tiennm99/loto.git
cd loto/android
npm ci
npm run build      # builds web/ + cap sync into android/
```

## Build

### Debug APK

```bash
npm run build              # build web/ + cap sync (must run after any web/ change)
npm run assemble:debug     # → android/app/build/outputs/apk/debug/app-debug.apk
```

### Release AAB + APK (signed)

```bash
export LOTO_KEYSTORE_PATH=$HOME/.android/miti99-apps.p12
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

## Picking up web/ changes

`web/` and `android/` live in the same repository, so there is no pin to bump —
rebuild and re-sync after any change under `web/`:

```bash
npm run build              # rebuild web/ + re-sync into android/
```

## Why no INTERNET permission?

The whole web build (HTML, JS, CSS, fonts, manifest, icons, all 184 MP3s)
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

1. Download the APK from the [Actions artifact](https://github.com/tiennm99/loto/actions) (debug)
   or [Releases](https://github.com/tiennm99/loto/releases) (signed).
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

Workflows live at the repository root in `.github/workflows/`.

| Workflow | Trigger | Result |
|----------|---------|--------|
| `ci` (`android-debug` job) | push to `main`, any PR touching `web/` or `android/` | unsigned debug APK uploaded as artifact |
| `android-release` | tag `v*.*.*` | tests, then signed AAB + APK attached to GH Release |

Both `cap sync` a web bundle into `android/android/`, then run Gradle there.
They differ in where the bundle comes from: the `android-debug` job downloads
the artifact `ci` already built, so `web/` is never built twice in one run,
while `android-release` builds `web/` itself from the tagged tree. Neither
runs `npm run build` in `android/` — that script would rebuild `web/`.

Toolchain setup is shared through the composite actions
`.github/actions/setup-web` and `.github/actions/setup-android`.

### GitHub Secrets (release only)

| Secret | Required for | Description |
|--------|--------------|-------------|
| `KEYSTORE_BASE64` | signed build | `base64 -w0 miti99-apps.p12` |
| `KEYSTORE_PASSWORD` | signed build | Keystore password |
| `KEY_ALIAS` | signed build | Key alias |
| `KEY_PASSWORD` | signed build | Key password |
| `PLAY_SERVICE_ACCOUNT_JSON` | Play Store auto-publish (optional) | Full JSON content of Google Cloud service account key |

**Never commit `*.jks`, `*.keystore`, `*.p12`, service-account JSON, or `.env`.**

## Google Play Store

### One-time manual setup (cannot be automated)

1. Sign up at [play.google.com/console](https://play.google.com/console/signup) ($25 one-time)
2. Create the app entry with package name `com.miti99.loto`
3. Build a signed AAB (`npm run assemble:release` locally, or push a `v*.*.*` tag to use `android-release.yml`) and **upload manually** to the Internal Testing track via the Play Console UI — Google requires the first upload to be manual
4. Fill out store listing: icon (512×512), feature graphic (1024×500), 2–8 screenshots, short + full description, category, content rating, target audience, **privacy policy URL** (host on GH Pages), data safety form (declare "No data collected" since the app is offline)
5. Submit for review (1–7 days first time)

### Auto-publish setup (after first manual upload)

See [`docs/play-store-publishing.md`](../docs/play-store-publishing.md) for the
full walkthrough: service-account creation, granting Play Console permissions,
setting the GitHub secrets (bash + PowerShell commands), cutting a release,
and troubleshooting. Short version: once `PLAY_SERVICE_ACCOUNT_JSON` is set,
every `v*.*.*` tag builds a signed AAB + APK, attaches both to a GitHub
Release, and uploads the AAB to the Play Console **Internal track**. Promote
internal → closed → open → production via the Play Console UI (or change
`tracks: internal` in `android-release.yml` to automate further).

**Important:** every release must increment `versionCode` in `android/app/build.gradle` before tagging — Play Console rejects duplicate versionCodes.

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

Bundled by the web app under `web/static/audio/{hoai-my,nam-minh}/{1..90,cho,kinh}.mp3`,
served by the wrapper from `https://localhost/audio/...`. No audio post-processing
on the Android side.

## License

Apache-2.0 — see [`LICENSE`](../LICENSE) at the repository root.
