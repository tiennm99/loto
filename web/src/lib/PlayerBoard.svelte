<script>
  import {
    generateGrid,
    getWaitingNumber,
    isRowComplete,
    loadCrossedState,
    loadGrid,
    saveCrossedState,
    saveGrid,
  } from "$lib/game-logic.js";

  /**
   * @typedef {Object} Props
   * @property {string} [storagePrefix] localStorage key prefix; allows multiple
   *   independent boards (e.g. user vs master)
   */

  /** @type {Props} */
  let { storagePrefix = "loto" } = $props();

  let grid = $state(/** @type {number[][] | null} */ (null));
  let crossed = $state(/** @type {boolean[][]} */ ([]));
  let showCongrats = $state(false);
  let congratsRow = $state(-1);
  let toast = $state(/** @type {string | null} */ (null));

  // Plain refs (not reactive)
  /** @type {ReturnType<typeof setTimeout> | null} */
  let toastTimer = null;
  const celebratedRows = new Set();
  const notifiedWaitingRows = new Set();

  // Memoized per-row completeness — avoid 81×/render isRowComplete calls
  const rowCompleteness = $derived(
    grid && crossed.length
      ? grid.map((_, r) => isRowComplete(grid, crossed, r))
      : []
  );

  function dismissToast() {
    toast = null;
    if (toastTimer) {
      clearTimeout(toastTimer);
      toastTimer = null;
    }
  }

  /** @param {string} msg */
  function showToast(msg) {
    dismissToast();
    toast = msg;
    toastTimer = setTimeout(() => {
      toast = null;
    }, 5000);
  }

  // Initial load from localStorage. Re-runs if storagePrefix changes.
  $effect(() => {
    const savedGrid = loadGrid(storagePrefix);
    if (!savedGrid) return;

    grid = savedGrid;
    const savedCrossed =
      loadCrossedState(storagePrefix) ??
      savedGrid.map((row) => row.map(() => false));
    crossed = savedCrossed;

    celebratedRows.clear();
    notifiedWaitingRows.clear();
    for (let i = 0; i < savedGrid.length; i++) {
      if (isRowComplete(savedGrid, savedCrossed, i)) celebratedRows.add(i);
      if (getWaitingNumber(savedGrid, savedCrossed, i) !== null)
        notifiedWaitingRows.add(i);
    }
  });

  // Persist crossed state on change
  $effect(() => {
    if (crossed.length > 0) saveCrossedState(crossed, storagePrefix);
  });

  // Detect newly completed and waiting rows. Two passes prevent skipped resets.
  $effect(() => {
    if (!grid || crossed.length === 0) return;

    // Pass 1: at most one bingo popup per render
    for (let i = 0; i < grid.length; i++) {
      if (!celebratedRows.has(i) && isRowComplete(grid, crossed, i)) {
        celebratedRows.add(i);
        notifiedWaitingRows.add(i);
        congratsRow = i + 1;
        showCongrats = true;
        break;
      }
    }

    // Pass 2: update waiting state for every non-celebrated row
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
    if (grid && !confirm("Bạn có muốn tạo lại bảng không?")) return;
    const newGrid = generateGrid();
    const newCrossed = newGrid.map((row) => row.map(() => false));
    grid = newGrid;
    crossed = newCrossed;
    saveGrid(newGrid, storagePrefix);
    saveCrossedState(newCrossed, storagePrefix);
    celebratedRows.clear();
    notifiedWaitingRows.clear();
    dismissToast();
  }

  /**
   * @param {number} row
   * @param {number} col
   */
  function handleCellClick(row, col) {
    crossed = crossed.map((r, ri) =>
      ri === row ? r.map((v, ci) => (ci === col ? !v : v)) : r
    );
  }

  /** @param {KeyboardEvent} e */
  function onModalKeydown(e) {
    if (e.key === "Escape") showCongrats = false;
  }

  // Tân Tân physical card: 3 stacked 3x9 mini-cards with these labels
  // (top → bottom). Underlying 9x9 data is unchanged; this is purely
  // visual segmentation.
  const SECTIONS = /** @type {const} */ ([0, 3, 6]);
  const SECTION_LABELS = [
    "Tân Tân",
    "An khang thịnh vượng",
    "Tân Tân tốt nhất",
  ];
</script>

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
      {#each SECTIONS as startRow, sectionIdx (sectionIdx)}
        {#if sectionIdx > 0}
          <div class="section-divider" aria-hidden="true"></div>
        {/if}
        <div class="section-label">{SECTION_LABELS[sectionIdx]}</div>
        <div class="loto-grid">
          {#each grid.slice(startRow, startRow + 3).flat() as num, idx (idx)}
            {@const row = startRow + Math.floor(idx / 9)}
            {@const col = idx % 9}
            {@const hasNumber = num > 0}
            {@const isCrossed = hasNumber && !!crossed[row]?.[col]}
            {@const rowComplete = hasNumber && rowCompleteness[row]}

            {#if !hasNumber}
              <div
                aria-hidden="true"
                class="relative aspect-[3/5] border-r border-b border-slate-300/60 dark:border-slate-700/60"
                style:background-color="var(--empty-cell-bg)"
              ></div>
            {:else}
              <button
                type="button"
                aria-label="Số {num}{isCrossed ? ', đã đánh dấu' : ''}"
                aria-pressed={isCrossed}
                onclick={() => handleCellClick(row, col)}
                class="tan-tan-num relative flex items-center justify-center
                       aspect-[3/5] text-2xl sm:text-3xl
                       border-r border-b border-slate-300/60 dark:border-slate-700/60
                       transition-all select-none cursor-pointer
                       focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-400
                       {isCrossed
                         ? rowComplete
                           ? 'cell-crossed bg-emerald-100 dark:bg-emerald-900/40 text-emerald-600 dark:text-emerald-400'
                           : 'cell-crossed bg-red-50 dark:bg-red-950/30 text-red-500 dark:text-red-500'
                         : 'bg-white dark:bg-slate-800 text-black dark:text-slate-100 hover:bg-indigo-50 dark:hover:bg-indigo-950/30 hover:text-indigo-600 dark:hover:text-indigo-400'}"
              >
                {num}
              </button>
            {/if}
          {/each}
        </div>
      {/each}
    </div>

    {#if toast}
      <div
        role="status"
        aria-live="polite"
        class="absolute inset-0 flex items-center justify-center pointer-events-none z-10"
      >
        <button
          type="button"
          onclick={dismissToast}
          aria-label="Đóng thông báo"
          class="pointer-events-auto px-6 py-3 rounded-2xl bg-amber-500/90 dark:bg-amber-600/90 text-white text-xl sm:text-2xl font-black shadow-xl animate-toast"
        >
          {toast}
        </button>
      </div>
    {/if}
  </div>
{:else}
  <div class="text-center text-slate-400 dark:text-slate-500 py-20 text-sm">
    Nhấn "Tạo bảng mới" để bắt đầu chơi
  </div>
{/if}

{#if showCongrats}
  <div
    role="dialog"
    aria-modal="true"
    aria-labelledby="congrats-title"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm animate-fade-in"
  >
    <button
      type="button"
      aria-label="Đóng"
      onclick={() => (showCongrats = false)}
      onkeydown={onModalKeydown}
      class="absolute inset-0 cursor-default"
    ></button>
    <div
      class="relative mx-4 max-w-sm w-full rounded-3xl bg-white dark:bg-slate-800 p-8 text-center shadow-2xl animate-pop-in"
    >
      <div
        class="absolute -top-6 left-1/2 -translate-x-1/2 text-6xl animate-bounce-slow"
      >
        🎉
      </div>
      <div class="absolute top-2 left-4 text-2xl animate-spin-slow">✨</div>
      <div class="absolute top-2 right-4 text-2xl animate-spin-slow-reverse">
        🎊
      </div>

      <h2
        id="congrats-title"
        class="mt-6 text-3xl font-black bg-gradient-to-r from-amber-500 via-pink-500 to-purple-500 bg-clip-text text-transparent"
      >
        Kinh!
      </h2>
      <p class="mt-3 text-lg text-slate-600 dark:text-slate-300">
        Hàng <span class="font-bold text-pink-500">{congratsRow}</span> đã đầy đủ!
      </p>
      <p class="mt-1 text-sm text-slate-400 dark:text-slate-500">
        Hãy hô to "Kinh!" 🎶
      </p>
      <button
        onclick={() => (showCongrats = false)}
        class="mt-6 px-8 py-2.5 rounded-full font-semibold text-white
               bg-gradient-to-r from-pink-500 to-purple-500
               hover:from-pink-600 hover:to-purple-600
               active:scale-95 transition-all shadow-lg"
      >
        Tuyệt vời! 🥳
      </button>
    </div>
  </div>
{/if}
