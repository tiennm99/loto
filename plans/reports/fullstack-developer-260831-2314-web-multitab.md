# Web M5 fix: simultaneous-tab double-freeze

Plan: `plans/260831-2314-review-followups/plan.md` Phase 1.
Source: `plans/reports/code-reviewer-260831-2213-web-review.md` M5.

## Change

`web/src/lib/active-tab.svelte.js`:
- Added module-scoped `myClaimTs` (last `Date.now()` this tab claimed) and a
  `postClaim()` helper that sets it and broadcasts `{type:"claim", id, ts}`.
  Called from both `watchActiveTab()` (mount) and `claimActiveTab()`
  (reclaim) — same call sites as the old unconditional `postMessage`.
- `onmessage` now applies newest-claim-wins: ignore `peerTs < myClaimTs`;
  freeze if `peerTs > myClaimTs`; on exact tie, freeze only if peer `id` is
  lexicographically greater than `TAB_ID` (deterministic, exactly one tab
  wins). Reclaim's fresh `myClaimTs` means a stale/late peer claim can no
  longer re-freeze the reclaiming tab.
- `+layout.svelte` untouched — `watchActiveTab()`/`claimActiveTab()` keep
  their signatures, reclaim re-hydration flow unchanged.

`web/src/lib/active-tab.test.js`:
- `FakeBC.postMessage` now defers delivery via `queueMicrotask` and
  re-reads the peer list at delivery time instead of snapshotting it at
  call time. This matches real `BroadcastChannel`'s queued-task delivery
  and — critically — is what makes a true "both tabs mount in the same
  tick" test possible: with the old synchronous/snapshot delivery, a tab
  created after another tab's `postMessage` call could never receive that
  earlier message, making one side of any tie-break untestable (and, per
  spec, unfaithful to the real API). Added `deliverBcMessages()` helper
  (`await Promise.resolve()`) to flush it in tests.
- `beforeEach` now stubs `Date.now` with a strictly-increasing counter by
  default (removes flakiness/ambiguity in the pre-existing tests, which
  don't care about specific ts values) and restores mocks in `afterEach`.
- Added three tests: unequal-ts simultaneous mount (older freezes, newer
  stays active), equal-ts tie-break (both tabs receive each other's claim
  via the same-tick delivery fix above; exactly one — the lexicographically
  greater id — stays active), and reclaim-updates-myClaimTs (a stale,
  older-ts claim from a peer after reclaim doesn't re-freeze).
- Existing 4 tests updated only to `await deliverBcMessages()` after
  actions that need cross-tab delivery; assertions/intent unchanged.

## Verification (from `web/`)

- `npx vitest run` → 141/141 passed (10 files), incl. 7/7 in
  `active-tab.test.js`.
- `npx eslint .` → 0 problems.
- `npx svelte-check --tsconfig ./jsconfig.json` → 0 errors, 0 warnings
  (429 files, `checkJs: true`).

Builds skipped per instruction (unaffected — no import/signature changes).

## Unresolved questions

None — decision and algorithm were fully specified; no ambiguity hit
during implementation.

Status: DONE
Summary: Implemented newest-claim-wins with id tie-break in `active-tab.svelte.js`; extended the test file (and made its `BroadcastChannel` fake deliver messages the way the real API does) to cover unequal-ts, equal-ts tie-break, and reclaim-clears-staleness; all gates green.
Concerns/Blockers: none

## M-1 follow-up — losing claimer never told it lost (2026-08-31)

Source: `plans/reports/code-reviewer-260831-2314-precommit-review.md` M-1.

### Change

`web/src/lib/active-tab.svelte.js` `onmessage` handler:
- `peerTs` now falls back to `Infinity` when `e.data.ts` is not a number
  (legacy pre-fix peer broadcasting `{type, id}` only) — a no-ts claim
  always wins, matching the old always-freeze behavior instead of being
  silently ignored (`peerTs > myClaimTs` / `=== ` were both `false` against
  `undefined`).
- Added an `else` branch: when this tab does **not** lose (`!peerWins`) and
  is currently active, it echoes its own unchanged `{type:"claim", id:
  TAB_ID, ts: myClaimTs}` back on the channel. The peer that sent the
  losing claim receives this echo, re-evaluates, strictly loses (same
  `myClaimTs` it already lost against, or a higher `ts`/`id` under the
  `Infinity` legacy path), and freezes. A frozen tab only ever hits the
  `peerWins` branch, so it never echoes — the exchange terminates after one
  echo round by construction, no counter/dedup needed.

No public API change — `watchActiveTab()`/`claimActiveTab()` signatures
and `activeTab` shape untouched.

`web/src/lib/active-tab.test.js`:
- Updated "reclaim updates myClaimTs..." — a stale claim from B now also
  asserts `tabB.activeTab.inactive === true` after an extra
  `deliverBcMessages()` flush (previously only checked A, per M-1 finding
  #1).
- Added "a losing claimer is echoed at and freezes instead of leaving both
  tabs active" — reproduces the asymmetric-delivery race (tabA's initial
  claim is flushed with zero peers before tabB even mounts, so tabB never
  receives it, matching real `BroadcastChannel` non-replay), equal ts, id
  tie-break; asserts exactly one tab ends up active.
- Added "the echo does not loop" — spies on `FakeBC.prototype.postMessage`,
  asserts message count settles at exactly 3 (two initial claims + one
  echo) and stays there across further flushes.
- Added "a peer claim with no ts (legacy tab) always wins" — posts a raw
  `{type:"claim", id}` (no `ts`) from a second `BroadcastChannel` instance
  directly and asserts the tab freezes.

### Verification (from `web/`)

- `npx vitest run` → 144/144 passed (10 files), incl. 10/10 in
  `active-tab.test.js`.
- `npx eslint .` → 0 problems.
- `npx svelte-check --tsconfig ./jsconfig.json` → 0 errors, 0 warnings
  (429 files).

Files touched: `web/src/lib/active-tab.svelte.js`,
`web/src/lib/active-tab.test.js` only, per assignment. Not committed.

Status: DONE
Summary: Fixed M-1 by echoing a winning claim back at a losing peer (so it freezes instead of silently ignoring the loss) and treating a missing peer `ts` as `Infinity` (legacy peer always wins); added echo, no-loop, and legacy-no-ts test coverage; all gates green.
Concerns/Blockers: none
