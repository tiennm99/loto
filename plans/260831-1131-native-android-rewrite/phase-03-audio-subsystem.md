---
phase: 3
title: "Audio Subsystem"
status: done
priority: P1
effort: "1d"
dependencies: [1]
---

# Phase 3: Audio Subsystem

## Overview

Play the bundled Vietnamese voice clips (2 voices × 92 clips: 1–90 +
`cho` + `kinh`, mounted from `web/static/audio` in Phase 1) with the same
sequencing and cancellation semantics as `web/src/lib/voice.js`.

## Requirements

- Functional: announce a drawn number; announce "Chờ" (optionally
  followed by the awaited number — the `voiceWaitingNumber` setting);
  announce "Kinh"; switching voice takes effect on the next utterance;
  a new utterance cancels any in-flight one (token semantics of
  `voice.js`).
- Non-functional: no audio focus grab beyond transient playback
  (`plans/todo.md` flags the unanswered incoming-call question — decide
  and document here: use `AudioAttributes` USAGE_MEDIA +
  `setHandleAudioBecomingNoisy`, request no focus or transient-may-duck;
  record the choice in the code and android README); no INTERNET.

## Architecture

Package `com.miti99.loto.audio`:

- `VoiceCatalog.kt` — parses `assets/audio/manifest.json`
  (`{voices: [{id, edgeName, label, gender}]}`, same file the web
  imports); exposes `VOICES`, default voice id, and
  `clipPath(voiceId, clip)` where clip ∈ 1..90 | cho | kinh. Malformed
  manifest → hardcoded fallback to `hoai-my`/`nam-minh` (mirror the web's
  defensive posture).
- `VoicePlayer.kt` — wraps a single Media3 ExoPlayer on the main thread;
  `speak(vararg clips)` builds a gapless playlist (e.g. `cho` + `34`)
  from `asset:///audio/...` URIs; each call increments a token and stops
  the previous playback. `release()` from Application/Activity teardown.
- Interface `VoicePlayerApi` so ViewModels depend on an abstraction and
  unit tests use a recording fake; the ExoPlayer implementation is
  covered by a small instrumentation test (runs in Phase 10's device
  pass).

## Related Code Files

- Create: `app/src/main/java/com/miti99/loto/audio/VoiceCatalog.kt`,
  `.../audio/VoicePlayer.kt`
- Create tests: `app/src/test/java/com/miti99/loto/audio/VoiceCatalogTest.kt`
  (JSON parse + fallback), fake player for ViewModel tests
- Spec: `web/src/lib/voice.js`, `voice.test.js`,
  `web/src/lib/audio-manifest.js`, `web/static/audio/manifest.json`

## Implementation Steps

1. Read `voice.js` + its test; write down the cancellation token rules
   and the exact chờ+number sequence gating (`voiceEnabledPlayer`,
   `voiceWaitingNumber`).
2. Implement `VoiceCatalog` + parse test against the real manifest file
   (read from test resources via the same relative path).
3. Implement `VoicePlayer` (ExoPlayer, playlist, token cancellation).
4. Wire construction in `LotoApplication`; release in `onTerminate`
   path/activity destroy as appropriate.

## Success Criteria

- [ ] Catalog test parses the real `manifest.json` and falls back on garbage input
- [ ] Fake-player unit tests assert sequencing + cancellation token rules
- [ ] Manual emulator check: number clip plays; rapid draws cut each other off; chờ+N plays gaplessly (deferred to Phase 10 checklist)

## Risk Assessment

- **ExoPlayer `asset:///` quirks on API 24** — the old port's research
  already picked ExoPlayer over SoundPool (decision record:
  `git show f7cbb6e:android/plans/todo.md`; the standalone research
  reports no longer exist). Signal: playback failure on
  the API 24 emulator in Phase 10. Response: fall back to
  `AssetFileDescriptor` + `ProgressiveMediaSource`, or MediaPlayer for
  single clips — decide then, not now.
