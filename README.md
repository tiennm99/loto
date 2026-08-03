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

Workflows live at the repository root in `.github/workflows/`; each is prefixed
with the subproject it serves and filtered on the paths it cares about.

| Workflow | Trigger | Result |
|----------|---------|--------|
| `web-verify-build` | push/PR touching `web/` | `pnpm test && pnpm build` |
| `web-deploy-github-pages` | push to `main` touching `web/` | publishes to GitHub Pages |
| `web-firebase-hosting-merge` | push to `main` touching `web/` | deploys to Firebase Hosting |
| `web-firebase-hosting-pull-request` | PR touching `web/` | Firebase preview channel |
| `android-build-debug` | push/PR touching `web/` or `android/` | unsigned debug APK artifact |
| `android-release` | tag `v*.*.*` | signed AAB + APK on the GH Release |

## History

This repository is the merge of the former `tiennm99/loto` (now `web/`) and
`tiennm99/loto-android` (now `android/`). Both histories are preserved commit
for commit and interleaved in commit-time order under a single root, so
`git log` reads as one timeline and `git log -- web/` or `git log -- android/`
each resolve on the current paths.

## License

Apache-2.0 — see [`LICENSE`](LICENSE).
