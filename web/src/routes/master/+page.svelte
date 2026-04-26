<script module>
  const STORAGE_KEY = "loto_master";

  /**
   * Build the 11x9 board, aligned by last digit so column N row R holds
   * the number whose tens-digit is N (col 0 = ones, col 8 = eighties/90)
   * and whose ones-digit is R. Row 0 holds the *0 multiples (10..80),
   * rows 1-9 hold 1..9, 11..19, ..., 81..89, and row 10 holds 90 alone.
   * Empty slots are 0.
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
          // Only 90 sits in this trailing row, last column.
          if (col === 8) num = 90;
        } else if (row === 0) {
          // First col has no *0 number in 1-9 range; others get 10, 20, ..., 80.
          if (col > 0) num = col * 10;
        } else {
          // rows 1..9 hold digit `row`: col 0 -> row, col N -> N*10 + row.
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
  import { base } from "$app/paths";
  import PlayerBoard from "$lib/PlayerBoard.svelte";
  import SettingsButton from "$lib/SettingsButton.svelte";

  let state = $state(/** @type {{called: number[], remaining: number[]} | null} */ (null));
  let lastCalled = $state(/** @type {number | null} */ (null));

  // Load on mount
  $effect(() => {
    const saved = loadState();
    if (saved && saved.called.length > 0) {
      state = saved;
      lastCalled = saved.called[saved.called.length - 1];
    }
  });

  // Persist on change
  $effect(() => {
    if (state) saveState(state);
  });

  // Map number -> 1-based draw order; lets the master grid show "this
  // was the Nth call" for fast Kinh! verification.
  const callOrder = $derived(
    new Map((state?.called ?? []).map((n, i) => [n, i + 1])),
  );

  function handleNewGame() {
    if (state && !confirm("Bạn có muốn tạo ván mới không?")) return;
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
</script>

<div class="flex flex-col flex-1 items-center px-3 py-8 sm:py-12">
  <div class="w-full max-w-2xl">
    <!-- Header -->
    <header class="relative text-center mb-6">
      <div class="absolute right-0 top-0">
        <SettingsButton />
      </div>
      <h1
        class="text-3xl sm:text-4xl font-extrabold tracking-tight bg-gradient-to-r from-orange-500 to-red-500 bg-clip-text text-transparent"
      >
        Quản trò
      </h1>
      <p class="mt-1 text-sm text-slate-500 dark:text-slate-400">
        Xổ số và theo dõi bảng lô tô
      </p>
      <a
        href="{base}/"
        class="mt-2 inline-block text-xs text-indigo-500 dark:text-indigo-400 hover:underline"
      >
        ← Về trang người chơi
      </a>
    </header>

    <!-- Controls -->
    <div class="flex justify-center gap-3 mb-6">
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
    </div>

    <!-- Current number -->
    {#if lastCalled}
      <div class="flex flex-col items-center mb-6">
        <div
          class="text-xs uppercase tracking-widest text-slate-400 dark:text-slate-500 mb-1"
        >
          Số vừa xổ
        </div>
        <div
          class="w-20 h-20 sm:w-24 sm:h-24 rounded-2xl bg-gradient-to-br from-orange-500 to-red-500 flex items-center justify-center shadow-xl shadow-orange-500/30"
        >
          <span class="text-4xl sm:text-5xl font-black text-white">
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
                     text-sm sm:text-base font-bold tabular-nums rounded-full
                     border-[3px] bg-amber-50 dark:bg-amber-100
                     text-rose-700 dark:text-rose-800
                     {isLow
                       ? 'border-pink-500 dark:border-pink-400'
                       : 'border-emerald-500 dark:border-emerald-400'}"
            >
              {num}
            </span>
          {/each}
        </div>
      </div>
    {/if}

    <!-- Master 9x10 tracking board -->
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
                         text-lg sm:text-xl font-bold tabular-nums
                         border-[3px] transition-all
                         {!isCalled
                           ? 'border-slate-300 dark:border-slate-600 bg-slate-50/40 dark:bg-slate-700/30 text-slate-400 dark:text-slate-500 opacity-70'
                           : isLow
                             ? 'border-pink-500 dark:border-pink-400 bg-amber-50 dark:bg-amber-100 text-rose-700 dark:text-rose-800'
                             : 'border-emerald-500 dark:border-emerald-400 bg-amber-50 dark:bg-amber-100 text-rose-700 dark:text-rose-800'}
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
      <div class="text-center text-slate-400 dark:text-slate-500 py-20 text-sm">
        Nhấn "Ván mới" để bắt đầu
      </div>
    {/if}

    <!-- Master's own playing card -->
    <div class="mt-10">
      <div class="text-center mb-4">
        <h2 class="text-lg font-bold text-slate-700 dark:text-slate-200">
          Bảng của quản trò
        </h2>
        <p class="text-xs text-slate-400 dark:text-slate-500">
          Quản trò cũng có thể chơi cùng
        </p>
      </div>
      <PlayerBoard storagePrefix="loto_master_card" />
    </div>

    <footer
      class="mt-10 text-center text-xs text-slate-400 dark:text-slate-600"
    >
      Made with ❤️ by{" "}
      <a
        href="https://miti99.com"
        target="_blank"
        rel="noopener noreferrer"
        class="text-indigo-500 hover:underline"
      >
        miti99
      </a>
    </footer>
  </div>
</div>
