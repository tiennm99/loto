# Next-session TODO

Hand-off list as of 2026-10-12 (reconciled against the code after the
2026-09-01 fix cycle). Covers both `web/` (SvelteKit PWA,
deployed to GitHub Pages at the `/loto/` base) and `android/` (native
Kotlin + Jetpack Compose app — the Capacitor wrapper was retired
2026-08-31). Shipped plan folders are swept after release; residual and
new items live here directly.

## Highest leverage (start here)

### Android device QA

The acceptance spec for the native rewrite's release QA
(`plans/260831-1131-native-android-rewrite/phase-10-qa-and-release.md`).
Every item is device/emulator-only.

- [ ] Tap a cell → vibrates; system "remove animations" → silent.
- [ ] Auto-call 3+ min untouched → screen stays lit. Stop → sleeps normally.
- [ ] Background during auto-call, return → still lit.
- [ ] Finish a round (remaining 0) → lock releases.
- [ ] Back with bingo modal open → modal closes, app stays.
- [ ] Back with settings open → sheet closes.
- [ ] Back at root → Vietnamese exit dialog; "Ở lại" keeps state.
- [ ] System font 200% → grid intact (Compose sp scaling); Settings →
      Cỡ chữ bảng resizes; survives reload.
- [ ] Gesture nav + status bar clear of the settings gear and footer.
- [ ] Volume rocker during a call → media slider.
- [ ] Launcher shows `Lô tô` with the brand icon; themed-icon mode tints it.
- [ ] Cold start in dark mode → dark splash, no white flash.
- [ ] Offline: airplane mode, kill app, relaunch — fully functional.
- [ ] Audio: draw a number → Vietnamese voice plays; both voices switch
      correctly (`hoai-my` ↔ `nam-minh`); rapid draws cut each other off;
      chờ + number plays gaplessly when the setting is on.
      v0.2.1 moved the clips to the assets root (paths lost the `audio/`
      prefix); CI built and tested it, but playback is unconfirmed on a
      device.
- [ ] Audio focus: playback layers over background music without pausing
      it (decided: no focus request — see `ExoVoicePlayer`); incoming
      call behaves normally.
- [ ] Sideload onto real devices (API 24, 31, 36) and BlueStacks.
- [ ] Store listing assets: feature graphic 1024×500, 2–8 phone
      screenshots, short (≤80) and full (≤4000) descriptions.

**versionCode footgun (resolved 2026-08-31).** `android-release.yml` now
fails fast when the tag's `versionCode` in `android/app/build.gradle.kts`
is not greater than the previous release tag's. Bumping stays manual;
forgetting is no longer silent.

### Play production access (status unrecorded)

12 testers were collected on 2026-08-19, so the 14-day continuous window
could have closed around 2026-09-02 if opt-ins never dropped below 12.
Whether the production-access application was filed or granted is not
recorded here — check Play Console and record the outcome. The
application asks for tester feedback and the features they exercised;
Play has been rejecting thin engagement since April 2026.

- [ ] Record the production-access outcome (applied / granted / window
      reset).

Delivery is wired: `android-release.yml` uploads to `tracks: alpha`, the
closed track, as of 2026-08-20. Tagged releases now reach the testers
directly. v0.1.1 and earlier went to the internal track — promote them by
hand if any is wanted in the closed test. See
`docs/play-store-publishing.md` → "Closed testing → production access".

- [x] Track id confirmed. v0.1.2 (2026-08-20) logged
      `Validating tracks: 'alpha'` and committed the edit, so `alpha`
      exists on `com.miti99.loto` and the AAB reached it.

### PWA install verification (manual, post-deploy)

**Priority after the SvelteKit 3 upgrade (2026-10-12).** Before the
upgrade, the Pages build's `sw.js` precached `/` and bound navigations to
`/`. Both point at the origin root, which returns 404 on Pages, so the
service worker most likely never installed there. The new build (plain
`vite-plugin-pwa`, replacing `@vite-pwa/sveltekit`, which breaks under
Kit 3) uses `/loto/`. On Pages, confirm that the service worker installs,
that the app works offline, and that the update banner appears after the
next deploy.

Needs production deploy on GitHub Pages + physical Android Chrome +
iOS Safari. No code; verification only.

**Lighthouse — GitHub Pages (`/loto/` base)**
- Open `https://tiennm99.github.io/loto/` in incognito Chrome.
- DevTools → Lighthouse → PWA + Perf + Best Practices + a11y.
- PWA score = 100. No mixed-content warnings.
- Confirm SW URL `/loto/sw.js`, manifest `/loto/manifest.webmanifest`,
  icons `/loto/icons/...` resolve.

**Android Chrome (physical or emulator)**
- "Add to Home Screen" → install → launch.
- Splash uses theme color (`#1565c0` light / `#0a0f1f` dark, see
  `app.html:9-10`). Status bar matches.
- Standalone display (no Chrome chrome).
- Airplane mode → reload from home → app shell + default voice work.
- Maskable icon: long-press app icon, ensure mask doesn't crop the
  centered glyph. Verify in DevTools "Show maskable preview" too.
  The maskable art (`source-maskable.svg`) is now full-bleed with the
  glyph inside the 80% safe zone, matching the Android adaptive icon.

**iOS Safari**
- Share → Add to Home Screen.
- Icon uses `apple-touch-icon` (`/icons/icon-192.png`) — round-ish
  glyph, no white bars.
- Launch standalone, fonts legible under translucent status bar.
- Airplane mode → app shell + default voice play.

**Common gotchas**
- Manifest paths break under `/loto/` base → check `vite.config.js`
  PWA `manifest: false` + `app.html` uses `%sveltekit.assets%`.
- iOS install shows wrong icon → ensure `icons/icon-192.png` is
  192×192 actual size.

## Supply chain

- [x] `npm audit` `web/` — 0 vulnerabilities as of 2026-10-12 (vitest
      moved to `^4.1.11`). `android/` has no npm dependencies.
- [x] Dependabot covers `npm` (`web/`), `gradle` (`android/`) and
      `github-actions` (`.github/dependabot.yml`).

## Parked (Android)

- Share sheet for the called-numbers card (native `Intent.ACTION_SEND`).
- Background audio (play numbers while the screen is off) — needs a
  foreground service.
- iOS target — would now mean a separate SwiftUI app or reviving a
  cross-platform layer; substantially bigger than in the wrapper era.

## Decisions record (Android)

- **Fresh native Kotlin/Compose rewrite** (2026-08-31) replaced the
  Capacitor wrapper, reversing the 2026-05-10 retirement decision (user
  decision; trade-off accepted: web features now need manual Kotlin
  ports). Fresh code — nothing resurrected from the old port. Wrapper
  history preserved in git, like the old port before it.
- **No INTERNET permission** — makes "fully offline" a hard guarantee.
  The native app strips Media3's merged `ACCESS_NETWORK_STATE` too.
  Would have to come back if any remote feature is introduced.
- **No audio focus** (2026-08-31): voice clips are ~1s speech cues; the
  player never claims focus so background music keeps playing, but pauses
  on becoming-noisy (headphones unplugged). See `ExoVoicePlayer`.
- Historical: **Capacitor 8 wrapper, not TWA** (2026-08) — chosen for the
  first-launch offline guarantee. **Native Kotlin/Compose port retired**
  (2026-05-10) in favour of the wrapper, for maintenance parity; that
  port lives at commits `fe52232` / `9a35686`.

## UX polish (carried over)

- **`MasterEmptyState` ↔ PlayerBoard ghost-grid duplication.** Two
  near-identical decorative components — extract a shared
  `<GhostBoardPreview rows={N} />` only if a third use appears.
  (Rule-of-three not met yet.)

## Tech debt

- **M6 recomposition perf** — needs a profiled baseline on a device first.
  Reviewed 2026-10-12: Kotlin 2.2 enables strong skipping, but the 99
  `MasterBoard` cells are inlined in `for` loops inside one composable,
  so every draw re-runs all of them. If profiling shows jank, the cheap
  fix is a `MasterCell(num, order, isLast)` composable so only the two
  changed cells recompose per draw.
- **Voice list growth.** If we add voices > 2 (esp. > 10), revisit
  the precache strategy — currently we precache only the default
  voice. The 7d runtime cache covers the rest, but cold-start cost
  on alternate voices grows linearly.

## New features (parking lot)

- Multiple cards per player.
- Long-press hero to undo last call (master).
- Spacebar to draw next number (master power-user shortcut).
- Internationalization (English UI). Vi default; en strings in a flat
  map. Maybe pick `paraglide-js` or roll our own minimal map.

## Open questions (carried from the Android pass)

- `boardTextScale` rungs are 0.9 / 1 / 1.15 / 1.3. Enough range for
  users who already run system font at 200%?
- The exit confirm fires at root regardless of round state. Worth
  gating on "is a round live" so an idle app just exits?
