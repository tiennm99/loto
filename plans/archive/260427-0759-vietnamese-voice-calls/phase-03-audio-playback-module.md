# Phase 3 — Audio playback module

## Context
- [plan.md](plan.md). Depends on Phase 2 (committed MP3s).
- Single module both Master and Player import from.

## Overview
- Priority: P0
- Status: TODO
- Effort: ~25 min

## Goal

Three exports both call sites use:

```
playNumber(n)    // master: plays {voice}/{n}.mp3
playWaiting(n)   // player: plays {voice}/cho.mp3 then {voice}/{n}.mp3
playBingo()      // player: plays {voice}/kinh.mp3
cancelPlayback() // pauses any in-flight audio (used on new game / new card)
```

Active voice resolved at call time from `settings.voice`. URLs go
through `import { base } from "$app/paths"` for basePath safety.

## Files

| File | Change |
|---|---|
| `src/lib/voice.js` | NEW — playback functions, `<audio>` cache, sequencer |

## Implementation sketch

```js
import { base } from "$app/paths";
import { settings } from "$lib/settings-store.svelte.js";

/** @type {Map<string, HTMLAudioElement>} */
const cache = new Map();

/** @type {HTMLAudioElement | null} */
let activePrimary = null; // currently-playing or chained chain leader

/** @param {string} url */
function getAudio(url) {
  let a = cache.get(url);
  if (!a) {
    a = new Audio(url);
    a.preload = "auto";
    cache.set(url, a);
  }
  return a;
}

function clipUrl(name) {
  return `${base}/audio/${settings.voice}/${name}.mp3`;
}

export function cancelPlayback() {
  if (activePrimary) {
    activePrimary.onended = null;
    activePrimary.pause();
    activePrimary.currentTime = 0;
    activePrimary = null;
  }
}

/**
 * Play a single clip; resolves when it ends (or errors / is canceled).
 * @param {string} url
 */
function playClip(url) {
  return new Promise((resolve) => {
    const a = getAudio(url);
    a.currentTime = 0;
    activePrimary = a;
    const done = () => {
      a.onended = null;
      a.onerror = null;
      if (activePrimary === a) activePrimary = null;
      resolve();
    };
    a.onended = done;
    a.onerror = done;
    a.play().catch(done); // autoplay blocked, etc.
  });
}

/** @param {number} n */
export function playNumber(n) {
  cancelPlayback();
  void playClip(clipUrl(String(n)));
}

/** @param {number} n */
export function playWaiting(n) {
  cancelPlayback();
  // Sequence: cho → number. Re-check cancelPlayback between clips so
  // a fast user click can interrupt mid-sequence.
  (async () => {
    const cho = getAudio(clipUrl("cho"));
    const num = getAudio(clipUrl(String(n)));
    activePrimary = cho;
    await playClip(clipUrl("cho"));
    if (activePrimary !== null && activePrimary !== num) return; // canceled
    await playClip(clipUrl(String(n)));
  })();
}

export function playBingo() {
  cancelPlayback();
  void playClip(clipUrl("kinh"));
}
```

## Manifest loading

Phase 4 imports `manifest.json` directly via Vite's `?json` query so
it lands in the JS bundle (no fetch needed):

```js
// In settings-store.svelte.js or SettingsButton.svelte
import manifest from "../../static/audio/manifest.json";
```

If `static/audio/manifest.json` doesn't exist yet (script not run on
fresh checkout), import fails at build time. Phase 7 mentions this in
the README.

Alternative if static-import is awkward: copy the manifest into `src/lib/`
during the script run. Defer that micro-optimization.

## Edge cases

| Case | Behavior |
|---|---|
| Voice clip 404 (e.g., user upgraded but didn't pull MP3s) | `audio.onerror` fires, promise resolves silently — game keeps working |
| User changes voice mid-game | Next call uses new URL; cache holds both voices' clips. Memory cost: each Audio node ~few KB; acceptable. |
| Rapid manual draws or auto-call at 1s | Each call hits `cancelPlayback` first; only the latest plays |
| iOS Safari autoplay block | All call sites trigger from a click handler (Tạo bảng / Xổ số / cell click) — autoplay policy satisfied |
| `playWaiting` interrupted mid-sequence | Activity flag check between clips bails out; prevents stale tail playing on top of a newer event |
| Browser tab backgrounded | Browser pauses Audio elements automatically; nothing for us to do |

## Tests

Skip unit tests for this module — it's a thin wrapper around the
DOM Audio element. Phase 1 covers the only branchy logic
(`numberToVietnamese`). Manual smoke test in browser is the validation.

If we ever want to test it: stub `Audio` with a mock that records
`play()` / `pause()` calls. Defer until there's a real bug to chase.

## Success criteria

- `import { playNumber, playWaiting, playBingo } from "$lib/voice.js"`
  works in components.
- `playNumber(45)` plays the chosen voice's "bốn mươi lăm" clip.
- `playWaiting(45)` plays "Chờ" then "bốn mươi lăm" with no audible
  gap > 200 ms.
- Rapid calls cancel prior playback (no overlap, no backlog).
- Module no-ops cleanly if a clip is missing (logs only).

## Risks

| Risk | Mitigation |
|---|---|
| Cache grows unbounded across voices | At most 92 × N voices ≈ 184 nodes after full warmup. Bounded; acceptable. |
| `base` not resolved at module-init time on SSR | We're SSR-disabled (`ssr: false` in `+layout.js`); `base` is always set when these functions run |

## Next
- Phase 4 wires settings + voice picker.
