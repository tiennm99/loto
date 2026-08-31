# 2026-08-31 — Native Android rewrite: phases 1–9 implemented, phase 10 QA on API 36

## What happened

Executed `plans/260831-1131-native-android-rewrite/` end to end in one
session. Phases 1–9 done; phase 10 QA ran on the API 36 emulator (the only
installed system image). API 24/31 runs and the `v0.2.0` tag remain open.

- **Scaffold:** fresh Gradle project (AGP 8.13.0 + Gradle 8.14.3 — the pair
  the wrapper already proved on this machine and CI), Kotlin 2.2.20,
  Compose BOM 2025.09.00. Audio mounts from `../web/static` with an
  `ignoreAssetsPattern` excluding `icons`/`manifest.webmanifest`.
- **Parity ports:** game logic, auto-cross, settings contract, voice
  semantics ported one-to-one from `web/src/lib/`; every web test case has
  a named Kotlin twin (100 unit tests).
- **UI:** Compose player board / master panel / settings sheet matching
  the web pixel-for-pixel where practical (verified by emulator
  screenshots vs the Svelte sources).
- **Wrapper deleted**, project moved to `android/`, CI rewired to
  Gradle-only, versionCode guard added to the release workflow.

## Hard-won lessons

1. **Media3 merges `ACCESS_NETWORK_STATE` into the manifest.** The
   offline guarantee ("VIBRATE only") needed
   `tools:node="remove"` — caught by `aapt dump permissions`, not by any
   build error.
2. **File-backed DataStore cannot be unit-tested on a Windows JVM.** Its
   atomic rename fails whenever the target exists (`File.renameTo`
   semantics), so every *second* write throws. Repositories now test
   against an in-memory `DataStore` fake; "process death" = new repository
   over the same store instance.
3. **JUnit `@Before` must return void.** `fun setUp() = runBlocking {...}`
   returns `Preferences` and kills the runner with an opaque
   `initializationError`. Wrap the body instead.
4. **Instrumentation tests share the app's DataStore.** Persisted rounds
   from one test class turn "Tạo bảng mới" into a confirm dialog in the
   next. Tests must reset settings in `@Before` and click through the
   confirm dialog when present.
5. **The emulator booted in landscape**, which put the sheet's mode
   section below the fold — taps silently hit the scrim. Rotating to
   portrait fixed the "failing" test; the real product fix that fell out:
   the settings sheet now opens fully expanded (`skipPartiallyExpanded`),
   which is also closer to the web's full-height modal.
6. **Windows holds directory locks aggressively.** `mv android-native
   android` failed (Gradle daemon + shell CWD + an editor handle);
   copy-then-delete worked. Stop daemons before moving project roots.

## Code review

`code-reviewer` returned DONE_WITH_CONCERNS (report:
`plans/reports/code-reviewer-260831-native-android-rewrite.md`): logic
parity verified statement-by-statement; defects clustered where Svelte's
reactive effects became explicit call sites. Fixed same-session: DataStore
corruption/IO handling everywhere (H1), chờ/kinh detection after
generate/clear replays (H2), auto-call lifecycle pause on background (H3),
color sliders commit on drag end (H4), R8 `assembleRelease` in CI (H5),
auto-cross cursor clamp + ván-mới on shrink (M1), wall-clock countdown
immune to animator scale (M3), `grep -Fvx` in the tag guard (L2). Four new
tests cover H2/H3/M1. Minors deferred to `plans/todo.md` tech debt.

## State for the next session

- `git status`: one large staged+unstaged change set on `main` (wrapper
  deletion + native app + CI + docs). Not committed — user decides.
- Open: API 24/31 emulator QA (system images not installed), TalkBack +
  font-scale spot check, tag `v0.2.0` (user decision: ship when ready,
  window running or not), CI green run verification after push.
