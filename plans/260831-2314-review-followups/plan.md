# Review follow-ups: multi-tab contract, backup rules, settings-loaded race

Status: done (2026-08-31) | Branch: main | Mode: auto (user delegated decisions)

Completion: Phases 1-3 done. Pre-commit review surfaced H-1/M-1..M-4
regressions in the fixes themselves; all fixed same cycle (reports
`fullstack-developer-260831-2314-web-multitab.md`,
`fullstack-developer-260831-2334-android-regression-fixes.md`,
`code-reviewer-260831-2314-precommit-review.md`). Final: web 144/144
tests, eslint/svelte-check clean, both builds + verify-pwa pass;
android 129/129 tests, lint clean. Deferred (unchanged): M6
recomposition perf (needs profiled baseline); reviewer lows —
web loadMaster deck validation parity, unlockAudio player-gesture,
loading-flag timeouts, verify-pwa chunk heuristic, empty onCleared,
Robolectric-less VoicePlayerHolder glue test.
Source evidence: `plans/reports/code-reviewer-260831-2213-web-review.md` (M5),
`plans/reports/code-reviewer-260831-2213-android-review.md` (M4 residual, L9),
`plans/reports/fullstack-developer-260831-2226-android-fixes.md` (M4 gap detail).

## Outcome

Close the last three review items left pending as "product decisions", using
the decisions delegated to the assistant:

1. **Web M5** — two tabs opened simultaneously freeze each other. Decision:
   newest claim wins, deterministic tie-break.
2. **Android L9** — decision: exclude in-progress round state
   (`loto_game_state` DataStore) from Android backup/device-transfer; keep
   settings backed up. Rationale: a fairground round is device-local and
   transient; restoring it on another device mid-round is confusing, settings
   are worth keeping.
3. **Android M4 residual** — master restore landing before settings resolves
   replays history under default PLAYER mode. Decision: thread a
   settings-loaded signal and gate master replay on it.

## Non-goals

- Master-panel recomposition perf (M6): deferred until a profiled baseline on
  the API 24 QA device exists.
- Any redesign of the multi-tab UX beyond fixing the double-freeze.

## Phases

### Phase 1 — web: claim contract (owner: web agent)

Files: `web/src/lib/active-tab.svelte.js`, `web/src/lib/active-tab.test.js`,
`web/src/routes/+layout.svelte` (only if wiring changes).

- Add a claim timestamp: record `myClaimTs` when this tab claims (mount and
  `claimActiveTab()`). Include `ts` + `id` in the broadcast.
- On peer claim: ignore if `ts < myClaimTs` (this tab is newer); freeze if
  newer; tie-break equal `ts` deterministically on `id` so exactly one tab
  wins.
- Keep the H2 reclaim re-hydration flow intact.

Acceptance: simulated simultaneous mount (equal + unequal ts) leaves exactly
one tab active; existing reclaim/handover tests still pass; 138+ vitest suite
green, eslint + svelte-check (checkJs on) clean.

### Phase 2 — android: backup rules + settings-loaded signal (owner: android agent)

Files: `android/app/src/main/AndroidManifest.xml`, new
`android/app/src/main/res/xml/data_extraction_rules.xml` (+
`backup_rules.xml` for API < 31), `settings/SettingsRepository.kt`,
`LotoApplication.kt`, `state/PlayerBoardViewModel.kt` (+ MasterStore only if
needed), tests under `app/src/test`.

- Backup: `android:dataExtractionRules` + `android:fullBackupContent`
  excluding the game-state DataStore file(s) (verify actual on-disk name);
  settings DataStore stays included.
- Settings-loaded signal: expose loaded-state from `SettingsRepository`
  (e.g. nullable initial value or `loaded: StateFlow<Boolean>`); ViewModel
  must not consume master emissions (must not advance the auto-cross cursor)
  until settings have resolved — closes the reverse interleaving documented
  in the prior fix report §M4.

Acceptance: new SlowDataStore-based test proving master-restore-before-
settings no longer mis-consumes history under default mode (fails without
fix); backup XML referenced from manifest and lint-clean; full
`:app:testDebugUnitTest` + `:app:lintDebug` green.

### Phase 3 — verify + review (owner: controller)

- `tester` agent: full web + android suites, builds.
- `code-reviewer` agent: diff review vs acceptance criteria, contract/blast-
  radius check.
- Finalize: plan sync-back, docs impact check, offer commit.

## Risk / rollback

All changes additive and localized; rollback = git checkout of touched files.
No schema/data migrations. Backup-rule change affects only future device
backups.
