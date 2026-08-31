<script>
  import { SvelteSet } from "svelte/reactivity";

  import {
    generateGrid,
    getWaitingNumber,
    isRowComplete,
    loadCrossedState,
    loadGrid,
    loadManualUnticks,
    saveCrossedState,
    saveGrid,
    saveManualUnticks,
  } from "$lib/game-logic.js";
  import { focusTrap } from "$lib/focus-trap.js";
  import { masterState } from "$lib/master-store.svelte.js";
  import { pushOverlay } from "$lib/overlay-history.js";
  import { applyMasterCalls } from "$lib/player-auto-cross.js";
  import { settings } from "$lib/settings-store.svelte.js";
  import { cancelPlayback, playBingo, playWaiting } from "$lib/voice.js";

  const STORAGE_PREFIX = "loto";
  const PREVIEW_CELLS = [...Array(27).keys()];

  function prefersReducedMotion() {
    return (
      typeof window !== "undefined" &&
      window.matchMedia?.("(prefers-reduced-motion: reduce)").matches
    );
  }

  let grid = $state(/** @type {number[][] | null} */ (null));
  let crossed = $state(/** @type {boolean[][]} */ ([]));
  // Numbers the user explicitly unticked AFTER an auto-cross; suppresses
  // re-cross on subsequent passes (e.g. regen replay). Manual re-ticks
  // remove from the set.
  /** @type {SvelteSet<number>} */
  const manualUnticks = new SvelteSet();
  let showCongrats = $state(false);
  let congratsRow = $state(-1);
  let celebrationTier = $state(/** @type {1 | 2} */ (1));
  let toast = $state(/** @type {string | null} */ (null));
  // Bumped on every showToast() call so `{#key toastId}` force-remounts the
  // toast node — otherwise a 2nd "Chờ" within 5s reuses the same DOM node,
  // the CSS fade-out animation never restarts, and the node (still
  // pointer-events-auto) sits invisible-but-clickable over the board.
  let toastId = $state(0);

  // 12 confetti emoji indices. Stable per-render — values don't matter,
  // only the count drives the {#each}.
  const CONFETTI = Array.from({ length: 12 }, (_, i) => i);
  // Hội-chợ flavour added (lantern, bamboo, chopsticks) so the
  // celebration set reads as Vietnamese fair, not generic party.
  const CONFETTI_EMOJI = ["🎊", "✨", "🎉", "🥳", "🥢", "🎋", "🏮"];

  /** @param {Iterable<number>} values */
  function replaceManualUnticks(values) {
    manualUnticks.clear();
    for (const value of values) manualUnticks.add(value);
  }

  // Timer handle stays plain; row trackers are reactive so Svelte can observe Set reads.
  /** @type {ReturnType<typeof setTimeout> | null} */
  let toastTimer = null;
  const celebratedRows = new SvelteSet();
  const notifiedWaitingRows = new SvelteSet();
  // How many entries of masterState.called we've already replayed.
  // Advances strictly even on no-op passes so a single draw never
  // re-fires (auto-cross effect dedup). Resets to 0 when the host
  // starts a new game (called → []).
  let lastHandledIndex = $state(0);
  // Tracks called.length across reactivity ticks so we can detect a
  // master "Ván mới" (length transitions from >0 → 0) and clear
  // player crossed in both mode per locked product decision.
  let prevCalledLen = $state(0);

  // Memoized per-row completeness — avoid 81×/render isRowComplete calls.
  // Reads `grid` into a local `g` first: TS can't carry the null-narrowing
  // from the ternary condition into the `.map()` callback closure for a
  // mutable outer binding, even though nothing reassigns `grid` mid-eval.
  const rowCompleteness = $derived.by(() => {
    const g = grid;
    return g && crossed.length
      ? g.map((_, r) => isRowComplete(g, crossed, r))
      : [];
  });

  // Per-row "Chờ" flag — one cell away AND not already complete. Reuses
  // rowCompleteness so we don't re-walk the row twice.
  const waitingRows = $derived.by(() => {
    const g = grid;
    return g && crossed.length
      ? g.map(
          (_, r) =>
            !rowCompleteness[r] &&
            getWaitingNumber(g, crossed, r) !== null,
        )
      : [];
  });

  // "row,col" keys of cells holding the awaited number for any waiting
  // row. Drives the per-cell pulse animation so the user can spot which
  // number to find without reading text. Multiple rows may be waiting
  // simultaneously — each gets its own pulsing cell.
  const waitingCells = $derived.by(() => {
    /** @type {Set<string>} */
    const set = new SvelteSet();
    if (!grid || crossed.length === 0) return set;
    for (let r = 0; r < grid.length; r++) {
      if (rowCompleteness[r]) continue;
      const num = getWaitingNumber(grid, crossed, r);
      if (num === null) continue;
      const c = grid[r].indexOf(num);
      if (c >= 0) set.add(`${r},${c}`);
    }
    return set;
  });

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
    toastId++;
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
    replaceManualUnticks(loadManualUnticks(STORAGE_PREFIX));

    celebratedRows.clear();
    notifiedWaitingRows.clear();
    for (let i = 0; i < savedGrid.length; i++) {
      if (isRowComplete(savedGrid, savedCrossed, i)) celebratedRows.add(i);
      if (getWaitingNumber(savedGrid, savedCrossed, i) !== null)
        notifiedWaitingRows.add(i);
    }
    // No cursor baseline here on purpose. We let `lastHandledIndex` stay
    // at its $state init (0) so the auto-cross effect re-applies the full
    // master history. Already-crossed cells are skipped by
    // `findUncrossedCell`, so reload is idempotent. This avoids racing
    // mount order between PlayerBoard and MasterPanel mode toggles.
  });

  // Persist manualUnticks whenever it changes.
  $effect(() => {
    if (grid) saveManualUnticks(manualUnticks, STORAGE_PREFIX);
  });

  // Persist crossed state on change
  $effect(() => {
    if (crossed.length > 0) saveCrossedState(crossed, STORAGE_PREFIX);
  });

  // Detect newly completed and waiting rows. Two passes prevent skipped resets.
  $effect(() => {
    // Captured into `g` so the null-narrowing survives the `.some()`
    // callback below (TS drops narrowing of a mutable outer binding once
    // it's referenced inside a nested function expression).
    const g = grid;
    if (!g || crossed.length === 0) return;

    // The master takes over announcer duties in "both" mode, so its
    // voice flag also drives Chờ/Kinh. Solo players keep their own flag.
    const announce =
      settings.voiceEnabledPlayer ||
      (settings.voiceEnabledMaster && settings.mode === "both");

    // Set when pass 1 fires playBingo() this run, so pass 2 knows not to
    // cut it off — playWaiting()/playBingo() both start with
    // cancelPlayback(), so an unconditional pass-2 call would silence the
    // win announcement in the exact "completed one row, one away on
    // another" run where it matters most.
    let announcedBingo = false;

    // Pass 1: at most one bingo popup per render
    for (let i = 0; i < g.length; i++) {
      if (!celebratedRows.has(i) && isRowComplete(g, crossed, i)) {
        celebratedRows.add(i);
        notifiedWaitingRows.add(i);
        congratsRow = i + 1;
        showCongrats = true;
        // Tier 2 confetti: 2nd bingo, OR 1st bingo while another row
        // is one cell away. The previous "3+ bingos" threshold rarely
        // fired on a 9-row card so most wins felt under-celebrated.
        const hasActiveCho = g.some(
          (_, r) =>
            !celebratedRows.has(r) &&
            getWaitingNumber(g, crossed, r) !== null,
        );
        celebrationTier =
          celebratedRows.size >= 2 ||
          (celebratedRows.size >= 1 && hasActiveCho)
            ? 2
            : 1;
        if (announce) {
          playBingo();
          announcedBingo = true;
        }
        break;
      }
    }

    // Pass 2: update waiting state for every non-celebrated row
    for (let i = 0; i < g.length; i++) {
      if (celebratedRows.has(i)) continue;
      const waitNum = getWaitingNumber(g, crossed, i);
      if (waitNum !== null && !notifiedWaitingRows.has(i)) {
        notifiedWaitingRows.add(i);
        showToast(`Chờ ${waitNum}`);
        if (announce && !announcedBingo) playWaiting(waitNum);
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

  // Bingo modal dismissal. Window-level Escape so it works regardless of
  // which element holds focus (the inline backdrop button rarely does), plus
  // a history sentinel so the Android back gesture closes the modal instead
  // of leaving the app.
  $effect(() => {
    if (!showCongrats) return;
    const dispose = pushOverlay(() => (showCongrats = false));
    /** @param {KeyboardEvent} e */
    const onKey = (e) => {
      if (e.key === "Escape") showCongrats = false;
    };
    window.addEventListener("keydown", onKey);
    return () => {
      window.removeEventListener("keydown", onKey);
      // No-op when back already popped the sentinel; pops it otherwise.
      dispose();
    };
  });

  // Auto-cross on master draws (only in "both" mode). Reads
  // `masterState.called` directly — full history, no bus, no 1ms
  // collision risk.
  //
  // Self-write safety: this effect reads `crossed` and `lastHandledIndex`
  // and may write both. Convergence is guaranteed because (a) writes are
  // gated on `result.changed` / cursor-mismatch and (b) on re-run after
  // a write, `applyMasterCalls`'s cursor-at-length short-circuit returns
  // a no-op result. Don't unguard the writes without re-testing.
  $effect(() => {
    const result = applyMasterCalls({
      grid,
      crossed,
      called: masterState.called,
      lastHandledIndex,
      manualUnticks,
      mode: settings.mode,
    });
    if (result.lastHandledIndex !== lastHandledIndex) {
      lastHandledIndex = result.lastHandledIndex;
    }
    if (result.changed) crossed = result.crossed;
  });

  // Detect master "Ván mới" — `called` length transitions from >0 → 0.
  // Per locked product decision, force-clear player crossed (and
  // manualUnticks) in both mode so a fresh round starts truly fresh.
  //
  // Self-write safety: explicit early-return when `len === prev` avoids
  // a same-value write to `prevCalledLen` that could spuriously re-run.
  $effect(() => {
    const len = masterState.called.length;
    const prev = prevCalledLen;
    if (len === prev) return;
    prevCalledLen = len;
    if (prev > 0 && len === 0 && settings.mode === "both" && grid) {
      crossed = grid.map((row) => row.map(() => false));
      manualUnticks.clear();
      lastHandledIndex = 0;
      celebratedRows.clear();
      notifiedWaitingRows.clear();
    }
  });

  function handleGenerate() {
    if (grid && !confirm("Bạn có muốn tạo lại bảng không?")) return;
    cancelPlayback();
    const newGrid = generateGrid();
    let newCrossed = newGrid.map((row) => row.map(() => false));
    manualUnticks.clear();
    // Replay master's called[] onto the fresh grid so the host doesn't
    // restart from zero when they regenerate mid-game (locked decision).
    if (settings.mode === "both") {
      const result = applyMasterCalls({
        grid: newGrid,
        crossed: newCrossed,
        called: masterState.called,
        lastHandledIndex: 0,
        manualUnticks,
        mode: "both",
      });
      newCrossed = result.crossed;
      lastHandledIndex = result.lastHandledIndex;
    } else {
      lastHandledIndex = masterState.called.length;
    }
    grid = newGrid;
    crossed = newCrossed;
    saveGrid(newGrid, STORAGE_PREFIX);
    saveCrossedState(newCrossed, STORAGE_PREFIX);
    saveManualUnticks(manualUnticks, STORAGE_PREFIX);
    celebratedRows.clear();
    notifiedWaitingRows.clear();
    dismissToast();
    showCongrats = false;
  }

  function handleClear() {
    if (!grid) return;
    const hasMarks = crossed.some((row) => row.some(Boolean));
    if (hasMarks && !confirm("Bạn có muốn xoá tất cả đánh dấu không?")) return;
    cancelPlayback();
    let cleared = grid.map((row) => row.map(() => false));
    manualUnticks.clear();
    // In both mode, immediately replay master's called[] (locked
    // decision: clear → re-cross all currently-called numbers).
    if (settings.mode === "both") {
      const result = applyMasterCalls({
        grid,
        crossed: cleared,
        called: masterState.called,
        lastHandledIndex: 0,
        manualUnticks,
        mode: "both",
      });
      cleared = result.crossed;
      lastHandledIndex = result.lastHandledIndex;
    } else {
      lastHandledIndex = masterState.called.length;
    }
    crossed = cleared;
    celebratedRows.clear();
    notifiedWaitingRows.clear();
    dismissToast();
    showCongrats = false;
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
    if (!grid) return;
    const num = grid[row][col];
    const wasCrossed = crossed[row]?.[col] === true;
    const willBeCrossed = !wasCrossed;
    // Track manual unticks of called numbers so future regen/clear
    // replays skip them. Untracking uncalled numbers would pollute the
    // set with no visible effect — auto-cross only acts on called nums.
    if (num > 0 && masterState.called.includes(num)) {
      if (wasCrossed && !willBeCrossed) manualUnticks.add(num);
      else if (!wasCrossed && willBeCrossed) manualUnticks.delete(num);
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
           bg-rose-600 hover:bg-rose-700
           active:scale-95 transition-all shadow-md"
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
                {@const isWaitingCell = hasNumber && waitingCells.has(`${row},${col}`)}

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
                    aria-label="Số {num}{isCrossed ? ', đã đánh dấu' : ''}{isWaitingCell ? ', đang chờ' : ''}"
                    aria-pressed={isCrossed}
                    onclick={() => handleCellClick(row, col)}
                    class="tan-tan-num board-num relative flex items-center justify-center
                           aspect-[3/4] sm:aspect-[3/5]
                           border border-slate-400/50 dark:border-slate-600/40
                           transition-all select-none cursor-pointer active:scale-90
                           focus:outline-none focus:ring-2 focus:ring-inset focus:ring-rose-400
                           {isWaitingCell ? 'cell-waiting' : ''}
                           {isCrossed
                             ? rowComplete
                               ? 'bg-sky-100 dark:bg-sky-900/60 text-sky-700 dark:text-sky-200'
                               : 'bg-red-50 dark:bg-red-950/40 text-red-700 dark:text-red-300'
                             : 'bg-white dark:bg-slate-800 text-black dark:text-slate-100 hover:bg-rose-50 dark:hover:bg-rose-950/30 hover:text-rose-600 dark:hover:text-rose-400'}"
                  >
                    {num}
                    {#if isCrossed}
                      <span
                        aria-hidden="true"
                        class="cell-crossed {rowComplete ? 'cell-crossed-win' : ''}"
                      ></span>
                    {/if}
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
      <!-- Centered overlay over the card. Lower opacity + pointer-events
           routing keeps cell taps unblocked while the chip is visible.
           Cell-pulse on the awaited number is the persistent visual
           anchor; this chip is the textual fresh-event cue.
           {#key toastId} forces a full remount on every showToast() call
           so the 5s CSS fade-out always restarts from a fresh node — see
           toastId's declaration for why a reused node was a real bug. -->
      {#key toastId}
        <div
          role="status"
          aria-live="polite"
          class="absolute inset-0 z-10 flex items-center justify-center pointer-events-none"
        >
          <button
            type="button"
            onclick={dismissToast}
            aria-label="Đóng thông báo"
            class="pointer-events-auto px-6 py-3 rounded-2xl bg-amber-500/75 dark:bg-amber-600/75 text-white text-2xl sm:text-3xl font-black shadow-2xl animate-toast backdrop-blur-sm"
          >
            {toast}
          </button>
        </div>
      {/key}
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
        {#each PREVIEW_CELLS as i (i)}
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
      <span class="font-semibold text-rose-600 dark:text-rose-400 not-italic">"Tạo bảng mới"</span>
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
    use:focusTrap
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
        class="mt-6 text-4xl font-black text-amber-600 dark:text-amber-400"
      >
        Kinh!
      </h2>
      <p class="mt-3 text-base text-slate-700 dark:text-slate-200">
        Hàng
        <span class="block text-5xl sm:text-6xl font-black text-rose-600 dark:text-rose-400 my-1 tabular-nums">
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
               bg-rose-600 hover:bg-rose-700
               active:scale-95 transition-all shadow-md"
      >
        Tuyệt vời! 🥳
      </button>
    </div>
  </div>
{/if}
