# Next-session TODO

Hand-off list as of 2026-08-19. Covers both `web/` (SvelteKit PWA,
deployed to GitHub Pages at the `/loto/` base) and `android/` (Capacitor
wrapper). Shipped plan folders are swept after release; residual and new
items live here directly.

## Highest leverage (start here)

### Android device QA (blocked: no device or emulator)

Carried from the shipped Android experience pass. The native code is
merged and statically reviewed, but every item below is device-only —
this environment is headless ARM64 with no emulator and no Android SDK.

- [ ] Tap a cell → vibrates; enable reduced-motion → silent.
- [ ] Auto-call 3+ min untouched → screen stays lit. Stop → sleeps normally.
- [ ] Background during auto-call, return → still lit.
- [ ] Finish a round (remaining 0) → lock releases.
- [ ] Back with bingo modal open → modal closes, app stays.
- [ ] Back with settings open → sheet closes.
- [ ] Back at root → Vietnamese exit dialog; "Ở lại" keeps state.
- [ ] System font 200% → grid intact; Settings → Cỡ chữ bảng resizes;
      survives reload.
- [ ] Gesture nav + status bar clear of the settings gear and footer.
- [ ] Volume rocker during a call → media slider.
- [ ] Launcher shows `Lô tô` with the brand icon; themed-icon mode tints it.
- [ ] Cold start in dark mode → dark splash, no white flash.

Two assumptions the checklist doubles as a test for: the WebView has no
history at rest (SvelteKit hydrates with `replaceState`), so back at root
should exit rather than navigate; and `OnBackPressedCallback` / `Bridge`
resolve transitively through `appcompat` / `capacitor-android` — never
compiled here, so a build failure would show up first.

### Android wrapper — open items

Merged from `android/plans/todo.md` on 2026-08-19. Items that the Android
experience pass already shipped (haptics, back button, safe-area insets,
launcher icon, splash + night variant) were dropped rather than carried;
verified present in `MainActivity.java`, `web/src/app.css`, and
`android/android/app/src/main/res/`.

- [ ] Sideload onto real devices (API 24, 31, 36) and BlueStacks. Overlaps
      the device QA list above — do both in one sitting.
- [ ] Cold start: app loads `https://localhost/`, no white flash > 1s.
- [ ] Offline: airplane mode, kill app, relaunch — must work.
- [ ] Audio: tap a number → Vietnamese voice plays; both voices switch
      correctly (`hoai-my` ↔ `nam-minh`).
- [ ] First-launch SW registration: confirm Workbox precaches the bundled
      audio without errors (should be idempotent — assets already on disk).
- [ ] Audio focus: the WebView `<audio>` never claims `AUDIOFOCUS_GAIN`.
      Test an incoming call mid-round — does playback duck or pause?
      Explicitly out of scope for the experience pass; still unanswered.
- [ ] Confirm `android:supportsRtl="true"` (Capacitor default) does not
      flip the Vietnamese UI.
- [ ] Store listing assets: feature graphic 1024×500, 2–8 phone
      screenshots, short (≤80) and full (≤4000) descriptions.

**versionCode footgun (unresolved).** Every release must increment
`versionCode` in `android/android/app/build.gradle` — currently 4, at
versionName 0.1.0 — before tagging, or Play rejects the upload. Still
manual discipline. Options never decided: a pre-tag CI check that fails
on an unbumped code, or deriving it from the git tag.

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

- [ ] Cut v0.1.2 to confirm the first upload actually lands on `alpha`.
      Untested until a tag runs; the track id is unverified against the
      Console. A wrong id fails the step and prints the valid tracks.

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

- `@capacitor/share` for sharing the called-numbers card.
- Background audio (play numbers while the screen is off). WebView audio
  is foreground-only; needs a native plugin or service shim.
- iOS target (`npx cap add ios`) — same wrapper, near-zero extra code.

## Decisions record (Android)

- **Capacitor 8 wrapper, not TWA** — chosen for the first-launch offline
  guarantee; a TWA needs network for the first PWA fetch.
- **No INTERNET permission** — makes "fully offline" a hard guarantee.
  Would have to come back if any remote feature is introduced.
- **Native Kotlin/Compose port retired** (2026-05-10) in favour of the
  wrapper, for maintenance parity. The ExoPlayer + Compose UI lives at
  commits `fe52232` / `9a35686` if it is ever wanted back.

## UX polish (carried over)

- **`MasterEmptyState` ↔ PlayerBoard ghost-grid duplication.** Two
  near-identical decorative components — extract a shared
  `<GhostBoardPreview rows={N} />` only if a third use appears.
  (Rule-of-three not met yet.)

## Tech debt

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
  plumbing "is a round live" to native so an idle app just exits?
- `textZoom` is pinned as an accessibility override. It is only
  defensible alongside the in-app size control — if that setting is
  ever cut, cut the pin too.
