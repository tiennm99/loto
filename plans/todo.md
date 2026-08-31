# Next-session TODO

Hand-off list as of 2026-08-31. Covers both `web/` (SvelteKit PWA,
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

### Play production access (clock running)

12 testers collected as of 2026-08-19 — the closed-test minimum is met.
Now the 14-day continuous window has to hold: if opt-ins drop below 12 at
any point the window resets, so keep spares on hand. Log tester feedback
and which features they exercised while the test runs — the production
application asks for both, and Play has been rejecting thin engagement
since April 2026.

Delivery is wired: `android-release.yml` uploads to `tracks: alpha`, the
closed track, as of 2026-08-20. Tagged releases now reach the testers
directly. v0.1.1 and earlier went to the internal track — promote them by
hand if any is wanted in the closed test. See
`docs/play-store-publishing.md` → "Closed testing → production access".

- [x] Track id confirmed. v0.1.2 (2026-08-20) logged
      `Validating tracks: 'alpha'` and committed the edit, so `alpha`
      exists on `com.miti99.loto` and the AAB reached it.

### Clipped web PWA icons

`web/static/icons/*.png` are visibly clipped. Root cause is
`font-size="240"` in `source.svg`, which overflows the 512 canvas. The
Android art was regenerated at a corrected 200/160, but the web icons
still carry the bug — it was out of scope for that pass. Fixing it
changes the GitHub Pages / PWA / OG card images.

### PWA install verification (manual, post-deploy)

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
  May need to drop safe-zone 70% → 65% if mask crops tight.

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

- [ ] `npm audit` both `web/` and `android/`. The old note about auditing
      "the loto submodule" is obsolete — `web/` is a plain directory in
      this monorepo, there is no submodule.
- [ ] Consider Dependabot or Renovate for both `package.json` files.

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

- **Code-review follow-ups (native rewrite, 2026-08-31).** Deferred minors
  from `plans/reports/code-reviewer-260831-native-android-rewrite.md`
  (H1–H5, M1, M3, L2 were fixed the same day): `VoicePlayerApi.release()`
  has no call site (app-scoped player lives for the process — fine, but
  dead API); `rememberReducedMotion()` reads the animator scale once per
  composition (stale until restart if toggled mid-session); voice-manifest
  parse runs on the main thread at startup (sub-ms, StrictMode-visible);
  `ignoreAssetsPattern` is a denylist over `web/static` (a new web static
  file would silently ship in the APK — add a web-side note or allowlist);
  empty `KEYSTORE_BASE64` yields a confusing zero-byte keystore failure in
  the release workflow.

- **`web/package.json` overrides — re-checked 2026-08-19.**
  `cookie` is still doing work: `@sveltejs/kit` declares `^0.6.0`, and
  the override is what forces 0.7.2. `serialize-javascript` is now
  marginal — the consumer is `@rollup/plugin-terser` at `^7.0.3` (not
  `workbox-build`, as this note used to claim), so the pin only holds
  the floor above 7.0.4. `ws` is redundant: `happy-dom` already
  declares `^8.21.0`, above the `^8.20.1` pin. Drop `ws` first if
  trimming; keep `cookie` until Kit moves.
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
- "New version available" reload toast for SW autoUpdate. Today the
  swap is silent; if users complain about content jumping mid-game,
  add a non-blocking notice.

## Open questions (carried from the Android pass)

- `boardTextScale` rungs are 0.9 / 1 / 1.15 / 1.3. Enough range for
  users who already run system font at 200%?
- The exit confirm fires at root regardless of round state. Worth
  gating on "is a round live" so an idle app just exits?
