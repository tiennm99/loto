# Phase 6 — Player: play "Chờ N" and "Kinh"

## Context
- [plan.md](plan.md). Depends on Phases 2–4.
- Hook into the existing waiting/celebrated `$effect` block in
  `PlayerBoard.svelte` (lines 83–108). Sound is a sibling output to
  the toast and bingo popup, gated by the existing `notifiedWaitingRows`
  / `celebratedRows` sets to dedupe.

## Overview
- Priority: P1
- Status: TODO
- Effort: ~10 min

## Files

| File | Change |
|---|---|
| `src/lib/PlayerBoard.svelte` | Import `playWaiting`, `playBingo`, `cancelPlayback`. Call beside existing toast + popup triggers. Cancel on `handleGenerate` / `handleClear`. |

## Diff (illustrative)

```diff
+ import { playWaiting, playBingo, cancelPlayback } from "$lib/voice.js";
+ import { settings } from "$lib/settings-store.svelte.js";
  // ...

  $effect(() => {
    if (!grid || crossed.length === 0) return;

    // Pass 1: at most one bingo popup per render
    for (let i = 0; i < grid.length; i++) {
      if (!celebratedRows.has(i) && isRowComplete(grid, crossed, i)) {
        celebratedRows.add(i);
        notifiedWaitingRows.add(i);
        congratsRow = i + 1;
        showCongrats = true;
+       if (settings.voiceEnabledPlayer) playBingo();
        break;
      }
    }

    // Pass 2: update waiting state for every non-celebrated row
    for (let i = 0; i < grid.length; i++) {
      if (celebratedRows.has(i)) continue;
      const waitNum = getWaitingNumber(grid, crossed, i);
      if (waitNum !== null && !notifiedWaitingRows.has(i)) {
        notifiedWaitingRows.add(i);
        showToast(`Chờ ${waitNum}`);
+       if (settings.voiceEnabledPlayer) playWaiting(waitNum);
      } else if (waitNum === null && notifiedWaitingRows.has(i)) {
        notifiedWaitingRows.delete(i);
      }
    }
  });

  function handleGenerate() {
    if (grid && !confirm("Bạn có muốn tạo lại bảng không?")) return;
+   cancelPlayback();
    // ...existing...
  }

  function handleClear() {
    if (!grid) return;
    const hasMarks = crossed.some((row) => row.some(Boolean));
    if (hasMarks && !confirm("Bạn có muốn xoá tất cả đánh dấu không?")) return;
+   cancelPlayback();
    // ...existing...
  }
```

## De-duplication is already there

The existing `notifiedWaitingRows` and `celebratedRows` sets gate the
toast + popup; the audio call hangs on the same gates so we never
play twice for the same row. ✓

## Edge cases

| Case | Behavior |
|---|---|
| Multiple rows enter "waiting" same render | Each calls `playWaiting`, but cancel-then-play (Phase 3) means only the LAST plays. Acceptable; queueing is a future tune-up. |
| Bingo + waiting on same render | Impossible — `if (celebratedRows.has(i)) continue` short-circuits the waiting check. ✓ |
| Player crosses fast, toggles waiting on/off | `notifiedWaitingRows.delete(i)` lets re-firing happen next entry into waiting. ✓ |
| Player uncrosses winning cell | No "un-bingo" sound. Acceptable — the popup already requires explicit dismiss. |
| Master + player speech race | Latest call wins (cancel-then-play). Player events are low-frequency, so this is rarely user-visible. |

## Success criteria

- Cross 4 of 5 cells in a row → "Chờ {N}" plays.
- Cross the 5th → bingo popup + "Kinh" plays.
- "Tạo bảng mới" / "Xoá đánh dấu" stop any pending playback.
- `voiceEnabledPlayer=false` → silent on both events; toast and popup
  still appear.

## Risks

| Risk | Mitigation |
|---|---|
| Master and player speech overlap (e.g., player marks a cell during master draw) | Cancel-then-play documented behavior; latest event wins |
| iOS Safari first-event no-play (no prior gesture) | First waiting state requires 4 prior cell clicks → gesture already satisfied. ✓ |

## Next
- Phase 7 docs sync.
