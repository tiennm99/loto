<script module>
  const STORAGE_KEY = "loto_master";

  /**
   * 11x9 board, aligned by ones-digit. Row = ones digit, col = tens digit.
   * Col 0 holds 1..9, col 8 holds 80..90 (90 sits alone in row 10 col 8).
   * Empty slots = 0.
   * @returns {number[][]}
   */
  function buildBoard() {
    /** @type {number[][]} */
    const board = [];
    for (let row = 0; row < 11; row++) {
      /** @type {number[]} */
      const cells = [];
      for (let col = 0; col < 9; col++) {
        let num = 0;
        if (row === 10) {
          if (col === 8) num = 90;
        } else if (row === 0) {
          if (col > 0) num = col * 10;
        } else {
          num = col === 0 ? row : col * 10 + row;
        }
        cells.push(num);
      }
      board.push(cells);
    }
    return board;
  }

  /** @returns {{ called: number[], remaining: number[] }} */
  function createFreshState() {
    const all = Array.from({ length: 90 }, (_, i) => i + 1);
    for (let i = all.length - 1; i > 0; i--) {
      const j = Math.floor(Math.random() * (i + 1));
      [all[i], all[j]] = [all[j], all[i]];
    }
    return { called: [], remaining: all };
  }

  /** @param {{ called: number[], remaining: number[] }} state */
  function saveState(state) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
    } catch {
      /* ignore */
    }
  }

  function loadState() {
    try {
      const data = localStorage.getItem(STORAGE_KEY);
      if (!data) return null;
      return JSON.parse(data);
    } catch {
      return null;
    }
  }

  const BOARD = Object.freeze(buildBoard().map((row) => Object.freeze(row)));
  const BOARD_FLAT = Object.freeze(BOARD.flatMap((r) => r));
</script>

<script>
  import { settings } from "$lib/settings-store.svelte.js";

  let state = $state(
    /** @type {{called: number[], remaining: number[]} | null} */ (null),
  );
  let lastCalled = $state(/** @type {number | null} */ (null));
  let autoRunning = $state(false);

  $effect(() => {
    const saved = loadState();
    if (saved && saved.called.length > 0) {
      state = saved;
      lastCalled = saved.called[saved.called.length - 1];
    }
  });

  $effect(() => {
    if (state) saveState(state);
  });

  // Map number -> 1-based draw order for fast Kinh! verification.
  const callOrder = $derived(
    new Map((state?.called ?? []).map((n, i) => [n, i + 1])),
  );

  // Auto-call interval. Single $effect that depends on autoRunning,
  // settings.autoCallSpeed, AND settings.autoCallEnabled — Svelte tears
  // down + re-arms cleanly whenever any of them change. Includes the
  // "user disabled auto setting mid-run" auto-stop as part of the same
  // dependency tracking.
  $effect(() => {
    if (!autoRunning || !settings.autoCallEnabled) {
      autoRunning = false;
      return;
    }
    const ms = settings.autoCallSpeed * 1000;
    const id = setInterval(() => {
      if (!state || state.remaining.length === 0) {
        autoRunning = false;
        return;
      }
      handleDrawNext();
    }, ms);
    return () => clearInterval(id);
  });

  function handleNewGame() {
    if (state && !confirm("Bạn có muốn tạo ván mới không?")) return;
    autoRunning = false;
    state = createFreshState();
    lastCalled = null;
  }

  function handleDrawNext() {
    if (!state || state.remaining.length === 0) return;
    const next = state.remaining[0];
    state = {
      called: [...state.called, next],
      remaining: state.remaining.slice(1),
    };
    lastCalled = next;
  }

  function toggleAuto() {
    if (!state || state.remaining.length === 0) return;
    autoRunning = !autoRunning;
  }
</script>

<!-- Controls -->
<div class="flex justify-center gap-3 mb-6 flex-wrap">
  <button
    onclick={handleNewGame}
    class="px-8 py-4 rounded-full font-semibold text-white text-lg
           bg-gradient-to-r from-orange-500 to-red-500
           hover:from-orange-600 hover:to-red-600
           active:scale-95 transition-all shadow-lg shadow-orange-500/25"
  >
    Ván mới
  </button>
  {#if state && state.remaining.length > 0}
    {#if settings.autoCallEnabled}
      <button
        onclick={toggleAuto}
        class="px-10 py-4 rounded-full font-semibold text-white text-lg
               bg-gradient-to-r {autoRunning
                 ? 'from-red-500 to-rose-500 hover:from-red-600 hover:to-rose-600 shadow-red-500/25'
                 : 'from-emerald-500 to-teal-500 hover:from-emerald-600 hover:to-teal-600 shadow-emerald-500/25'}
               active:scale-95 transition-all shadow-lg"
      >
        {autoRunning ? "Dừng" : "Bắt đầu"}
      </button>
    {:else}
      <button
        onclick={handleDrawNext}
        class="px-10 py-4 rounded-full font-semibold text-white text-lg
               bg-gradient-to-r from-emerald-500 to-teal-500
               hover:from-emerald-600 hover:to-teal-600
               active:scale-95 transition-all shadow-lg shadow-emerald-500/25"
      >
        Xổ số
      </button>
    {/if}
  {/if}
</div>

{#if settings.autoCallEnabled && state && state.remaining.length > 0}
  <div
    class="text-center text-xs text-slate-500 dark:text-slate-400 mb-4 tabular-nums"
  >
    Tự động: {settings.autoCallSpeed}s/số
  </div>
{/if}

<!-- Current number -->
{#if lastCalled}
  {@const lastIsLow = lastCalled <= 49}
  <div class="flex flex-col items-center mb-6">
    <div
      class="text-xs uppercase tracking-widest text-slate-400 dark:text-slate-500 mb-1"
    >
      Số vừa xổ
    </div>
    <div
      class="w-24 h-24 sm:w-28 sm:h-28 rounded-full
             bg-amber-50 dark:bg-amber-100
             border-[6px] sm:border-[7px]
             flex items-center justify-center
             shadow-xl
             {lastIsLow
               ? 'border-pink-500 dark:border-pink-400 shadow-pink-500/30'
               : 'border-emerald-500 dark:border-emerald-400 shadow-emerald-500/30'}"
    >
      <span
        class="text-5xl sm:text-6xl font-black tabular-nums
               {lastIsLow
                 ? 'text-pink-500 dark:text-pink-400'
                 : 'text-emerald-500 dark:text-emerald-400'}"
      >
        {lastCalled}
      </span>
    </div>
    {#if state}
      <div class="mt-2 text-xs text-slate-400 dark:text-slate-500">
        Đã xổ: {state.called.length}/90 &middot; Còn lại: {state.remaining
          .length}
      </div>
    {/if}
  </div>
{/if}

<!-- Called history -->
{#if state && state.called.length > 0}
  <div class="mb-6 px-1">
    <div class="text-xs text-slate-400 dark:text-slate-500 mb-1">
      Thứ tự đã xổ:
    </div>
    <div class="flex flex-wrap gap-1.5">
      {#each state.called as num, i (i)}
        {@const isLow = num <= 49}
        <span
          class="inline-flex items-center justify-center w-8 h-8 sm:w-9 sm:h-9
                 text-sm sm:text-base font-black tabular-nums rounded-full
                 border-[3px] bg-amber-50 dark:bg-amber-100
                 {isLow
                   ? 'border-pink-500 dark:border-pink-400 text-pink-500 dark:text-pink-400'
                   : 'border-emerald-500 dark:border-emerald-400 text-emerald-500 dark:text-emerald-400'}"
        >
          {num}
        </span>
      {/each}
    </div>
  </div>
{/if}

<!-- 11x9 master tracking board -->
{#if state}
  <div
    aria-label="Bảng theo dõi số đã xổ"
    class="rounded-2xl overflow-hidden shadow-xl shadow-slate-200/50 dark:shadow-black/30 border border-slate-200 dark:border-slate-700"
  >
    <div class="master-grid">
      {#each BOARD_FLAT as num, idx (idx)}
        {@const hasNumber = num > 0}
        {@const order = hasNumber ? callOrder.get(num) : undefined}
        {@const isCalled = order !== undefined}
        {@const isLast = isCalled && num === lastCalled}
        {@const isLow = hasNumber && num <= 49}
        <div
          style:background-color={hasNumber ? null : "var(--empty-cell-bg)"}
          class="relative flex items-center justify-center
                 aspect-square
                 border-r border-b border-slate-200/80 dark:border-slate-700/60
                 bg-white dark:bg-slate-800 select-none
                 {isLast ? 'z-10' : ''}"
        >
          {#if hasNumber}
            <!-- Token: cream inner when called (pink ≤49 / green ≥50 ring),
                 gray-ringed and dim when uncalled. -->
            <div
              class="flex items-center justify-center
                     w-[82%] h-[82%] rounded-full
                     text-lg sm:text-xl font-black tabular-nums
                     border-[3px] transition-all
                     {!isCalled
                       ? 'border-slate-300 dark:border-slate-600 bg-slate-50/40 dark:bg-slate-700/30 text-slate-400 dark:text-slate-500 opacity-70'
                       : isLow
                         ? 'border-pink-500 dark:border-pink-400 bg-amber-50 dark:bg-amber-100 text-pink-500 dark:text-pink-400'
                         : 'border-emerald-500 dark:border-emerald-400 bg-amber-50 dark:bg-amber-100 text-emerald-500 dark:text-emerald-400'}
                     {isLast
                       ? 'ring-2 ring-red-500 dark:ring-red-400 ring-offset-1 ring-offset-white dark:ring-offset-slate-800 scale-110 shadow-md'
                       : ''}"
            >
              {num}
            </div>
            {#if isCalled}
              <span
                class="absolute top-0.5 right-0.5 text-[9px] sm:text-[10px] font-semibold leading-none text-slate-500 dark:text-slate-400 tabular-nums"
              >
                {order}
              </span>
            {/if}
          {/if}
        </div>
      {/each}
    </div>
  </div>
{:else}
  <div class="text-center text-slate-400 dark:text-slate-500 py-10 text-sm">
    Nhấn "Ván mới" để bắt đầu
  </div>
{/if}
