# Brainstorm — Android experience pass

- Date: 2026-08-14
- Branch: `main` (c6c839f)
- Mode: brainstorm (no `--html`, no `--wiki`)
- Status: design approved, ready for `/ck:plan`

## Problem statement

App ships two targets from one commit: static web (`web/`) and a Capacitor 8 APK
(`android/`). Wrapper is thin — bare `BridgeActivity`, zero plugins, no native code.
Consequence: several behaviours diverge between browser and APK, and none of the
divergences reproduce on desktop where development happens.

Goal: close the Android-only gaps without breaking the offline guarantee.

## Locked constraints

| Constraint | Decision |
|---|---|
| INTERNET permission | Never. Non-negotiable. Offline is a hard guarantee, not a convention. |
| Implementation bias | Web-first. Native only where the web platform cannot reach. |
| Multi-device sync | Out of scope this round. |
| Play data safety | Must stay "no data collected". |

## Findings (verified against source)

### Tier 1 — defects that exist only in the APK

1. **Haptics dead.** `PlayerBoard.svelte:341-346` calls `navigator.vibrate(10)`.
   `AndroidManifest.xml` declares no `android.permission.VIBRATE`. Works in Chrome
   (Chrome holds the permission), silent no-op in the WebView.
2. **Screen sleeps mid-round.** Auto-call is `setInterval` at 1–10 s/number
   (`MasterPanel.svelte:85-104`); a 90-number round is 7.5–15 min. Zero wake-lock
   usage anywhere (grepped). Caller device dims, timers throttle, calls stop.
   Worst real-world failure; invisible on desktop.
3. **Back button exits instantly.** Bare `BridgeActivity` + single-route SPA = no
   history, so back quits. Bingo modal and settings close on Escape
   (`PlayerBoard.svelte:218-226`) but not on back. State survives in localStorage;
   an in-flight auto-call run does not.

### Tier 2 — suspected, no device available to confirm

4. **Edge-to-edge collision.** targetSdk 36 → Android 15+ forces edge-to-edge.
   No `viewport-fit=cover` in `app.html`, no `env(safe-area-inset-*)` in CSS.
   Settings gear is absolutely positioned at header `top: 0`.
5. **System font scale breaks the grid.** WebView honours system font size. Board is
   9 fixed columns, `text-xl…text-3xl` in `aspect-[3/4]` cells. Audience skews toward
   users who enlarge system fonts. Note: unconfirmed whether container-relative units
   (`cqw`/`clamp`) survive Android's `textZoom` multiplier — likely not.
6. **Audio focus.** `new Audio()` in WebView may not take Android audio focus;
   other apps' audio would play over number calls. Not confirmed broken.

### Tier 3 — product, deferred by decision

7. **Every phone is an island.** `active-tab.svelte.js` is explicitly a same-origin
   same-device coordinator. Real lô tô = one caller, many players. Cross-device sync
   is the single largest available UX win and is blocked by the offline vow.
   Owner kept the vow; sync stays out.

### Checked and dismissed

- **APK bloat** — audio totals 2.2 MB across both voices. Non-issue.
- **Service-worker asset duplication** — SW re-caches ~2 MB already on disk in the
  APK. Real but not worth engineering time.
- **`allowBackup=true`** — stored data is grid, crossed cells, UI settings. Nothing
  sensitive; backup lets a player restore their card on a new phone. Correct as-is.

## Decisions

| Question | Options weighed | Chosen | Why |
|---|---|---|---|
| Scope | Tier 1 / Tier 2 / Tier 3 / store polish | Tier 1 + Tier 2 + polish | Tier 3 blocked by offline vow |
| Offline vow | keep / LAN-only / server | Keep, non-negotiable | Owner decision |
| Native code | web-only / web-first / native-free | Web-first, native where forced | Keeps wrapper thin, PWA benefits |
| Back button | silent exit / confirm / double-back | Close overlays, confirm exit | Losing a live round warrants one first-party plugin |
| Font scale | pin only / pin + setting / CSS only / defer | Pin `textZoom` + in-app size control | Only option that fixes grid without removing a11y |
| Wake lock | always / active round / toggle | While round active | Caller device sits untouched; player taps self-serve |
| Tier 2 verification | defensive / wait / checklist | Design defensively | Fixes are correct regardless, cost nothing if bug absent |

## Work items

### Phase 1 — Tier 1 (no device needed)

**W1 · VIBRATE permission.** Add `<uses-permission android:name="android.permission.VIBRATE" />`
to `android/android/app/src/main/AndroidManifest.xml`. Normal permission, no runtime
prompt, no data-safety change. Existing reduced-motion guard unaffected.
*Accept:* cell tap buzzes on device; reduced-motion still silent.

**W2 · Wake lock.** New `web/src/lib/wake-lock.svelte.js` wrapping
`navigator.wakeLock.request("screen")`. Must re-acquire on `visibilitychange` —
Android auto-releases when the page hides. Consumed by `$effect` in
`MasterPanel.svelte` keyed on `autoRunning || hasGame` (both already local, lines
47/55-57). No-ops when `navigator.wakeLock` absent (old WebView, minSdk 24).
*Accept:* 3+ min auto-call untouched, screen stays lit; stop releases; background →
foreground re-acquires.

**W3 · Back button.**
- Web half: new `web/src/lib/overlay-history.js`. Overlay open pushes history entry;
  single `popstate` listener closes topmost. Careful case: close via button/Escape
  must pop the sentinel without re-triggering close. Touches `PlayerBoard.svelte`
  (`showCongrats`) and `SettingsButton.svelte`.
- Native half (exit guard): **unresolved fork.** targetSdk 36 enables predictive back
  by default and ignores legacy `onBackPressed` interception. Verify whether
  Capacitor 8 `App.addListener('backButton')` still fires before writing code.
  Fallback: AndroidX `OnBackPressedCallback` in `MainActivity.java` + Vietnamese
  confirm dialog from `strings.xml`. Fallback also avoids giving `web/` a Capacitor
  dependency.

*Accept:* back closes an open modal rather than quitting; back at root during a live
round asks first.

### Phase 2 — Tier 2, defensive

**W4 · Safe areas.** `viewport-fit=cover` in `web/src/app.html`;
`env(safe-area-inset-*)` padding in `app.css` / `+page.svelte` container.
Also fixes iOS PWA notch.
*Accept:* gear + footer fully visible on Android 15/16 with gesture nav.

**W5 · Font scale.** `MainActivity.java` pins `textZoom = 100`. Paired with new
`boardTextScale` setting (0.9 / 1.0 / 1.15 / 1.3) in `settings-store.svelte.js`,
own validator following the existing per-key pattern, UI in the settings sheet,
drives a CSS var multiplier on `.tan-tan-num`.
*Accept:* grid intact at 200% system font; in-app control resizes numbers; persists
across reload.

**W6 · Volume rocker.** `setVolumeControlStream(STREAM_MUSIC)` in `MainActivity.java`.
One line, unambiguously correct for an audio-centric app.

**Deferred: audio focus / MediaSession.** Unconfirmed problem. Building a MediaSession
layer around 1-second clips speculatively violates YAGNI. Revisit with device evidence.

### Phase 3 — Store polish

- `strings.xml`: `app_name` → `Lô tô` (diacritics). Currently ASCII-mangled `Lo To`
  in the launcher while the app itself is "Lô tô — Hội chợ TN1".
- Monochrome layer in `mipmap-anydpi-v26/ic_launcher.xml` (Android 13+ themed icons),
  derived from `web/static/icons/source.svg`.
- `values-night/styles.xml` — dark splash. App has a full dark theme; splash flashes light.
- `versionCode` + `versionName` bump per existing documented process. Suggest 0.1.0,
  not 1.0.0 — still internal track.
- `android/README.md`: document VIBRATE, wake lock, back-button behaviour. Leave the
  "no INTERNET permission" section verbatim.

## Touchpoints

| File | Work |
|---|---|
| `android/android/app/src/main/AndroidManifest.xml` | W1 |
| `android/android/app/src/main/java/com/miti99/loto/MainActivity.java` | W3 (fallback), W5, W6 |
| `android/android/app/src/main/res/values/strings.xml` | W3 (dialog strings), P3 |
| `android/android/app/src/main/res/values-night/styles.xml` | P3 (new) |
| `android/android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | P3 |
| `android/android/app/build.gradle` | P3 version bump |
| `web/src/lib/wake-lock.svelte.js` | W2 (new) |
| `web/src/lib/overlay-history.js` | W3 (new) |
| `web/src/lib/MasterPanel.svelte` | W2 |
| `web/src/lib/PlayerBoard.svelte` | W3 |
| `web/src/lib/SettingsButton.svelte` | W3, W5 |
| `web/src/lib/settings-store.svelte.js` | W5 |
| `web/src/app.html` | W4 |
| `web/src/app.css` | W4, W5 |
| `android/README.md` | P3 docs |

## Risks

1. **Predictive back vs Capacitor 8** (W3) — resolve before implementation; fallback
   identified. Highest-uncertainty item in the plan.
2. **`textZoom` pin overrides accessibility** — mitigated only by W5's in-app control.
   They ship together or not at all; cutting the setting means cutting the pin.
3. **`web/` gaining a Capacitor dependency** — if the exit guard uses `@capacitor/app`,
   the standalone GH Pages build inherits it. Native fallback avoids this.
4. **No device in the working environment** — W4/W5 designed on reasoning, not
   observation. Both safe-by-default; harmless if the bug does not reproduce.

## Validation

- `cd web && pnpm test` — settings-store gains `boardTextScale` validator coverage.
- New unit coverage: `overlay-history` push/pop/dismiss ordering; `wake-lock`
  re-acquire on visibility change.
- Manual on Android 15/16 device: haptics, 3-min auto-call with screen untouched,
  back from modal, back at root mid-round, system font at 200%, gesture-nav insets,
  volume rocker during a call.
- `cd android && npm run build && npm run assemble:debug` must stay green.

## Out of scope

Multi-device sync, INTERNET permission, audio focus/MediaSession, APK size,
`allowBackup` changes, service-worker asset dedup.

## Open questions

1. W3 native path — Capacitor 8 `backButton` under predictive back: fires or not?
   Decides whether `web/` takes a Capacitor dependency.
2. Confirm-on-exit trigger — only when a round is live (needs the flag plumbed to
   native), or unconditionally at root (simpler, mildly noisier)?
3. `boardTextScale` steps — are 0.9/1.0/1.15/1.3 the right rungs, or is a continuous
   slider preferred alongside the existing colour picker?

## Next step

`/ck:plan` with this report as input. Default mode recommended over `--tdd`: work is
mostly additive across new modules and native config, not a refactor of covered
behaviour. Exception — W5 touches `settings-store.svelte.js`, which has 369 lines of
existing tests worth preserving.
