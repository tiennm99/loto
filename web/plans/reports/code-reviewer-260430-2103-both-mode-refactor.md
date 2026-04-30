# Code review: both-mode state consistency refactor

Plan: `plans/260430-2050-both-mode-state-consistency/`
Reviewer scope: static review of `master-store.svelte.js`,
`player-auto-cross.js`, `MasterPanel.svelte`, `PlayerBoard.svelte`,
`game-logic.js` (manualUnticks helpers), the three new/extended test
files, and `+page.svelte` to confirm mode mounting.

## Severity counts

- **Critical:** 0
- **High:** 1
- **Medium:** 4
- **Low:** 4
- **Nits / informational:** 3

Net: refactor is solid. The shared store is small, deletes more code
than it adds, and the bus is fully retired (`grep call-bus|auto-tick|
broadcastDraw|resetBus` only finds a stale doc comment in
`game-logic.js` line 309). The four product flows are all wired per
locked decisions. One real cross-component bug + a few subtle traps
documented below.

---

## High

### H1 — `MasterPanel` unmount in mode=player drops live master state silently

`+page.svelte` lines 33–50 conditionally renders MasterPanel only when
`settings.mode !== "player"`. Persistence and `loadMaster()` live in
`MasterPanel`'s `$effect`, so:

- In **player** mode the master panel never mounts → `loadMaster()`
  never runs → `masterState.called` stays `[]` for the entire session.
  That's fine in solo player mode.
- BUT the moment the host toggles **player → both**, the panel mounts
  fresh, `loadMaster()` reads persisted state, and `masterState.called`
  jumps `0 → N` reactively. This trips PlayerBoard's master-reset
  detection effect *in reverse*: it sees `prevCalledLen=0, len=N`, no
  reset fires (correct), and `applyMasterCalls` sees a backlog of N
  calls with `lastHandledIndex=N` (initialized that way at PlayerBoard
  mount). Result: **the player board does NOT replay master's persisted
  history when switching to both mode** — phase 3's "open question"
  behavior, but undocumented in code.

Phase 3 doc says this is intentional ("cursor was advanced past it"),
but the load order is fragile: PlayerBoard's load $effect runs *first*
because PlayerBoard always mounts (it lives outside the
`mode !== "master"` gate? — actually it's inside `mode !== "master"`,
so it unmounts in master-only mode but mounts in both mode). When mode
flips player→both:
- PlayerBoard already mounted with `lastHandledIndex = 0` (first mount
  in player mode read `masterState.called.length === 0`).
- Then MasterPanel mounts → `loadMaster()` → `masterState.called` becomes
  the persisted N entries.
- Reactivity fires the auto-cross effect. `mode === "both"` now, grid
  exists, `lastHandledIndex (0) < called.length (N)` → it WILL replay
  the entire back-history at once.

So the documented "no replay on toggle" property is **only true if
mode toggle happens after MasterPanel was already mounted at least
once**. If the user lands the page in `mode=player` and toggles to
`both` for the first time, every persisted master draw will auto-cross
the player board in one shot — surprising, possibly desirable, but not
what phase 3's "open question" claims.

**Fix options:**
1. Accept the behavior, update phase 3's "open question" to "we do
   replay on first mount in both/master modes — known and OK".
2. Move `loadMaster()` to module scope (or `+page.svelte`), so master
   state is always primed. Then PlayerBoard's mount-time
   `lastHandledIndex = masterState.called.length` line correctly
   captures the persisted backlog as "already in sync with persisted
   crossed", honoring the docstring.

Option 2 is cleaner and matches the docstring "Treat reload as already
in sync with master's full history". Recommend this.

---

## Medium

### M1 — Effect self-trigger: `applyMasterCalls` $effect reads `crossed`, writes `crossed`

PlayerBoard.svelte:205–218.

The effect reads `grid`, `crossed`, `masterState.called`,
`lastHandledIndex`, `manualUnticks`, `settings.mode`, then conditionally
writes `lastHandledIndex` and `crossed`. Svelte 5 effects re-run on
ANY tracked-dep change, including the very ones they wrote.

- When `applyMasterCalls` returns `changed: true`, the effect writes a
  new `crossed`. That triggers a re-run.
- Re-run: `lastHandledIndex` was bumped to `called.length` on the same
  pass, so guard `lastHandledIndex >= called.length` short-circuits to
  `{changed: false, lastHandledIndex unchanged}`. No write, no further
  re-run. **Safe.**
- When `manualUnticks` changes (user untick): the effect re-runs with
  the same `lastHandledIndex === called.length` → short-circuit. **Safe.**
- When `crossed` changes via manual click: same — short-circuit. **Safe.**

Verdict: not a bug, but the safety hinges on `applyMasterCalls`'s early
return at line 33–35. Add a comment in PlayerBoard pointing at the
guard, otherwise a future "always recompute crossed from called[]" pure
refactor would silently introduce an infinite loop.

### M2 — Master-reset detection effect — first-run semantics

PlayerBoard.svelte:223–234.

```js
$effect(() => {
  const len = masterState.called.length;
  const wasReset = prevCalledLen > 0 && len === 0;
  prevCalledLen = len;
  if (wasReset && settings.mode === "both" && grid) { ... }
});
```

The effect declares a dep on `masterState.called.length` (read) and
writes `prevCalledLen` (written). It does not read `prevCalledLen` —
wait, it DOES read it (`prevCalledLen > 0`). So it reads + writes
`prevCalledLen` and reads `masterState.called.length`. Self-trigger
risk is real **but** the write happens unconditionally to the current
length, and once they're equal the read+write pair is idempotent: write
the same value → Svelte's proxy short-circuits identical assignments to
the underlying signal? In Svelte 5 runes, `$state` writes that produce
the same value DO NOT trigger reactivity (proxy short-circuit on
primitive equality). So no infinite loop.

Mount semantics: `prevCalledLen` is initialized to `0` in `$state`, then
the load effect at line 119 sets it to `masterState.called.length`.
Effect ordering between the load effect and the reset-detect effect is
NOT guaranteed by Svelte. If the reset-detect effect runs first on
mount with `prevCalledLen = 0` and persisted `called.length > 0`, then
`wasReset = (0 > 0 && N === 0)` = false. **Safe by accident** — only
the `>0 → 0` shape qualifies as reset, so first-mount transitions
`0 → N` and `N → N` are both no-ops.

Suggest hardening with a one-line comment: `// prevCalledLen=0 + len>0
is NOT a reset — only >0 → 0 qualifies, so init order vs load $effect
doesn't matter.`

### M3 — `manualUnticks` correctness: pre-call manual cross then user untick

`handleCellClick` lines 318–323:

```js
if (num > 0 && masterState.called.includes(num)) {
  ...
}
```

Walk-through: user manually crosses cell holding `42` BEFORE master
draws 42. `crossed[r][c] = true`, `manualUnticks` unchanged (because
`called.includes(42) === false`). Master then draws 42; auto-cross
runs `findUncrossedCell(grid, crossed, 42)` which returns null (already
crossed) → no-op, `lastHandledIndex` advances. So far so good.

Now user clicks the cell again (untick). `wasCrossed = true`,
`willBeCrossed = false`. `called.includes(42)` is now true →
`next.add(42)`. `manualUnticks = {42}`. Cell becomes false. ✓ Replay
flows skip 42 thereafter. Correct.

Edge case: user manually crosses 42 pre-draw, master draws, user does
NOT untick, then user clicks "Xoá đánh dấu". `manualUnticks` was empty
at handleClear time → it's reset to `new Set()`, then `applyMasterCalls`
re-crosses 42 (because `findUncrossedCell` finds it on the cleared
grid). Correct outcome — the manual cross was indistinguishable from
auto, replay redoes it.

Edge case: `includes()` is O(n) and runs on every cell click. With max
90 calls it's trivial — fine. (Could be a Set on `masterState`, but
YAGNI.)

**Verdict:** logic is correct. Documented `O(called)` cost is
acceptable.

### M4 — `lastCalled` re-derive over the whole array

MasterPanel.svelte:59–63 reads `masterState.called[masterState.called.length-1]`.
Fine. But line 77 `callOrder` rebuilds a `Map` on every change to
`masterState.called`. Since `called` is replaced (not mutated) on every
draw, this is unavoidable cost — O(n) per draw, n ≤ 90. Trivial. Just
flagging that `derived` does not memoize per-element diffs.

---

## Low

### L1 — `applyMasterCalls` builds a new outer array per call (allocation churn)

Line 49–53 does `next = next.map(...)` once per matched call. Replaying
90 back-history hits: 90 outer-array allocations of length 9. Negligible
(<10 µs total). Could pre-clone once and mutate, but that breaks the
pure-function contract documented at the top. Leave as is — KISS.

### L2 — Deep-equal short-circuit absent in `applyMasterCalls` no-flip path

Line 33–35 short-circuits on cursor-at-end. Line 39–41 short-circuits
on mode mismatch. But line 44–55 walks all calls even if every single
one is in `manualUnticks` or off-board, returning the same `next`
reference. The test `returns same crossed reference when no flip
happens` (line 122) confirms this works for off-board nums. Not a bug,
just verifying the test asserts it correctly. ✓

### L3 — `saveMaster` / persistence effect runs on mount with empty arrays

MasterPanel.svelte:69–74 — second $effect reads `called` + `remaining`
and calls `saveMaster()`. On mount BEFORE the load effect runs, both
are empty → `saveMaster()` writes `{"called":[],"remaining":[]}` to
storage, **clobbering any persisted state**. Then the load effect runs
and reads … the just-clobbered empty state. Game state lost on every
mount.

Wait — let me re-read. The two `$effect`s are sibling effects.
Per Svelte 5, on initial mount they run in declaration order: load
($effect at 65) runs first, populates `masterState`, THEN save effect
at 69 runs and persists what was just loaded. **Safe by declaration
order.**

But this depends on declaration order. Move them out of order and you
silently corrupt storage. Add a comment: `// IMPORTANT: load effect
must remain declared before save effect, or save will clobber storage
on mount.`

Edge: if `loadMaster()` is a no-op (no key in localStorage), save
effect writes `{[],[]}`. That's fine — persists "no game" cleanly.

### L4 — `prevCalledLen` exposed as `$state` but only used internally

It doesn't need to be `$state` for the current logic — a plain `let`
read+written in the same effect works. Making it `$state` adds a
reactive dep that the effect both reads and writes (see M2). Switching
to plain `let` would remove the self-trigger concern entirely. Same for
`lastHandledIndex` — it's only read inside `applyMasterCalls`, and
written in a single effect; tagging it `$state` opts it into Svelte's
reactivity graph for no UI consumption. Consider non-reactive `let` for
both. Cosmetic only.

---

## Nits / informational

### N1 — Stale doc comment in `game-logic.js`

Line 309 still says `Used by the master→player auto-tick path.` The
"auto-tick" name dies with this refactor (replaced by "auto-cross").
Cosmetic — update to `auto-cross` for greppability.

### N2 — `resetMaster` vs `startNewGame` from player POV

PlayerBoard's reset-detect effect fires on `called.length: >0 → 0`.
- `startNewGame`: sets `called = []`, then `remaining = shuffled1to90()`.
  Two writes, but Svelte batches sibling reactive writes within a tick.
  Effect re-runs once with `called.length === 0` → triggers reset.
  Correct.
- `resetMaster`: sets both to `[]`. Effect sees `called.length === 0` →
  triggers reset. Same outcome.

Player POV is identical. ✓ matches the question in the brief.

### N3 — Test coverage gaps

- No PlayerBoard.svelte component test. The four product flows
  (master Ván mới wipes, player regen replays, player Xoá đánh dấu
  replays, master draw auto-crosses) are tested at the helper level
  only. A tiny `vitest-svelte` mount + `flushSync` harness for one
  flow would catch effect-ordering regressions like H1 / L3.
- No test for `prevCalledLen` mount-edge (load order vs effect order).
- No test for "manual cross before draw → master draws → manual untick
  populates manualUnticks correctly" (M3 walkthrough).

Not blocking; flag for follow-up.

---

## Persistence-ordering audit

Multiple effects write to localStorage on the same reactive change:

- `MasterPanel`: 1 save effect on `masterState`. ✓
- `PlayerBoard`: 3 save effects — `manualUnticks`, `crossed`, plus
  `saveGrid` inline in `handleGenerate`. They all touch different keys
  (`loto_master`, `loto_manualUnticks`, `loto_crossed`, `loto_grid`).
  No key collision → no stale-write race. Svelte batches state
  mutations within a tick, so a single user action triggers each save
  effect at most once per tick.

**Verdict:** no race. Keys are disjoint, writes are last-write-wins per
key, and there's only one writer per key.

---

## Mode-toggle behavior matrix (verified)

| Initial mode | Toggle to | Master state behavior | Player crossed behavior |
|---|---|---|---|
| both | player | MasterPanel unmounts; cancelPlayback fires | PlayerBoard stays mounted; auto-cross effect re-runs with `mode=player` → advances cursor, no flips ✓ |
| both | master | PlayerBoard unmounts | masterState retained; on remount PlayerBoard reloads, sets `lastHandledIndex = called.length` (sync) ✓ |
| player | both | MasterPanel mounts, loads persisted master state → `called.length` jumps `0 → N` | PlayerBoard's auto-cross effect sees `lastHandledIndex=0 < N` → **replays full back-history** (see H1) |
| master | both | PlayerBoard mounts fresh, `lastHandledIndex = called.length` = N | No replay (in sync with persisted crossed) ✓ |

Row 3 is the H1 surprise. Row 1 is correctly handled by the cursor
advance in `applyMasterCalls` non-both branch.

---

## Recommended actions (in priority order)

1. **H1**: decide policy for player→both toggle (replay all or skip).
   If "replay all" is desired, hoist `loadMaster()` to module-init or
   `+page.svelte`. If "skip", add a guard that bumps `lastHandledIndex
   = masterState.called.length` whenever `settings.mode` transitions
   into `both`.
2. **M1, M2 comments**: short pointer comments locking in the
   self-trigger safety conditions so a future refactor doesn't break
   them silently.
3. **L3 comment**: declaration-order dependency in MasterPanel.
4. **N1**: rename "auto-tick" stale comment.
5. **N3**: one component test for the master Ván mới reset flow would
   add real value; leave the rest if quota is tight.

---

## Positive observations

- Pure helper extraction (`applyMasterCalls`) is clean — easy to test,
  easy to reason about, no Svelte coupling.
- `findUncrossedCell` reused (DRY) for both auto-cross and the regen
  replay path.
- All persistence helpers validate input (`isValidNumberArray`,
  `isUnticksArray`, size cap, `__proto__` reviver). Defense-in-depth
  pattern is consistent across the new `loto_master` key and the
  pre-existing `loto_grid`/`loto_crossed` paths.
- Bus is fully retired. Only stale reference is a doc comment.
- The locked product decisions (master Ván mới wipes; player Xoá đánh
  dấu replays) are implemented exactly as specified.
- `manualUnticks` solves the "regen wipes my manual untick" problem
  cleanly — minimal state, mirrors a single user intent.

---

## Unresolved questions

1. H1: is the player→both first-toggle replay surprising or expected?
   Phase 3 doc claims "no replay on toggle" but actual behavior depends
   on whether MasterPanel mounted earlier.
2. Should `prevCalledLen` and `lastHandledIndex` drop the `$state`
   wrapper since they're not consumed by templates? (perf nit, not
   correctness)
3. Multi-tab scenario explicitly out of scope (#20) — confirmed in
   plan.md, no action.
