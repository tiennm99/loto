# Phase 4 — Extract MasterPanel component

## Context
- [plan.md](plan.md). Independent of Phase 2/3. Blocks Phase 5/6.
- Goal: pull all of `/master`'s state + UI (except page-level header) into a
  reusable `MasterPanel.svelte` so both `/` (when master mode on) and
  `/master` (kept for deep link) render the same logic without duplication.

## Overview
- Priority: P0 for Phase 5/6 dependency
- Status: TODO
- Effort: ~30 min (mechanical refactor + verify)

## What goes into MasterPanel

From `src/routes/master/+page.svelte`:
- `<script module>` block (BOARD constants, storage helpers).
- `<script>` reactive state (`state`, `lastCalled`, `callOrder`, effects, handlers).
- DOM:
  - "Ván mới" + "Xổ số" controls.
  - "Số vừa xổ" hero token.
  - "Thứ tự đã xổ" history.
  - 11×9 master tracking grid.
  - The host's own player card (`<PlayerBoard storagePrefix="loto_master_card" />`).

## What stays out

- Page-level header (H1 "Quản trò", back-to-player link, the SettingsButton mount).
- The `<PageFooter />` mount.
- The outer `<div class="flex flex-col flex-1 items-center px-3 py-8 …">` wrapper.

## File contract

`src/lib/MasterPanel.svelte`:
- No props (mirrors current behavior).
- Self-contained — owns its own localStorage, its own draw state, etc.
- Renders top-down: controls → "Số vừa xổ" → history → master grid → host's own card.
- Uses the `loto_master` and `loto_master_card_*` storage keys (unchanged).

`src/routes/master/+page.svelte` shrinks to:
```svelte
<script>
  import { base } from "$app/paths";
  import MasterPanel from "$lib/MasterPanel.svelte";
  import SettingsButton from "$lib/SettingsButton.svelte";
  import PageFooter from "$lib/PageFooter.svelte";
</script>

<div class="flex flex-col flex-1 items-center px-2 py-4 sm:px-3 sm:py-12">
  <div class="w-full max-w-2xl">
    <header class="relative text-center mb-6">
      <div class="absolute right-0 top-0"><SettingsButton /></div>
      <h1 class="text-3xl sm:text-4xl font-extrabold ...">Quản trò</h1>
      <a href="{base}/" class="...">← Về trang người chơi</a>
    </header>
    <MasterPanel />
    <PageFooter />
  </div>
</div>
```

(Phase 5 will further re-shape this route.)

## Implementation Steps

1. Create `src/lib/MasterPanel.svelte`. Copy:
   - Entire `<script module>` block.
   - The reactive `<script>` block (state, derived, effects, handlers — but NOT the `import { base }` since the back link stays on the route).
   - The DOM from "Controls" through "Master 9x10 tracking board" through "Master's own playing card" `<PlayerBoard storagePrefix="loto_master_card" />`. Wrap in a top-level `<section aria-label="Bảng quản trò">…</section>` (since the page already has its own header).
2. In `src/routes/master/+page.svelte`, delete the moved code and replace with `<MasterPanel />`. Verify imports — keep only what's still used: `base`, `SettingsButton`, `MasterPanel`, `PageFooter` (Phase 3 added).
3. svelte-check + dev-browser verify the `/master` route renders identically to before.

## Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Module-scope `BOARD` constants accidentally duplicated when component re-mounts | None — `<script module>` is one-time per module | — | n/a |
| Storage prefix typo breaks existing user data | Low | High | Keep `loto_master` and `loto_master_card` exactly. Grep before/after. |
| Route still imports unused symbols (lint warning) | Med | Low | clean up after move |

## Success criteria

- `/master` page renders identically pre/post (visually + functionally).
- No code duplicated between MasterPanel and the route.
- `npm test`/`lint`/`svelte-check`/`build` all clean.

## Next
- Phase 5: mount `MasterPanel` conditionally on `/`.
- Phase 6: extend MasterPanel with auto-call.
