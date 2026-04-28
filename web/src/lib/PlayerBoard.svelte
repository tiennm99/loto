<script>
  import { processAutoTick } from "$lib/auto-tick.js";
  import { bus, resetBus } from "$lib/call-bus.svelte.js";
  import {
    generateGrid,
    getWaitingNumber,
    isRowComplete,
    loadCrossedState,
    loadGrid,
    saveCrossedState,
    saveGrid,
  } from "$lib/game-logic.js";
  import { settings } from "$lib/settings-store.svelte.js";
  import { cancelPlayback, playBingo, playWaiting } from "$lib/voice.js";

  const STORAGE_PREFIX = "loto";

  function prefersReducedMotion() {
    return (
      typeof window !== "undefined" &&
      window.matchMedia?.("(prefers-reduced-motion: reduce)").matches
    );
  }

  let grid = $state(/** @type {number[][] | null} */ (null));
  let crossed = $state(/** @type {boolean[][]} */ ([]));
  let showCongrats = $state(false);
  let congratsRow = $state(-1);
  let celebrationTier = $state(/** @type {1 | 2} */ (1));
  let toast = $state(/** @type {string | null} */ (null));

  // 12 confetti emoji indices. Stable per-render — values don't matter,
  // only the count drives the {#each}.
  const CONFETTI = Array.from({ length: 12 }, (_, i) => i);
  // Hội-chợ flavour added (lantern, bamboo, chopsticks) so the
  // celebration set reads as Vietnamese fair, not generic party.
  const CONFETTI_EMOJI = ["🎊", "✨", "🎉", "🥳", "🥢", "🎋", "🏮"];

  // Plain refs (not reactive)
  /** @type {ReturnType<typeof setTimeout> | null} */
  let toastTimer = null;
  const celebratedRows = new Set();
  const notifiedWaitingRows = new Set();
  // Last bus draw we acted on. Compared against bus.lastDrawn.at so the
  // auto-tick effect only fires on a NEW draw — re-runs caused by
  // crossed/grid changes (manual untick, clear, regen) skip cleanly.
  let lastHandledDrawAt = 0;

  // Memoized per-row completeness — avoid 81×/render isRowComplete calls
  const rowCompleteness = $derived(
    grid && crossed.length
      ? grid.map((_, r) => isRowComplete(grid, crossed, r))
      : []
  );

  // Per-row "Chờ" flag — one cell away AND not already complete. Reuses
  // rowCompleteness so we don't re-walk the row twice.
  const waitingRows = $derived(
    grid && crossed.length
      ? grid.map(
          (_, r) =>
            !rowCompleteness[r] &&
            getWaitingNumber(grid, crossed, r) !== null,
        )
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

  // Initial load from localStorage. Re-runs if STORAGE_PREFIX changes.
  $effect(() => {
    const savedGrid = loadGrid(STORAGE_PREFIX);
    if (!savedGrid) return;

    grid = savedGrid;
    const savedCrossed =
      loadCrossedState(STORAGE_PREFIX) ??
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
    if (crossed.length > 0) saveCrossedState(crossed, STORAGE_PREFIX);
  });

  // Detect newly completed and waiting rows. Two passes prevent skipped resets.
  $effect(() => {
    if (!grid || crossed.length === 0) return;

    // The master takes over announcer duties in "both" mode, so its
    // voice flag also drives Chờ/Kinh. Solo players keep their own flag.
    const announce =
      settings.voiceEnabledPlayer ||
      (settings.voiceEnabledMaster && settings.mode === "both");

    // Pass 1: at most one bingo popup per render
    for (let i = 0; i < grid.length; i++) {
      if (!celebratedRows.has(i) && isRowComplete(grid, crossed, i)) {
        celebratedRows.add(i);
        notifiedWaitingRows.add(i);
        congratsRow = i + 1;
        showCongrats = true;
        // Tier 2 confetti: 2nd bingo, OR 1st bingo while another row
        // is one cell away. The previous "3+ bingos" threshold rarely
        // fired on a 9-row card so most wins felt under-celebrated.
        const hasActiveCho = grid.some(
          (_, r) =>
            !celebratedRows.has(r) &&
            getWaitingNumber(grid, crossed, r) !== null,
        );
        celebrationTier =
          celebratedRows.size >= 2 ||
          (celebratedRows.size >= 1 && hasActiveCho)
            ? 2
            : 1;
        if (announce) playBingo();
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
        if (announce) playWaiting(waitNum);
      } else if (waitNum === null && notifiedWaitingRows.has(i)) {
        notifiedWaitingRows.delete(i);
      }
    }
  });

  // Stop any in-flight clip + dismiss any pending toast on unmount so
  // audio and timers don't outlive the board.
  $effect(() => () => {
    cancelPlayback();
    dismissToast();
  });

  // Bingo modal: window-level Escape so it works regardless of which
  // element holds focus (the inline backdrop button rarely does).
  $effect(() => {
    if (!showCongrats) return;
    /** @param {KeyboardEvent} e */
    const onKey = (e) => {
      if (e.key === "Escape") showCongrats = false;
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });

  // Auto-tick on master draw (only in "both" mode). Reads `bus.lastDrawn`
  // reactively; the dedup-by-`at` invariant lives in `processAutoTick` —
  // see `auto-tick.test.js` for the full case matrix.
  $effect(() => {
    const result = processAutoTick({
      grid,
      crossed,
      lastDraw: bus.lastDrawn,
      lastHandledAt: lastHandledDrawAt,
      mode: settings.mode,
    });
    lastHandledDrawAt = result.lastHandledAt;
    if (result.changed) crossed = result.crossed;
  });

  function handleGenerate() {
    if (grid && !confirm("Bạn có muốn tạo lại bảng không?")) return;
    cancelPlayback();
    const newGrid = generateGrid();
    const newCrossed = newGrid.map((row) => row.map(() => false));
    grid = newGrid;
    crossed = newCrossed;
    saveGrid(newGrid, STORAGE_PREFIX);
    saveCrossedState(newCrossed, STORAGE_PREFIX);
    celebratedRows.clear();
    notifiedWaitingRows.clear();
    dismissToast();
    showCongrats = false;
    resetBus();
  }

  function handleClear() {
    if (!grid) return;
    const hasMarks = crossed.some((row) => row.some(Boolean));
    if (hasMarks && !confirm("Bạn có muốn xoá tất cả đánh dấu không?")) return;
    cancelPlayback();
    crossed = grid.map((row) => row.map(() => false));
    celebratedRows.clear();
    notifiedWaitingRows.clear();
    dismissToast();
    showCongrats = false;
    resetBus();
  }

  /**
   * @param {number} row
   * @param {number} col
   */
  function handleCellClick(row, col) {
    if (
      typeof navigator !== "undefined" &&
      navigator.vibrate &&
      !prefersReducedMotion()
    ) {
      navigator.vibrate(10);
    }
    crossed = crossed.map((r, ri) =>
      ri === row ? r.map((v, ci) => (ci === col ? !v : v)) : r
    );
  }

  // Tân Tân physical card: 3 stacked 3x9 mini-cards with these labels
  // (top → bottom). Underlying 9x9 data is unchanged; this is purely
  // visual segmentation.
  const SECTIONS = /** @type {const} */ ([0, 3, 6]);
  const SECTION_LABELS = [
    "Lô tô",
    "TN1 (2014-2017)",
    "Độc-Đỉnh-Điên",
  ];

  // Persistent ring on the section band when any of its 3 rows is
  // waiting on a single number. Reduces reliance on the 5s toast.
  const sectionHasWaiting = $derived(
    SECTIONS.map((startRow) =>
      waitingRows.slice(startRow, startRow + 3).some(Boolean)
    )
  );
</script>

<div class="flex flex-wrap justify-center gap-3 mb-6">
  <button
    onclick={handleGenerate}
    class="px-8 py-3 rounded-full font-semibold text-white
           bg-gradient-to-r from-indigo-500 to-purple-500
           hover:from-indigo-600 hover:to-purple-600
           active:scale-95 transition-all shadow-lg shadow-indigo-500/25"
  >
    Tạo bảng mới
  </button>
  {#if grid}
    <button
      onclick={handleClear}
      class="px-6 py-3 rounded-full font-semibold
             text-slate-700 dark:text-slate-200
             bg-white dark:bg-slate-800
             border-2 border-slate-300 dark:border-slate-600
             hover:bg-slate-50 dark:hover:bg-slate-700
             active:scale-95 transition-all"
    >
      Xoá đánh dấu
    </button>
  {/if}
</div>

{#if grid}
  <div class="relative">
    <div
      aria-label="Bảng lô tô"
      class="rounded-md overflow-hidden shadow-xl shadow-slate-200/50 dark:shadow-black/30"
    >
      <div class="flex">
        <!-- Left frame -->
        <div class="section-divider-vertical" aria-hidden="true"></div>
        <div class="flex-1">
          {#each SECTIONS as startRow, sectionIdx (sectionIdx)}
            <div
              class="section-label {sectionHasWaiting[sectionIdx] ? 'section-label-waiting' : ''}"
            >
              {SECTION_LABELS[sectionIdx]}
            </div>
            <div class="loto-grid">
              {#each grid.slice(startRow, startRow + 3).flat() as num, idx (idx)}
                {@const row = startRow + Math.floor(idx / 9)}
                {@const col = idx % 9}
                {@const hasNumber = num > 0}
                {@const isCrossed = hasNumber && !!crossed[row]?.[col]}
                {@const rowComplete = hasNumber && rowCompleteness[row]}

                {#if !hasNumber}
                  <!-- Empty cell. Dark-mode dim is an overlay div, not a CSS
                       filter, so we don't accidentally create a stacking
                       context on every cell. -->
                  <div
                    aria-hidden="true"
                    class="relative aspect-[3/4] sm:aspect-[3/5] border border-slate-400/50 dark:border-slate-600/40"
                    style:background-color="var(--empty-cell-bg)"
                  >
                    <span
                      aria-hidden="true"
                      class="hidden dark:block absolute inset-0 bg-black/15 pointer-events-none"
                    ></span>
                  </div>
                {:else}
                  <button
                    type="button"
                    aria-label="Số {num}{isCrossed ? ', đã đánh dấu' : ''}"
                    aria-pressed={isCrossed}
                    onclick={() => handleCellClick(row, col)}
                    class="tan-tan-num relative flex items-center justify-center
                           aspect-[3/4] sm:aspect-[3/5]
                           text-xl sm:text-2xl md:text-3xl
                           border border-slate-400/50 dark:border-slate-600/40
                           transition-all select-none cursor-pointer active:scale-90
                           focus:outline-none focus:ring-2 focus:ring-inset focus:ring-indigo-400
                           {isCrossed
                             ? rowComplete
                               ? 'cell-crossed cell-crossed-win bg-emerald-100 dark:bg-emerald-900/60 text-emerald-700 dark:text-emerald-200'
                               : 'cell-crossed bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-300'
                             : 'bg-white dark:bg-slate-800 text-black dark:text-slate-100 hover:bg-indigo-50 dark:hover:bg-indigo-950/30 hover:text-indigo-600 dark:hover:text-indigo-400'}"
                  >
                    {num}
                  </button>
                {/if}
              {/each}
            </div>
          {/each}
          <!-- Decorative bottom band (matches the section-label hatch).
               Attribution lives in the page footer, not here, to avoid
               duplicating "Made by miti99". -->
          <div class="section-label" aria-hidden="true"></div>
        </div>
        <!-- Right frame -->
        <div class="section-divider-vertical" aria-hidden="true"></div>
      </div>
    </div>

    {#if toast}
      <!-- Anchor toast ABOVE the grid (not centered over playable cells)
           so it announces without blocking row taps. Pointer-events
           still routed to the dismiss button only. -->
      <div
        role="status"
        aria-live="polite"
        class="absolute left-1/2 -translate-x-1/2 -top-3 sm:-top-4 z-10 pointer-events-none"
      >
        <button
          type="button"
          onclick={dismissToast}
          aria-label="Đóng thông báo"
          class="pointer-events-auto px-5 py-2.5 rounded-2xl bg-amber-500/95 dark:bg-amber-600/95 text-white text-lg sm:text-xl font-black shadow-xl animate-toast"
        >
          {toast}
        </button>
      </div>
    {/if}
  </div>
{:else}
  <div class="text-center py-10">
    <div
      aria-hidden="true"
      class="mx-auto max-w-xs opacity-30 pointer-events-none mb-6
             rounded-md overflow-hidden border border-slate-300 dark:border-slate-600"
    >
      <div class="grid grid-cols-9 gap-px bg-slate-300 dark:bg-slate-700">
        {#each Array(27) as _, i (i)}
          {@const filled = i % 3 === 0}
          <div
            class="aspect-square text-[0.55rem] flex items-center justify-center
                   text-black {filled ? 'bg-white dark:bg-slate-800 dark:text-slate-100' : ''}"
            style:background-color={filled ? undefined : "var(--empty-cell-bg)"}
          >
            {filled ? ((i * 7) % 90) + 1 : ""}
          </div>
        {/each}
      </div>
    </div>
    <p class="text-base text-slate-600 dark:text-slate-300 italic">
      Nhấn
      <span class="font-semibold text-rose-500 dark:text-rose-400 not-italic">"Tạo bảng mới"</span>
      để bắt đầu chơi
    </p>
    <p class="text-sm text-slate-500 dark:text-slate-400 mt-1.5">
      🎫 Chúc cả nhà một ván vui vẻ
    </p>
  </div>
{/if}

{#if showCongrats && celebrationTier >= 2}
  <!-- z-[60] sits above the bingo modal (z-50) so confetti reads through
       the backdrop blur instead of being dimmed by it. -->
  <div
    aria-hidden="true"
    class="fixed inset-0 z-[60] pointer-events-none overflow-hidden"
  >
    {#each CONFETTI as i (i)}
      <span
        class="confetti"
        style:--x="{(i * 8.3 + (i % 3) * 11) % 100}%"
        style:--delay="{(i * 37) % 400}ms"
        style:--rot="{(i * 67) % 360}deg"
        style:--size="{(1.5 + ((i * 13) % 11) / 10).toFixed(2)}rem"
      >
        {CONFETTI_EMOJI[i % CONFETTI_EMOJI.length]}
      </span>
    {/each}
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
        class="mt-6 text-4xl font-black bg-gradient-to-r from-amber-500 via-pink-500 to-purple-500 bg-clip-text text-transparent"
      >
        Kinh!
      </h2>
      <p class="mt-3 text-base text-slate-700 dark:text-slate-200">
        Hàng
        <span class="block text-5xl sm:text-6xl font-black text-pink-500 dark:text-pink-400 my-1 tabular-nums">
          {congratsRow}
        </span>
        đã đầy đủ!
      </p>
      <p class="mt-1.5 text-sm text-slate-500 dark:text-slate-400">
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
