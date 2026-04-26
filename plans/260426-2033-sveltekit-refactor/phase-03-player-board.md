---
phase: 3
title: Port PlayerBoard component to Svelte 5 runes
priority: high
effort: M
status: planned
---

# Phase 3 — PlayerBoard component

Port `components/player-board.jsx` (224 lines, React + JSDoc) to
`src/lib/PlayerBoard.svelte` (Svelte 5 runes). This is the most state-heavy
component and where runes pay off vs. React.

## React → runes mapping

| React | Svelte 5 |
|---|---|
| `useState(null)` | `let grid = $state(null)` |
| `useState([])` | `let crossed = $state([])` |
| `useRef(new Set())` | `let celebratedRows = new Set()` (mutable, no rune needed — refs aren't reactive) |
| `useMemo(() => grid.map(isRowComplete), [grid, crossed])` | `let rowCompleteness = $derived(grid ? grid.map((_, r) => isRowComplete(grid, crossed, r)) : [])` |
| `useEffect(() => { ... save }, [crossed])` | `$effect(() => { if (crossed.length) saveCrossedState(crossed, storagePrefix); })` |
| `useCallback(handler, [deps])` | Plain function — Svelte tracks deps via `$state` reads automatically |

## Component skeleton

```svelte
<script>
  import {
    generateGrid,
    getWaitingNumber,
    isRowComplete,
    loadCrossedState,
    loadGrid,
    saveCrossedState,
    saveGrid,
  } from '$lib/game-logic.js';

  /**
   * @typedef {Object} Props
   * @property {string} [storagePrefix]
   */

  /** @type {Props} */
  let { storagePrefix = 'loto' } = $props();

  let grid = $state(/** @type {number[][] | null} */ (null));
  let crossed = $state(/** @type {boolean[][]} */ ([]));
  let showCongrats = $state(false);
  let congratsRow = $state(-1);
  let toast = $state(/** @type {string | null} */ (null));

  // Refs: mutable, non-reactive
  let toastTimer = null;
  const celebratedRows = new Set();
  const notifiedWaitingRows = new Set();

  // Derived: precompute row completeness once per render
  const rowCompleteness = $derived(
    grid && crossed.length ? grid.map((_, r) => isRowComplete(grid, crossed, r)) : []
  );

  function dismissToast() {
    toast = null;
    if (toastTimer) {
      clearTimeout(toastTimer);
      toastTimer = null;
    }
  }

  function showToast(msg) {
    dismissToast();
    toast = msg;
    toastTimer = setTimeout(() => { toast = null; }, 5000);
  }

  // Initial load — runs once on mount per storagePrefix change
  $effect(() => {
    const savedGrid = loadGrid(storagePrefix);
    if (!savedGrid) return;
    grid = savedGrid;
    crossed = loadCrossedState(storagePrefix) ?? savedGrid.map((row) => row.map(() => false));
    celebratedRows.clear();
    notifiedWaitingRows.clear();
    for (let i = 0; i < savedGrid.length; i++) {
      if (isRowComplete(savedGrid, crossed, i)) celebratedRows.add(i);
      if (getWaitingNumber(savedGrid, crossed, i) !== null) notifiedWaitingRows.add(i);
    }
  });

  // Persist crossed state
  $effect(() => {
    if (crossed.length > 0) saveCrossedState(crossed, storagePrefix);
  });

  // Detect newly completed and waiting rows. Two passes prevent skipped resets.
  $effect(() => {
    if (!grid || crossed.length === 0) return;

    for (let i = 0; i < grid.length; i++) {
      if (!celebratedRows.has(i) && isRowComplete(grid, crossed, i)) {
        celebratedRows.add(i);
        notifiedWaitingRows.add(i);
        congratsRow = i + 1;
        showCongrats = true;
        break;
      }
    }

    for (let i = 0; i < grid.length; i++) {
      if (celebratedRows.has(i)) continue;
      const waitNum = getWaitingNumber(grid, crossed, i);
      if (waitNum !== null && !notifiedWaitingRows.has(i)) {
        notifiedWaitingRows.add(i);
        showToast(`Chờ ${waitNum}`);
      } else if (waitNum === null && notifiedWaitingRows.has(i)) {
        notifiedWaitingRows.delete(i);
      }
    }
  });

  function handleGenerate() {
    if (grid && !confirm('Bạn có muốn tạo lại bảng không?')) return;
    const newGrid = generateGrid();
    grid = newGrid;
    crossed = newGrid.map((row) => row.map(() => false));
    saveGrid(newGrid, storagePrefix);
    saveCrossedState(crossed, storagePrefix);
    celebratedRows.clear();
    notifiedWaitingRows.clear();
    dismissToast();
  }

  function handleCellClick(row, col) {
    crossed = crossed.map((r, ri) =>
      ri === row ? r.map((v, ci) => (ci === col ? !v : v)) : r
    );
  }
</script>

<!-- Generate button -->
<div class="flex justify-center mb-6">
  <button
    onclick={handleGenerate}
    class="px-8 py-3 rounded-full font-semibold text-white
           bg-gradient-to-r from-indigo-500 to-purple-500
           hover:from-indigo-600 hover:to-purple-600
           active:scale-95 transition-all shadow-lg shadow-indigo-500/25"
  >
    Tạo bảng mới
  </button>
</div>

{#if grid}
  <div class="relative">
    <div
      aria-label="Bảng lô tô"
      class="rounded-2xl overflow-hidden shadow-xl shadow-slate-200/50 dark:shadow-black/30 border border-slate-200 dark:border-slate-700"
    >
      <div class="loto-grid">
        {#each grid.flat() as num, idx}
          {@const row = Math.floor(idx / 9)}
          {@const col = idx % 9}
          {@const hasNumber = num > 0}
          {@const isCrossed = hasNumber && !!crossed[row]?.[col]}
          {@const rowComplete = hasNumber && rowCompleteness[row]}

          {#if !hasNumber}
            <div aria-hidden="true" class="cell-empty" />
          {:else}
            <button
              type="button"
              aria-label="Số {num}{isCrossed ? ', đã đánh dấu' : ''}"
              aria-pressed={isCrossed}
              onclick={() => handleCellClick(row, col)}
              class="cell-num"
              class:cell-crossed={isCrossed}
              class:cell-completed={rowComplete}
            >
              {num}
            </button>
          {/if}
        {/each}
      </div>
    </div>

    {#if toast}
      <div
        role="status"
        aria-live="polite"
        onclick={dismissToast}
        class="absolute inset-0 flex items-center justify-center pointer-events-auto cursor-pointer z-10"
      >
        <div class="px-6 py-3 rounded-2xl bg-amber-500/90 dark:bg-amber-600/90 text-white text-xl sm:text-2xl font-black shadow-xl animate-toast">
          {toast}
        </div>
      </div>
    {/if}
  </div>
{:else}
  <div class="text-center text-slate-400 dark:text-slate-500 py-20 text-sm">
    Nhấn "Tạo bảng mới" để bắt đầu chơi
  </div>
{/if}

{#if showCongrats}
  <!-- bingo modal — same markup as React version with svelte:on:keydown for Escape -->
  ...
{/if}

<style>
  .cell-empty { @apply relative flex items-center justify-center aspect-square border-r border-b border-slate-200/80 dark:border-slate-700/60 bg-slate-50 dark:bg-slate-900/60; }
  .cell-num { @apply relative flex items-center justify-center aspect-square text-base sm:text-xl font-bold border-r border-b border-slate-200/80 dark:border-slate-700/60 transition-all select-none cursor-pointer focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-400 bg-white dark:bg-slate-800 text-slate-800 dark:text-slate-100 hover:bg-indigo-50 dark:hover:bg-indigo-950/30 hover:text-indigo-600 dark:hover:text-indigo-400; }
  .cell-crossed { @apply bg-red-50 dark:bg-red-950/30 text-red-400 dark:text-red-500; }
  .cell-completed { @apply bg-emerald-100 dark:bg-emerald-900/40 text-emerald-500 dark:text-emerald-400; }
</style>
```

The full bingo-modal block follows the React structure 1:1 — `role="dialog"`,
`aria-labelledby="congrats-title"`, click-outside-to-dismiss, Escape key
handler. Use Svelte's `onkeydown` and a `tabindex={-1}` wrapper.

## Behavior parity checklist (map to original fixes)

- [ ] Bingo popup fires only once per row — guarded by `celebratedRows` Set
- [ ] Two-pass effect prevents skipped reset for higher-index rows — same
      structure as React fix
- [ ] `isRowComplete` requires ≥1 numbered cell — already in `game-logic.js`
- [ ] Memoized `rowCompleteness` via `$derived` (was `useMemo`)
- [ ] localStorage shape validation via `safeParse` — in `game-logic.js`
- [ ] Real `<button>` cells with `aria-label`, `aria-pressed`, focus ring
- [ ] Modal: `role="dialog"`, `aria-labelledby`, Escape closes
- [ ] Toast: `role="status"`, `aria-live="polite"`

## Cell click immutability

React used `prev.map((r) => [...r])` for crossed toggle. Svelte 5 with `$state`
deeply tracks objects, so simpler:

```js
crossed[row][col] = !crossed[row][col];
```

…would also work because runes proxy nested writes. But explicit immutable
update (as in skeleton above) keeps reactivity behavior predictable across
older Svelte 5 versions. Pick whichever, document the choice.

## Files affected

- create: `src/lib/PlayerBoard.svelte`

## Verify

- `npm run dev` — visit `/`, generate card, click cells, see crossed state
- Click 4 of 5 in a row — see "Chờ X" toast
- Click the 5th — see "Kinh!" popup
- Reload — state persists for the default `loto` prefix
- (After Phase 4) `/master` shows host card with `storagePrefix="loto_master_card"`,
  storage stays separate from `/`

## Out of scope

Routes, layout (Phase 4). Codeserver verification (Phase 5).

## Status: planned
