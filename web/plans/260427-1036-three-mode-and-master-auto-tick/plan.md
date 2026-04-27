---
slug: three-mode-and-master-auto-tick
created: 2026-04-27
status: completed
completedAt: 2026-04-27
mode: fast
blockedBy: []
blocks: []
---

# Three-mode settings + master auto-tick + master Chờ/Kinh

## What this changes (in user terms)

- Settings replaces the single "Hiện bảng quản trò" switch with a 3-way
  picker: **Người chơi** (player only — today's default), **Quản trò**
  (master only — for projector/casting host), **Cả hai** (both inline,
  same as today's `masterMode=true`).
- In **Cả hai** mode, when the master draws a number, the player board
  automatically marks that number if present — host doesn't have to
  tap twice.
- "Quản trò đọc số" now ALSO speaks **Chờ** / **Kinh** announcements
  whenever they trigger (in addition to the called number). Player
  voice toggle stays for solo-player mode.

## Decisions (locked unless flagged)

- `masterMode: boolean` → `mode: "player" | "master" | "both"`. Saved
  data with `masterMode: true` migrates to `mode: "both"`; everything
  else falls to `"player"`.
- Defaults: `mode = "player"`, `voiceEnabledMaster = true`,
  `voiceEnabledPlayer = false`, `voiceWaitingNumber = false` (unchanged).
- Master ↔ Player wiring uses a tiny shared store (`call-bus.svelte.js`)
  — one reactive `lastDrawn` slot. Avoids prop-drilling through the
  page route, keeps each component self-contained.
- Auto-tick is **only** active in `mode === "both"`. In `master` alone
  there is no player board, and in `player` alone there is no master.
- "Master speaks Chờ/Kinh" is a behavior change of the existing
  `voiceEnabledMaster` flag — no new setting. Player voice events fire
  when `voiceEnabledPlayer` OR (`voiceEnabledMaster` AND `mode === "both"`).
- No localStorage breakage: keeping all existing keys, only adding
  `mode`. Old `masterMode` is migrated then ignored.

## Phases

| # | File | Status | Effort |
|---|------|--------|--------|
| 1 | `phase-01-three-mode-settings.md` | done | 25 min |
| 2 | `phase-02-master-broadcast-and-auto-tick.md` | done | 35 min |
| 3 | `phase-03-master-cho-kinh-announcements.md` | done | 20 min |

Total: ~80 min. Each phase ships independently, tests pass after each.

## Dependencies

- None external. All work in `src/lib/` and `src/routes/+page.svelte`.
- Phase 2 depends on phase 1 (mode === "both" check).
- Phase 3 depends on phase 1 (mode === "both" check) but can ship
  independent of phase 2.

## Risks

- **Migration silently dropping master mode users**: anyone currently
  using `masterMode: true` will get `mode: "both"` after the migration
  runs once. Verified by test (phase 1).
- **Auto-tick + manual cross collision**: if a player crosses a cell,
  then master draws the same number, marking the cell again would
  toggle it OFF. Fix: auto-tick only sets `crossed=true` when not
  already true; never toggles.
- **Voice race**: if master draws and the player board immediately
  hits Chờ in `both` mode, two clips would queue. Voice module already
  cancels in-flight playback on each `play*()` call — last wins. Verify.

## Rollback

Single-commit revert per phase. No DB, no migrations beyond a one-shot
read transformation in `loadSettings()`.
