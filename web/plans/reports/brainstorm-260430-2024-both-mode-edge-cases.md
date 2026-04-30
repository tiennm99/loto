# Brainstorm — "Both Mode" Edge Cases & Inconsistencies

Date: 2026-04-30
Scope: Hypotheses only. No code verification. Cold-eyes triage list for the team.
Repo: /config/workspace/tiennm99/loto

Mode model recap:
- `mode = "player" | "master" | "both"` — in `both`, MasterPanel + PlayerBoard mount on same page.
- Master draw → `broadcastDraw(num)` writes `{num, at: Date.now()}` to in-memory bus.
- Player `$effect` watches `bus.lastDrawn`; `processAutoTick` dedupes by `at`, advances `lastHandledAt` on every new `at`.
- Master state in `localStorage["loto_master"]`; player state in prefixed keys (`loto_grid`, `loto_crossed`); bus NOT persisted.
- Auto-call interval, voice (master-call + player "Chờ N"/"Kinh"), AutoCountdown component all overlay this.

Legend: critical = data loss / can't recover game state | major = wrong gameplay outcome / user confusion | minor = cosmetic / rare | unknown = needs spike.

---

## 1. Bus history loss on player board regeneration (CONFIRMED)
- Risk: **major**
- Why: Player presses "Tạo bảng mới" mid-game → fresh grid won't auto-cross numbers already called pre-regenerate, because bus only carries `lastDrawn`. Player must manually cross or wait for next draw.
- Verify: `PlayerBoard.svelte` regenerate handler + `auto-tick.js` (no replay of `master.called`).

## 2. Bus is in-memory only → reload silently desyncs both panels
- Risk: **critical**
- Why: After page reload in `both` mode, `loto_master.called` rehydrates but bus.lastDrawn is null. If player grid had uncrossed numbers from before reload, no auto-replay happens — player relies on master's *next* draw to ever fire `$effect`. Player display "last called" pill / "Chờ N" toast goes blank vs master's visible called list.
- Verify: `call-bus.svelte.js` initial state + `+page.svelte` mount order + `PlayerBoard.svelte` `$effect`.

## 3. `lastHandledAt` not persisted → after reload first new draw may double-process or be skipped
- Risk: **major**
- Why: If `lastHandledAt` lives in component state only, post-reload it resets to 0/null. On next master draw the `$effect` will process it, but if `lastDrawn` already exists from a *previous* in-memory state mid-session, behavior is undefined. Worse: auto-tick may treat a stale `at` as fresh.
- Verify: `auto-tick.js` dedup logic + where `lastHandledAt` is held (component vs store).

## 4. Two broadcasts in same millisecond → one dropped
- Risk: **minor** (low likelihood) but **major** when it bites
- Why: `at: Date.now()` has 1ms resolution. Auto-call at high speed or clock with low-res timers (some Windows VMs) → identical `at` → dedup-by-`at` discards second broadcast. Number called by master never reaches player auto-cross.
- Verify: `call-bus.svelte.js` broadcastDraw, `auto-tick.js` dedup; consider monotonic counter instead of `at`.

## 5. System clock backwards jump → `at` stops advancing
- Risk: **minor**
- Why: NTP correction or user changing clock can make new `at` ≤ old `at`. Dedup-by-`at` would treat new draw as old → skipped silently.
- Verify: `call-bus.svelte.js`; recommend `performance.now()` or sequence number.

## 6. Rapid mode toggle player→both→master→both during a draw
- Risk: **major**
- Why: Player `$effect` may re-mount/unmount mid-tick. If `broadcastDraw` fires while PlayerBoard is unmounted (mode=master), player misses it; on toggling back to both, bus.lastDrawn is the missed number but `at` may already be ≤ player's `lastHandledAt` if it was persisted, or it gets re-processed if not.
- Verify: `+page.svelte` conditional rendering + lifecycle of PlayerBoard `$effect`.

## 7. Mode toggle wipes wrong state
- Risk: **major**
- Why: Switching to player mode and back may clear master's auto-call interval but not its `loto_master`, OR may unmount MasterPanel mid-auto-call leaving an orphan `setInterval` that keeps broadcasting. Either way, "both"-mode timing breaks.
- Verify: `MasterPanel.svelte` onMount/onDestroy, AutoCountdown lifecycle, `settings-store.svelte.js` mode transitions.

## 8. Auto-call interval not cleared on mode change / route change
- Risk: **major**
- Why: If MasterPanel registers `setInterval` but only clears in onDestroy, switching to a route that doesn't unmount it (SvelteKit nav can keep layout) leaves it firing. Numbers keep getting called against a hidden master.
- Verify: `MasterPanel.svelte` interval cleanup, `+layout.svelte`.

## 9. Auto-call speed change mid-run
- Risk: **minor**
- Why: Changing `autoCallSpeed` while interval running typically requires clear+set; if implemented as reactive `$effect` watching speed, may double-register, leaving old interval ticking + new one. Player gets bursts.
- Verify: `MasterPanel.svelte` (or wherever interval is owned) + `settings-store.svelte.js`.

## 10. AutoCountdown drift vs actual interval
- Risk: **minor**
- Why: Countdown likely uses `setInterval(1000)` or rAF; setInterval throttled in background tabs to ≥1s, rAF paused. Master's draw interval may also be throttled. Visible countdown can desync from actual draw firing — user sees "0s" but draw fires 5s later.
- Verify: `AutoCountdown.svelte`, visibility handling.

## 11. Tab backgrounded → setInterval throttling
- Risk: **major**
- Why: Backgrounded host tab → master's auto-call throttled to 1Hz min (sometimes paused). Player on same tab is also throttled. When tab refocuses, multiple draws may fire in burst, voice queue overflows, and `lastHandledAt` skips multiple `at`s — but only ONE will be processed (only the latest is in bus).
- Verify: visibilitychange handlers (likely none); `MasterPanel.svelte` interval; `voice.js` queueing.

## 12. Voice queue collisions in "both" mode
- Risk: **major**
- Why: Master speaks "Số N" and player speaks "Chờ N" / "Kinh" from same `speechSynthesis` queue on same page. Either they serialize (lag, "Chờ" announced 5s after the call) or one cancels the other (`speechSynthesis.cancel()` typical pattern). Either is wrong UX.
- Verify: `voice.js` — does it `cancel()` before `speak()`? Single shared queue?

## 13. "Kinh" (bingo) speaks twice — once on auto-cross, once on detection
- Risk: **minor/major**
- Why: If bingo is detected both during `processAutoTick` (auto-cross caused win) and during render `$derived(bingo)`, two voice triggers may fire. Or worse, "Chờ N" speaks for the winning number first, then "Kinh" — confusing.
- Verify: `PlayerBoard.svelte` bingo detection effect, `voice.js`, ordering vs `processAutoTick`.

## 14. "Chờ N" toast/voice fires for stale numbers after Tạo bảng mới
- Risk: **major**
- Why: After regenerate, board has new numbers. If `processAutoTick` re-runs against current bus.lastDrawn (because `lastHandledAt` reset), it announces "Chờ N" for a number that was called minutes ago — misleading the player it's a fresh call.
- Verify: regenerate handler + auto-tick re-run logic.

## 15. Master "Ván mới" doesn't reset player
- Risk: **major**
- Why: Master clears `loto_master.called/remaining/last`. Player still has `loto_grid + loto_crossed` from previous game. Player sees old marks, no new "Chờ" because bus now empty. Need explicit player reset signal — does the bus carry "reset" event?
- Verify: MasterPanel "Ván mới" handler; bus contract; PlayerBoard listening for reset.

## 16. Master "Ván mới" while player mid-bingo
- Risk: **minor**
- Why: Player has bingo state showing, master starts new game, player still announcing "Kinh" while master draws number 1 of new game. Voice collision + player's bingo ribbon stays.
- Verify: bingo state lifecycle, reset propagation.

## 17. Player "Xoá đánh dấu" doesn't replay history either
- Risk: **major** (same root cause as #1)
- Why: Clears `crossed` only. Master's already-called list is still valid but player won't re-cross them automatically. Player must re-cross by hand.
- Verify: PlayerBoard clear handler.

## 18. localStorage shape drift after settings refactor
- Risk: **major**
- Why: Old users with `loto_master` v1 schema (e.g. array vs object, missing `last`) load on new code → JSON.parse succeeds but destructure yields undefined → app crashes or silently breaks (called list shows blank). No version field visible from filename.
- Verify: `settings-store.svelte.js` parse + default-merge; check for `schemaVersion` field; wrap parse in try/catch with reset fallback.

## 19. localStorage quota exceeded / disabled (private mode, Safari)
- Risk: **minor**
- Why: Setting `loto_master` throws `QuotaExceededError`. If unhandled, mode toggling and game state silently fails to persist; reload returns blank board. Master-only writes might succeed while player writes fail (or vice versa) → divergence.
- Verify: try/catch around localStorage.setItem in stores; user-facing error toast?

## 20. Multiple tabs of the host
- Risk: **critical**
- Why: Two tabs both running master = two independent bus instances (in-memory, per-tab). Each writes to same `loto_master` localStorage key, last-write-wins, called numbers from one tab overwrite the other. Player in tab A sees draws from tab A only. No `storage` event listener to reconcile.
- Verify: any `window.addEventListener('storage', ...)`; document this is unsupported or add BroadcastChannel.

## 21. Storage event in another tab triggers $effect cascade
- Risk: **unknown**
- Why: If reactive store does subscribe to storage events, cross-tab edits could rehydrate state mid-draw, replacing `called` array out from under MasterPanel render — visible flicker, possible double-add.
- Verify: settings-store + master state hydration.

## 22. ARIA live regions stack on auto-call
- Risk: **minor**
- Why: Each draw probably writes to `aria-live="assertive"` (called pill, "Chờ N" toast, master's call display). At 2s/draw, screen reader queues 3+ announcements per number → unintelligible.
- Verify: any `aria-live="assertive"` in PlayerBoard / MasterPanel / AutoCountdown; prefer `polite` or single region.

## 23. AutoCountdown announces every second to AT
- Risk: **minor**
- Why: If countdown digit is in an aria-live region, it will read "5… 4… 3…" every tick. Annoying and pre-empts important call announcement.
- Verify: `AutoCountdown.svelte` aria attributes.

## 24. Bingo detection runs on every cross including auto
- Risk: **minor**
- Why: Auto-cross from `processAutoTick` flips a cell → bingo `$derived` recomputes → if it triggers a side-effect (voice "Kinh", confetti) inside an `$effect` that also fires for manual crosses, the path may differ subtly. E.g. on manual cross, master's draw display has updated; on auto-cross it has too — but order of effects between voice "Chờ" and bingo detection is the question.
- Verify: PlayerBoard ordering of `$effect`s.

## 25. processAutoTick advances lastHandledAt even when number not on grid
- Risk: **minor** (intentional) but **major** if combined with regenerate
- Why: Stated behavior: dedup advances on every new `at`, even if no cell flipped. Fine — until player regenerates and now the number IS on grid but `lastHandledAt` has already moved past `at`. No retro-cross happens. Same root as #1.
- Verify: `auto-tick.js`.

## 26. PWA service worker serves stale JS, but localStorage is fresh
- Risk: **major**
- Why: User had v1 app open, we deploy v2 with new bus contract. SW caches v1 assets; localStorage has v2 schema written by another device or refresh-on-other-tab. v1 code reads v2 data → crash or wrong rendering. Or vice versa.
- Verify: `service-worker.js` (if exists), Workbox/SvelteKit PWA config, schemaVersion.

## 27. PWA offline: bus state lost, called list survives
- Risk: **major**
- Why: User goes offline, app keeps running from SW cache. Page reload offline — works. But bus history lost on every reload, divergence becomes more frequent because user reloads more often without connectivity feedback.
- Verify: SW + #2.

## 28. Visibility change: rejoin race
- Risk: **major**
- Why: Tab returns from background. Master's interval was throttled → catches up by firing draws back-to-back. Player `$effect` sees rapid `at` increments but only the LAST `lastDrawn` is in bus → all intermediate numbers silently lost from auto-cross perspective, but they ARE in master's `called` list. Massive divergence.
- Verify: visibilitychange handler; need bus to be a queue or to replay from `master.called`.

## 29. `$effect` re-runs on grid change post-cross
- Risk: **minor**
- Why: If `processAutoTick` is called from an `$effect` that depends on both `bus.lastDrawn` and `grid`, swapping the grid triggers re-run with same `lastDrawn` — but `lastHandledAt` already advanced, so it's a no-op. Confirm dedup is robust to this.
- Verify: PlayerBoard `$effect` deps list.

## 30. Two PlayerBoards on page (future / accidental)
- Risk: **unknown**
- Why: If `both` mode somehow renders both routes' PlayerBoard or component is reused, both subscribe to bus; both write to the same `loto_grid` key → last-write-wins, crossed cells flicker.
- Verify: `+page.svelte` + `+layout.svelte`; current code likely single mount but worth checking.

## 31. Master's "remaining" pool out of sync with "called"
- Risk: **major**
- Why: If "called" array is updated optimistically before "remaining" splice (or vice versa) and a render happens between, draw next could pick a number already called. Especially under React-style batching that Svelte 5 may or may not apply.
- Verify: MasterPanel draw handler; consider single transactional update.

## 32. Manual call entry vs auto-call collide
- Risk: **major**
- Why: If master can manually enter a number while interval is running, two `broadcastDraw` paths exist. They could fire in the same ms (#4) or out of order. Also, manual entry might bypass "remaining" pool update.
- Verify: MasterPanel manual call (if exists) + auto-call interaction.

## 33. Voice "Chờ N" speaks for number not on player's board
- Risk: **minor**
- Why: If "Chờ N" is announced whenever a draw happens regardless of whether it's on grid (vs the intent: announce only when player needs to wait/has it). Spec ambiguity — "Chờ" = "wait for N"? clarify.
- Verify: `voice.js` + PlayerBoard call site.

## 34. Speech synthesis voice not loaded yet
- Risk: **minor**
- Why: `voiceschanged` event fires async. First few calls may speak with default voice (English) instead of Vietnamese. Especially on mobile Safari where voices load on first user gesture.
- Verify: `voice.js` voice selection + fallback.

## 35. iOS audio policy: needs user gesture
- Risk: **major** for iOS users
- Why: Auto-call interval fires draw without user gesture → speechSynthesis silent on iOS Safari. User thinks voice is broken. Especially in `both` mode where master never clicked draw button after enabling auto-call.
- Verify: `voice.js`; consider primer gesture.

## 36. settings-store mode=both with stale prefix
- Risk: **minor**
- Why: `storagePrefix` setting (player keys like `loto_grid`) could be edited while in both mode; old keys remain orphaned in localStorage; new prefix has empty grid; player auto-rehydrates blank.
- Verify: settings-store prefix change handler.

## 37. broadcastDraw called with non-number / 0 / NaN
- Risk: **minor**
- Why: Auto-tick dedup runs but cell match `grid.includes(NaN)` returns false; lastHandledAt advances. Player silently ignores. But voice announces "Số NaN".
- Verify: type guards in `call-bus.svelte.js` + `voice.js`.

## 38. Negative auto-call speed / 0
- Risk: **minor**
- Why: If user sets autoCallSpeed=0 in settings (or via DevTools), `setInterval(fn, 0)` = ~4ms minimum, draws 90 numbers in ~1 sec. Bus only retains last; ALL but final auto-cross lost.
- Verify: settings-store validation min/max.

## 39. Master draws 90 numbers, then 91st click
- Risk: **minor**
- Why: Empty `remaining` array → draw next fails silently or throws. If interval is still on, it fires every Ns hitting empty array — does it auto-stop?
- Verify: MasterPanel handleDrawNext when remaining empty + interval guard.

## 40. processAutoTick runs in master mode (mode=master)
- Risk: **minor**
- Why: Player effect should be guarded by mode!==master. If guard is missing or off-by-one ("both" treated as master), auto-cross runs but no PlayerBoard renders, `lastHandledAt` advances pointlessly. Not data-corrupting but wastes work.
- Verify: `processAutoTick({mode})` mode check.

## 41. lastHandledAt advancement inside test vs production
- Risk: **unknown**
- Why: `auto-tick.test.js` exists — if tests pass with mocked Date.now but production uses real Date.now plus throttling, test coverage may not catch #28 / #11.
- Verify: `auto-tick.test.js`; add throttling/burst test.

## 42. Confetti / celebration replay on reload after bingo
- Risk: **minor**
- Why: Bingo state is `$derived(crossed)` → on reload, crossed rehydrates → bingo true → confetti fires again. Annoying.
- Verify: PlayerBoard bingo effect + flag like "celebrated".

## 43. Both mode disables one panel by mistake
- Risk: **minor**
- Why: A `if (mode === 'master')` instead of `if (mode === 'master' || mode === 'both')` on a master button hides it in `both` mode. Vice versa for player. Small typos with three-way enum.
- Verify: every `mode ===` check across components.

## 44. Settings change persists before save
- Risk: **minor**
- Why: SettingsButton may use 2-way binding directly to store instead of staging — toggling mode in dialog immediately remounts panels behind the dialog. UX confusion + state loss if user cancels.
- Verify: `SettingsButton.svelte` binding model.

## 45. Reload during auto-call
- Risk: **major**
- Why: Auto-call running, user F5. `loto_master` saved up to last draw. On reload, auto-call interval is NOT auto-restarted (probably) — game appears paused without indication. Or auto-restarted from autoplay setting → first draw fires with no UI feedback yet.
- Verify: MasterPanel onMount + `autoCall` setting persistence.

## 46. Network/CDN-cached audio mismatch
- Risk: **minor**
- Why: `audio-manifest.js` may reference numbered MP3s; if some 404 due to cache miss, voice fallback to TTS for some numbers and audio for others — inconsistent UX.
- Verify: `audio-manifest.js` + `voice.js` fallback chain.

## 47. Master's "called" history > UI display window
- Risk: **minor**
- Why: After 50+ calls, called list display may overflow / paginate. If player only sees last N, reload doesn't help — but that's master display only. Just confirm.
- Verify: MasterPanel called list rendering.

## 48. Reactive cycle: $effect → state change → $effect re-runs
- Risk: **major**
- Why: If `processAutoTick` mutates `lastHandledAt` AND the `$effect` reads it, infinite loop possible. Svelte 5 has guards but they're not free — perf hit, console warnings.
- Verify: `auto-tick.js` return contract + how PlayerBoard wires it.

## 49. "Kinh" voice on regen-induced auto-cross sweep (if #1 is fixed)
- Risk: **major** (forward-looking)
- Why: If team fixes #1 by replaying `master.called` on regen, the replay might trigger 5+ auto-crosses, last one a bingo, which speaks "Kinh" instantly when user hits "Tạo bảng mới" — startling and wrong (didn't actually win this round).
- Verify: any future replay logic; suppress voice during replay.

## 50. AutoCountdown shows when auto-call disabled
- Risk: **minor**
- Why: Recently added component — if its mount logic doesn't guard on `autoCall` setting, it shows stale "0s" countdown when manual mode active.
- Verify: `AutoCountdown.svelte` mount conditions.

## 51. processAutoTick assumes `crossed` is mutable Set
- Risk: **unknown**
- Why: If crossed is a `$state` reactive Set, mutating in-place vs replacing affects reactivity. Auto-tick may flip cell but PlayerBoard not re-render.
- Verify: `auto-tick.js` mutation strategy + PlayerBoard.

## 52. localStorage write thrash during auto-call
- Risk: **minor**
- Why: Each draw writes `loto_master`. At 2s cadence x 90 draws = 90 writes. Player on same tab also writes `loto_crossed`. Combined with reactive sync (every state change writes), localStorage is hot. Devices with slow storage hitch every draw.
- Verify: persistence layer in stores; consider debounce.

## 53. Dialog/Modal stealing focus during draw
- Risk: **minor**
- Why: SettingsButton dialog open while auto-call fires — focus trap doesn't know about toast/announcement; SR users may miss draws.
- Verify: SettingsButton focus management.

## 54. Reload during call-bus dispatch (race)
- Risk: **minor**
- Why: User hits F5 between `master.called.push(N)` and `broadcastDraw(N)`. localStorage has N in called, bus never broadcast. After reload bus is empty anyway (#2) so net effect is same divergence — but called list now contains N that was never voiced.
- Verify: MasterPanel handleDrawNext atomicity.

## 55. Settings test coverage (false confidence)
- Risk: **unknown**
- Why: `settings-store.test.js` is 369 lines but tests probably mock localStorage. Real-world quota / disabled storage / private mode untested → #19 lurks.
- Verify: test file scenarios.

## 56. broadcastDraw before player mounts (initial both-mode)
- Risk: **minor**
- Why: First render of `+page.svelte` mounts MasterPanel and PlayerBoard. Order matters: if MasterPanel onMount triggers a draw (e.g. resume autoplay) before PlayerBoard `$effect` registered, player misses #1.
- Verify: mount order; defer master autoplay to next tick.

## 57. `Date.now()` in test environment vs SSR
- Risk: **minor**
- Why: SvelteKit may SSR `+page.svelte`. `Date.now()` differs between server and client → hydration mismatch warnings if `at` is used in rendered output.
- Verify: any direct render of `at` value; SSR config.

---

## Triage Recommendation (top 10 to fix first)

1. **#2 Bus reload desync** + **#1 regenerate replay**: root-cause fix = persist `lastHandledAt` AND replay `master.called` on regenerate/reload. Single shared design.
2. **#28 Visibility burst loss**: bus must become queue or pull from `master.called` since last `at`.
3. **#15 Master "Ván mới" doesn't reset player**: define explicit "session reset" signal on bus.
4. **#20 Multiple host tabs**: at minimum show warning, ideally BroadcastChannel.
5. **#12 Voice queue collisions**: define ownership — only player speaks "Chờ/Kinh" after master finishes "Số N". Or pick one speaker in both mode.
6. **#18 Schema drift**: add `schemaVersion`, parse defensively.
7. **#35 iOS gesture**: prime audio on first user click.
8. **#11 Background throttling**: visibilitychange listener to flush queue / pause auto-call.
9. **#7 Mode-toggle interval orphan**: audit interval/effect cleanup.
10. **#43 Three-way enum typos**: grep all `mode ===` usages, normalize to helper `isHost(mode)` / `isGuest(mode)`.

---

## Unresolved questions

- Q1: Is `lastHandledAt` persisted? (drives #3, #14, #25)
- Q2: Does master's "Ván mới" emit any signal player can react to, or is it localStorage-only? (drives #15)
- Q3: What is the exact spec of "Chờ N" — fired for every draw, or only when N is on grid and uncrossed? (drives #33)
- Q4: Is auto-call resumed on reload? (drives #45)
- Q5: Is there any `addEventListener('storage')` for cross-tab? (drives #20, #21)
- Q6: Does bus have any "reset" / "session" event type, or only number broadcasts? (drives #15, #49)
- Q7: How does `processAutoTick` handle `mode==="master"` — early return or run anyway? (drives #40)
- Q8: Is voice serialized via `cancel()+speak()` or queued? (drives #12, #13)
- Q9: Is there a schemaVersion in any localStorage payload? (drives #18, #26)
- Q10: SSR — does `+page.svelte` actually render dynamic state on server, or fully client-only? (drives #57)
