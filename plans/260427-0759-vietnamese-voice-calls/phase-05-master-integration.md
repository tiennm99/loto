# Phase 5 — Master: play called number

## Context
- [plan.md](plan.md). Depends on Phases 2–4.
- Single hook point: `handleDrawNext()` in `MasterPanel.svelte:119`.
  Both manual click ("Xổ số") and auto-call interval funnel through it.

## Overview
- Priority: P1
- Status: TODO
- Effort: ~5 min

## Files

| File | Change |
|---|---|
| `src/lib/MasterPanel.svelte` | Import `playNumber`, `cancelPlayback`. Call `playNumber(next)` after state update; `cancelPlayback()` on new game. |

## Diff (illustrative)

```diff
+ import { playNumber, cancelPlayback } from "$lib/voice.js";
  // ...

  function handleNewGame() {
    if (state && !confirm("Bạn có muốn tạo ván mới không?")) return;
+   cancelPlayback();
    autoRunning = false;
    state = createFreshState();
    lastCalled = null;
  }

  function handleDrawNext() {
    if (!state || state.remaining.length === 0) return;
    const next = state.remaining[0];
    state = {
      called: [...state.called, next],
      remaining: state.remaining.slice(1),
    };
    lastCalled = next;
+   if (settings.voiceEnabledMaster) playNumber(next);
  }
```

## Why one hook covers both manual + auto

The auto-call `$effect` (line ~96 in current file) calls
`handleDrawNext()` directly. No duplicate hook needed. ✓

## Edge cases

| Case | Behavior |
|---|---|
| User mashes "Xổ số" rapidly | `playNumber` cancels prior playback (Phase 3) — only the latest plays. ✓ |
| Auto-call at 1s with ~1.5s clips | Same — cancel-then-play. Truncation is the cost of fast mode; users adjust speed if needed. ✓ |
| Toggle `voiceEnabledMaster` off mid-game | Next draw stays silent. Already-playing clip finishes (acceptable) or `cancelPlayback()` if we want immediate silence — KISS, skip. |
| `state.remaining.length === 0` | Early-return guards; `playNumber` never called. ✓ |
| Voice changed mid-game | Next draw uses new voice's clip. ✓ |

## Success criteria

- Click "Xổ số" with `voiceEnabledMaster=true` → audible Vietnamese
  number in chosen voice.
- Auto-call announces every drawn number.
- Toggle off → next draws silent.
- "Ván mới" → any pending playback stops.

## Risks

| Risk | Mitigation |
|---|---|
| Clip 404 (corrupt static dir) | Phase 3's `audio.onerror` no-ops silently |
| iOS Safari autoplay block | First "Xổ số" click satisfies the user-gesture requirement |

## Next
- Phase 6 mirrors this in PlayerBoard.
