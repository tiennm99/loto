# Phase 5 — Master mode integration on `/`

## Context
- [plan.md](plan.md). Depends on Phase 1 (settings.masterMode) and Phase 4 (MasterPanel).
- Goal: a single-page experience. Player board on top; master panel below
  when `settings.masterMode === true`. The `/master` route stays as a deep
  link.

## Overview
- Priority: P1
- Status: TODO
- Effort: ~25 min

## UX

**Player page (`/`)**:
- Header (H1 "Lô tô" + SettingsButton).
- PlayerBoard.
- If `settings.masterMode`:
  - Visual divider (e.g., `<hr>` or just margin) + small heading "Quản trò".
  - `<MasterPanel />`.
- PageFooter.

**Settings**: a "Chế độ quản trò" toggle (on/off, default off). When the user flips it on, the master panel appears inline immediately (rune-reactive — no reload). When off, the panel unmounts and the master state in localStorage stays put (next toggle restores it).

**`/master` deep link**: continues to render `<MasterPanel />` standalone (Phase 4's slim shell). Doesn't mutate the masterMode setting (least surprise — user lands on /master, sees master panel, leaves; their `/` view stays the way they left it).

## Files

| File | Change |
|---|---|
| `src/routes/+page.svelte` | Import `MasterPanel`. Read `settings.masterMode`. Conditionally render `<MasterPanel />` between PlayerBoard and PageFooter. |
| `src/lib/SettingsButton.svelte` | Add a "Chế độ quản trò" toggle row (above the color fieldset, below theme). Wire to `settings.masterMode` + `saveSettings`. |
| `src/routes/master/+page.svelte` | No further change in this phase (Phase 4 already shrank it). |

## Implementation Steps

1. `+page.svelte`:
   ```svelte
   <script>
     import MasterPanel from "$lib/MasterPanel.svelte";
     import { settings } from "$lib/settings-store.svelte.js";
     // ... existing imports
   </script>

   <PlayerBoard />

   {#if settings.masterMode}
     <div class="mt-10">
       <h2 class="text-center text-lg font-bold text-orange-500 dark:text-orange-400 mb-4">
         Quản trò
       </h2>
       <MasterPanel />
     </div>
   {/if}

   <PageFooter />
   ```

2. `SettingsButton.svelte`: add the toggle in the modal. Keep it minimal — a labelled checkbox or a 2-state pill. Suggest pill for visual consistency with theme buttons.

   ```svelte
   <fieldset class="mb-5">
     <legend class="text-sm font-semibold ...">Chế độ quản trò</legend>
     <p class="text-xs text-slate-400 dark:text-slate-500 mb-2">
       Hiện bảng quản trò bên dưới bảng người chơi
     </p>
     <button onclick={() => toggleMasterMode()}
             class="w-full px-3 py-2 rounded-lg border-2 ...
                    {settings.masterMode
                      ? 'border-emerald-500 bg-emerald-50 dark:bg-emerald-950/30 text-emerald-700 dark:text-emerald-300'
                      : 'border-slate-300 dark:border-slate-600'}">
       {settings.masterMode ? "Đang bật" : "Đang tắt"}
     </button>
   </fieldset>
   ```

3. Verify: toggle on/off in settings → master panel appears/disappears live on `/`. `/master` still works.

## Edge cases

- User flips master mode off mid-game: panel unmounts; `loto_master` storage stays so next-on restores. ✓ Default Svelte behavior since the component manages its own state from storage on mount.
- User has no `loto_master` data when toggling on for the first time: MasterPanel's existing render handles this (`{:else} Nhấn "Ván mới" để bắt đầu`). ✓

## Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Unmount mid-auto-call interval (Phase 6) leaks timer | Med | Memory | $effect cleanup in Phase 6 covers it |
| Navigation from `/` to `/master` with masterMode=on shows master panel twice (once on /, once on /master) | n/a | — | Different routes — only one renders at a time |
| Removing "Trang quản trò" header link in Phase 3 leaves no nav to `/master` | True by design | Acceptable | `/master` becomes deep-link-only; users discover master mode via Settings |

## Success criteria

- `settings.masterMode === false`: `/` shows player board + footer only. No "Trang quản trò" link or master content.
- `settings.masterMode === true`: `/` shows player board, then "Quản trò" subhead, then full MasterPanel, then footer.
- `/master` still works (renders MasterPanel + back link to `/`).
- Toggling the setting repaints `/` live.

## Next
- Phase 6: auto-call extends MasterPanel.
