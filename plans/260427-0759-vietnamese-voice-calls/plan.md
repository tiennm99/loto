---
slug: vietnamese-voice-calls
created: 2026-04-27
updated: 2026-04-27
status: completed
completedAt: 2026-04-27
mode: fast
blockedBy: []
blocks: []
---

# Vietnamese voice calls — bundled MP3 clips, no runtime APIs

Speak the called number aloud (master), and "Chờ {N}" / "Kinh!"
(player) when those events fire. **Free + offline + zero runtime API
calls** — pre-generate Vietnamese audio once during dev (using the
free `edge-tts` Microsoft neural voice) and ship the MP3s as static
assets bundled with the app.

## Why this approach (vs Web Speech API)

User wants the audio bundled in the project — no runtime API or browser
TTS dependency.

| Concern | Web Speech API | Bundled MP3 (this plan) |
|---|---|---|
| Runtime cost | free | free |
| Voice quality | OS-dependent (Linux Chrome often has no `vi`) | uniform, neural, Vietnamese |
| Bundle weight | 0 KB | ~2.2 MB (92 clips × ~12 KB × 2 voices, lazy-loaded) |
| Offline | yes | yes |
| Privacy | TTS may upload text to OS service | fully self-hosted |
| Build complexity | none | one-time `python` script |

Trade-off accepted: ~1.1 MB added to repo + initial page weight, in
exchange for consistent, high-quality Vietnamese audio everywhere.

## Decisions (locked)

- **TTS engine for generation**: `edge-tts` (Python). Free, no API key,
  Microsoft Neural Voice quality.
- **All Vietnamese voices generated**: script auto-discovers every
  `vi-*` voice in edge-tts and produces a separate clip set per voice
  under `static/audio/{voiceId}/`. As of writing: `vi-VN-HoaiMyNeural`
  (female) and `vi-VN-NamMinhNeural` (male) — script picks up new
  voices automatically on regen.
- **Active voice configurable in settings**: new `voice` key (string,
  matches a voice's `id` from `manifest.json`). Default = first voice
  in manifest (`hoai-my`). User picks in the "Âm thanh" fieldset.
- **Runtime playback**: HTML5 `<audio>` via plain `new Audio(url)`. No
  Web Audio API. Files lazy-loaded on first request; browser caches
  after that.
- **Asset layout**: `static/audio/{voiceId}/{1..90}.mp3` +
  `static/audio/{voiceId}/cho.mp3` + `static/audio/{voiceId}/kinh.mp3`,
  plus `static/audio/manifest.json` (voice list). 92 files per voice.
- **"Chờ N" composition**: play `cho.mp3` then `{N}.mp3` in sequence
  via `audio.onended`. Saves 90 files vs pre-generating "Chờ 1.mp3"
  through "Chờ 90.mp3".
- **Cancel-then-play**: each request stops any in-flight audio first.
  Auto-call at 1s with ~1.5s clips → without cancel we'd grow a
  backlog. Latest number is what matters.
- **basePath aware**: URLs go through `import { base } from "$app/paths"`
  so the same code works on `loto.miti99.com`, `tiennm99.github.io/loto`,
  and code-server `/absproxy/{port}`.
- **Three settings keys**:
  - `voiceEnabledMaster: boolean` (default `true`) — speak called number
  - `voiceEnabledPlayer: boolean` (default `true`) — speak "Chờ N" / "Kinh"
  - `voice: string` (default first manifest entry) — which voice to use
- **No volume slider, no rate/pitch tuning** — device volume + a curated
  voice list. KISS.
- **Hook points**:
  - Master: inside `handleDrawNext()` (covers manual click + auto-call)
  - Player: inside the existing waiting/celebrated `$effect` in
    `PlayerBoard.svelte` (uses existing dedupe sets)

## Phases

| # | Phase | File |
|---|---|---|
| 1 | Vietnamese number utility (1–90) — provides TTS prompts + tests | DONE | [phase-01-vietnamese-number-utility.md](phase-01-vietnamese-number-utility.md) |
| 2 | Audio generation script (all `vi-*` voices via edge-tts) → commit `static/audio/{voiceId}/*.mp3` + `manifest.json` | DONE (script + placeholder manifest; user runs script to materialize MP3s) | [phase-02-audio-generation-script.md](phase-02-audio-generation-script.md) |
| 3 | `voice.js` playback module — `<audio>` lazy cache, sequencer, cancel, voice-aware URL builder | DONE | [phase-03-audio-playback-module.md](phase-03-audio-playback-module.md) |
| 4 | Settings: 3 keys (master/player toggles + voice picker) + "Âm thanh" fieldset + tests | DONE | [phase-04-settings-integration.md](phase-04-settings-integration.md) |
| 5 | MasterPanel: announce drawn number in `handleDrawNext()` | DONE | [phase-05-master-integration.md](phase-05-master-integration.md) |
| 6 | PlayerBoard: announce "Chờ N" / "Kinh" in waiting/bingo $effect | DONE | [phase-06-player-integration.md](phase-06-player-integration.md) |
| 7 | Tests pass + docs sync | DONE (98/98 tests; reviewer fixes applied) | [phase-07-tests-and-docs.md](phase-07-tests-and-docs.md) |

## Files Touched / Created

| File | Phase |
|---|---|
| `src/lib/vietnamese-number.js` (NEW) | 1 |
| `src/lib/vietnamese-number.test.js` (NEW) | 1 |
| `scripts/generate-audio.py` (NEW) | 2 |
| `scripts/README.md` (NEW, optional) | 2 |
| `static/audio/*.mp3` (NEW, 92 files, committed) | 2 |
| `src/lib/voice.js` (NEW) | 3 |
| `src/lib/settings-store.svelte.js` | 4 |
| `src/lib/settings-store.test.js` | 4 |
| `src/lib/SettingsButton.svelte` | 4 |
| `src/lib/MasterPanel.svelte` | 5 |
| `src/lib/PlayerBoard.svelte` | 6 |
| `docs/codebase-summary.md` | 7 |
| `docs/project-overview-pdr.md` | 7 |
| `docs/development-roadmap.md` | 7 (drop stale "Sound Effects" idea) |
| `.gitattributes` (optional, for LFS-free MP3 handling) | 2 |
| `README.md` (mention audio regen) | 2 |

## Out of Scope

- Audio sprite / single-file packing (HTTP/2 makes 92 small fetches fine)
- Volume slider, voice picker, alt voices, male voice, child voice
- Pre-rendered "Chờ 1..90" combo clips (sequence at runtime)
- Sound effects on bingo (chimes / confetti audio)
- Service Worker pre-caching (PWA territory; future plan)
- Lô tô fairground rhythm / chant style — flat reading only
- Runtime TTS APIs (edge-tts is build-time only; no runtime API)
