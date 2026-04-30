---
title: "Both-mode state consistency refactor"
status: completed
created: 2026-04-30
completed: 2026-04-30
slug: both-mode-state-consistency
---

# Both-mode state consistency refactor

Fix the cross-panel inconsistencies surfaced in the 2026-04-30 audit
(`plans/reports/code-reviewer-260430-2024-both-mode-consistency.md` and
`plans/reports/brainstorm-260430-2024-both-mode-edge-cases.md`).

Targets findings F1, F2, F4, F6, F7, F8, F10. Out of scope: F9 (voice
collision) and #20 (multi-tab) — separate plans to follow.

## Why

Single-slot `call-bus` carries only the latest draw. Any state event
that happens off-bus (player regen, master "Ván mới", reload, mode
toggle, throttled tab) silently loses history. Symptom the host hit:
fresh player board doesn't replay master's existing draws.

## Product decisions (locked 2026-04-30)

1. Master "Ván mới" → **force-clear** player's crossed in both mode.
2. Player "Xoá đánh dấu" in both mode → **replay all** called numbers
   immediately after the clear.

## Approach (surgical, KISS)

Lift master's `called[]` to a shared reactive store. Player auto-cross
becomes a $effect on `masterStore.called` length growth, not a bus
slot. Existing tests keep passing; the bus dies because nothing reads
it. No full crossed-derivation rewrite — keep `crossed` as $state to
preserve manual cross/uncross UX.

## Phases

| # | Phase | Status |
|---|-------|--------|
| 1 | [Lift master state to shared store](phase-01-lift-master-state.md) | completed |
| 2 | [Player auto-cross via shared store](phase-02-player-via-store.md) | completed |
| 3 | [Replay flows + retire bus](phase-03-replay-flows.md) | completed |
| 4 | [Tests + verify](phase-04-tests-and-verify.md) | completed |

## Key Files

- Create: `src/lib/master-store.svelte.js`
- Modify: `src/lib/MasterPanel.svelte`, `src/lib/PlayerBoard.svelte`,
  `src/lib/auto-tick.js` (or replace with new helper)
- Delete (after migration): `src/lib/call-bus.svelte.js`,
  `src/lib/call-bus.test.js`, `src/lib/auto-tick.js`,
  `src/lib/auto-tick.test.js` (if signature changes too much to keep)

## Out of Scope

- F9 voice ownership (master vs player playback collision)
- #20 multi-tab guard (banner / single-master lock)
- Full derived-crossed model (would erase manualUntick UX)
- AutoCountdown — already shipped, untouched here
