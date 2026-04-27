# Phase 3 — Installable PWA with offline audio

Ship the app as a Progressive Web App so users can install it to home
screen and play without signal. Critical for fairground use where
venue Wi-Fi is unreliable.

**Status**: not started
**Priority**: P1
**Effort**: ~60 min
**Depends on**: nothing (independent of phases 1 & 2)

## Files to add

- `static/manifest.webmanifest` — PWA metadata
- `static/icons/icon-192.png`, `icon-512.png`, `icon-maskable-512.png`
  — exported from a single SVG source
- `static/icons/source.svg` (optional — keeps a vectorized origin
  alongside the rasters)

## Files to modify

- `package.json` — add `@vite-pwa/sveltekit` dev dependency
- `vite.config.js` — register the SvelteKitPWA plugin
- `src/app.html` — `<link rel="manifest">`, `<meta name="theme-color">`,
  apple-touch-icon
- `svelte.config.js` — confirm static adapter still works with PWA
  plugin (it does, but verify)

## Plugin choice — `@vite-pwa/sveltekit`

Mature, actively maintained, integrates cleanly with `adapter-static`.
Generates the service worker via Workbox under the hood with sensible
defaults.

```bash
npm install -D @vite-pwa/sveltekit
```

```js
// vite.config.js
import { sveltekit } from "@sveltejs/kit/vite";
import { SvelteKitPWA } from "@vite-pwa/sveltekit";
import tailwindcss from "@tailwindcss/vite";

export default {
  plugins: [
    tailwindcss(),
    sveltekit(),
    SvelteKitPWA({
      registerType: "autoUpdate",
      includeAssets: ["favicon.ico", "icons/*.png", "audio/**/*.mp3"],
      manifest: false, // we ship our own static/manifest.webmanifest
      workbox: {
        globPatterns: [
          "**/*.{js,css,html,svg,png,woff2,webmanifest}",
          "audio/**/*.mp3",
        ],
        // 184 audio clips × ~10KB ≈ 2MB. Workbox default is 2MB so
        // bump explicitly to be safe.
        maximumFileSizeToCacheInBytes: 5 * 1024 * 1024,
        navigateFallback: "/index.html",
      },
      devOptions: { enabled: false }, // don't pollute dev with SW
    }),
  ],
};
```

## manifest.webmanifest

```json
{
  "name": "Lô tô — Hội chợ TN1",
  "short_name": "Lô tô",
  "description": "Bàn số của trò chơi Lô tô",
  "start_url": ".",
  "display": "standalone",
  "orientation": "portrait",
  "theme_color": "#1565c0",
  "background_color": "#0a0f1f",
  "lang": "vi",
  "icons": [
    {
      "src": "/icons/icon-192.png",
      "sizes": "192x192",
      "type": "image/png"
    },
    {
      "src": "/icons/icon-512.png",
      "sizes": "512x512",
      "type": "image/png"
    },
    {
      "src": "/icons/icon-maskable-512.png",
      "sizes": "512x512",
      "type": "image/png",
      "purpose": "maskable"
    }
  ]
}
```

`theme_color` matches the section accent. `background_color` matches
dark-theme base (so the splash screen feels intentional).

## app.html additions

```html
<head>
  <link rel="manifest" href="/manifest.webmanifest" />
  <meta name="theme-color" content="#1565c0"
        media="(prefers-color-scheme: light)" />
  <meta name="theme-color" content="#0a0f1f"
        media="(prefers-color-scheme: dark)" />
  <link rel="apple-touch-icon" href="/icons/icon-192.png" />
</head>
```

## Icons

Use a single 512×512 SVG source. Export 192/512 standard + 512
maskable (with 20% safe-zone padding for Android adaptive icons).

Quick path: ImageMagick.

```bash
magick static/icons/source.svg -resize 192x192 static/icons/icon-192.png
magick static/icons/source.svg -resize 512x512 static/icons/icon-512.png
# Maskable: pad 20% transparent safe zone
magick static/icons/source.svg -background none -gravity center \
  -resize 70%x70% -extent 512x512 static/icons/icon-maskable-512.png
```

Source SVG: a stylized bingo card grid + the rose/amber gradient
from the page header. Or (simpler) a centered "L" wordmark in the
brand gradient.

## CSP for the manifest

Confirm `static/_headers` allows `manifest-src 'self'` and the icon
fetches. Today's CSP has `default-src 'self'` which covers it.

## Testing

```bash
npm run build
npx serve build
# Open http://localhost:3000 in Chrome
# DevTools → Application → Manifest (verify icons + theme)
# DevTools → Application → Service Workers (verify registration)
# Network tab → check "Offline" → reload — app still works
# Audio playback: turn off network, draw a number — clip plays
```

## Update flow

`autoUpdate` mode: on each new deploy, the SW detects the new build
and swaps in. Users see the new version on next navigation. Don't
add a "new version" toast in v1 — the swap is automatic and silent.
If users complain about content jumping mid-game, layer on a manual
prompt later (out of scope).

## Todo

- [ ] `npm install -D @vite-pwa/sveltekit`
- [ ] Add SvelteKitPWA to `vite.config.js`
- [ ] Create `static/manifest.webmanifest`
- [ ] Generate three icons (192, 512, maskable-512) from source SVG
- [ ] Add manifest + theme-color meta tags + apple-touch-icon to
      `src/app.html`
- [ ] Verify `static/_headers` CSP still permits manifest + SW
- [ ] `npm run build && npx serve build` — manual offline test
- [ ] Confirm `npm test` still passes (PWA shouldn't touch test paths)
- [ ] Lighthouse audit — PWA category should hit 100

## Success criteria

- Chrome shows the install prompt (≥1 visit + qualifying engagement)
- iOS Safari "Add to Home Screen" launches in standalone, no browser
  chrome
- Splash screen uses the right theme color and dark background
- Offline reload renders the app and plays audio clips for previously
  downloaded voices
- Lighthouse PWA score = 100, perf score doesn't drop > 5 points
- Service worker registration is silent (no console errors)

## Risks

- **Service worker stale-content trap**: `autoUpdate` registers a new
  SW; old tabs keep the old build until reload. Acceptable; users
  reload between rounds.
- **Cloudflare Pages SW caching headers**: Cloudflare caches `/sw.js`
  by default; need `Cache-Control: no-cache` on `sw.js` so updates
  propagate. Add to `static/_headers`:
  ```
  /sw.js
    Cache-Control: no-cache
  ```
- **Audio precache size**: 2 voices × 92 clips. Measure with
  `ls -la static/audio/*/*.mp3 | awk '{s+=$5} END {print s}'`.
  If > 5MB, consider runtime-cached strategy instead of precache.
- **iOS quirks**: Safari requires `apple-mobile-web-app-capable`
  meta + apple-touch-icon for proper standalone launch. Both
  included.
- **GitHub Pages mirror**: SW path expectations differ when served
  from `/loto/` base. `@vite-pwa/sveltekit` reads SvelteKit's
  `paths.base` so this works automatically. Verify in
  `npm run build:gh`.

## Out of scope

- "New version available" toast (deferred — autoUpdate is silent v1)
- Push notifications
- Background sync (no backend to sync with)
- Workbox runtime caching strategies beyond defaults
- Custom install prompt UI (use browser native)
