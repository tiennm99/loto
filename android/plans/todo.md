# Lô tô Android — TODO

## Direction change (2026-05-10)

Repo migrated from **native Kotlin/Compose port** to a **Capacitor wrapper**
around the upstream `tiennm99/loto` SvelteKit PWA. Native code lives in git
history at commits `9a35686` and `fe52232` — recoverable but no longer the
ship target.

Reason: maintenance parity. Loto evolves; the wrapper rebuilds and ships
without re-porting features.

## P0 — Verify CI build (cannot do locally, no JDK/SDK in dev sandbox)

- [ ] Push to a PR branch → confirm `build-debug` workflow assembles APK
- [ ] Sideload the artifact onto a real device (API 24, API 31, API 36)
- [ ] Smoke test cold start: app loads `https://localhost/`, no white flash > 1s
- [ ] Smoke test offline: enable airplane mode, kill app, relaunch — must work
- [ ] Smoke test audio: tap any number → Vietnamese voice plays
- [ ] Verify both voices switch correctly (`hoai-my` ↔ `nam-minh`)

## P0 — Wrapper-specific concerns

- [ ] First-launch SW registration: confirm Workbox precaches the bundled audio
      (should be idempotent since assets already on-disk, but verify no errors)
- [ ] Back button behavior: Capacitor default closes app — decide whether to
      add `App.addListener('backButton', …)` for in-app history navigation
- [ ] Safe-area / status-bar inset: SvelteKit UI may render under the status bar.
      Check; add `<StatusBar>` plugin if needed
- [ ] Audio focus: WebView `<audio>` doesn't claim AUDIOFOCUS_GAIN. Test what
      happens when a phone call comes in mid-call (does playback duck/pause?)
- [ ] Verify `android:supportsRtl="true"` (Capacitor default) doesn't flip the
      Vietnamese UI

## P1 — Polish before public release

- [ ] Replace placeholder launcher icon (Capacitor ships a generic Material icon).
      Use loto's existing `static/icons/` rasters; consider `@capacitor/assets`
      to generate adaptive + splash from a single source
- [ ] Splash screen: `@capacitor/splash-screen` plugin with brand colors
- [ ] Status bar styling: brand-colored, light/dark text via `@capacitor/status-bar`
- [ ] App-store screenshots (3–5 phone shots): cold start, mid-game, master panel,
      settings, bingo modal

## P1 — Distribution

- [ ] Generate release keystore: `keytool -genkey -v -keystore ~/.android/loto-release.jks -keyalg RSA -keysize 4096 -validity 36500 -alias loto`
- [ ] Add GitHub secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- [ ] Tag `v1.0.0` and push → exercises `release.yml`
- [ ] (Optional) Play Console listing in Vietnamese — privacy policy, content
      rating, target audience

## P2 — Supply-chain hygiene

- [ ] `npm audit` — wrapper repo (currently 0 vuln)
- [ ] `npm audit` — loto submodule (currently 2 high — bump and re-pin)
- [ ] Dependabot / Renovate for both `package.json` and the loto submodule SHA

## P2 — Future features (not committed)

- [ ] Add `@capacitor/share` for sharing the called-numbers card
- [ ] Add `@capacitor/haptics` for bingo confirmation
- [ ] Background audio (play numbers while screen off) — WebView audio is
      foreground-only; would need a native plugin or service shim
- [ ] iOS target (`npx cap add ios`) — same wrapper, ~zero extra code

## Decisions record

- **Architecture**: Capacitor 8 wrapper, not TWA — chosen for first-launch
  offline guarantee (TWA needs network for first PWA fetch)
- **Loto integration**: git submodule pinned to a SHA, not npm package — loto
  isn't published; submodule keeps the wrapper reproducible
- **No INTERNET permission**: enforces "fully offline" as a hard guarantee.
  Add back if any remote feature is introduced
- **Native port retired**: lives at commits `fe52232`/`9a35686` if someone
  ever wants the ExoPlayer + Compose UI back
