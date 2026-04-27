---
slug: ui-polish-and-pwa-v2
created: 2026-04-27
status: completed
completedAt: 2026-04-27
mode: fast
blockedBy: []
blocks: []
---

# UI polish v2 + installable PWA

Finishes the UI/UX audit items deferred in `ad6291e` (Vietnamese font,
master empty state, mode picker iconography, color picker, brand mood)
and ships the app as an installable PWA so it works offline at the
fairground (no signal in many venues).

## Why now

- The audit's remaining P1/P2 items are all small, concrete fixes with
  sketched solutions — bundle them before the codebase grows.
- The Vietnamese font gap is the single biggest legibility risk on
  Android phones, which is the primary device for fairground use.
- "Works offline at a fairground with poor signal" is the killer
  feature that justifies installing it. PWA support is straightforward
  with `@vite-pwa/sveltekit` (already-static export).

## Decisions (locked unless flagged)

- **Font policy**: self-host one weight (Roboto Condensed Bold or
  Oswald Vietnamese subset) via `@font-face` with `font-display: swap`.
  Add to `tan-tan-num` stack as the primary face. Subset to `latin` +
  `latin-ext` + `vietnamese`. ~30-50KB gz'd.
- **PWA stack**: `@vite-pwa/sveltekit`. Service-worker strategy:
  `precache` all 184 audio clips (2 voices × 92 names) + app shell;
  `NetworkFirst` for navigation; cache-first for static assets.
- **No backend, no install hostility**: ship `manifest.webmanifest`
  with icons, theme color, name. Don't show a "Install this app"
  banner — let the browser native flow handle it. (Out of scope for
  v2; can layer on later if data shows it's needed.)
- **Mode picker icons**: SVG inline, no icon library. 3 small glyphs
  hand-drawn from primitives (rectangle + ellipse).
- **Color picker**: keep the 10-swatch + native input layout, but
  group them in one card with `Tuỳ chỉnh` / `Mẫu` sub-headers.
- **Header brand polish**: drop the all-caps tracking-[0.28em] sub
  and replace with decorative dashes. Keep current font stack.
- **Per-row Chờ indicator**: a subtle ring on the row label band
  (`section-label`) when a Chờ row exists in that section. Optional
  — fall back to today's toast if implementation stretches.

## Phases

| # | File | Status | Notes |
|---|------|--------|-------|
| 1 | Vietnamese font + MasterEmptyState | DONE | @fontsource/roboto-condensed added to tan-tan-num stack |
| 2 | Mode picker + color picker + brand polish | DONE | SVG glyphs inline, bordered card with sub-headers, header subline with decorative dashes |
| 3 | Installable PWA (offline-capable) | DONE | @vite-pwa/sveltekit + manifest + icons; audio uses runtime CacheFirst (not precache due to static adapter timing) |

All phases shipped in commit `fba3e91`. Total delivery: ~2.5 hr.

## Out of scope (parking lot)

- Multiple cards per player.
- Long-press hero to undo last call.
- Spacebar to draw next number.
- Internationalization (English UI).
- LRU on audio cache (deferred from security audit — only matters at
  voices > 10).
- Bumping `@sveltejs/kit` to ship `cookie >=0.7.0` (next dependency
  sweep — not exploitable in static export today).
- Tier-2 confetti threshold tweak.
- 'unsafe-inline' style → hashed CSP (requires Svelte build tweak).

## Risks

- **Font swap visible flash on slow connections**: `font-display: swap`
  shows fallback first. Acceptable; alternative (`block`) blocks
  rendering up to 3s.
- **Service worker stale-content trap**: `@vite-pwa/sveltekit` ships
  with `autoUpdate` mode. Will use it; users see a "new version
  available" reload toast. Verify the SW skipWaiting flow doesn't
  drop in-flight audio.
- **Install on iOS Safari is awkward**: Safari requires "Add to
  Home Screen" manually; manifest still helps standalone display.
- **Audio precache size**: 2 voices × 92 clips. If clips are ~10KB
  each, total ~1.8MB. Manageable but worth measuring before locking
  precache strategy.

## Rollback

Each phase is one-commit revertible. PWA can be feature-flagged
(don't register SW in dev) so reverts don't strand cached SWs in
users' browsers.
