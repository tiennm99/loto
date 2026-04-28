---
name: PWA install verification
phase: 9
status: todo
priority: medium
effort: 1h (manual)
---

# Phase 9 — PWA install verification checklist

## Context
- TODO: Lighthouse PWA = 100/100 + manual install on Android Chrome
  and iOS Safari. Splash + theme color flow.
- `BUILD_PROFILE=gh` deploy under `/loto/` base — confirm SW + manifest
  paths still resolve.
- Manifest: `static/manifest.webmanifest` (existing, hand-written).
- This phase is **verification, not code**. Output is a checklist
  filled in after running checks.

## Pre-flight
1. Phases 1-8 merged (or at least nothing breaking).
2. Production deploy on Cloudflare Pages and a `BUILD_PROFILE=gh`
   build for GitHub Pages comparison.

## Checklist

### Lighthouse (Cloudflare Pages — root base)
- [ ] Open `https://loto.miti99.com/` in incognito Chrome.
- [ ] DevTools → Lighthouse → PWA + Performance + Best Practices + a11y → Analyze.
- [ ] Score PWA = 100. If not, capture failures here:
  - …
- [ ] No CSP violations in console (Phase 7 strict CSP active).
- [ ] No mixed-content warnings.

### Lighthouse (GitHub Pages — `/loto/` base)
- [ ] Open `https://tiennm99.github.io/loto/` in incognito.
- [ ] Same scoring run.
- [ ] Confirm SW URL resolves: `/loto/sw.js` (not `/sw.js`).
- [ ] Confirm manifest URL: `/loto/manifest.webmanifest`.
- [ ] Manifest icons load from `/loto/icons/...`.

### Android Chrome (physical device or emulator)
- [ ] Visit production URL.
- [ ] "Add to Home Screen" prompt offered (auto or via menu).
- [ ] Install. Launch from home screen.
- [ ] Splash screen renders with theme color (`#1565c0` light /
      `#0a0f1f` dark) — match `app.html:9-10`.
- [ ] Status bar uses theme color.
- [ ] App opens in standalone mode (no Chrome chrome).
- [ ] Airplane mode → reload from home screen → app shell + default
      voice clips work offline.
- [ ] Maskable icon: long-press app icon, ensure shape mask doesn't
      crop the centered glyph (TODO mentions 70% safe-zone — verify
      in Chrome DevTools "Show maskable preview" too).

### iOS Safari
- [ ] Visit production URL.
- [ ] Share → Add to Home Screen.
- [ ] Icon uses `apple-touch-icon` (`/icons/icon-192.png`) — round-ish
      glyph, no white bars.
- [ ] Launch. iOS uses `apple-mobile-web-app-status-bar-style` =
      `black-translucent` — content goes under status bar, fonts legible.
- [ ] Standalone display (no Safari toolbar).
- [ ] Airplane mode → app shell loads, default voice clips play.

### CSP + headers (production)
- [ ] `curl -I https://loto.miti99.com/` shows `Content-Security-Policy`,
      `X-Content-Type-Options: nosniff`, `Referrer-Policy`,
      `Permissions-Policy`, `X-Frame-Options: DENY`.
- [ ] CSP `script-src` no longer contains `'unsafe-inline'` (post-Phase 7).

## Failures → Action
If any check fails, file a follow-up issue (or extend `plans/todo.md`).
Common gotchas:
- **Manifest paths break under `/loto/` base** → check `vite.config.js`
  PWA `manifest: false` + `app.html` uses `%sveltekit.assets%` (it does).
- **iOS install flow shows wrong icon** → ensure `icons/icon-192.png`
  exists and is 192×192 actual size, not just declared.
- **Splash flicker dark→light** → `theme-color` media queries in
  `app.html:9-10` must match the active scheme on first paint; SvelteKit
  emits these statically so should be fine.

## Success
- PWA score 100 on both deploys.
- Install succeeds on Android + iOS.
- Offline-capable shell + default voice.
- No CSP/headers regressions.

## Out of scope
- Maskable icon redesign at 65%/70% safe-zone — separate phase if mask
  crops too tight (TODO entry).
- iOS PWA push notifications, share targets, etc. — not requested.
