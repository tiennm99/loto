# Phase 6 — Auto-call: toggle + interval lifecycle

## Context
- [plan.md](plan.md). Depends on Phase 1 (settings.autoCallEnabled, autoCallSpeed) and Phase 4 (MasterPanel).

## Overview
- Priority: P1
- Status: TODO
- Effort: ~30 min

## UX

In MasterPanel, when `settings.autoCallEnabled === true`:
- Add a 2nd line below the controls: speed slider 1–10 sec (label: "Tốc độ tự động: {N} giây/số") wired to `settings.autoCallSpeed`.
- "Xổ số" button label changes:
  - Not running → **"Bắt đầu"** (green gradient).
  - Running → **"Dừng"** (red gradient).
- Clicking starts/stops a `setInterval` that calls the existing `handleDrawNext()` every N seconds.
- Auto-stop when `state.remaining.length === 0`.

When `settings.autoCallEnabled === false` (default): unchanged — manual "Xổ số" per click.

The Auto on/off toggle itself lives in **Settings** (per user spec). Keeping the speed slider inline in MasterPanel — close to the Bắt đầu/Dừng button it controls — feels more discoverable than burying it in Settings. Acceptable deviation: speed lives in MasterPanel, master-mode + auto-enabled live in Settings. (Alt: put speed in Settings too. Decided inline because it's a runtime knob, not a preference.)

Wait — re-read user spec: "When auto, Xổ số became Start/Stop". And "auto-call speed (1-10s/number, default 5)" persisted in settings. The toggle is also in settings.

So:
- **Settings**: `autoCallEnabled` (toggle), `autoCallSpeed` (slider).
- **MasterPanel**: when `autoCallEnabled`, button switches to start/stop; reads speed from settings live (so changing speed in Settings while running re-arms the interval).

Let me put the speed slider in Settings (cleaner), but display its current value as a small caption near the start/stop button so the host knows what's about to fire.

## Lifecycle (the careful bit)

```js
// Inside MasterPanel
let autoRunning = $state(false);

$effect(() => {
  // Re-arm interval whenever autoRunning OR speed changes
  if (!autoRunning) return;
  const ms = settings.autoCallSpeed * 1000;
  const id = setInterval(() => {
    if (!state || state.remaining.length === 0) {
      autoRunning = false; // triggers cleanup
      return;
    }
    handleDrawNext();
  }, ms);
  return () => clearInterval(id);
});

// Auto-stop also if user disables the auto setting mid-run
$effect(() => {
  if (!settings.autoCallEnabled && autoRunning) autoRunning = false;
});

// Auto-stop when starting a new game
function handleNewGame() { /* ... */ autoRunning = false; }
```

Race conditions handled:
- **Speed change while running**: $effect re-runs (its dependency `settings.autoCallSpeed` changed) → cleanup old `setInterval` → start new one. ✓
- **Remaining hits 0 during a tick**: tick checks first, sets `autoRunning = false`, returns without drawing. The $effect re-runs, sees `!autoRunning`, returns without setting up a new interval. ✓
- **User unmounts MasterPanel (master mode off) mid-run**: Svelte fires effect cleanup → `clearInterval`. ✓
- **User disables auto setting while running**: secondary $effect catches it, flips `autoRunning = false`, primary effect cleans up. ✓
- **Multiple intervals stacking**: impossible, primary $effect always runs cleanup before re-running. ✓

## Files

| File | Change |
|---|---|
| `src/lib/MasterPanel.svelte` | Add `autoRunning` state, the two `$effect`s above, swap "Xổ số" button label/style based on `settings.autoCallEnabled` and `autoRunning`. Display speed caption when auto on. |
| `src/lib/SettingsButton.svelte` | Add "Tự động xổ" toggle + speed slider 1–10 (visible only when masterMode is on, OR always; pick: always — toggling auto without master mode is a no-op so harmless). |
| `src/lib/settings-store.test.js` | (already covered Phase 1) — no new tests for the slider UI itself. |
| (no new tests for the $effect lifecycle — relies on browser timers; manual verify in dev) |

## Implementation Steps

1. `MasterPanel.svelte`:
   - Add `import { settings } from "$lib/settings-store.svelte.js"` if not already present.
   - Add `let autoRunning = $state(false)`.
   - Add the two $effects (above).
   - Modify the "Xổ số" button:
     ```svelte
     {#if settings.autoCallEnabled}
       <button onclick={() => (autoRunning = !autoRunning)}
               disabled={!state || (state.remaining.length === 0 && !autoRunning)}
               class="px-10 py-4 rounded-full font-semibold text-white text-lg
                      bg-gradient-to-r {autoRunning
                        ? 'from-red-500 to-rose-500'
                        : 'from-emerald-500 to-teal-500'}
                      hover:opacity-90 active:scale-95 transition-all shadow-lg">
         {autoRunning ? "Dừng" : "Bắt đầu"}
       </button>
     {:else if state && state.remaining.length > 0}
       <button onclick={handleDrawNext} class="...">Xổ số</button>
     {/if}
     ```
   - Reset `autoRunning = false` in `handleNewGame`.
   - Below the buttons, when auto on: small caption "Tự động: {settings.autoCallSpeed}s/số".

2. `SettingsButton.svelte`:
   - Add a "Tự động xổ" fieldset (after Master mode) with:
     - On/off toggle pill (same style as masterMode).
     - When on: a `<input type="range" min="1" max="10" step="1" bind:value={settings.autoCallSpeed} oninput={saveSettings}>` plus a label showing "{N} giây/số".

3. svelte-check + manual dev verify:
   - Master mode on, auto off: "Xổ số" works as before.
   - Master mode on, auto on, speed=2: click "Bắt đầu" → numbers draw every 2s. Click "Dừng" → stops. Click "Bắt đầu" again → resumes. Reaching 90 numbers stops automatically.
   - Change speed slider while running → interval re-arms at new speed.

## Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Tab in background throttles `setInterval` to ≥1s | Real | If speed=1, browser may still fire ~1s. UX: slightly slower in background. | Acceptable — host typically has tab focused |
| User toggles autoCallEnabled rapidly while running | Low | Stale interval | Secondary $effect catches it |
| `handleDrawNext` mutates `state` during interval tick — does Svelte 5 reactivity batch correctly? | Low | If reactive update is slow, intervals could overlap. | `setInterval` ticks are independent of render; `handleDrawNext` is sync; no overlap risk in practice. |
| Confirm dialog in `handleNewGame` (`if (state && !confirm(...))`) blocks during auto-call → interval keeps firing while modal is up | Low | Visual glitch | `handleNewGame` is user-triggered, not from interval. Safe. |

## Success criteria

- Auto setting off → manual "Xổ số" unchanged.
- Auto setting on → "Bắt đầu"/"Dừng" button. Drawing every N seconds. Stops on Dừng. Stops at 90/90. Speed change re-arms.
- Disabling auto in Settings while running stops it.
- Tests still pass (no logic regression).
- No timer leaks (verified by toggling master-mode off while running — Svelte cleanup handles).

## Done = Plan complete
- Mark plan.md status → `completed`. Run `/ck:project-management` sync if needed.
- Update docs (PDR, codebase-summary, system-architecture, dev-roadmap).
- Move "Theme switcher" from roadmap "Idea Phase" → "Currently Implemented".
