---
phase: 1
title: "Build AutoCountdown component"
status: completed
priority: P2
effort: "1h"
dependencies: []
---

# Phase 1: Build `AutoCountdown.svelte`

## Overview

Self-contained countdown component: shows seconds-remaining number with a
circular SVG ring that depletes from full → empty over each tick interval.
No knowledge of game state — pure visual driven by props.

## Requirements

**Functional**
- Display integer seconds remaining (e.g. `5 → 4 → 3 → 2 → 1`)
- Render circular progress ring (SVG) that depletes smoothly during the tick
- Reset to full whenever a new tick starts (parent signals via `tickKey` prop change)
- Pause/hide cleanly when `running === false`

**Non-functional**
- Smooth animation via `requestAnimationFrame` — no `setInterval` polling
- Respect `prefers-reduced-motion`: skip ring animation, show only number
- File ≤ 200 lines (KISS — keep visual logic only)
- No localStorage / no settings reads — props-driven only

## Architecture

**Props**
```js
{
  running: boolean,    // master is auto-calling
  duration: number,    // seconds per tick (settings.autoCallSpeed, 1..10)
  tickKey: number,     // changes on each draw — triggers ring reset
}
```

**Internal state**
- `tickStart` (`$state`, ms timestamp): set to `performance.now()` when
  `tickKey` changes or `running` flips on
- `now` (`$state`, ms): updated by rAF loop while `running === true`
- `secondsRemaining` (`$derived`): `Math.ceil(duration - (now - tickStart) / 1000)`
  clamped to `[0, duration]`
- `progress` (`$derived`): `(now - tickStart) / (duration * 1000)` clamped to `[0, 1]`

**rAF loop**
- Single `$effect` keyed on `running`: starts loop when true, cancels on cleanup
- Loop sets `now = performance.now()`, then `requestAnimationFrame(loop)`
- When `running === false` → no rAF active, render last frame statically

**SVG ring**
- Outer `<svg viewBox="0 0 100 100">` square, sized via wrapper class
- Background track: full circle, light stroke
- Progress arc: same circle, `stroke-dasharray = circumference`,
  `stroke-dashoffset = circumference * progress` → arc shrinks as time elapses
- Rotated `-90deg` so depletion starts at 12 o'clock and goes clockwise

**Reduced motion fallback**
- Detect once via `window.matchMedia('(prefers-reduced-motion: reduce)')`
- If set → skip rAF loop; update `now` only on `tickKey` change (one frame)
- Number still updates per tick (jumps from `5 → 4 → 3 ...`); ring stays full

## Related Code Files

- Create: `src/lib/AutoCountdown.svelte`

## Implementation Steps

1. Scaffold `<script>` block with props (`running`, `duration`, `tickKey`)
2. Add `tickStart` / `now` `$state` and derived `secondsRemaining` / `progress`
3. Add `$effect` to (a) reset `tickStart` on `tickKey` or `running` rising edge
   and (b) drive rAF loop while `running`
4. Wire reduced-motion check (one-time, module-scope or component-scope const)
5. Render SVG: track circle + progress arc with dynamic `stroke-dashoffset`
6. Center seconds number with `text-3xl font-black tabular-nums`
7. Match token color language: amber-50 background, sky/emerald rings —
   pick **one** neutral color (slate or amber) since this isn't a number token
8. Self-test: `console.log` derived values briefly (remove before commit)

## Success Criteria

- [ ] `AutoCountdown.svelte` < 200 lines
- [ ] Renders nothing visually intrusive when `running === false`
- [ ] Ring depletes from full to empty over `duration` seconds
- [ ] Number ticks down: `duration → duration-1 → ... → 1`
- [ ] Resets cleanly when `tickKey` changes
- [ ] No memory leaks — rAF cancelled on `running=false` and component unmount
- [ ] Reduced-motion: ring static, number still updates

## Risk Assessment

- **rAF leak**: forgetting cleanup when `running` flips false → loop keeps
  running invisibly. Mitigation: single `$effect` with `cancelAnimationFrame`
  in cleanup; verify with DevTools Performance panel.
- **Off-by-one number flash**: `Math.ceil(0)` → 0 right at tick edge before
  parent resets `tickKey`. Mitigation: clamp lower bound to 1 while
  `running && progress < 1`, allow 0 only when stopped.
- **Drift from setInterval**: parent uses `setInterval`, this uses `rAF` →
  microsecond drift over many ticks. Acceptable: ring is visual feedback,
  not authoritative timer; parent's interval still fires on schedule.

## Notes

- Keep visual style consistent with `MasterPanel`'s "Số vừa xổ" hero —
  reuse `border-[6px]`, `rounded-full`, `tabular-nums`, `font-black`
