# Phase 06 — Tests + verification

## Context

Land tests for phases 01-04 (and 05 if adopted). Port the upstream test
fixture deltas where applicable. Verify build + smoke test on a device.

## Priority

P0 — gate before commit.

## Status

pending.

## Related code files

- `app/src/test/java/com/miti99/loto/state/PlayerBoardViewModelTest.kt`
- `app/src/test/java/com/miti99/loto/state/AutoTickTest.kt` (only if
  phase-05 adopted)

## Implementation steps

1. **PlayerBoardViewModelTest — confetti tier rule (phase 01)**

   Add cases:

   - 1st bingo, no other waiting row → `bingoEvent.tier == 1`
   - 1st bingo, another row in Chờ → `bingoEvent.tier == 2`
   - 2nd bingo, regardless → `bingoEvent.tier == 2`
   - Update existing test that asserted "tier=2 needs ≥3 bingos" — that
     contract is gone. Replace with the 2nd-bingo case.

   Sketch:

   ```kotlin
   @Test
   fun `tier-2 confetti fires on 1st bingo when another row is in Cho`() = runTest {
       // craft a grid where rowA has 1 number remaining and rowB has 1
       // number remaining; cross rowA's last cell → first bingo
       // expect: state.bingoEvent.tier == 2
   }
   ```

2. **PlayerBoardViewModelTest — section Chờ ring (phase 02)**

   Add cases:

   - No waiting row → `sectionHasWaiting == [false, false, false]`
   - Row 4 has exactly 1 uncrossed → `sectionHasWaiting[1] == true`
     (section 1 covers rows 3..5)
   - Bingo on row 4 → `sectionHasWaiting[1]` flips back to false (because
     row is now complete, not waiting)

3. **VoiceTogglesTest** (compose UI test, phase 04)

   Optional but cheap. Skip if test infra not yet set up — manual smoke
   in §5 covers it.

4. **Phase 05 (if adopted)**

   Port the 8 cases from `auto-tick.test.js`:
   - NEW event flips matching cell
   - Dedup: same event id no-op
   - Mode=master: advances id, no flip
   - Mode=player: advances id, no flip
   - Off-board number: advances id, no flip
   - Null grid: advances id (event consumed), no flip
   - Null event: no-op
   - Re-cross after manual untick: a fresh event-id flips the cell again

5. **Manual smoke (cannot run from here, requires device)**

   ```bash
   ./gradlew :app:lint :app:test :app:assembleDebug
   ./gradlew :app:installDebug
   ```

   Golden path:
   - Generate grid → mark 4 of 5 cells in row 0 → section 0 label glows amber
   - Mark the 5th → bingo modal + confetti (tier 1 since no other row in Chờ)
   - Mark 4/5 of row 4 → section 1 glows
   - Bingo row 1 → tier-2 confetti (1st bingo + Chờ on row 4)
   - Settings → switch mode to "Cả hai" → master toggle hint reads "+
     báo Chờ/Kinh"; switch to "Quản trò" → hint drops the "+ báo Chờ/Kinh"
   - Master panel "Ván mới" button is solid amber, not gradient
   - Master ≤49 number tokens render sky-blue (light) / sky-400 (dark)
   - Light mode background reads as cream, not pure white

## Todo

- [ ] PlayerBoardViewModelTest: 3 new tier cases, update old assertions
- [ ] PlayerBoardViewModelTest: 3 sectionHasWaiting cases
- [ ] (Phase-05) AutoTickTest: 8 cases
- [ ] `./gradlew :app:test` clean
- [ ] `./gradlew :app:assembleDebug` clean
- [ ] Manual smoke per checklist
- [ ] Update `plans/todo.md` once verified

## Success criteria

- `./gradlew :app:test` exits 0
- `./gradlew :app:lint :app:assembleDebug` exits 0
- All bullets in §5 manual smoke pass

## Risks

- Some tier-rule tests existed under the old `>=3` contract; they will
  fail when the rule flips. This is intended — replace, don't preserve.

## Next

Commit per phase, push branch, open PR (or land directly if working on
main per project convention).
