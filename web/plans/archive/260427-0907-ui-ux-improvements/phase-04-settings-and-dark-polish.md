# Phase 4 — Settings modal width + dark-mode purple + switch toggles

## Context
- [plan.md](plan.md). Independent.
- Reviewer P1: modal feels cramped on tablet+; empty cells use full-
  saturation Excel purple in both themes (neon-bright in dark mode);
  "Đang bật / Đang tắt" reads like state, not action.

## Overview
- Priority: P1
- Status: TODO
- Effort: ~25 min

## Files

| File | Change |
|---|---|
| `src/lib/SettingsButton.svelte` | Modal `max-w-sm` → `max-w-sm sm:max-w-md`; convert master/auto/voice toggles to switch UI |
| `src/app.css` | Dark-mode override for `--empty-cell-bg` (or filter on dark) |

## Modal width

```diff
  <div
-   class="relative mx-4 max-w-sm w-full max-h-[90vh] overflow-y-auto
+   class="relative mx-4 max-w-sm sm:max-w-md w-full max-h-[90vh] overflow-y-auto
           rounded-3xl bg-white dark:bg-slate-800 p-6 shadow-2xl animate-pop-in"
  >
```

Cell color swatch grid stays 5-col; the extra width gives breathing
room without re-flow.

## Switch UI for boolean toggles

A small reusable inline pattern (no new component file — this is a
two-line snippet replacement, KISS):

```svelte
<!-- Replace the master-mode "Đang bật / Đang tắt" full-width button -->
<label class="flex items-center justify-between gap-3 px-3 py-2 rounded-lg
              border-2 border-slate-200 dark:border-slate-600 cursor-pointer
              hover:border-slate-300 dark:hover:border-slate-500 transition-colors">
  <span class="text-sm text-slate-700 dark:text-slate-200">
    Hiện bảng quản trò
  </span>
  <span
    role="switch"
    aria-checked={settings.masterMode}
    tabindex="0"
    onclick={toggleMaster}
    onkeydown={(e) => (e.key === " " || e.key === "Enter") && (e.preventDefault(), toggleMaster())}
    class="relative inline-block w-10 h-6 rounded-full transition-colors
           {settings.masterMode
             ? 'bg-emerald-500'
             : 'bg-slate-300 dark:bg-slate-600'}"
  >
    <span class="absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full transition-transform
                 {settings.masterMode ? 'translate-x-4' : 'translate-x-0'}"></span>
  </span>
</label>
```

Apply the same pattern to:
- Auto-call enable (`autoCallEnabled`)
- Voice master (`voiceEnabledMaster`)
- Voice player (`voiceEnabledPlayer`)

Voice picker (radio-style buttons) stays as-is — switch UI doesn't
generalize to >2 options.

## Dark-mode empty-cell desaturation

In `src/app.css`:

```diff
  :root {
    --empty-cell-bg: #7030A0;
  }
+
+ :where(.dark) {
+   --empty-cell-bg: #5a2480;
+ }
```

Or, if user has a custom hex via the color picker, the override above
gets clobbered when they pick. Better: keep user's choice as the
source of truth, but apply a `filter: brightness(0.85)` in dark mode
on the empty-cell elements.

Decision: **respect user choice in both modes**. Skip the dark
override; user can pick their own color for dark mode if they want.
Just dim slightly via filter:

```css
:where(.dark) [style*="--empty-cell-bg"],
:where(.dark) [style*="background-color"] {
  /* Too broad — second selector hits everything. Skip. */
}
```

Actually the cleanest fix that doesn't fight user choice:

```svelte
<!-- PlayerBoard.svelte empty cell -->
- style:background-color="var(--empty-cell-bg)"
+ class="dark:[filter:brightness(0.85)_saturate(0.9)]"
+ style:background-color="var(--empty-cell-bg)"
```

This keeps user's color but tones it down ~15% in dark mode.

## Edge cases

| Case | Handling |
|---|---|
| Switch keyboard activation on iOS VoiceOver | `role="switch"` + `aria-checked` + Space/Enter keydown handler — works |
| Dark-mode filter on cells with custom colors close to white | Filter is multiplicative; near-white stays near-white, near-purple goes near-deeper-purple. Acceptable. |
| Modal `max-w-md` overflows on landscape phones | `mx-4` gutter still applies; max width is a cap, not a floor |
| Hover styles on touch devices stick after tap | Existing pattern in the codebase; no regression |

## Success criteria

- On a 768px+ viewport, settings modal feels balanced (not narrow).
- Boolean toggles read as switches, not buttons.
- In dark mode, default purple `#7030A0` reads as a muted purple, not
  neon. Custom colors get the same dimming.
- All settings keep persisting correctly (Phase 4 of voice plan
  test-suite still green).

## Risks

| Risk | Mitigation |
|---|---|
| Switch behavior diverges across keyboard/mouse/touch | Tested keyboard handler; tap-on-label still works because the inner span is the actual switch role |
| Custom dark color picked at full saturation looks worse with filter | User can override; trade-off accepted |
| Filter on every cell adds GPU layer | 81 cells × 1 filter = no measurable cost on modern devices |

## Next
- Phase 5 celebration tiering.
