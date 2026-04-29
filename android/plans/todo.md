# Lô tô Android — TODO

Outstanding work, grouped by priority. The 11 implementation phases from
`260427-1153-loto-android-port/` are complete and pushed to GitHub.

## P0 — Verify build (cannot do here, no JDK/SDK)

- [ ] Open `/config/workspace/tiennm99/loto-android/` in Android Studio Ladybug+
- [ ] Sync Gradle, fix any version-resolution gaps
- [ ] `./gradlew :app:lint :app:test :app:assembleDebug`
- [ ] Run on emulator (API 24, API 31, API 35) — golden path: generate card → tap row to bingo → "Kinh!" modal + audio
- [ ] Run instrumentation: `./gradlew :app:connectedDebugAndroidTest` (`MainActivityComposeSmokeTest`)
- [ ] Sanity-check first-build CI run: GitHub Actions tab → `build-debug` workflow

## P1 — Tests not written by the agent

- [ ] `CallBusTest` — Turbine: broadcast/reset/monotonic id (specced in phase-06)
- [ ] `PlayerBoardViewModelTest` — bingo idempotency, forward-only auto-tick, mode gating, voice announce rule (specced in phase-06).
      Tier rule (post-260429-1511 sync): `tier == 2` when `celebrated.size >= 2` OR `(celebrated.size >= 1 AND any other row in Chờ)`. Also assert `sectionHasWaiting[s]` flips amber when any row in section `s` has 1 uncrossed number, drops once that row completes.
- [ ] `MasterPanelViewModelTest` — `runTest` with `advanceTimeBy` for auto-call cadence, settings-mid-run cancel, remaining-empty stop (specced in phase-06)
- [ ] `VoicePlayerInstrumentationTest` — assets resolve, clip plays end-to-end (specced in phase-04)
- [ ] `MasterBoardLayoutTest` — verify the 11×9 ones-digit layout (90 in row10/col8, edge cells correct)
- [ ] `VoiceManifestTest` — JSON parse, malformed input fallback

## P1 — Known concerns flagged during build

- [ ] `BringIntoViewRequester` carries `@OptIn(ExperimentalFoundationApi::class)` in `MasterPanelScreen.kt` — check if Compose BOM 2025.01+ has stabilized this; if so, drop the annotation
- [ ] Confetti fall-speed multiplier is `screenHeightDp * 3f` — heuristic, tune on real devices if particles look off
- [ ] Auto-call coroutine restarts on every speed change — verify the cancel/restart doesn't leak the prior delay timer

## P2 — Polish before public release

- [ ] Replace the placeholder `ic_launcher_foreground.xml` (stylized "L" stroke) with a real adaptive icon — rose→amber wordmark or a fairground motif
- [ ] App-screenshot generation for Play Store / GitHub README (3–5 phone shots: cold start, mid-game, master panel, settings, bingo modal)
- [ ] Splash screen via `androidx.core:core-splashscreen` (currently the white themes.xml flash is acceptable but unbranded)
- [ ] Dark mode visual pass — verify each cell/token combination, especially dark-mode empty-cell overlay
- [ ] Per-density rasters for the launcher icon (`ic_launcher.webp` at mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi) for pre-API 26 devices
- [ ] Accessibility: TalkBack content descriptions on every interactive cell + toast; verify 4.5:1 contrast in both themes

## P2 — Distribution

- [ ] Generate release keystore: `keytool -genkey -v -keystore ~/.android/loto-release.jks -keyalg RSA -keysize 4096 -validity 36500 -alias loto`
- [ ] Add GitHub secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
- [ ] Tag `v1.0.0` and push → exercises `release.yml` (signed AAB+APK to GH Release)
- [ ] (Optional) Play Console listing — privacy policy, content rating, target audience, store listing in Vietnamese

## P3 — Future features (not committed)

- [ ] Host's own player card (web app supports it via `loto_master_card` prefix but doesn't render it; same here)
- [ ] Undo last crossed cell
- [ ] Sound effects on bingo (separate from voice)
- [ ] PWA-style "add to home screen" — N/A on Android (already an app)
- [ ] Multiplayer sync via WebSocket
- [ ] English i18n — `values-en/strings.xml`

## Decisions record

- **Plan archive**: `/config/workspace/plans/260427-1153-loto-android-port/plan.md` (overview only; phase docs deleted post-implementation)
- **Research reports**: `/config/workspace/plans/reports/researcher-260427-1153-{loto-codebase-analysis,android-stack,android-audio}.md`
- **Audio framework pick**: Media3 ExoPlayer (over SoundPool) — see audio research report
- **DI pick**: manual construction in `LotoApp.onCreate` (no Hilt) — revisit at >10 surfaces
- **Theme**: brand-fixed colors, `dynamicColor = false` — Material You system colors would mute the rose→amber identity
