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

  /** Hard cap so a poisoned origin can't stall the UI on mount.
   *  90-number called list serializes to ~500 bytes; 16 KB has 30× headroom. */
  const MAX_STORAGE_BYTES = 16_384;

  function loadState() {
    try {
      const data = localStorage.getItem(STORAGE_KEY);
      if (!data || data.length > MAX_STORAGE_BYTES) return null;
      const parsed = JSON.parse(data, (k, v) =>
        k === "__proto__" || k === "constructor" ? undefined : v,
      );
      // Minimal shape check — Array.isArray on both halves is enough
      // for all callers; deeper validation isn't worth the complexity.
      if (
        !parsed ||
        !Array.isArray(parsed.called) ||
        !Array.isArray(parsed.remaining)
      ) {
        return null;
      }
      return parsed;
    } catch {
      return null;
    }
  }

  const BOARD = Object.freeze(buildBoard().map((row) => Object.freeze(row)));
  const BOARD_FLAT = Object.freeze(BOARD.flatMap((r) => r));
</script>

<script>
  import { broadcastDraw, resetBus } from "$lib/call-bus.svelte.js";
  import MasterEmptyState from "$lib/MasterEmptyState.svelte";
  import { settings } from "$lib/settings-store.svelte.js";
  import { cancelPlayback, playNumber } from "$lib/voice.js";

  let state = $state(
    /** @type {{called: number[], remaining: number[]} | null} */ (null),
  );
  let lastCalled = $state(/** @type {number | null} */ (null));
  let heroEl = $state(/** @type {HTMLDivElement | null} */ (null));
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
    if (!autoRunning) return;
    if (!settings.autoCallEnabled) {
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

  // Stop any in-flight clip when the component unmounts (e.g. master mode
  // toggled off mid-clip) so audio doesn't outlive the panel.
  $effect(() => () => cancelPlayback());

  // Scroll the "Số vừa xổ" hero into view on each new draw so the host
  // doesn't have to chase it on a phone. Gated by a user-interaction flag
  // so reloading with persisted state doesn't yank the page on mount.
  let scrollOnNextDraw = false;

  $effect(() => {
    if (lastCalled !== null && heroEl && scrollOnNextDraw) {
      scrollOnNextDraw = false;
      const reduceMotion =
        typeof window !== "undefined" &&
        window.matchMedia?.("(prefers-reduced-motion: reduce)").matches;
      requestAnimationFrame(() =>
        heroEl?.scrollIntoView({
          behavior: reduceMotion ? "auto" : "smooth",
          block: "center",
        }),
      );
    }
  });

  function handleNewGame() {
    if (state && !confirm("Bạn có muốn tạo ván mới không?")) return;
    cancelPlayback();
    autoRunning = false;
    state = createFreshState();
    lastCalled = null;
    resetBus();
  }

  function handleDrawNext() {
    if (!state || state.remaining.length === 0) return;
    const next = state.remaining[0];
    state = {
      called: [...state.called, next],
      remaining: state.remaining.slice(1),
    };
    lastCalled = next;
    scrollOnNextDraw = true;
    broadcastDraw(next);
    if (settings.voiceEnabledMaster) playNumber(next);
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
           bg-amber-600 hover:bg-amber-700
           active:scale-95 transition-all shadow-md"
  >
    Ván mới
  </button>
  {#if state && state.remaining.length > 0}
    {#if settings.autoCallEnabled}
      <button
        onclick={toggleAuto}
        class="px-10 py-4 rounded-full font-semibold text-white text-lg
               {autoRunning
                 ? 'bg-rose-600 hover:bg-rose-700'
                 : 'bg-emerald-600 hover:bg-emerald-700'}
               active:scale-95 transition-all shadow-md"
      >
        {autoRunning ? "Dừng" : "Bắt đầu"}
      </button>
    {:else}
      <button
        onclick={handleDrawNext}
        class="px-10 py-4 rounded-full font-semibold text-white text-lg
               bg-emerald-600 hover:bg-emerald-700
               active:scale-95 transition-all shadow-md"
      >
        Xổ số
      </button>
    {/if}
  {/if}
</div>

{#if settings.autoCallEnabled && state && state.remaining.length > 0}
  <div
    class="text-center text-sm text-slate-600 dark:text-slate-300 mb-4 tabular-nums"
  >
    Tự động: {settings.autoCallSpeed}s/số
  </div>
{/if}

<!-- Current number -->
{#if lastCalled}
  {@const lastIsLow = lastCalled <= 49}
  <div class="flex flex-col items-center mb-6">
    <div
      class="text-sm uppercase tracking-[0.2em] font-semibold text-slate-500 dark:text-slate-400 mb-2"
    >
      Số vừa xổ
    </div>
    <div
      bind:this={heroEl}
      role="status"
      aria-live="polite"
      aria-atomic="true"
      class="w-32 h-32 sm:w-56 sm:h-56 rounded-full
             bg-amber-50 dark:bg-amber-100
             border-[6px] sm:border-[10px]
             flex items-center justify-center
             shadow-xl scroll-mt-4
             {lastIsLow
               ? 'border-sky-600 dark:border-sky-400 shadow-sky-500/30'
               : 'border-emerald-500 dark:border-emerald-400 shadow-emerald-500/30'}"
    >
      <span
        class="text-6xl sm:text-8xl font-black tabular-nums
               {lastIsLow
                 ? 'text-sky-700 dark:text-sky-400'
                 : 'text-emerald-500 dark:text-emerald-400'}"
      >
        {lastCalled}
      </span>
    </div>
    {#if state}
      <div class="mt-2.5 text-sm text-slate-600 dark:text-slate-300 tabular-nums">
        Đã xổ: <strong class="font-semibold">{state.called.length}</strong>/90
        &middot; Còn lại: <strong class="font-semibold">{state.remaining.length}</strong>
      </div>
    {/if}
  </div>
{/if}

<!-- Called history -->
{#if state && state.called.length > 0}
  <div class="mb-6 px-1">
    <div class="text-sm font-medium text-slate-600 dark:text-slate-300 mb-1.5">
      Thứ tự đã xổ:
    </div>
    <div class="flex flex-wrap gap-1.5">
      {#each state.called as num, i (i)}
        {@const isLow = num <= 49}
        <span
          class="inline-flex items-center justify-center w-9 h-9 sm:w-10 sm:h-10
                 text-base sm:text-lg font-black tabular-nums rounded-full
                 border-2 bg-amber-50 dark:bg-amber-100
                 {isLow
                   ? 'border-sky-600 dark:border-sky-400 text-sky-700 dark:text-sky-400'
                   : 'border-emerald-500 dark:border-emerald-400 text-emerald-600 dark:text-emerald-500'}"
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
            <!-- Token: cream inner when called (sky ≤49 / emerald ≥50 ring),
                 gray-ringed and dim when uncalled. -->
            <div
              class="flex items-center justify-center
                     w-[82%] h-[82%] rounded-full
                     text-xl sm:text-2xl font-black tabular-nums
                     border-[3px] transition-all
                     {!isCalled
                       ? 'border-slate-300 dark:border-slate-600 bg-slate-50/40 dark:bg-slate-700/30 text-slate-400 dark:text-slate-500 opacity-70'
                       : isLow
                         ? 'border-sky-600 dark:border-sky-400 bg-amber-50 dark:bg-amber-100 text-sky-700 dark:text-sky-700'
                         : 'border-emerald-600 dark:border-emerald-400 bg-amber-50 dark:bg-amber-100 text-emerald-700 dark:text-emerald-700'}
                     {isLast
                       ? 'ring-2 ring-red-500 dark:ring-red-400 ring-offset-1 ring-offset-white dark:ring-offset-slate-800 scale-110 shadow-md'
                       : ''}"
            >
              {num}
            </div>
            {#if isCalled}
              <span
                class="absolute top-0.5 right-0.5 text-[10px] sm:text-[11px] font-bold leading-none text-slate-600 dark:text-slate-300 tabular-nums"
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
  <MasterEmptyState />
{/if}
