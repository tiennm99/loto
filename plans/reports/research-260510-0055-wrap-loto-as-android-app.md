# Research Report: Wrapping the `loto` SvelteKit PWA as an Android App (No Source Changes)

**Date:** 2026-05-10
**Question:** Can we wrap `tiennm99/loto` inside an Android app — writing only Android-side code, leaving the loto codebase untouched?

---

## TL;DR

**Yes, easily.** The loto project is already a near-perfect candidate:

- SvelteKit + `@sveltejs/adapter-static` → ships as a static `build/` folder
- Already a PWA (`@vite-pwa/sveltekit`, hand-written `manifest.webmanifest`, Workbox SW)
- Audio is bundled MP3s in `static/audio/` → no network needed at runtime once cached
- Already deploys to GitHub Pages at `/loto` (`BUILD_PROFILE=gh`)

Three viable Android-only wrapper approaches, ranked:

| # | Approach | Modifies loto? | Effort | Best when |
|---|----------|----------------|--------|-----------|
| 1 | **TWA via Bubblewrap** | No | XS | You're OK requiring HTTPS hosting + Chrome on device |
| 2 | **Capacitor (web-shell)** | No (just consumes `build/`) | S | You want fully offline-from-install, Play Store distribution |
| 3 | **Raw Android WebView** | No | M | You want pure Android code, no Node toolchain in the wrapper repo |

The current `loto-android` repo is a *native Kotlin port* — that's a much bigger surface than any of the above. If maintenance burden is a concern, a wrapper would have been ~10× less code.

---

## 1. Approach Overview

### Option 1 — Trusted Web Activity (TWA) via Bubblewrap **[recommended]**

Bubblewrap is Google's CLI that generates an Android Studio project that launches a deployed PWA in a Chrome Custom Tab branded as your app. ([GoogleChromeLabs/bubblewrap](https://github.com/GoogleChromeLabs/bubblewrap))

**How it fits loto:**
- Loto already deploys to GitHub Pages → `https://tiennm99.github.io/loto/` (HTTPS ✓)
- `manifest.webmanifest` already exists ✓
- Bubblewrap reads the manifest and generates the Android project; you publish to Play Store

**Steps (high-level):**
```bash
npm i -g @bubblewrap/cli
bubblewrap init --manifest=https://tiennm99.github.io/loto/manifest.webmanifest
bubblewrap build         # produces signed APK + AAB
```

Plus one server-side step: publish `assetlinks.json` at `https://tiennm99.github.io/.well-known/assetlinks.json` to verify domain ownership. Without it, the URL bar shows up — minor but ugly.

**Pros**
- Zero loto changes, zero JS shim, zero WebView quirks (it *is* Chrome)
- App auto-updates the moment the PWA deploys — no Play Store release needed for content
- Smallest APK (~2 MB) and best audio/perf since it's the real Chromium
- Official Google path, supported on Play Store

**Cons**
- Requires Chrome (or any TWA-capable browser) on the device — true for ~99% of Android phones with Play Services
- Needs network for first launch (PWA caches kick in afterwards). If first launch must be offline → use Capacitor.
- `assetlinks.json` setup needed for clean address-bar-less UX
- Min Android 5.0 (Lollipop) — well below loto-android's current `minSdk 24`

---

### Option 2 — Capacitor

Capacitor wraps any static web build (HTML/CSS/JS) inside a native Android (and iOS) shell, serving assets locally via `https://localhost`. Officially supported with SvelteKit `adapter-static`. ([capacitorjs.com/solution/svelte](https://capacitorjs.com/solution/svelte), [bryanhogan.com](https://bryanhogan.com/blog/web-to-app-sveltekit-capacitor))

**How it fits loto:**
- Build loto as static (`npm run build` → `./build/`)
- Point Capacitor's `webDir` at `loto/build`
- `cap add android` generates an Android Studio project; `cap sync` copies assets in
- Audio MP3s in `static/` end up bundled in the APK

**Configuration nuance:** loto's `BUILD_PROFILE=gh` sets `paths.base = "/loto"`. For Capacitor use the **default profile** (`npm run build`, base = `""`) so assets resolve at `https://localhost/`.

**Pros**
- Fully offline from first launch (audio + app shell shipped in APK)
- No reliance on user having Chrome/TWA
- Plugin ecosystem if you ever want native features (haptics, share, etc.)
- Wrapper lives in a separate repo, loto is consumed as a built artifact

**Cons**
- Wrapper repo needs Node + `npx cap` tooling, not pure-Android
- APK ~10–15 MB larger (bundled WebView assets + audio)
- WebView ≠ Chrome; less common quirks but they exist
- The ServiceWorker layer adds a footgun under WebView (see option 3 caveat)

---

### Option 3 — Raw Android WebView (pure Android code)

Plain `WebView` inside an `Activity`, loading either:
- **(a)** the remote PWA URL (`https://tiennm99.github.io/loto/`) — like a TWA but uglier and you maintain it, or
- **(b)** the bundled `build/` copied into `app/src/main/assets/` and served via `WebViewAssetLoader` at a virtual `https://appassets.androidplatform.net/...` host.

**Pros**
- 100% Android code in the wrapper repo — no Node toolchain
- Full control over Activity lifecycle, splash, intents, deep links

**Cons**
- You re-implement what TWA/Capacitor give for free: back-button handling, file uploads, permissions, audio focus, etc.
- **Service Worker pitfall:** WebView's SW lifecycle is shaky — registrations can drop on cold start unless you wire up `ServiceWorkerController` + `ServiceWorkerClient` correctly. ([w3tutorials.net](https://www.w3tutorials.net/blog/how-to-use-service-workers-in-android-webview/))
- Audio MIME types, autoplay policy, `loadWithOverviewMode` quirks all become your problem.
- Updates require Play Store release if (b); requires network if (a).

Use this only if you have a hard "no Node in the build pipeline" rule.

---

## 2. Comparative Analysis

| Dimension | TWA / Bubblewrap | Capacitor | Raw WebView |
|---|---|---|---|
| Loto code changes | none | none | none |
| First-launch offline | ✗ (cache after 1st load) | ✓ | ✓ if assets bundled |
| APK size | ~2 MB | ~10–15 MB | ~5–15 MB |
| Audio/perf fidelity | Chrome (best) | System WebView | System WebView |
| Auto-update on PWA deploy | ✓ | ✗ (need new APK) | ✓ if remote, ✗ if bundled |
| Wrapper toolchain | Node CLI once, then Android | Node + `npx cap` ongoing | Pure Android |
| Maintenance burden | Lowest | Low–Medium | Medium |
| Play Store eligibility | ✓ (official) | ✓ | ✓ |

---

## 3. Recommendation for loto

**Pick Bubblewrap/TWA unless first-launch offline is a hard requirement.** Reasons:

1. The loto PWA already has manifest + SW + bundled audio — the Workbox `additionalManifestEntries` precaches the default voice's clips on first visit, so once a user opens the app once, they're offline-capable. Acceptable for a fairground bingo app where users typically install before the event.
2. Cuts maintenance to ~near zero. Loto evolves; TWA app body never needs touching.
3. `assetlinks.json` is the only friction. Place at `https://tiennm99.github.io/.well-known/assetlinks.json` → done. (Note GitHub Pages serves the user-page apex domain, not project-page subpaths, so you'd need a thin `tiennm99.github.io` repo with a `static.json`/`.well-known/` route — or use a custom domain.)

**If first-launch offline is required** (e.g., users install at the venue with no Wi-Fi): use **Capacitor**. Slightly larger APK, but everything is in the bundle.

**Skip raw WebView** unless the requirement is explicitly "no Node tooling." It's the most code with the worst payoff.

---

## 4. Implication for the existing `loto-android` repo

The current repo is a full-blown Kotlin/Compose **rewrite** of loto: `GameLogicTest`, `VietnameseNumberTest`, ExoPlayer audio, DataStore settings, Compose UI, etc. That's ~30+ files of native code that must track every loto feature change.

A wrapper approach (any of the three) would have been:
- 1 `MainActivity` + manifest + a few drawables (TWA), or
- A Capacitor scaffold + 1 plugin-wiring file

That's the trade-off. Native gives you native feel + tighter Play Store store presence; wrapper gives you ~zero divergence cost. The `loto-android` README already documents a parallel codebase ("Native Android port of tiennm99/loto"), which is the long-term cost you've taken on.

If the goal of this question is **"should we replace loto-android with a wrapper?"** — yes, you can, and the loto code itself doesn't need to change. The question is whether you want to throw away the existing native UI work. If the native port already shipped and tests pass, sunk cost is real.

---

## 5. Quick-Start Snippets

### Bubblewrap

```bash
# In a new directory, NOT the loto repo
npm i -g @bubblewrap/cli
bubblewrap init --manifest=https://tiennm99.github.io/loto/manifest.webmanifest
# Answer prompts: app id (com.miti99.loto.twa), display name, etc.
bubblewrap build
# Output: app-release-bundle.aab + app-release-signed.apk

# Get the SHA256 fingerprint Bubblewrap printed and put this in
# https://tiennm99.github.io/.well-known/assetlinks.json:
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.miti99.loto.twa",
    "sha256_cert_fingerprints": ["<FROM BUBBLEWRAP>"]
  }
}]
```

### Capacitor

```bash
# In a new wrapper repo:
npm init -y
npm i @capacitor/core @capacitor/android
npm i -D @capacitor/cli
npx cap init "Lo To" com.miti99.loto --web-dir=../loto/build
# Build loto first (default profile, NOT BUILD_PROFILE=gh)
( cd ../loto && npm run build )
npx cap add android
npx cap sync
npx cap open android   # opens in Android Studio
```

### Raw WebView (sketch)

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val assetLoader = WebViewAssetLoader.Builder()
      .addPathHandler("/assets/", AssetsPathHandler(this))
      .build()
    val webView = WebView(this).apply {
      settings.javaScriptEnabled = true
      settings.domStorageEnabled = true
      settings.mediaPlaybackRequiresUserGesture = false
      webViewClient = object : WebViewClient() {
        override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
          assetLoader.shouldInterceptRequest(request.url)
      }
      loadUrl("https://appassets.androidplatform.net/assets/index.html")
    }
    setContentView(webView)
  }
}
```

You'd also need `ServiceWorkerController.getInstance().setServiceWorkerClient(...)` so SW fetches go through the same `assetLoader`.

---

## 6. Common Pitfalls

- **Capacitor + GH base path:** if you `BUILD_PROFILE=gh` before `cap sync`, all asset URLs become `/loto/...` and break under `https://localhost/`. Use the default build profile.
- **Bubblewrap + no `assetlinks.json`:** app launches with a Chrome-style URL bar. Fixable post-launch but jarring for first impressions.
- **WebView SW persistence:** if the offline experience randomly breaks after app restart, it's the Service Worker registration getting GC'd. Configure `ServiceWorkerController`.
- **Audio autoplay:** WebView blocks autoplay by default. Set `settings.mediaPlaybackRequiresUserGesture = false` (only for option 3 — TWA/Capacitor handle this).
- **Play Store policy:** TWAs and Capacitor apps are explicitly allowed; raw WebView shells that are "thin browsers" are sometimes flagged. Add native value (offline-first, push, etc.) to be safe.

---

## 7. Resources

### Official
- [GoogleChromeLabs/bubblewrap (CLI)](https://github.com/GoogleChromeLabs/bubblewrap)
- [TWA Quick Start — Android Developers](https://developer.android.com/develop/ui/views/layout/webapps/guide-trusted-web-activities-version2)
- [Adding Your PWA to Google Play (codelab)](https://developers.google.com/codelabs/pwa-in-play)
- [Capacitor + Svelte](https://capacitorjs.com/solution/svelte)
- [SvelteKit Service Workers](https://svelte.dev/docs/kit/service-workers)
- [vite-pwa/sveltekit](https://github.com/vite-pwa/sveltekit) (already in use by loto)

### Walkthroughs
- [From Web to Native: SvelteKit & Capacitor — Bryan Hogan](https://bryanhogan.com/blog/web-to-app-sveltekit-capacitor)
- [Cross-Platform SvelteKit & Capacitor — Ionic Blog](https://ionic.io/blog/cross-platform-sveltekit-capacitor-application-yes-its-possible)
- [PWA + Bubblewrap — Thinktecture](https://www.thinktecture.com/en/pwa/twa-bubblewrap/)
- [Submitting a PWA via Bubblewrap — Vaadin](https://vaadin.com/blog/submitting-a-pwa-to-google-play-store-using-bubblewrap)
- [Service Workers in Android WebView](https://www.w3tutorials.net/blog/how-to-use-service-workers-in-android-webview/)

---

## Unresolved Questions

1. Is the current native `loto-android` already published to Play Store / shipped to users? If yes, ABI-compat (`com.miti99.loto`) means a wrapper migration would replace the existing app — UX/data-migration plan needed (DataStore settings won't survive).
2. Do users typically install **before** going to the venue (likely yes — fairground game) or at the venue with no Wi-Fi (would force Capacitor over TWA)?
3. Does loto need any feature that pure-web can't do well (background audio while screen off, lock-screen controls, system media notification)? If yes, native (current path) wins; if no, wrapper wins.
4. Custom domain plan? `assetlinks.json` on `tiennm99.github.io` requires writing to the user-page repo — using a custom domain (e.g., `loto.miti99.com`) sidesteps this cleanly.
