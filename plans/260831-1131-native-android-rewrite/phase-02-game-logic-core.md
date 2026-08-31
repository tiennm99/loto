---
phase: 2
title: "Game Logic Core"
status: done
priority: P1
effort: "1d"
dependencies: [1]
---

# Phase 2: Game Logic Core

## Overview

Port the pure game rules from `web/src/lib/game-logic.js` and the draw
deck from `master-store.svelte.js` into dependency-free Kotlin, with the
web test suites ported alongside. No Android APIs in this phase — plain
Kotlin, fully unit-testable on the JVM.

## Requirements

- Functional: byte-for-byte behavioral parity with the web logic —
  read `web/src/lib/game-logic.js` and `game-logic.test.js` line by line
  before writing Kotlin; every web test case has a Kotlin twin.
- Non-functional: no `android.*` imports; deterministic under injected
  `kotlin.random.Random` for tests.

## Architecture

Package `com.miti99.loto.game`:

- `CardGenerator.kt` — the 9×9 player card: exactly 5 numbers per row
  AND per column, column `c` holds numbers from its decade
  (ones-digit-aligned ranges as the web does it), plus the web's soft
  constraint (rejection-sampling against 3+ consecutive fully-filled
  columns — confirm the exact rule from `generateGrid()` before coding).
- `PlayerCard.kt` — immutable grid model + crossed/manual-untick state,
  `findUncrossedCell`, `isRowComplete`, `getWaitingNumber` (chờ = row
  has 4 of 5 crossed; kinh = row complete — row-based, not full-card).
- `DrawDeck.kt` — 1..90 shuffle, `drawNext()`, called list, remaining
  count, reset; forward-only semantics matching `drawNext()` in
  `master-store.svelte.js`.
- All models are plain data classes; serialization concerns live in
  Phase 4.

## Related Code Files

- Create: `app/src/main/java/com/miti99/loto/game/CardGenerator.kt`,
  `.../game/PlayerCard.kt`, `.../game/DrawDeck.kt`
- Create tests: `app/src/test/java/com/miti99/loto/game/CardGeneratorTest.kt`,
  `PlayerCardTest.kt`, `DrawDeckTest.kt`
- Spec: `web/src/lib/game-logic.js`, `game-logic.test.js`,
  `master-store.svelte.js`, `master-store.test.js`

## Implementation Steps

1. Read the four spec files; extract every invariant the web tests
   assert into a checklist inside the test files (as test names, not
   comments).
2. Implement `CardGenerator` with injectable `Random`; property-style
   test: 1000 generated cards all satisfy row/column/decade invariants.
3. Implement `PlayerCard` state transitions (cross, un-cross with
   manual-untick tracking, auto-cross candidate lookup).
4. Implement `DrawDeck`; test shuffle coverage, exhaustion (remaining 0),
   reset, monotonic call order.
5. Port remaining web test cases one-to-one.

## Success Criteria

- [ ] `./gradlew :app:test` green
- [ ] Every test case in `game-logic.test.js` + `master-store.test.js` has a named Kotlin counterpart (list the mapping in the PR description)
- [ ] 1000-card property test passes the row/col/decade invariants

## Risk Assessment

- **Silent spec divergence** (e.g. misreading the column-run soft
  constraint). Signal: a web test case with no Kotlin twin, or a twin
  asserting different values. Response: the one-to-one mapping list in
  the PR is the gate — reviewer rejects if a case is missing.
