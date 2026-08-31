# Lô Tô

*Lô tô hội chợ* — Vietnamese fairground bingo. One number caller, two shipping
targets: a static web app and a fully-offline native Android app.

Live: [tiennm99.github.io/loto](https://tiennm99.github.io/loto)

## Layout

| Path | What it is |
|------|------------|
| [`web/`](web) | SvelteKit static app — the game itself. npm, Vitest, deployed to GitHub Pages and Firebase Hosting. |
| [`android/`](android) | Native Kotlin + Jetpack Compose app at feature parity with `web/`. Pure Gradle — no Node. |

The two apps share the voice clips: the Android build mounts
`web/static/audio/` straight into the APK's assets, so the MP3s exist once in
the repo. Game logic is hand-ported — a behavior change in `web/src/lib/`
needs a matching Kotlin change (the web app is the behavioral spec).

## Quick start

```bash
# web
cd web && npm install && npm run dev

# android debug APK (requires JDK 21 + Android SDK)
cd android && ./gradlew :app:assembleDebug
```

Per-project detail lives in [`web/README.md`](web/README.md) and
[`android/README.md`](android/README.md).

## CI

Workflows live at the repository root in `.github/workflows/`. Because both
subprojects ship from the same commit, ordinary CI is a single `ci.yml`;
only the tag-driven release stands apart.

| Workflow | Trigger | Result |
|----------|---------|--------|
| `ci` | push to `main` or PR touching `web/` or `android/` | see the job graph below |
| `android-release` | tag `v*.*.*` | versionCode guard, lint + tests, then a signed AAB + APK on the GH Release, plus Play Store closed-testing upload |

`ci` builds the web app exactly twice — once per base path — and the Android
job builds independently from the same checkout:

```
test (npm test)
└── build (root, base "")   ── deploy-firebase   push to main
    │                       └─ preview-firebase  PR from this repo
    └── build (gh, base /loto) ── deploy-pages    push to main

android-debug (gradle lint + test + assembleDebug) ── unsigned APK artifact
```

Nothing deploys unless `test` is green. Deploy jobs are skipped on pull
requests, and the Firebase preview is skipped for PRs from forks, which
cannot read the service-account secret.

Shared toolchain setup lives in `.github/actions/setup-web` (Node) and
`.github/actions/setup-android` (JDK 21 + Gradle caching) — composite actions
used by both workflows.

Release/secret setup for the Play Store pipeline is documented in
[`docs/play-store-publishing.md`](docs/play-store-publishing.md).

## History

This repository is the merge of the former `tiennm99/loto` (now `web/`) and
`tiennm99/loto-android` (now `android/`). Both histories are preserved commit
for commit and interleaved in commit-time order under a single root, so
`git log` reads as one timeline and `git log -- web/` or `git log -- android/`
each resolve on the current paths.

`android/` has lived three lives: a native Kotlin/Compose port (retired
2026-05-10), a Capacitor WebView wrapper around the web app, and — since
2026-08-31 — a fresh native Kotlin/Compose rewrite that replaced the wrapper.
All three are preserved in history.

## License

Apache-2.0 — see [`LICENSE`](LICENSE).
