# Phase 3 — Master mode focal point

## Context
- [plan.md](plan.md). Independent of Phases 1-2.
- Reviewer P0: "Số vừa xổ" hero is too small (96px), called number is
  buried mid-page below the player card. Master mode toggles in
  abruptly with no transition. No `aria-live` for screen readers.

## Overview
- Priority: P0
- Status: TODO
- Effort: ~25 min

## Files

| File | Change |
|---|---|
| `src/lib/MasterPanel.svelte` | Hero "Số vừa xổ" 2× larger; container has `aria-live`; auto-`scrollIntoView` after each draw |
| `src/routes/+page.svelte` | Slide transition on the master section mount |

## Hero scaling

In `MasterPanel.svelte` find the "Số vừa xổ" block (~line 184):

```diff
- <div class="...flex items-center justify-center
-             w-24 h-24 sm:w-28 sm:h-28
-             rounded-full ring-4 ...">
-   <span class="text-5xl sm:text-6xl font-black ...">
+ <div bind:this={heroEl}
+      role="status" aria-live="assertive" aria-atomic="true"
+      class="...flex items-center justify-center
+             w-40 h-40 sm:w-56 sm:h-56
+             rounded-full ring-4 ...
+             scroll-mt-4">
+   <span class="text-7xl sm:text-8xl font-black ...">
      {lastCalled}
    </span>
  </div>
```

Add the auto-scroll wiring (after the existing `$state` block):

```js
let heroEl = $state(/** @type {HTMLDivElement | null} */ (null));

$effect(() => {
  if (lastCalled !== null && heroEl) {
    // microtask after DOM patch
    requestAnimationFrame(() =>
      heroEl?.scrollIntoView({ behavior: "smooth", block: "center" })
    );
  }
});
```

## Slide-in master section

In `+page.svelte`:

```diff
+ <script>
+   import { slide } from "svelte/transition";
+   // …existing imports…
+ </script>

  {#if settings.masterMode}
-   <div class="mt-10">
+   <div class="mt-10" transition:slide={{ duration: 250 }}>
      <h2 class="text-center text-lg font-bold text-orange-500 dark:text-orange-400 mb-4">
        Quản trò
      </h2>
      <MasterPanel />
    </div>
  {/if}
```

Also drop the gradient on H2 (reviewer P2): keep `text-orange-500` —
it stays visually subordinate to H1's rose-amber.

## Edge cases

| Case | Handling |
|---|---|
| Auto-scroll on every render (e.g., toggling auto-call) | `$effect` dep is `lastCalled` only; toggling auto-call doesn't change it |
| Hero `scrollIntoView` competes with toast on player side | Toast is `position: absolute` inside the player area, not affected by window scroll |
| Screen reader reads "45" out of context | `aria-live="assertive"` + Vietnamese voice announcement together — redundant for hearing users; SR users get the live region. Acceptable. |
| User has reduced motion preference | `behavior: "smooth"` ignores prefers-reduced-motion in some browsers; trade-off accepted in v1. Follow-up: branch on `matchMedia('(prefers-reduced-motion: reduce)').matches`. |
| Section unmounts during slide | Svelte's `slide` handles in/out; on toggle off the section collapses cleanly |

## Success criteria

- "Số vừa xổ" circle ~160-220px on desktop, ~160px on mobile.
- After `Xổ số`, the hero scrolls smoothly into view (block: center).
- Screen reader announces each new number.
- Toggling master mode on slides the section in over 250ms (no
  layout jump).
- No regression to auto-call interval timing.

## Risks

| Risk | Mitigation |
|---|---|
| `scrollIntoView` triggers on initial load if `lastCalled` was persisted | `loadState` already sets `lastCalled` on mount; the effect runs once but the user expects to land on the hero anyway. Fine. |
| Bigger hero pushes the rest of the panel down | Intentional — the hero is the focal point |
| Slide transition causes CLS warnings | One-off, only on master toggle; not a Core Web Vitals path |

## Next
- Phase 4 settings polish.
