# Phase 2 — Tier 2, designed defensively

Suspected but unconfirmed without a device. Every change here is correct whether
or not the bug reproduces, and costs nothing if it does not.

## W4 · Safe-area insets

`web/src/app.html` — viewport meta gains `viewport-fit=cover`:

```html
<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover" />
```

`web/src/app.css` — pad `body` by the insets:

```css
body {
  padding-top: env(safe-area-inset-top);
  padding-right: env(safe-area-inset-right);
  padding-bottom: env(safe-area-inset-bottom);
  padding-left: env(safe-area-inset-left);
}
```

Why `body` and not the page container: `html` carries the background colour and
`body` is already `transparent` + `min-h-full flex flex-col`, so padding here
insets content while the background stays full-bleed. `fixed inset-0` overlays
(bingo modal, settings sheet, inactive-tab curtain) are viewport-relative and
deliberately unaffected — their backdrops should cover the whole screen; their
content is centred and already clear of the bars.

Bonus: fixes the iOS PWA notch for free.

Zero effect where insets are 0 (desktop, older Android).

## W5 · Font scale

### Native pin

`MainActivity.java`, after `super.onCreate`:

```java
getBridge().getWebView().getSettings().setTextZoom(100);
```

Stops the system font multiplier from breaking a 9-column fixed grid.

### In-app replacement control

Taking the system control away obliges giving one back. Both ship or neither.

`web/src/lib/settings-store.svelte.js`:

- `boardTextScale: 1` in `DEFAULT_SETTINGS`.
- `validBoardTextScale(v)` — allowlist `[0.9, 1, 1.15, 1.3]`, same
  per-key-validator pattern as the existing keys.
- `applyBoardTextScale()` sets `--board-text-scale` on `documentElement`,
  mirroring `applyEmptyCellColor()`; called from `applyAll()`.
- Wire into `loadSettings()` and `resetSettings()`.

`web/src/app.css` — new classes. `.tan-tan-num` sets font-family/weight only;
sizes come from Tailwind utilities on the elements, so overriding there would be
a specificity fight. Dedicated classes instead, replacing the utilities:

```css
/* Player card cell — was text-xl sm:text-2xl md:text-3xl */
.board-num { font-size: calc(1.25rem * var(--board-text-scale, 1)); }
@media (min-width: 640px) { .board-num { font-size: calc(1.5rem * var(--board-text-scale, 1)); } }
@media (min-width: 768px) { .board-num { font-size: calc(1.875rem * var(--board-text-scale, 1)); } }

/* Master tracking token — was text-xl sm:text-2xl */
.master-num { font-size: calc(1.25rem * var(--board-text-scale, 1)); }
@media (min-width: 640px) { .master-num { font-size: calc(1.5rem * var(--board-text-scale, 1)); } }
```

Two classes, not one: the master board has two breakpoint rungs, the player card
three. Merging them would silently enlarge the master board on desktop.

Consumers:

- `PlayerBoard.svelte:451-462` — swap `text-xl sm:text-2xl md:text-3xl` → `board-num`.
- `MasterPanel.svelte:287` — swap `text-xl sm:text-2xl` → `master-num`.

Scope: grid cells only. Master hero number, called-history chips, and all UI
chrome keep their Tailwind sizes — clipping is a grid problem.

### Settings UI

`web/src/lib/SettingsButton.svelte` — new fieldset "Cỡ chữ bảng", 4 buttons
(Nhỏ / Vừa / Lớn / Rất lớn), same `aria-pressed` button-group pattern as the
existing theme/mode pickers.

Tests — extend `web/src/lib/settings-store.test.js`: default, valid values
persist, invalid/out-of-set falls back to 1, reset restores 1.

## W6 · Volume rocker → media stream

`MainActivity.java`:

```java
setVolumeControlStream(AudioManager.STREAM_MUSIC);
```

One line. Volume keys adjust media rather than ringtone even before first
playback. Unambiguously right for an app whose job is calling numbers aloud.

## Deferred — audio focus / MediaSession

Still unconfirmed as broken. Building a MediaSession layer around 1-second clips
for a hypothetical problem is speculative work. Revisit with device evidence.

## Files

| File | Change |
|------|--------|
| `web/src/app.html` | `viewport-fit=cover` |
| `web/src/app.css` | safe-area body padding, `.board-num`, `.master-num` |
| `web/src/lib/settings-store.svelte.js` | `boardTextScale` + validator + apply |
| `web/src/lib/settings-store.test.js` | coverage for the new key |
| `web/src/lib/SettingsButton.svelte` | size picker fieldset |
| `web/src/lib/PlayerBoard.svelte` | `board-num` class swap |
| `web/src/lib/MasterPanel.svelte` | `master-num` class swap |
| `android/.../MainActivity.java` | textZoom pin, volume stream |

## Validation

- `pnpm test` — settings-store suite covers the new key; existing 369 lines of
  settings tests must stay green (this file has the most regression surface).
- `pnpm lint`, `pnpm build`.
- Device QA: system font at 200% → grid intact; in-app size control changes
  numbers and survives reload; gesture-nav and status bar clear of gear/footer;
  volume rocker shows the media slider during a call.
