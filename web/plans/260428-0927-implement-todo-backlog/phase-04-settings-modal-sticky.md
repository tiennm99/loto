---
name: Settings modal sticky on small screens
phase: 4
status: completed
priority: medium
effort: 30m
completed: 2026-04-28
---

# Phase 4 — Settings modal sticky header/footer

## Context
- `src/lib/SettingsButton.svelte:166-459` — modal panel.
- Wrapper: `max-h-[90vh] overflow-y-auto` (line 167).
- TODO: on iPhone SE (375×667) the `<h2 id="settings-title">` and the
  bottom action row scroll out of view, so user has to scroll back to
  reach "Xong" / "Đặt lại".

## Decision
Make the title (`<h2>` block lines 169-177 — title + subtitle) and the
action row (lines 436-457) sticky inside the scrollable panel. Use
`position: sticky` rather than restructuring into a flex layout because
the existing `overflow-y-auto` container already creates a scrolling
context.

## Files
- Modify: `src/lib/SettingsButton.svelte`

## Changes

```html
<!-- Outer panel (line 166-167) — same -->
<div
  class="relative mx-4 max-w-sm sm:max-w-md w-full max-h-[90vh] overflow-y-auto rounded-3xl bg-white dark:bg-slate-800 shadow-2xl animate-pop-in"
>
  <!-- Sticky header — replace lines 169-177 -->
  <div class="sticky top-0 z-10 bg-white dark:bg-slate-800 px-6 pt-6 pb-3 -mx-px rounded-t-3xl">
    <h2 id="settings-title" class="text-2xl font-bold text-slate-800 dark:text-slate-100 mb-1">
      Cài đặt
    </h2>
    <p class="text-sm text-slate-500 dark:text-slate-400">
      Tuỳ chỉnh giao diện bảng lô tô
    </p>
  </div>

  <!-- Body wrapper — wraps existing fieldsets -->
  <div class="px-6">
    {/* existing fieldsets here, unchanged */}
  </div>

  <!-- Sticky footer — replace lines 436-457 -->
  <div class="sticky bottom-0 z-10 bg-white dark:bg-slate-800 px-6 py-4 border-t border-slate-200 dark:border-slate-700 flex justify-between gap-2 rounded-b-3xl">
    <!-- existing buttons unchanged -->
  </div>
</div>
```

Note: panel `p-6` is removed and replaced with per-section padding so
sticky regions can render edge-to-edge backgrounds.

## Steps
1. Move panel padding: drop `p-6` from outer; add `px-6 pt-6 pb-3` to
   sticky header, `px-6` wrapper around fieldsets, `px-6 py-4` on footer.
2. Apply `sticky top-0` / `sticky bottom-0` with matching bg + z-10.
3. Add `border-t` divider on footer for visual separation when content
   is behind it.
4. Test scroll on iPhone SE viewport (Chrome DevTools 375×667) —
   verify h2 stays pinned at top, button row pinned at bottom.
5. Test desktop — modal still pops in cleanly, no double-rounding.

## Success
- iPhone SE: title visible while scrolling middle of modal; close
  button reachable without scrolling.
- Desktop: visual unchanged or improved (subtle border above footer).
- Dark mode: sticky bg matches panel bg (no color seam).

## Risks
- `animate-pop-in` may conflict with `position: sticky` during the open
  animation. Mitigation: sticky elements compute fine inside an
  animated parent; if jitter shows up, scope the animation to a child
  div instead of the panel.
