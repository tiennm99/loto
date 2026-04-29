# Phase 01 — Confetti & celebration tier rule

## Context

Upstream commit `a60ea08` (2026-04-28). Two changes bundled here:

1. **Tier rule**: confetti now fires on the 2nd bingo, OR on the 1st bingo
   when another row is in Chờ. Old rule (`>= 3`) rarely fired on a 9-row
   card, so most wins felt under-celebrated.
2. **Confetti emoji set + size jitter**: add 🥢 🎋 🏮 (chopsticks, bamboo,
   lantern) for hội-chợ flavour; per-piece size 1.5–2.4 sp scale.

## Priority

P0 — game feel.

## Status

pending.

## Key insights

- The "Chờ" check needs the live grid + crossed state at bingo time. The VM
  already has both in `_state.value`. We have to compute "is any *other*
  row in Chờ" at the moment we evaluate the tier (just after marking the
  new bingo into `celebratedRows`).
- Compose can change `Text.fontSize` per particle without a `--size` CSS
  variable equivalent — keep the size jitter inside `Particle.fontSizeSp`.

## Related code files

- `app/src/main/java/com/miti99/loto/state/PlayerBoardViewModel.kt`
- `app/src/main/java/com/miti99/loto/ui/board/Confetti.kt`

## Implementation steps

1. **PlayerBoardViewModel.kt — tier rule**

   Replace `applyCrossedChange` Pass-1 tier calculation:

   ```kotlin
   // OLD (line ~166):
   // val tier = if (celebrated.size >= 3) 2 else 1

   val newCelebrated = celebrated + i
   val hasActiveCho = grid.indices.any { otherRow ->
       otherRow !in newCelebrated &&
           GameLogic.getWaitingNumber(gridArr, crossedArr, otherRow) != null
   }
   val tier = when {
       newCelebrated.size >= 2 -> 2
       newCelebrated.size >= 1 && hasActiveCho -> 2
       else -> 1
   }
   celebrated = newCelebrated
   ```

   Note: `crossedArr` in this scope already reflects the new mark for row
   `i`, so `getWaitingNumber` returns null for the just-completed row —
   the `otherRow !in newCelebrated` filter is belt-and-braces.

2. **Confetti.kt — emoji + size jitter**

   ```kotlin
   private val CONFETTI_EMOJI = listOf("🎊", "✨", "🎉", "🥳", "🥢", "🎋", "🏮")

   private data class Particle(
       val emoji: String,
       val startXFraction: Float,
       val speed: Float,
       val rotationSpeed: Float,
       val delayNs: Long,
       val fontSizeSp: Float,   // NEW
   )
   ```

   In the `remember { List(...) }` initializer, derive
   `fontSizeSp = 1.5f + ((i * 13) % 11) / 10f` then `* 16f` for sp scale —
   final clamp to `[24f, 38f]`. Pass into `Text(fontSize = p.fontSizeSp.sp)`.

3. Compile + run:

   ```bash
   ./gradlew :app:compileDebugKotlin
   ```

## Todo

- [ ] Update tier rule in PlayerBoardViewModel.kt
- [ ] Add 🥢 🎋 🏮 to CONFETTI_EMOJI
- [ ] Add `fontSizeSp` to Particle and use it in Text
- [ ] Compile clean

## Success criteria

- 1st bingo with another row in Chờ → `tier == 2` (Confetti fires).
- 2nd bingo (regardless of Chờ) → `tier == 2`.
- 1st bingo with no Chờ row → `tier == 1` (no Confetti).
- Confetti renders 7 distinct emoji across particles, with visible size
  variance.

## Risks

- `getWaitingNumber` is a 9-col scan per row; called 9 times per bingo —
  trivial cost. No mitigation needed.
- A row could have just been completed by the same draw that triggers the
  bingo; the `otherRow !in newCelebrated` filter prevents that row from
  counting itself toward `hasActiveCho`.

## Next

Phase 02 (section Chờ ring) — independent, can run in parallel if needed.
