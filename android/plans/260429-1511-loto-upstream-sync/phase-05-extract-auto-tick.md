# Phase 05 — Extract auto-tick into pure helper (OPTIONAL)

## Context

Upstream commit `3e6cb90` (2026-04-28). Pulled the bus-driven auto-tick
effect body out of `PlayerBoard.svelte` into `src/lib/auto-tick.js` so
the dedup-by-`at` invariant — which already caught a P0 — is unit-testable
without mounting the component.

Android equivalent: `PlayerBoardViewModel.consumeBusEvent`. It already
encodes the dedup invariant via `lastConsumedEventId`. **Behavior is
already correct.** This phase is a refactor for testability parity.

## Priority

P3 — optional. Skip without consequence; the invariant is already
exercised in `PlayerBoardViewModelTest`.

## Status

pending.

## Decision

Recommend **defer** this phase unless a test gap emerges during phase 06.
The Kotlin VM-level test already covers monotonic-id dedup — extracting a
pure helper just shuffles the test boundary.

If we later want test parity:

## Implementation outline (if adopted)

1. **Create** `app/src/main/java/com/miti99/loto/state/AutoTick.kt`:

   ```kotlin
   package com.miti99.loto.state

   import com.miti99.loto.settings.SettingsState

   data class AutoTickResult(
       val crossed: List<List<Boolean>>,
       val lastConsumedEventId: Long,
       val changed: Boolean,
   )

   /**
    * Pure auto-tick rule — port of upstream `processAutoTick`.
    * Always advances `lastConsumedEventId` on any NEW event (id > last)
    * even when no cell flips, so reactive re-runs of cross/grid changes
    * don't re-fire the same event.
    */
   fun processAutoTick(
       grid: List<List<Int>>?,
       crossed: List<List<Boolean>>,
       event: DrawEvent?,
       lastConsumedEventId: Long,
       mode: SettingsState.Mode,
   ): AutoTickResult {
       if (event == null || event.id <= lastConsumedEventId) {
           return AutoTickResult(crossed, lastConsumedEventId, false)
       }
       val advanced = event.id
       if (mode != SettingsState.Mode.BOTH) {
           return AutoTickResult(crossed, advanced, false)
       }
       if (grid == null || crossed.isEmpty()) {
           return AutoTickResult(crossed, advanced, false)
       }
       var hit = false
       val updated = crossed.mapIndexed { r, line ->
           line.mapIndexed { c, v ->
               if (!hit && grid[r][c] == event.num && !v) { hit = true; true } else v
           }
       }
       return if (hit) AutoTickResult(updated, advanced, true)
       else AutoTickResult(crossed, advanced, false)
   }
   ```

2. **Refactor** `PlayerBoardViewModel.consumeBusEvent` to call it:

   ```kotlin
   private fun consumeBusEvent(event: DrawEvent) {
       val cur = _state.value
       val mode = currentSettings().mode
       val r = processAutoTick(
           grid = cur.grid,
           crossed = cur.crossed,
           event = event,
           lastConsumedEventId = cur.lastConsumedEventId,
           mode = mode,
       )
       if (r.changed) applyCrossedChange(r.crossed, r.lastConsumedEventId)
       else _state.update { it.copy(lastConsumedEventId = r.lastConsumedEventId) }
   }
   ```

3. **Tests** in `app/src/test/java/com/miti99/loto/state/AutoTickTest.kt`:
   port the 8 vitest cases from `auto-tick.test.js` (NEW draw, dedup,
   re-cross after manual untick, mode=master/player ignored, off-board,
   null event, null grid, advance even on miss).

## Todo

- [ ] Create AutoTick.kt
- [ ] Refactor consumeBusEvent
- [ ] Port 8 tests
- [ ] Compile + tests pass

## Success criteria

- All existing PlayerBoardViewModelTest cases still pass.
- New AutoTickTest passes 8/8.
- `consumeBusEvent` body shrinks to ≤ 12 lines.

## Risks

- None — pure refactor. Behavior change is impossible (function returns
  exactly the same `(crossed, lastConsumedEventId, changed)` tuple).

## Next

Phase 06 (tests + verification).
