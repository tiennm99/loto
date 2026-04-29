# Phase 02 — Section "Chờ" ring on label band

## Context

Upstream commit `a60ea08` (2026-04-28) part 1: each section's label band
glows amber while any of its 3 rows is one cell from bingo and not yet
complete. Reduces reliance on the 5 s waiting toast.

## Priority

P1 — UX nice-to-have, reinforces existing toast.

## Status

pending.

## Key insights

- Web computes `waitingRows[r]` per row, then `sectionHasWaiting[s]` =
  any of rows `[s*3 .. s*3+2]` waiting. Same shape works in VM.
- The flag must be derived from the same snapshot as `crossed`/`grid`.
  Cleanest is a derived field in `PlayerBoardUiState` recomputed inside
  `applyCrossedChange` and `loadFromStorage`.
- Compose has no `prefers-reduced-motion` CSS query. Use
  `LocalAccessibilityManager.current` (or similar `Settings.Global` read)
  to detect reduce-motion. Default to a static glow with no pulse if
  detection is too noisy.

## Related code files

- `app/src/main/java/com/miti99/loto/state/PlayerBoardUiState.kt`
- `app/src/main/java/com/miti99/loto/state/PlayerBoardViewModel.kt`
- `app/src/main/java/com/miti99/loto/ui/board/PlayerBoardGrid.kt`
- `app/src/main/java/com/miti99/loto/ui/theme/Color.kt` (amber band color, may
  reuse `BrandAmberLight`)

## Implementation steps

1. **State shape** (`PlayerBoardUiState.kt`):

   ```kotlin
   val sectionHasWaiting: List<Boolean> = listOf(false, false, false),
   ```

2. **VM derivation** (`PlayerBoardViewModel.kt`):

   - In `applyCrossedChange` (after computing `rowComplete`), add:

     ```kotlin
     val waitingRows = grid.indices.map { r ->
         !rowComplete[r] &&
             GameLogic.getWaitingNumber(gridArr, crossedArr, r) != null
     }
     val sectionHasWaiting = listOf(0, 3, 6).map { start ->
         (start until start + 3).any { r -> waitingRows.getOrElse(r) { false } }
     }
     ```

   - Pass `sectionHasWaiting = sectionHasWaiting` into the `_state.update`
     copy.
   - Mirror the same derivation in `loadFromStorage`, `clear`, `generate`
     (last two reset to `[false, false, false]` since no marks yet).

3. **UI** (`PlayerBoardGrid.kt`):

   Change the section-label `Text` to a `Box` wrapping the label, and
   apply a `border` + glow when `sectionHasWaiting[sectionIdx]`:

   ```kotlin
   val isWaiting = sectionHasWaiting.getOrNull(sectionIdx) ?: false
   val infinite = rememberInfiniteTransition(label = "cho-pulse")
   val alpha by infinite.animateFloat(
       initialValue = 0.55f, targetValue = 1.0f,
       animationSpec = infiniteRepeatable(
           tween(1500, easing = LinearEasing), RepeatMode.Reverse,
       ),
       label = "cho-alpha",
   )
   Box(
       modifier = Modifier
           .fillMaxWidth()
           .padding(start = 4.dp, top = if (sectionIdx > 0) 12.dp else 4.dp, bottom = 2.dp)
           .then(
               if (isWaiting) Modifier.background(
                   color = BrandAmberLight.copy(alpha = alpha * 0.18f),
                   shape = RoundedCornerShape(8.dp),
               ).border(
                   width = 1.dp,
                   color = BrandAmberLight.copy(alpha = alpha),
                   shape = RoundedCornerShape(8.dp),
               ).padding(horizontal = 8.dp, vertical = 2.dp)
               else Modifier
           ),
   ) {
       Text(
           text = stringResource(SECTION_LABEL_RES[sectionIdx]),
           fontSize = 11.sp,
           fontWeight = FontWeight.Bold,
           color = if (isWaiting) BrandAmberLight else MaterialTheme.colorScheme.onSurfaceVariant,
       )
   }
   ```

   - Update `PlayerBoardGrid` signature to accept
     `sectionHasWaiting: List<Boolean>`.
   - Caller (`PlayerBoardScreen`) passes `state.sectionHasWaiting`.

4. **Reduce-motion guard**:

   - Wrap the `infiniteTransition` block in
     `if (LocalAccessibilityManager.current?.isEnabled != true) infinite else stillPulse`,
     OR fall back to a static `alpha = 0.85f` for users with reduce-motion
     enabled. Implementation choice is fine either way; prefer the simple
     static fallback unless QA disagrees.

## Todo

- [ ] Add `sectionHasWaiting` to `PlayerBoardUiState`
- [ ] Compute it in VM (apply, load, clear, generate)
- [ ] Update `PlayerBoardGrid` signature + render
- [ ] Wire from `PlayerBoardScreen`
- [ ] Reduce-motion fallback

## Success criteria

- A row with exactly 1 uncrossed number flips its section label to amber
  glow; manually un-checking a different cell that breaks Chờ removes the
  glow.
- All 3 sections light up if all of them have a waiting row.
- A bingo'd section drops its glow once the row completes.
- No glow on cold start (no grid).

## Risks

- Compose `infiniteTransition` keeps recomposing while visible. Confirm
  this doesn't cause bad battery on long sessions — measure once during
  QA. If problematic, drop to a single-shot fade-in static state.

## Next

Phase 03 (palette).
