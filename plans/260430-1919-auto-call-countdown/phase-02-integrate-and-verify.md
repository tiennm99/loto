---
phase: 2
title: "Integrate into MasterPanel and verify"
status: completed
priority: P2
effort: "30m"
dependencies: [1]
---

# Phase 2: Integrate into MasterPanel and verify

## Overview

Mount `AutoCountdown` inside `MasterPanel.svelte`, drive it from the existing
auto-call `$effect`, and verify behavior in the browser. Replace (or augment)
the static "Tự động: Xs/số" caption with the live countdown.

## Requirements

**Functional**
- Countdown appears only when `settings.autoCallEnabled && autoRunning && state?.remaining.length > 0`
- Resets each time `handleDrawNext()` fires
- Disappears when host clicks "Dừng" or game runs out

**Non-functional**
- No regression to existing `setInterval` timing — countdown is decorative
- No additional re-renders on the master grid (3-cell wide affected area only)
- All existing 53 vitest tests still pass

## Architecture

**Tick key signaling**
- Add `let tickCount = $state(0)` to `MasterPanel`
- `handleDrawNext()` increments `tickCount` after `broadcastDraw`
- Pass `tickKey={tickCount}` to `AutoCountdown`

**Reset on toggle**
- When `toggleAuto()` flips `autoRunning` from false → true, also bump
  `tickCount` so countdown starts immediately at full duration
- Cleanest: bump `tickCount` inside the auto-call `$effect` on the rising
  edge of `autoRunning`

**Layout**
- Replace lines 220–226 (`{#if settings.autoCallEnabled && state && state.remaining.length > 0}` block)
  with a flex row that contains the countdown when running, and falls back
  to the static caption when not running
- Or simpler: keep the static caption, add countdown above the "Số vừa xổ"
  hero only while `autoRunning` — less rewiring of existing layout

**Recommendation**: keep static caption, render `AutoCountdown` immediately
above the "Số vừa xổ" hero (line 229), gated by `autoRunning && state?.remaining.length > 0`.
Size around `w-20 h-20 sm:w-24 sm:h-24` (smaller than the hero so it doesn't
compete for visual attention).

## Related Code Files

- Modify: `src/lib/MasterPanel.svelte`
  - Add import for `AutoCountdown`
  - Add `tickCount` state
  - Bump `tickCount` in `handleDrawNext` and on `autoRunning` rising edge
  - Render `<AutoCountdown>` above the hero block

## Implementation Steps

1. Import `AutoCountdown` from `$lib/AutoCountdown.svelte`
2. Add `let tickCount = $state(0);` near `autoRunning`
3. In `handleDrawNext()`: append `tickCount++;` after `broadcastDraw(next)`
4. In the auto-call `$effect`: on the rising edge of `autoRunning` (i.e. when
   the effect re-runs because `autoRunning` flipped to true), also `tickCount++`
   so the ring resets immediately rather than waiting for the first interval tick
5. Add markup before line 229's hero block:
   ```svelte
   {#if autoRunning && state && state.remaining.length > 0}
     <div class="flex justify-center mb-3">
       <AutoCountdown
         running={autoRunning}
         duration={settings.autoCallSpeed}
         tickKey={tickCount}
       />
     </div>
   {/if}
   ```
6. Run `npm run lint` — fix any warnings
7. Run `npm test` — ensure all 53 tests still pass
8. Run `npm run dev`, open browser, manually verify:
   - Enable master mode + auto-call in settings
   - Click "Bắt đầu" → countdown appears, ring depletes, number ticks down
   - Each new draw resets the countdown
   - Click "Dừng" → countdown disappears
   - Change `autoCallSpeed` mid-run → next tick uses new duration cleanly
   - Toggle reduced-motion (DevTools → Rendering tab) → ring stays static, number still updates

## Success Criteria

- [ ] Countdown visible only during active auto-call
- [ ] Smooth ring animation on default-motion devices
- [ ] Number resets to `autoCallSpeed` value at each tick
- [ ] No console errors / no leaked rAF after stopping
- [ ] All 53 existing vitest tests pass
- [ ] `npm run lint` clean
- [ ] `npm run build` succeeds

## Risk Assessment

- **Speed change mid-run**: parent's `$effect` tears down + re-arms the
  `setInterval` when `settings.autoCallSpeed` changes (already handled,
  see line 116). The `AutoCountdown` `duration` prop will also flow
  through, so its derived calculations re-base. Need to bump `tickCount`
  on speed change too — otherwise ring shows wrong progress until next
  natural tick. Mitigation: bump `tickCount++` inside the auto-call
  `$effect` body so any re-arm (running, speed, enabled) resets the ring.
- **CSP impact**: SVG inline + Svelte-injected style attrs (e.g.
  `stroke-dashoffset`) — already permitted by existing CSP setup
  (see `scripts/inject-csp-hashes.mjs`). No new CSP work needed.
- **Build size**: one small SVG component, negligible.

## Verification Checklist (manual)

- [ ] `npm run dev` starts cleanly
- [ ] Settings → enable "Chế độ quản trò" + "Tự động xổ"
- [ ] Click "Bắt đầu", observe countdown
- [ ] Watch ≥3 ticks — verify smooth depletion + reset
- [ ] Change speed slider → ring re-bases without glitch
- [ ] DevTools → Rendering → "prefers-reduced-motion: reduce" → verify static fallback
- [ ] Stop, verify countdown unmounts; rAF count in DevTools idle

## Docs Impact

- Minor: add brief note to `docs/codebase-summary.md` about the new component
  and to `docs/system-architecture.md` if it lists key UI components
