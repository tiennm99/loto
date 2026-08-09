# Lô Tô

*Lô tô hội chợ* — Vietnamese fairground bingo. One number caller, two shipping
targets: a static web app and a fully-offline Android APK that wraps it.

Live: [tiennm99.github.io/loto](https://tiennm99.github.io/loto)

## Layout

| Path | What it is |
|------|------------|
| [`web/`](web) | SvelteKit static app — the game itself. pnpm, Vitest, deployed to GitHub Pages and Firebase Hosting. |
| [`android/`](android) | Capacitor wrapper that bundles `web/build` into an APK. npm + Gradle; the native project sits in `android/android/`. |

`android/` builds `web/` as part of its own build, so a change under `web/`
reaches the APK on the next `npm run build` in `android/`. There is no version
pin between them — they ship from the same commit.

## Quick start

```bash
# web
cd web && pnpm install && pnpm dev

# android debug APK (builds web/ first)
cd android && npm ci && npm run build && npm run assemble:debug
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
| `android-release` | tag `v*.*.*` | tests, then a signed AAB + APK on the GH Release, plus Play Store internal-track upload |

`ci` builds the web app exactly twice — once per base path — and every
consumer downloads the artifact rather than rebuilding it:

```
test (pnpm test)
└── build (root, base "")   ── deploy-firebase   push to main
    │                       ├─ preview-firebase  PR from this repo
    │                       └─ android-debug     unsigned APK artifact
    └── build (gh, base /loto) ── deploy-pages    push to main
```

Nothing deploys unless `test` is green. Deploy jobs are skipped on pull
requests, and the Firebase preview is skipped for PRs from forks, which
cannot read the service-account secret.

Shared toolchain setup lives in `.github/actions/setup-web` and
`.github/actions/setup-android` — composite actions used by both workflows,
so Node stays on one version across the web build and the APK.

Release/secret setup for the Play Store pipeline is documented in
[`docs/play-store-publishing.md`](docs/play-store-publishing.md).

## History

This repository is the merge of the former `tiennm99/loto` (now `web/`) and
`tiennm99/loto-android` (now `android/`). Both histories are preserved commit
for commit and interleaved in commit-time order under a single root, so
`git log` reads as one timeline and `git log -- web/` or `git log -- android/`
each resolve on the current paths.

## License

Apache-2.0 — see [`LICENSE`](LICENSE).
