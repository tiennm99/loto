---
name: Implement TODO backlog
status: in-progress
created: 2026-04-28
priority: medium
blockedBy: []
blocks: []
---

# Implement TODO backlog

Source: `plans/todo.md` (commit `00fbc97`). Brutal cuts applied per
YAGNI — parking-lot features and upstream-blocked items skipped.
9 implementation phases + 1 verification checklist.

## Phases

| # | Phase | File |
|---|-------|------|
| 1 | Auto-tick integration test ✅ | `phase-01-auto-tick-test.md` |
| 2 | CI inline-script guard ✅ | `phase-02-ci-inline-script-guard.md` |
| 3 | Mode picker glyph redesign ✅ | `phase-03-mode-picker-glyphs.md` |
| 4 | Settings modal sticky on small screens ✅ | `phase-04-settings-modal-sticky.md` |
| 5 | Per-row "Chờ" indicator ✅ | `phase-05-cho-row-indicator.md` |
| 6 | Confetti polish (threshold + variety) ✅ | `phase-06-confetti-polish.md` |
| 7 | Strict CSP via hashed inline script | `phase-07-strict-csp-hashed.md` |
| 8 | Audio cache LRU rule | `phase-08-audio-cache-lru.md` |
| 9 | PWA install verification checklist | `phase-09-pwa-verify-install.md` |

## Cuts (YAGNI)

- GhostBoardPreview extraction — rule-of-three not met
- Drop `cookie`/`serialize-javascript` overrides — blocked on upstream
- Maskable icon 70% safe-zone — manual DevTools, no code
- Voice precache for >2 voices — explicit YAGNI in TODO
- Parking-lot features — multi-card, undo, spacebar, i18n, SW toast

## Dependencies

- Phase 1 first — test safety net for everything else
- Phase 2 before Phase 7 — CI guard catches CSP regressions
- Phase 9 last — runs after everything ships

## References

- `plans/todo.md` (source list)
- `plans/archive/260427-1930-ui-polish-and-pwa-v2/` (prior PWA + CSP work)
- `plans/reports/security-260427-2047-pass2-full.md` (CSP findings)
- `plans/reports/code-reviewer-260427-2047-pass2-full.md` (test gaps)
