---
title: "Auto-call countdown indicator"
status: completed
created: 2026-04-30
completed: 2026-04-30
slug: auto-call-countdown
---

# Auto-call countdown indicator

Show a visible countdown (number + shrinking circular ring) while the master
panel auto-calls numbers, so the host knows exactly when the next draw fires.

## Why

`MasterPanel.svelte` currently shows only a static "Tự động: Xs/số" line while
auto-call runs. Host has no per-tick feedback — UX feels dead between draws,
especially at slower speeds (5–10s).

## Phases

| # | Phase | Status |
|---|-------|--------|
| 1 | [Build `AutoCountdown.svelte`](phase-01-build-autocountdown.md) | completed |
| 2 | [Integrate into MasterPanel + verify](phase-02-integrate-and-verify.md) | completed |

## Key Files

- Create: `src/lib/AutoCountdown.svelte`
- Modify: `src/lib/MasterPanel.svelte`

## Out of Scope

- Sound/vibration on tick — voice already speaks when number is drawn
- Configurable countdown styling — match existing token visual language
- Player-side countdown (this is master-only)
