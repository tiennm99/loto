# Next-session TODO

Hand-off list as of 2026-05-10. Deploy target is GitHub Pages
(`/loto/` base). All prior plan folders + their reports have been
swept after shipping; residual / new items live here directly.

## Highest leverage (start here)

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

## UX polish (carried over)

- **`MasterEmptyState` ↔ PlayerBoard ghost-grid duplication.** Two
  near-identical decorative components — extract a shared
  `<GhostBoardPreview rows={N} />` only if a third use appears.
  (Rule-of-three not met yet.)

## Tech debt

- **`cookie` and `serialize-javascript` overrides are temporary.**
  Remove the `overrides` block in `package.json` once
  `@sveltejs/kit` and `workbox-build` ship releases that pull
  `cookie >= 0.7.0` and `serialize-javascript >= 7.0.5` upstream.
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
