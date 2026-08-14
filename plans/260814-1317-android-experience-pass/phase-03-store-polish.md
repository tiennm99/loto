# Phase 3 — Store polish

Independent of phases 1-2. Pure config/resource work.

## Launcher name

`android/android/app/src/main/res/values/strings.xml`:

- `app_name`: `Lo To` → `Lô tô`
- `title_activity_main`: `Lo To` → `Lô tô`

Vietnamese users currently get an ASCII-mangled launcher label while the app
itself is "Lô tô — Hội chợ TN1". Short form matches the webmanifest
`short_name`. AAPT handles UTF-8; the file already declares
`encoding='utf-8'`.

Leave `package_name` and `custom_url_scheme` alone — identifiers, not labels.

## Themed icon (Android 13+)

`android/android/app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` — add a
`<monochrome>` layer so the launcher can tint the icon to the user's theme.
Without it Android 13+ falls back to the full-colour icon on themed home
screens.

Source glyph: `web/static/icons/source.svg`. Monochrome layers must be a flat
silhouette on transparent — no gradients, no background plate. Add as
`res/drawable/ic_launcher_monochrome.xml` (vector) reusing the existing
foreground geometry.

## Dark splash

New `android/android/app/src/main/res/values-night/styles.xml` overriding
`AppTheme.NoActionBarLaunch` with a dark splash drawable. The app has a full
dark theme; the splash currently flashes light before the WebView paints.

Needs a `drawable-night/splash.xml` (or a night colour the existing
`@drawable/splash` references).

## Version bump

`android/android/app/build.gradle`:

- `versionCode 3` → `4` (Play rejects duplicates — already documented in
  `android/README.md`).
- `versionName "0.0.3"` → `"0.1.0"`. Still internal track; 1.0.0 would
  overclaim.

## Docs

`android/README.md`:

- Permissions: document VIBRATE and why (haptic cell feedback).
- New "Back button" note: closes overlays, confirms exit at root.
- New "Screen wake lock" note: held while a round is active.
- Version-bump section: reflect the new numbers.
- **Leave the "Why no INTERNET permission?" section verbatim.** The offline
  guarantee is unchanged and the wording is deliberate.

## Reassessed, no change

`allowBackup=true` was listed as a gap in the brainstorm. Stored data is grid,
crossed cells, and UI settings in localStorage — nothing sensitive, and backup
means a player restores their card on a new phone. Correct as-is.

## Files

| File | Change |
|------|--------|
| `android/.../res/values/strings.xml` | diacritic app name |
| `android/.../res/mipmap-anydpi-v26/ic_launcher.xml` | monochrome layer |
| `android/.../res/drawable/ic_launcher_monochrome.xml` | new |
| `android/.../res/values-night/styles.xml` | new, dark splash |
| `android/.../res/drawable-night/splash.xml` | new |
| `android/android/app/build.gradle` | versionCode 4, versionName 0.1.0 |
| `android/README.md` | permissions, back button, wake lock, versions |

## Validation

- No test surface — resources and docs only.
- Device QA: launcher shows `Lô tô`; themed-icon home screen tints correctly;
  cold start in dark mode does not flash light.
