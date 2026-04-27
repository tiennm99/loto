# Phase 7 — Tests pass + docs sync

## Context
- [plan.md](plan.md). Final phase. No new features — verification +
  doc drift cleanup.

## Overview
- Priority: P2
- Status: TODO
- Effort: ~15 min

## Verification

```bash
npm test
npm run build
```

Expected:
- 54 (current) + ~12 (Phase 1 number tests) + ~5 (Phase 4 settings
  tests) ≈ 71 tests passing.
- Build clean. `static/audio/` carried into `build/audio/` by
  adapter-static automatically.

Manual quick-check after build:
```bash
ls build/audio/hoai-my/ | head   # confirm clips bundled
```

## Manual smoke test (browser)

| Step | Expected |
|---|---|
| Fresh load `/`, click "Tạo bảng mới" | grid renders, no audio |
| Cross 4 cells in a row | "Chờ {N}" plays (chosen voice) |
| Cross the 5th | "Kinh" plays + bingo popup |
| Open Settings → toggle "Người chơi" off → cross to bingo | popup, no audio |
| Toggle master mode on, click "Xổ số" | called number plays |
| Settings → switch voice (Hoài Mỹ → Nam Minh) → click "Xổ số" | new voice plays |
| Auto-call at 1s | each draw plays, no backlog |
| Click "Ván mới" mid-clip | playback stops |
| Hard refresh, settings persisted | voice selection survives |
| DevTools → Network tab on first draw | one MP3 fetch from `/audio/{voice}/{n}.mp3`; subsequent draws use cache |

## Docs to sync

| File | Change |
|---|---|
| `docs/codebase-summary.md` | Add `vietnamese-number.js`, `voice.js`, `audio-manifest.js` rows. Update `SettingsButton.svelte` row to mention 5 fieldsets (was 4) + voice picker. Update `settings-store.svelte.js` description: 8 keys (was 5). New "Static Assets" subsection mentioning `static/audio/{voiceId}/`. |
| `docs/project-overview-pdr.md` | Settings section: add "Âm thanh" fieldset (master toggle, player toggle, voice picker). Add acceptance items for voice-on-number, voice-on-Chờ/Kinh, voice picker. Tech Stack: note "Free build-time TTS via edge-tts". |
| `docs/development-roadmap.md` | Drop "Sound Effects on Bingo" idea entry — done in better form (TTS, configurable voice). |
| `docs/deployment-guide.md` | Brief note: regenerating audio = `pip install edge-tts && python3 scripts/generate-audio.py`; no impact on CF Pages build (MP3s are committed). |
| `README.md` | "Regenerating audio" subsection (one short paragraph). |

Skip changes to `system-architecture.md` and `code-standards.md` —
voice is a leaf module with no architectural surface.

## Roadmap

The roadmap currently lists "Sound Effects on Bingo" as an Idea. After
this lands, **delete that section** — it's done in the better form
(neural TTS, configurable voice).

## Cross-language `numberToVietnamese` divergence guard

`scripts/generate-audio.py` and `src/lib/vietnamese-number.js` both
implement the same Vietnamese number rules. They have to stay in sync.
Mitigations:

1. JS unit tests (Phase 1) cover all tonal exceptions; if JS drifts,
   tests catch it.
2. Manual: after editing either file, re-read both side by side.
3. (Future) auto-check via a small CI script that calls Python +
   loads the JS via Node and compares 1..90. Skip in v1.

## Out of scope (intentionally)

- `voice.test.js` — Phase 3 explained why (DOM Audio mock churn not
  worth it; the only branchy logic is `numberToVietnamese`, already
  covered by Phase 1 tests).
- Component tests for SettingsButton voice toggles + voice picker —
  manual smoke test plus settings-store unit tests cover it.
- E2E tests for audio playback (requires Playwright + audio capture).

## Success criteria

- All tests pass.
- `npm run build` clean. `build/audio/` populated.
- Manual smoke checklist all green.
- Docs updated to reflect new files, new settings keys, new voice
  picker.
- No stale "Sound Effects" idea in roadmap.
