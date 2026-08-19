# Code review — upstream sync (6 changes)

**Plan:** plans/260429-1511-loto-upstream-sync/plan.md
**Recon:** plans/260429-1511-loto-upstream-sync/reports/xia-recon-260429-1511.md
**Scope:** 14 files, +215/-89 LOC. Correctness of 6 ported changes.
**Verdict:** **Approve** — no critical/major issues. 4 minors + 2 nits below.

## Critical
None.

## Major
None.

## Minor

### M1. Stale comment on tier-2 confetti gate
- **File:** app/src/main/java/com/miti99/loto/ui/board/PlayerBoardScreen.kt:160
- Comment reads `// Confetti for tier-2 bingo (≥ 3 rows complete)`. New rule is
  "≥2 OR (≥1 + Chờ)". Update to: `// Confetti for tier-2 bingo (2nd+ row, OR 1st row while another is in Chờ)`.
- Risk: future reader may revert to old threshold thinking comment is the spec.

### M2. Dead 0.dp Spacer
- **File:** app/src/main/java/com/miti99/loto/ui/master/MasterControls.kt:70
- `Spacer(Modifier.width(0.dp))` is a no-op — parent `Row` already has
  `Arrangement.spacedBy(8.dp)` (line 51). Drop the Spacer + its import isn't
  actually unused (Spacer survives through dead code), so this is purely cosmetic.

### M3. CalledCellCream fill = BackgroundCream — zero contrast
- **Files:** app/src/main/java/com/miti99/loto/ui/theme/Color.kt:40 (`BackgroundCream = #FFFBEB`)
  vs. line 53 (`CalledCellCream = #FFFBEB`).
- Light-theme page background and called-cell fill are now the same hex. The
  colored ring is the only thing that separates cells from the page. Master
  grid cells (tightly packed) read fine; the 160dp `CurrentNumberHero` circle
  now visually "floats" — only the ring outlines it. Matches upstream, but
  flag for design review.
- Suggested: either nudge `CalledCellCream` to amber-100 `#FEF3C7` (you
  already have `CalledCellCreamDark` at that value but never use it), or
  leave with explicit comment that this is intentional flat design.

### M4. PlayerBoardGrid pulse runs continuously even when no section is waiting
- **File:** app/src/main/java/com/miti99/loto/ui/board/PlayerBoardGrid.kt:60-69
- `rememberInfiniteTransition` + `animateFloat` always tick. When all three
  sections have `isWaiting == false`, `pulseAlpha` is never read inside the
  Modifier branch (the `else Modifier` path doesn't touch it), so no
  recomposition is triggered — confirmed safe.
- Non-issue for jank: animation cost is negligible and doesn't drive
  recompose. Optional optimization: gate the entire transition behind
  `if (sectionHasWaiting.any { it })` to also kill the value-update job
  when nothing waits, but YAGNI for a card with 9 rows.

## Nits

### N1. `CalledCellCreamDark` is declared but never referenced
- **File:** app/src/main/java/com/miti99/loto/ui/theme/Color.kt:54
- Either wire it into a dark-theme branch on the called-cell fill (parallel
  to the Sky400/Sky600 split) or delete it. Currently dead.

### N2. `BackgroundLight` (#FFFFFF) still referenced as `onPrimary/onSecondary/onTertiary`
- **File:** app/src/main/java/com/miti99/loto/ui/theme/Theme.kt:11,13,15
- Theme.kt swapped `background` → `BackgroundCream` but `onPrimary` etc.
  still point to `BackgroundLight` (= white). Intentional — these are
  contrast-on-color tokens, not the page color. Just confirming this isn't
  an oversight; it reads correctly.

## Verification of the 7 review questions

### 1. Tier rule correctness — Pass
Traced `applyCrossedChange` (PlayerBoardViewModel.kt:163-186) against
`GameLogic.getWaitingNumber` (GameLogic.kt:70-83):

- (a) **1st bingo + Chờ on another row** → `celebrated.size == 1`,
  `hasActiveCho == true` (other row's `getWaitingNumber` is unaffected by
  marking the just-completed row's last cell) → **tier 2**. ✓
- (b) **2nd bingo regardless** → `celebrated.size >= 2` short-circuits
  before `hasActiveCho` check → **tier 2**. ✓
- (c) **1st bingo, no other Chờ** → `celebrated.size == 1`,
  `hasActiveCho == false` → **tier 1**. ✓
- **Just-completed row not counted in `hasActiveCho`:** loop excludes
  `otherRow !in celebrated` (line 173), and even without the guard,
  `getWaitingNumber` returns null when 0 cells remain (GameLogic.kt:75-82
  — `remaining` stays null). ✓
- **`crossedArr` is `newCrossed`** (line 152), so the just-marked cell
  is reflected. ✓

Pre-existing edge case (not introduced by this PR): if a single draw
completes 2 rows simultaneously (two rows have the same waiting number),
Pass-1 only celebrates one per call. The 2nd row stays uncelebrated until
the next state change. This matches upstream Svelte semantics — flagging
only because it was on my radar.

### 2. Race / leak risk on shared infiniteTransition — No issue
The transition runs continuously but `pulseAlpha` is only read inside the
`if (isWaiting)` Modifier branch, so no useless recompose. Animation jobs
are owned by composition scope and torn down with the screen — no leak.

### 3. `isSystemInDarkTheme()` inside @Composable — All correct
- MasterCell.kt:57 — inside @Composable `MasterCell`. ✓
- CalledHistory.kt:74 — inside @Composable `CalledChip` (private). ✓
- CurrentNumberHero.kt:46 — inside @Composable `CurrentNumberHero`. ✓

### 4. Light/dark contrast on amber-cream cell — Adequate, see M3
Light theme: BackgroundCream `#FFFBEB` + Sky600 `#0284C7` ring → 6.1:1
contrast — passes WCAG AA. Sky600 text on CalledCellCream same value —
6.1:1 — fine. Emerald500 `#10B981` on cream — 2.1:1 — borderline for
small text but fine for the ring color (decorative). M3 flags the
fill==background issue separately.

### 5. VoiceToggles gating — Matches recon spec
VoiceToggles.kt:35 (`mode != PLAYER`) and :53 (`mode != MASTER`) hide the
master row in PLAYER-only mode and the player rows in MASTER-only mode.
Hint copy switches BOTH vs MASTER on line 41-44. Matches plan
phase-04-voice-hint-per-mode and recon §4.

### 6. Stale imports — None found
Grepped each modified file:
- KinhModal.kt: all 33 imports used. No stale `Brush` / gradient imports.
- MasterControls.kt: all 25 imports used. No stale gradient imports.
  `BrandEmeraldLight` still used by `AutoCallChip` (line 111, 118).
- PlayerBoardGrid.kt: all imports used.
- PlayerBoardScreen.kt: all imports used.
- Master compose files: imports for `BrandSky400/600` and `isSystemInDarkTheme`
  all referenced.
- Color.kt: comment references "former pink" — fine; no `BrandPinkX` /
  `BrandIndigoX` left in any source file (verified with grep).

### 7. Behavior preservation — One tier consumer, OK
`grep -rn "bingoEvent.tier"` returns a single hit:
PlayerBoardScreen.kt:161 `state.bingoEvent?.tier == 2`. The new rule
still uses `tier ∈ {1, 2}`, so the existing "tier == 2 ⇒ confetti"
conditional fires at a *higher* rate than before (intended — see recon
§1). No type/shape break.

## Summary

The 6 ports are clean, the tier rule is correctly threaded through
`applyCrossedChange`, the Section Chờ ring uses a shared transition
without leaks, and the gating in `VoiceToggles` matches the upstream
spec. Theme cleanup is consistent (no leftover `BrandPink/Indigo`).
Recommended: **approve and ship**, optionally fold M1 (1-line comment)
and M2 (drop dead Spacer) into the same commit. M3/M4 are observations
for the design/perf docket, not blockers.

## Unresolved questions
- M3: Is `CalledCellCream == BackgroundCream` (both `#FFFBEB`)
  intentional flat design, or should the called-cell fill nudge to
  amber-100 `#FEF3C7` for contrast? Upstream parity suggests intentional;
  worth a 1-line confirmation.
- N1: Should `CalledCellCreamDark` be wired into a dark-theme branch
  for the called-cell fill (parallel to Sky400/Sky600), or deleted?
