# Phase 3 — Master speaks Chờ / Kinh

## Overview

When `voiceEnabledMaster` is on AND `mode === "both"`, the master also
speaks "Chờ" / "Kinh" announcements as the player board hits those
states. Player voice toggle continues to work in solo `player` mode.
No new setting; the existing `voiceEnabledMaster` flag covers it.

**Status**: not started
**Priority**: P1
**Effort**: ~20 min
**Depends on**: Phase 1 (mode check)
**Independent of**: Phase 2 (different code path, different effects)

## Files to modify

- `src/lib/PlayerBoard.svelte` — broaden the voice trigger condition
  on the existing Bingo + Chờ effects
- (optional) `src/lib/SettingsButton.svelte` — small text update on
  the "Quản trò đọc số" hint to mention Chờ/Kinh

## Design

Today's PlayerBoard runs:

```js
if (settings.voiceEnabledPlayer) playBingo();
// ...
if (settings.voiceEnabledPlayer) playWaiting(waitNum);
```

Change to:

```js
if (shouldAnnouncePlayerVoice()) playBingo();
// ...
if (shouldAnnouncePlayerVoice()) playWaiting(waitNum);
```

Where:

```js
function shouldAnnouncePlayerVoice() {
  return (
    settings.voiceEnabledPlayer ||
    (settings.voiceEnabledMaster && settings.mode === "both")
  );
}
```

Inline the boolean if the helper feels heavy — DRY-but-tiny:

```js
const announce =
  settings.voiceEnabledPlayer ||
  (settings.voiceEnabledMaster && settings.mode === "both");
if (announce) playBingo();
```

### Why no new setting

- `voiceEnabledMaster` already implies "the host's audio role".
- In `both` mode, the master IS the announcer; Chờ/Kinh are part of
  what an announcer says.
- Adds no UX complexity; the existing toggle just covers more cases.

### Why this can ship without phase 2

The Chờ/Kinh announcements fire from the player board's existing
effect on `crossed` changes. Those changes happen whether the cell
was crossed manually OR by phase 2's auto-tick. So phase 3 works
solo: player taps a cell that completes a row → master speaks "Kinh"
in `both` mode.

### Settings hint copy

Current "Quản trò đọc số" has no description. Add a one-line note:

```svelte
<p class="text-xs text-slate-500 dark:text-slate-400 mt-1.5 px-1">
  Đọc số đã xổ + báo Chờ/Kinh khi ở "Cả hai".
</p>
```

(Only when in master-visible modes, to keep the panel uncluttered.)

## Todo

- [ ] Add `announce` derived boolean in PlayerBoard before each
      voice call
- [ ] Update both `playBingo()` and `playWaiting()` call sites
- [ ] Add small clarifying hint under "Quản trò đọc số"
- [ ] `npm test && npm run build` — green
- [ ] Manual smoke:
  - mode=player, voicePlayer=on, voiceMaster=off → Chờ/Kinh play
  - mode=player, voicePlayer=off → silent
  - mode=both, voicePlayer=off, voiceMaster=on → Chờ/Kinh play
  - mode=both, voicePlayer=off, voiceMaster=off → silent
  - mode=master alone → no player events to announce

## Success criteria

- Existing player-voice tests still pass.
- New behavior verified manually with above matrix.
- Voice cancellation logic in `voice.js` is unchanged; double-trigger
  scenarios resolve as "last wins" (already covered by existing
  cancellation pattern).

## Risks

- **Surprise users who turned off player voice on purpose**: low —
  the master flag is on by default and they likely want all master
  audio. If they specifically want master-number-only, they'd need a
  finer-grained setting; out of scope for this iteration. Note in
  follow-up.
- **Two clips overlapping**: if a draw fires `playNumber` (master) and
  the player auto-tick (phase 2) fires `playWaiting` immediately,
  voice.js cancels the in-flight clip. The most recent call wins.
  Acceptable tradeoff; the alternative (queueing) creates speech lag.

## Follow-up (out of scope)

- A "what does master speak?" sub-panel: number / Chờ / Kinh as
  individual toggles. Add only if a real user asks.
