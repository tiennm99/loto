# Phase 2 — Brand H1 marquee + first-run empty preview

## Context
- [plan.md](plan.md). Independent of Phase 1.
- Reviewer P0: H1 reads like a SaaS label; cold first paint shows a
  gray "Nhấn 'Tạo bảng mới'" placeholder with no personality.

## Overview
- Priority: P0
- Status: TODO
- Effort: ~20 min

## Files

| File | Change |
|---|---|
| `src/routes/+page.svelte` | H1: bigger, italic, warmer gradient, drop-shadow; add subtitle line |
| `src/lib/PlayerBoard.svelte` | Replace empty-state placeholder with a faded mini-card preview + warm welcome line |

## H1 marquee

```diff
- <h1 class="text-4xl sm:text-5xl font-extrabold
-            bg-gradient-to-r from-indigo-500 to-purple-500
-            bg-clip-text text-transparent">
-   Lô tô
- </h1>
+ <h1 class="text-5xl sm:text-7xl font-black italic tracking-tight
+            bg-gradient-to-r from-rose-500 via-amber-500 to-rose-500
+            bg-clip-text text-transparent
+            drop-shadow-[0_2px_0_rgba(0,0,0,0.15)]">
+   Lô tô
+ </h1>
+ <p class="text-xs sm:text-sm uppercase tracking-[0.3em]
+           text-slate-500 dark:text-slate-400 italic mt-1">
+   Hội chợ Tân Tân
+ </p>
```

## Empty-state preview

Replace the current placeholder block (`PlayerBoard.svelte` ~line 292):

```svelte
{:else}
  <div class="text-center py-10">
    <!-- Faded 3×9 preview row to set expectations -->
    <div class="mx-auto max-w-xs opacity-30 pointer-events-none mb-6
                rounded-md overflow-hidden border border-slate-300 dark:border-slate-600">
      <div class="grid grid-cols-9 gap-px bg-slate-300 dark:bg-slate-700">
        {#each Array(27) as _, i (i)}
          <div
            class="aspect-square text-[0.6rem] flex items-center justify-center
                   {i % 3 === 0 ? 'bg-white dark:bg-slate-800' : ''}"
            style:background-color={i % 3 === 0 ? undefined : 'var(--empty-cell-bg)'}
          >
            {i % 3 === 0 ? Math.floor(Math.random() * 90) + 1 : ''}
          </div>
        {/each}
      </div>
    </div>
    <p class="text-sm text-slate-500 dark:text-slate-400 italic">
      Nhấn <span class="font-semibold text-indigo-500 dark:text-indigo-400">"Tạo bảng mới"</span> để bắt đầu chơi
    </p>
    <p class="text-xs text-slate-400 dark:text-slate-500 mt-1">
      🎫 Chúc cả nhà một ván vui vẻ
    </p>
  </div>
{/if}
```

(The randomness on first paint is fine — preview re-renders only on
mount; it's decorative, not gameplay.)

## Edge cases

| Case | Handling |
|---|---|
| Tailwind purges `from-rose-500 via-amber-500 to-rose-500` | These are static class names — tailwind keeps them |
| User dislikes the new gradient | Trivial revert, single block |
| Empty preview confuses screen readers | Wrap in `aria-hidden="true"` (already pointer-events-none + decorative) |
| First paint shows different random numbers on every visit | Acceptable — purely decorative |

## Success criteria

- H1 visually dominates above the fold on mobile (text-5xl ≈ 48px).
- "Hội chợ Tân Tân" subtitle reads as a marquee subline.
- Cold reload (no `loto_grid` in localStorage) shows a faded preview
  card + welcome copy, not a gray placeholder.
- Dark-mode versions still readable (>=4.5:1 contrast on subtitle).

## Risks

| Risk | Mitigation |
|---|---|
| `drop-shadow-[…]` arbitrary value drops Safari support pre-15.4 | Acceptable — Safari 15.4 = March 2022; Cloudflare logs would tell us if a meaningful share is on older |
| Brand color shift (purple→rose/amber) might feel inconsistent with Excel-purple empty cells | The H1 is the wordmark, the cells are the data — different roles. Reviewer specifically flagged the purple as "generic SaaS" applied to the H1; cells stay purple by user setting. |

## Next
- Phase 3 master focal point.
