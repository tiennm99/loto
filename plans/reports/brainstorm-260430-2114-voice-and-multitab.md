# Brainstorm: Voice ownership in both mode + Multi-tab guard

**Date:** 2026-04-30
**Repo:** tiennm99/loto (SvelteKit, client-only, localStorage)
**Scale:** ~8 source files, single page, PWA installable
**Principles:** YAGNI / KISS / DRY

---

## Topic 1 — Voice ownership in both mode (F9)

### Problem recap

In both mode, master `playNumber(N)` and player `playWaiting(M)` share `cancelPlayback()`. Whichever fires last cancels the other mid-syllable. Worst case: master speaks "bốn mươi hai" and 200ms later "Chờ 42" stomps it. User wants `voiceWaitingNumber` suppressed in both mode regardless of setting.

### Options table

| # | Option | LoC | UX change | Failure modes | Test surface | Solves F9? |
|---|--------|-----|-----------|---------------|--------------|------------|
| a | Pure default + explicit gate: in both mode treat `voiceWaitingNumber` as false | ~3 lines (one ternary in PlayerBoard or in `playWaiting`) | Chờ becomes bare word in both mode; settings UI may still show toggle (confusing) | Settings UI lies if toggle stays user-editable in both mode → must hide/disable toggle too (~10 LoC SettingsButton) | 1 unit test (playWaiting in both mode skips number clip) | Yes, partially — collision still possible for "Chờ" alone vs "42" |
| b | Voice-owner enum `voiceOwner: "off"\|"master"\|"player"` | ~50–80 LoC (settings-store rewrite, migration, SettingsButton UI rework, all consumers) | Cleaner mental model; player loses ability to opt out of master voice without going silent everywhere | Migration churn for users with existing settings; product semantics for both mode unclear (master says all, player says all, or some hybrid?) | Settings migration tests + voice consumer tests + UI tests | Indirectly — only if "master" owner suppresses player Chờ entirely (different product) |
| c | Audio queue / serializer `voice-queue.js` | ~40–60 LoC new module + cancel semantics rework + integration in 3 sites | All clips play sequentially; total latency grows (Chờ delayed up to ~1.5s after number) | Queue grows unbounded if calls fire faster than playback; "Ván mới" needs flush; cancel semantics get murky | New module + queue tests + integration tests | Yes, but at cost of timing |
| d | Event-priority guard: master `playNumber` is "high", suppress Chờ/Kinh while master clip in-flight | ~10 LoC in `voice.js` (track activeKind, drop low-priority calls when high active) | Chờ/Kinh dropped silently if number announcement still playing | "Dropped" Chờ never gets a second chance — player misses the audio cue entirely | 2–3 unit tests in voice.test.js | Yes, fully — number wins by design |
| e | Coalescer: drop Chờ N if N was just announced within 1s | ~15 LoC + timestamp tracking | Smartest UX but invisible heuristic | Hard to reason about; window tuning is fiddly; doesn't help when waitingNumber ≠ lastCalled | New tests for window edge cases | Partially — only when called == waitingNumber |
| f | Hybrid (a) + (d) | ~15 LoC total | Bare "Chờ" word + master always wins audio | Combined behaviour but two rules to reason about | Tests for both rules | Yes, fully |

### Recommendation: **Option (a) — pure gate + hide toggle**

**Why (a) over (d)/(f):**
- F9 is about a *specific* product confusion ("Chờ + number" sounding like a second call), not a general collision problem. The collision between bare "Chờ" and a previous number announcement is acceptable (Chờ is the final state cue and it's only 300ms).
- (d) priority guard *silently drops* Chờ — bad for the player who never hears it. The current "cancel last wins" is actually OK if Chờ is short.
- (b) is over-engineering for a one-page app — YAGNI.
- (c) queue introduces 1+ second latency to Chờ which defeats its purpose as a real-time hint.
- (a) is 3 lines + UI hide. Documents the rationale. Reversible.

**Sketch:**

```js
// src/lib/voice.js — playWaiting:
const speakNumber = settings.voiceWaitingNumber && settings.mode !== "both";
//                                                  ^^^^ added guard

// src/lib/SettingsButton.svelte — hide the "Chờ + số" toggle when mode === "both"
{#if settings.mode !== "both"}
  <label>... voiceWaitingNumber checkbox ...</label>
{/if}
```

**One unresolved nit:** if user enables `voiceWaitingNumber` in solo player mode, then switches to both, the setting persists silently. Acceptable — switching back restores it. Document in code comment.

---

## Topic 2 — Multi-tab guard (#20)

### Problem recap

Two tabs in mode "both" both run auto-call intervals, both write `loto_master`, both speak audio. User wants new tab to silence old tab. Need to clarify: only one **master** tab, or only one tab period?

### Decision: scope of "lock"

The actual damage vectors are:
1. **Auto-call interval double-fire** (mode=both, autoCallEnabled=on) — corrupts state + double audio
2. **Two voice playbacks** — same audio twice
3. **Two writes to `loto_master`** — last-write-wins is benign for `called[]` if both observe the same draws, but if both draw independently → divergence

Pure player mode doesn't write to `loto_master` and doesn't draw — having two viewer tabs is harmless. So the lock should be on **master/both** tabs, not all tabs.

But the user spec says "old tab should do nothing, stop all actions" — simplest interpretation is **only one tab period, regardless of mode**. KISS reading.

### Options table

| # | Option | Browser support | LoC | UX | Failure modes | Solves spec? |
|---|--------|-----------------|-----|-----|----------------|--------------|
| a | BroadcastChannel API | Modern (Safari 15.4+, all Chrome/FF) — safe for Cloudflare Pages target | ~30 LoC: 1 channel, claim/relinquish msgs, banner | Cleanest; near-instant cross-tab signal | iOS Safari <15.4 (~3% global, mostly old iPads) silently no-op; tab crash leaves no relinquish msg (but new tab claim wins anyway) | Yes |
| b | `storage` event listener | Universal (IE9+) | ~40 LoC: write `loto_active_tab` token on focus, listen for changes | Works everywhere; ~5–50ms latency | Same-tab `storage` events don't fire (must update local state manually); two tabs at exact same ms → both write, last-write-wins | Yes |
| c | Web Locks API (`navigator.locks`) | ~95% (Safari 15.4+) | ~25 LoC: request lock with `ifAvailable`, hold for tab lifetime | Native single-writer guarantee | iOS <15.4 no-op; lock auto-releases on tab close (good); doesn't notify old tab proactively (must combine with BC) | Partial — guard but no UX feedback |
| d | Tab id + timestamp watchdog on every write | Universal | ~50 LoC: tab id in every state mutation + check on read | Works everywhere | Adds overhead to every write; very chatty; complexity creeps; race window during simultaneous writes | Yes but ugly |
| e | Doc/UI warning only (`document.hasFocus()` heuristic) | Universal | ~5 LoC | No protection, just a sign | Doesn't actually fix anything — auto-call still double-fires | No |
| f | Hard takeover with confirm dialog on new tab | Modern (any of above) | ~40 LoC | Friendly, reversible | Confirm dialog on every new tab is annoying for the rare honest case | Yes |
| g | Disable specific actions in non-active tab (auto-call + draws) | Modern | ~30 LoC + per-action gates | Allows viewing in non-active tab | Two surfaces to gate (master draws, voice playback) → spreading concern across files | Partial |

### Recommendation: **Option (a) BroadcastChannel + frozen-banner**

**Why (a) over (b)/(c)/(g):**
- BroadcastChannel is purpose-built for this; cleanest API. Chromium/Firefox/Safari 15.4+ all support it.
- iOS Safari ≥15.4 covers nearly all PWA users (PWA on iOS requires ≥16.4 anyway for proper installability). Pre-15.4 fallback: ignore the guard — those users are <3% and the existing race is rare.
- (b) `storage` event also works but lacks the same-origin "active tab" semantics — you'd reinvent BroadcastChannel on top of it.
- (c) Web Locks gives you the lock but doesn't give you the *banner UX* — you still need a side channel.
- (g) per-action gating spreads logic across MasterPanel + voice + state writes — violates KISS.
- The user spec ("old tab should do nothing, stop all actions") aligns with the simple frozen-banner — no per-feature gating.

**Reversibility:** when new tab closes, send a "released" broadcast → old tab unfreezes. Or simpler: old tab also re-claims on `visibilitychange` → focus → if no contender responds in 200ms, take back over.

**Sketch (~30 lines, single new module `src/lib/tab-lock.js`):**

```js
// src/lib/tab-lock.js
const CHANNEL = "loto_tab_lock";
const TAB_ID = crypto.randomUUID();

/** @param {() => void} onFrozen */
export function startTabLock(onFrozen) {
  if (typeof BroadcastChannel === "undefined") return () => {};
  const bc = new BroadcastChannel(CHANNEL);
  // Announce ourselves; any other tab will hear and freeze itself.
  bc.postMessage({ type: "claim", id: TAB_ID });
  bc.onmessage = (e) => {
    if (e.data?.type === "claim" && e.data.id !== TAB_ID) onFrozen();
  };
  return () => bc.close();
}
```

Mount in `+layout.svelte`:
```svelte
let frozen = $state(false);
$effect(() => startTabLock(() => { frozen = true; }));
```

Render frozen banner when `frozen === true`, replacing the page or overlaying full-screen with "Loto đang mở ở tab khác. Nhấn để kích hoạt lại tab này." → on click, postMessage claim again to take over.

**Side-effect kill:** in the frozen state, no need to clean up auto-call interval — the user can also just close the tab. But if we want a clean stop, add an effect: `$effect(() => { if (frozen) autoRunning = false; })` in MasterPanel (1 line). Voice naturally stops because `cancelPlayback` is called on PlayerBoard unmount, and freezing replaces the layout.

### What this does NOT solve

- **PWA installed on phone:** typically only one window, so no multi-tab scenario at all → guard is silent no-op. Fine.
- **Two devices on same Wi-Fi:** different localStorage origins per device — out of scope (no shared state to corrupt).
- **iOS <15.4 users:** no BroadcastChannel → no guard. Acceptable (rare, and the existing race is rare too).

---

## Combined unresolved questions

1. **Topic 1, settings UI:** when `mode === "both"`, should the "Chờ + số" toggle in SettingsButton be hidden, disabled-with-tooltip, or left alone? Hiding is cleanest but can confuse users who change settings, then change mode and wonder where the toggle went. Disabled-with-tooltip preserves discoverability but adds 5 LoC of tooltip plumbing. Recommend: **hidden** (KISS). Confirm with user.
2. **Topic 1, scope of suppression:** should the `mode === "both"` guard live in `voice.js` (closer to the cancellation root cause) or in `PlayerBoard.svelte` (closer to the behaviour decision)? Recommend: `voice.js` so it can't be bypassed by future call sites.
3. **Topic 2, lock granularity:** confirm whether the lock should fire in **all modes** (player included), or only when the active tab has master capabilities (mode `master` or `both`). User's spec is ambiguous. Recommend: **all modes** (KISS, matches user words "stop all actions").
4. **Topic 2, banner copy:** Vietnamese wording for the freeze banner. Suggest: "Loto đã mở ở tab khác. Tap để chuyển về tab này." — confirm tone (sếp-em vibe?).
5. **Topic 2, takeover behaviour:** if user clicks the banner in old tab to reclaim, should the new tab freeze (handover) or both stay live (race continues)? Recommend: handover via the same `claim` message. New tab's mount listener catches the new claim and freezes itself. Symmetry holds.
6. **Topic 2, fallback for old Safari:** silent no-op (current behaviour preserved) vs. visible warning ("Trình duyệt cũ — có thể xung đột giữa các tab")? Recommend: silent. Edge case noise not worth it.

