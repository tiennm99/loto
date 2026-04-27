# Phase 4 — Settings: 3 keys + voice picker UI

## Context
- [plan.md](plan.md). Adds 3 keys to `settings-store` and a new
  "Âm thanh" fieldset with toggles + a voice list.

## Overview
- Priority: P1
- Status: TODO
- Effort: ~25 min

## New settings keys

| Key | Type | Default | Effect |
|---|---|---|---|
| `voiceEnabledMaster` | boolean | `true` | Speak called number when master draws |
| `voiceEnabledPlayer` | boolean | `true` | Speak "Chờ N" / "Kinh" on player events |
| `voice` | string (voiceId) | first manifest entry | Which voice to play |

All persisted under existing `loto_settings` blob, same merge-and-validate
pattern as the other keys.

## Manifest source

```js
// In settings-store.svelte.js (or a small lib/audio-manifest.js):
import manifest from "../../static/audio/manifest.json";
export const VOICES = manifest.voices;
// e.g. [{ id: "hoai-my", edgeName: "...", label: "Hoai My (nữ)", gender: "female" }, ...]
```

Vite imports JSON natively — manifest is part of the JS bundle, no
fetch needed at runtime.

## Files

| File | Change |
|---|---|
| `src/lib/audio-manifest.js` (NEW, optional) | Re-exports manifest as `VOICES` so settings-store and SettingsButton share the source |
| `src/lib/settings-store.svelte.js` | Add 3 keys to `DEFAULTS`, validate in `loadSettings` (boolean for two, string-in-VOICES-ids for `voice`) |
| `src/lib/settings-store.test.js` | 4-5 tests: defaults, persistence round-trip, corruption fallback, invalid voiceId fallback |
| `src/lib/SettingsButton.svelte` | NEW "Âm thanh" fieldset: 2 toggles + voice radio/pill list |

## settings-store.svelte.js — pattern

```js
import { VOICES } from "$lib/audio-manifest.js";

const VOICE_IDS = new Set(VOICES.map((v) => v.id));
const DEFAULT_VOICE = VOICES[0]?.id ?? "hoai-my"; // graceful fallback

const DEFAULTS = {
  // ...existing 5 keys...
  voiceEnabledMaster: true,
  voiceEnabledPlayer: true,
  voice: DEFAULT_VOICE,
};

// In loadSettings(), per-key validate:
//   voiceEnabledMaster:  typeof v === "boolean"  → keep, else default
//   voiceEnabledPlayer:  typeof v === "boolean"  → keep, else default
//   voice:               VOICE_IDS.has(v)        → keep, else default
```

No CSS variables, no `<html>` class — pure values read by other modules.

## SettingsButton.svelte — UI fieldset

Place between "Tự động xổ" and "Màu ô trống" (logical grouping with
feedback features).

```svelte
<fieldset class="mb-5">
  <legend class="text-sm font-semibold ...">Âm thanh</legend>
  <p class="text-xs text-slate-400 dark:text-slate-500 mb-2">
    Đọc số bằng tiếng Việt
  </p>

  <button onclick={() => toggleVoiceMaster()} class="...pill...">
    Quản trò: {settings.voiceEnabledMaster ? "Bật" : "Tắt"}
  </button>

  <button onclick={() => toggleVoicePlayer()} class="...pill mt-2...">
    Người chơi (Chờ / Kinh): {settings.voiceEnabledPlayer ? "Bật" : "Tắt"}
  </button>

  <div class="mt-3">
    <p class="text-xs text-slate-500 dark:text-slate-400 mb-1">Giọng đọc</p>
    <div class="grid grid-cols-2 gap-2">
      {#each VOICES as v}
        <button
          onclick={() => saveSettings({ voice: v.id })}
          class="px-3 py-2 rounded-lg border-2 text-sm
                 {settings.voice === v.id
                   ? 'border-emerald-500 bg-emerald-50 dark:bg-emerald-950/30'
                   : 'border-slate-300 dark:border-slate-600'}"
        >
          {v.label}
        </button>
      {/each}
    </div>
  </div>
</fieldset>
```

Optional polish: tiny "▶" preview button next to each voice that calls
`playNumber(45)` so users can hear before committing. Skip in v1; add
if asked.

## Tests

In `settings-store.test.js`:

1. Defaults: `voiceEnabledMaster === true`, `voiceEnabledPlayer === true`,
   `voice === DEFAULT_VOICE` (first manifest entry).
2. Round-trip: `saveSettings({ voice: "nam-minh" })` persists and reloads.
3. Corrupt boolean → falls back to default for that key, others survive.
4. Invalid voice id (`"made-up"`) → falls back to default voice.
5. Empty manifest case (theoretical — if somehow no voices) → default voice
   string still set without crashing other settings.

## Success criteria

- Toggling either pill flips the boolean immediately (rune-reactive).
- Picking a voice updates `settings.voice` and next call uses new clips.
- Page reload preserves all 3 settings.
- Vitest still green; settings-store tests grow by 4-5.

## Risks

| Risk | Mitigation |
|---|---|
| Manifest absent on fresh checkout (Phase 2 not run) | Build fails fast at import — README tells contributors to run the script first |
| Manifest renamed a voice id; saved id no longer valid | `loadSettings` falls back to default voice (covered by test #4) |
| User picks a voice but its clips are missing on disk | Phase 3's `audio.onerror` keeps app functional; UI still works (silent) |

## Next
- Phase 5 reads `voiceEnabledMaster` to gate master speech.
- Phase 6 reads `voiceEnabledPlayer` to gate player speech.
