# Lô tô Android — TODO

## Direction change (2026-05-10)

Repo migrated from **native Kotlin/Compose port** to a **Capacitor wrapper**
around the upstream `tiennm99/loto` SvelteKit PWA. Native code lives in git
history at commits `9a35686` and `fe52232` — recoverable but no longer the
ship target.

Reason: maintenance parity. Loto evolves; the wrapper rebuilds and ships
without re-porting features.

## Done in 2026-05-10 session

- [x] Migrate to Capacitor wrapper, push to main (commit `dab4f7e`)
- [x] CI green: Node 22 + JDK 21 (commits `95358f2`, `4fde3f1`) — debug APK 4.9 MB
- [x] BlueStacks/emulator compat: `<uses-feature required=false>`, WebView debug auto-enabled in debug builds (commit `1ec4863`)
- [x] Optional Play Store auto-publish wired into `release.yml`, gated on `PLAY_SERVICE_ACCOUNT_JSON` (commit `793f009`)

## P0 — Smoke test on real device

- [x] CI build-debug workflow assembles APK (validated by run `25610204367`)
- [ ] Sideload the latest artifact onto a real device (API 24, API 31, API 36) and BlueStacks
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

## P1 — Distribution: signing + first GitHub Release

- [ ] Generate release keystore: `keytool -genkey -v -keystore ~/.android/loto-release.jks -keyalg RSA -keysize 4096 -validity 36500 -alias loto`
- [ ] Add GitHub secrets: `KEYSTORE_BASE64` (= `base64 -w0 loto-release.jks`), `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- [ ] Tag `v1.0.0` and push → `release.yml` builds signed AAB+APK, attaches to GH Release
      (Play Store step skips silently because `PLAY_SERVICE_ACCOUNT_JSON` not yet set)

## P1 — Google Play Store (ordered, do in sequence)

**Phase A — one-time Play Console setup (cannot be automated, ~$25 + 1–7 day review):**

- [ ] Sign up: https://play.google.com/console/signup ($25 one-time fee)
- [ ] Create app entry with package name `com.miti99.loto` (must match `applicationId`)
- [ ] Prepare store assets:
  - [ ] Icon 512×512 PNG
  - [ ] Feature graphic 1024×500 PNG/JPG
  - [ ] 2–8 phone screenshots (master panel, player board, settings, bingo modal, cold start)
  - [ ] Short description ≤ 80 chars
  - [ ] Full description ≤ 4000 chars (Vietnamese + optional English)
  - [ ] Privacy policy URL — host on GH Pages even though app is offline (required by Play)
- [ ] Fill content rating questionnaire (Casino/Card → likely Everyone or Teen)
- [ ] Fill target audience + data safety form (declare "No data collected" — fully offline)
- [ ] **Manually upload** the signed AAB from the v1.0.0 GH Release to **Internal Testing track**
      (Google policy: first upload must be manual; CI auto-publish only works for v1.0.1+)
- [ ] Submit for review (1–7 days for first app)

**Phase B — enable CI auto-publish (after Phase A approved):**

- [ ] Play Console → Setup → API access → link/create Google Cloud project
- [ ] In GCP Console: create Service Account → Keys → Add Key → JSON → download
- [ ] Back in Play Console API access: grant the service account *Release manager* + *View app information*
      (or *Admin* for simplicity)
- [ ] Add full JSON content as GH secret `PLAY_SERVICE_ACCOUNT_JSON`
- [ ] **Bump `versionCode` to 2** in `android/app/build.gradle` before next tag
      (Play rejects duplicate versionCodes — every release must increment)
- [ ] Tag `v1.0.1` and push → CI builds + uploads to **Internal track** automatically
- [ ] Promote internal → closed → open → production via Play Console UI when ready
      (or change `track: internal` in `release.yml` to `production` to fully automate)

**Open question: versionCode footgun** — every release must increment `versionCode` in
`android/app/build.gradle` before tagging. Options to consider:
- Manual discipline (current state)
- Pre-tag check workflow that fails if `versionCode` wasn't bumped
- Auto-bump from the git tag (e.g., `v1.0.5` → `versionCode = 5`) as a Gradle plugin or sed step in CI

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
