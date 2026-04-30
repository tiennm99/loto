# Both-mode consistency review

## Summary
13 findings: 4 critical, 6 major, 3 minor.

Theme: master's `called[]` is the source of truth, but the bus only carries `lastDrawn`. Any time the player's `crossed` is rebuilt or remounted while master mid-game, prior history is lost. `resetBus()` clears the slot but never resets the consumer's `lastHandledDrawAt`, so cross-side reset semantics are subtly broken.

## Findings

### F1: Player regen mid-game loses all prior master draws (CRITICAL)
**Where:** `src/lib/PlayerBoard.svelte:193-207` (`handleGenerate`)
**Symptom:** Master has called e.g. 30 numbers. Player taps "Tạo bảng mới" to reroll the card. New grid intersects with called numbers, but no cells are pre-crossed. Player must wait for the NEXT master draw (which will only mark that one number) — the other ~29 hits are silently lost forever.
**Cause:** Two reasons compounding:
1. `handleGenerate` calls `resetBus()` (line 206) which only nulls `bus.lastDrawn`. Master's `state.called[]` is still the truth, but no replay path exists.
2. The `processAutoTick` effect (line 181) is bus-driven, never history-driven. It can't "catch up" because there's nowhere to read history from.

Worse: `resetBus()` here punishes the master too. If the master is currently auto-running and the player hits regen, the next master draw will broadcast normally, BUT every other PlayerBoard mount (if `+page.svelte` ever rendered two) loses its bus slot too. The reset reaches across the trust boundary.

**Repro:**
1. Mode = both, master "Ván mới", draw 10 numbers.
2. Player "Tạo bảng mới" → confirm.
3. Inspect: zero crossed cells on new grid even when 3-4 numbers from `called[]` exist on it.
4. Master "Xổ số" once → only the just-drawn number gets crossed (if on grid). The historical 10 are gone.

**Fix idea:**
Make master's called list the authority. Either (a) export `getCalledNumbers()` from a shared store and have player's regen replay-cross all of them via `findUncrossedCell` in a loop, or (b) when `mode === "both"` skip the `resetBus()` call AND, on regen, walk `state.called[]` from MasterPanel (lift to a shared store) to pre-cross the new grid. Also drop `resetBus()` from `handleGenerate` — regenerating a card has nothing to do with the master's broadcast slot.

---

### F2: Master "Ván mới" leaves player's crossed marks stale (CRITICAL)
**Where:** `src/lib/MasterPanel.svelte:165-172` (`handleNewGame`)
**Symptom:** Master ends a game (everyone Kinh!), starts a new one. Player's grid is still the OLD card with OLD crossed marks. The new game's first `Xổ số` triggers an auto-tick that may flip a cell on the stale board. Player sees "Chờ X" toasts and even possibly a fake "Kinh!" celebration for a row that was already complete from the prior game.
**Cause:** `handleNewGame` resets the master's own `state` and calls `resetBus()`, but never signals the player to clear `crossed`. Player's `loto_crossed` localStorage entry is untouched. The `processAutoTick` effect's `lastHandledDrawAt` is never reset (it's a `let` $state in PlayerBoard), but `resetBus()` sets `bus.lastDrawn = null` which the effect ignores via the `!lastDraw` early-return — so the dedup cursor stays at the OLD game's last `at`. Then the new game's first broadcast (`{ num, at: Date.now() }`) has a fresh `at > old at`, so it ticks against the stale grid.
**Repro:**
1. Mode = both, master draws until player completes row 1 (Kinh modal shows).
2. Master "Ván mới" → confirm.
3. Master "Xổ số" → if the new number happens to be on the stale grid, player sees an auto-cross on what is visually still the old card.

**Fix idea:** Either (a) on master "Ván mới", clear player storage (`loto_crossed` only, keep grid) and broadcast a `gameReset` signal — extend the bus to `{ lastDrawn, gameId }`; player effect resets `crossed` when gameId changes; or (b) keep cards/marks across master games and only require player to manually "Xoá đánh dấu" — but document this and at minimum reset `lastHandledDrawAt` to 0 on a `gameReset` signal so timing math doesn't drift.

---

### F3: `resetBus()` is fired by the player but only resets a single shared slot (CRITICAL)
**Where:** `src/lib/PlayerBoard.svelte:206, 219` and `src/lib/call-bus.svelte.js:20-22`
**Symptom:** Player tapping "Tạo bảng mới" or "Xoá đánh dấu" wipes `bus.lastDrawn` for the master too. If two PlayerBoards were ever mounted (or the master's own logic ever depended on the last-drawn marker — currently it has its own `lastCalled`, but that coupling is fragile), this is a cross-component side effect.
More concretely: after player calls `resetBus()`, master's NEXT `Xổ số` broadcasts a fresh `{num, at}`, so player auto-tick fires again — but the player just chose to reset, expecting silence. With the dedup cursor still at the OLD `at`, master's new broadcast (`Date.now()` > old) does fire. So `resetBus` doesn't even achieve "ignore future master draws on this fresh card", it just creates a one-broadcast lull.
**Cause:** The bus has cross-component write semantics but no scoping. `resetBus` is a sledgehammer — the player can't tell "I want my local cursor to reset" from "I want to wipe the master's broadcast slot". Combining (a) the bus state (master broadcasts) with (b) the consumer cursor (player's `lastHandledDrawAt`) is the structural error.
**Repro:** Player taps "Xoá đánh dấu" (line 219) right after master's draw. Now `bus.lastDrawn` is null even though master drew. Immediately switching mode `both → player → both` (which doesn't republish) leaves player with no marker of what was last drawn. Combined with F11, the player's $effect can re-fire on `crossed` change and silently advance state.
**Fix idea:** Drop the `resetBus()` call from BOTH player handlers. Replace with a local-only cursor reset: `lastHandledDrawAt = bus.lastDrawn?.at ?? 0` so the player consumes the current slot without acting. The master owns the bus.

---

### F4: Page reload mid-game leaves bus null but `lastHandledDrawAt = 0`; first new master draw rewrites a fully-restored crossed grid (CRITICAL)
**Where:** `src/lib/PlayerBoard.svelte:47, 86-103, 181-191` + `src/lib/call-bus.svelte.js:10`
**Symptom:** Master has drawn 20, player has 20 cells crossed (auto-ticked + persisted to `loto_crossed`). User reloads the page. Both panels rehydrate from localStorage. Bus is module-state — fresh, `lastDrawn = null`. Player's `lastHandledDrawAt = 0` (its $state init). On the master's NEXT draw (say number 21), the bus publishes `{ num: 21, at: T }`. Player's effect sees `lastDraw.at !== lastHandledAt`, advances, ticks number 21. So far OK. But because the bus IS null on mount, if the player double-clicks "Tạo bảng mới"+"Xoá đánh dấu" quickly, the regen effect runs → `crossed` becomes empty, persisted. Now master's history is gone AND there's no in-bus draw to replay. Same as F1 but post-reload, harder to recover from because the user thinks the reload preserved state.
**Cause:** Bus is in-memory only (`call-bus.svelte.js`), but `crossed` is persisted. State coupling broken across reload.
**Repro:**
1. Mode = both, draw 20, player's grid is half-crossed and persisted.
2. Reload page.
3. Player taps "Tạo bảng mới" → ALL prior auto-ticks gone with no recourse.
4. Or: if master never draws again (game ended), player has no record at all of what was called — only that some cells were once crossed.

**Fix idea:** Persist the bus alongside master state, or expose master's `called[]` as the canonical source on mount and have player effect run an initial replay-cross pass when it detects `lastHandledDrawAt === 0` AND `state.called.length > 0`. Tied to F1 — same root fix.

---

### F5: `lastHandledDrawAt` is never persisted, so crossed-state and dedup cursor diverge (MAJOR)
**Where:** `src/lib/PlayerBoard.svelte:47`
**Symptom:** Across reloads, `crossed` is restored from localStorage but `lastHandledDrawAt = 0`. If `bus.lastDrawn` happens to be non-null (it never is on cold reload, but could be after HMR or if you ever persist the bus), the player would re-tick the latest already-crossed number. With current code: harmless because bus is null on reload. With any future change to persist the bus, this becomes a re-tick bug.
**Cause:** `let lastHandledDrawAt = 0` is in-memory only, while the state it dedupes against (the bus) and the state it gates (`crossed`) both have persistence stories.
**Repro:** Force-set `bus.lastDrawn` to `{ num: 5, at: 1 }` in a dev tool right after mount (simulating a future persisted bus). Effect fires, double-flips cell containing 5 if it was the only call.
**Fix idea:** Persist `lastHandledDrawAt` as part of `loto_crossed` payload (bump shape to `{ crossed, lastHandledAt }`) or derive it from master state on mount: `lastHandledDrawAt = bus.lastDrawn?.at ?? 0` immediately after the initial-load $effect.

---

### F6: Mode toggle player→both mid-game replays the LAST draw only (MAJOR, partial bug)
**Where:** `src/lib/PlayerBoard.svelte:181-191` + `src/lib/auto-tick.js:35-40`
**Symptom:** User starts in `mode = player` (solo). Master friend joins, host flips to `mode = both` after master has drawn 10 numbers. Player's effect re-fires (mode is reactive in `processAutoTick` args), sees `lastDraw.at !== lastHandledAt` (cursor was 0 in solo), `mode === "both"`, finds an uncrossed cell holding `bus.lastDrawn.num` → ticks ONE cell (the most recent draw). The other 9 historical draws are lost.
This is the same flavour as F1, just triggered by mode toggle. The auto-tick.test.js explicitly documents the "advance lastHandledAt even when mode mismatch" invariant, calling it intentional. The test-comment justification ("solo player switching to 'both' mid-game shouldn't replay a stale draw") explicitly bakes in the partial-replay bug.
**Cause:** `processAutoTick` advances `lastHandledAt` even when `mode !== "both"`. So during solo play, every master broadcast silently consumed the cursor. Toggling to both then has no history to replay.
**Repro:**
1. Mode = player. Master mode toggled off.
2. Set `mode = "both"` first to bind player effect, then back to "player". Master draws 5 numbers (broadcasts still fire). Player effect runs each time, advances `lastHandledDrawAt` to the latest `at`, but mode mismatches so no tick.
3. Switch to "both". Effect re-runs but `lastDraw.at === lastHandledAt` → no-op. Player has zero crosses for 5 already-called numbers.

**Fix idea:** Don't advance `lastHandledAt` when mode isn't "both" — let it sit at 0 until the first "both"-mode tick. Then on the mode flip, do a one-shot replay over master's `called[]`. Requires exposing `called[]` outside MasterPanel (lift to a shared `master-state.svelte.js`).

---

### F7: `Date.now()` collision swallows the second broadcast within the same ms (MAJOR)
**Where:** `src/lib/call-bus.svelte.js:17` + `src/lib/auto-tick.js:35-37`
**Symptom:** Two master draws within the same millisecond produce `{at: T}` twice with identical `at`. The second is treated as a re-fire and silently skipped by `processAutoTick`'s `lastDraw.at === lastHandledAt` check.
Realistic? Manual button mashing on mobile likely produces ≥2-3ms gaps, but: (a) auto-call interval ≥1s so safe there, (b) the master's `handleDrawNext` is synchronous and could be invoked twice in a microtask boundary if called from a synthetic test, (c) future code (e.g. "skip a number" UX) could draw twice in one tick. Bus assumes monotonic strictly-increasing `at`; `Date.now()` doesn't.
**Cause:** `Date.now()` has 1ms resolution; consumer compares with `===` not `>=`.
**Repro:** Synthetic test: `broadcastDraw(1); broadcastDraw(2);` in same tick; mock `Date.now()` to return `1000` for both. Player's effect runs once with `lastDrawn.num = 2`, ticks 2, never sees 1.
**Fix idea:** Use a monotonic counter instead of `Date.now()`: `let seq = 0; broadcastDraw = n => { bus.lastDrawn = { num: n, seq: ++seq } }`. Update `processAutoTick` to compare `seq`. Also guarantees ordering across clock skew (browser tab throttling can move clock).

---

### F8: `called[]` is the source of truth but only the latest leaks via the bus; effect throw drops history forever (MAJOR)
**Where:** `src/lib/MasterPanel.svelte:88-90, 174-186` + `src/lib/PlayerBoard.svelte:181-191`
**Symptom:** If the player's auto-tick `$effect` ever throws (e.g. `findUncrossedCell` is fed corrupted state, immutable map throws on a frozen sub-array, future feature adds a new code path with a bug), Svelte may continue but the cell flip is lost. There's no retry. `lastHandledDrawAt` may or may not have advanced depending on where in the function the throw happened. Subsequent master draws keep pushing forward, and the "missed" number is permanently lost — the bus only carries the latest.
**Cause:** Single-slot bus + no master-side authority for replay.
**Repro:** Hard to repro deliberately, but consider: `crossed.map(...)` allocates O(81) per draw. On a memory-constrained device, an OOM could throw. Or, more realistically, a future refactor introducing async into the effect breaks ordering.
**Fix idea:** Promote `called[]` to a shared `$state` store (lift from MasterPanel into `master-state.svelte.js`). Player effect derives "what should be crossed" from `called` + grid via a pure projection, not a per-event flip. The grid+called → crossed function is idempotent and immune to throw losses.

---

### F9: voiceEnabledMaster + voiceEnabledPlayer simultaneous → waiting/Kinh cancels number announcement (MAJOR)
**Where:** `src/lib/voice.js:53-95` (single `activeClip`/`activeToken` slot) + `PlayerBoard.svelte:140, 152` + `MasterPanel.svelte:185`
**Symptom:** Mode = both, both voice flags on. Master draws, calls `playNumber(n)` → audio "bốn mươi hai" begins. Same draw triggers `processAutoTick` → `crossed` updates → second $effect re-runs → if a row is now waiting OR complete, `playWaiting` or `playBingo` is called, which immediately `cancelPlayback()`s the master's "bốn mươi hai" mid-syllable, then plays "chờ" or "kinh!".
The host hears "bốn—chờ" or "bốn—kinh!" — the number itself is cut. Players around the host don't hear what was called, only the reaction.
**Cause:** `voice.js` uses a single global activeClip slot with `cancelPlayback()` at the top of every `playX`. There's no priority queue; last writer wins. Master and player publishers race during the same draw → render → effect chain.
**Repro:**
1. Mode = both, both voice toggles on, voiceWaitingNumber off.
2. Manually set up a player grid where one row needs exactly one number.
3. Master "Xổ số" → that exact number is the next call. Listen: number cut off mid-pronunciation by "Kinh!".

**Fix idea:** Introduce a small queue: `enqueue(clip, priority)`. Number takes precedence over Chờ; Kinh is highest. Or sequence them: number → 250ms gap → chờ/kinh. Or — simplest — when both flags are on and mode is both, suppress the player-side announcement (master is the announcer; player flag becomes redundant). The current effect at PlayerBoard.svelte:117-118 already partially routes around this for solo player, but doesn't suppress the duplication when both flags are on.

---

### F10: Player "Xoá đánh dấu" loses already-called numbers permanently (MAJOR)
**Where:** `src/lib/PlayerBoard.svelte:209-220` (`handleClear`)
**Symptom:** Player accidentally taps "Xoá đánh dấu" (or genuinely wants to clear). All marks gone. Master is mid-game with 30 called numbers. The player is now at zero crosses with no replay path. Next master draw will re-cross only that one number.
**Cause:** Same root as F1: master's history is unreachable from the player.
**Repro:**
1. Mode = both, master at 30 calls, player has ~17 crosses.
2. Player "Xoá đánh dấu" → grid blank.
3. Master keeps drawing — only new draws cross. The 17 historical hits never come back unless that exact number is re-broadcast (which it won't, it's been consumed from `remaining`).

**Fix idea:** On clear, in `mode === "both"`, replay-cross master's `called[]` against the (existing) grid before marking complete. Keep the manual single-cell untick separate from a wholesale clear. Additionally, prompt the user "This will clear and re-apply called numbers" when `mode === "both"` so the action is informed.

---

### F11: Reactive effect re-fires on `crossed`/`grid` change but is correctly gated — verify under Svelte 5 fine-grained reactivity (MINOR/uncertain)
**Where:** `src/lib/PlayerBoard.svelte:181-191`
**Symptom:** The auto-tick `$effect` reads `bus.lastDrawn`, `lastHandledDrawAt`, `grid`, `crossed`, `settings.mode`. Any of these changing re-fires it. The dedup-by-`at` invariant claims to prevent re-runs from causing a re-flip — and the `auto-tick.test.js` tests verify the pure function. But the effect WRITES `crossed` (line 190) which is one of its dependencies. Svelte 5 runes do detect cycles; usually batches and short-circuits.
However: if Svelte ever schedules the rerun BEFORE the assignment to `lastHandledDrawAt` is committed (line 189 fires a $state write), there's a brief window where `lastHandledAt` is the OLD value and `lastDraw.at` is the new one — re-firing would `findUncrossedCell` on the now-already-crossed cell, return null, no harm. So this is theoretically safe BUT the safety hangs entirely on the `findUncrossedCell` fallthrough.
**Cause:** Effect both reads and writes `crossed` and `lastHandledDrawAt` in the same execution. Standard Svelte 5 should serialize this, but there's no test covering "what if the reactive system schedules a re-run between line 189 and 190".
**Repro:** Hard. Theoretical. Code currently passes tests.
**Fix idea:** Use `untrack()` for the writes, or restructure: compute the result, then in a `flushSync`-style microtask write both. Or accept current semantics and add a comment + test for "re-entrant scheduling cannot double-tick".

---

### F12: `autoCallEnabled` toggle off mid-run leaves player partially up-to-date (MINOR)
**Where:** `src/lib/MasterPanel.svelte:120-139` (master auto-call effect)
**Symptom:** Master is auto-running. Host flips `autoCallEnabled` off in settings. Master effect tears down the interval (line 122-125 sets `autoRunning = false`), so no more auto-broadcasts. Player auto-tick effect doesn't care — it just stops receiving new draws. So far OK.
But: if host then flips `autoCallEnabled` back on, `autoRunning` is now false (it was reset on disable), so the master would have to manually press "Bắt đầu" again. Meanwhile the player has been quietly accruing nothing. No bug per se, just a UX cliff.
**Cause:** `autoRunning` and `autoCallEnabled` are two pieces of state with overlapping semantics. The disable path resets `autoRunning` to false (correct), but there's no "remembered intent" to restart on re-enable.
**Repro:**
1. Mode = both, autoCallEnabled = true, autoRunning = true. Master is calling every 5s.
2. Open settings, toggle autoCallEnabled off, then on.
3. Auto-call doesn't resume; manual "Bắt đầu" required.

**Fix idea:** Either document this in the settings UI ("Toggling off stops auto-run; tap Bắt đầu to resume") or persist `autoRunning` across the toggle. Probably the former — it's user-initiated.

---

### F13: Multiple PlayerBoard mounts share `lastHandledDrawAt = 0` initial state but each instance has its own copy — tested behavior unclear (MINOR)
**Where:** `src/lib/PlayerBoard.svelte:47` + `src/routes/+page.svelte:33-35`
**Symptom:** `+page.svelte` only mounts ONE PlayerBoard. But the architecture suggests "two cards on one device" might be a future ask (verified by F-search of the repo). If two PlayerBoards mounted, both share the same `bus.lastDrawn`, `STORAGE_PREFIX = "loto"` (so SAME localStorage key for grid and crossed — they'd overwrite each other). Each has its own `lastHandledDrawAt` $state, so each correctly handles its own dedup. But the localStorage collision means one's persistence eats the other's.
**Cause:** `STORAGE_PREFIX` is hardcoded; bus is single-slot global.
**Repro:** Mount two `<PlayerBoard />` instances in `+page.svelte`. Tap "Tạo bảng mới" on instance A → instance B's localStorage is overwritten on next persist effect run. Both grids end up showing the same card.
**Fix idea:** Accept `prefix` as a prop with default `"loto"`. Caller mounts `<PlayerBoard prefix="loto-1" />` and `<PlayerBoard prefix="loto-2" />`. Bus remains global (correct — both should auto-tick on master draw). Safe even if not used now.

---

## Prioritized fix path

1. **Lift `called[]` to a shared store** (`src/lib/master-state.svelte.js`): `{ called: $state([]), remaining: $state([]) }` with `drawNext()`, `newGame()`, `reset()`. MasterPanel imports this, so does PlayerBoard.
2. **Replace bus dedup with seq counter** (F7).
3. **Player auto-tick becomes `called`-derived**: `crossed` is a $derived projection of `(grid, called)` via a pure idempotent function, with manual untick handled by an "exclude" set. Eliminates F1, F2, F4, F6, F8, F10 in one structural change.
4. **Drop `resetBus()` calls from PlayerBoard handlers** (F3) — they don't belong there.
5. **Voice queue** (F9): trivially solved by suppressing player-side announcements when `mode === "both" && voiceEnabledMaster`.

## Unresolved questions

- Is "card persists across master Ván mới" the intended UX, or should new master game force-clear player marks? (Affects F2 fix shape.)
- Should "Xoá đánh dấu" in mode=both behave as "clear AND replay-cross called" or "true clear, ignore called"? Need product call. (Affects F10.)
- Future ask: multiple PlayerBoards on one device — is that on the roadmap? (Affects whether F13 is worth fixing now.)
- Manual untick behavior: today, a re-broadcast of the same number doesn't happen (master never repeats). But if it did (testing-only `replay` button), would re-cross be desired? Current pure function says yes.
