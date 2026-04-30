<script module>
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

  const BOARD = Object.freeze(buildBoard().map((row) => Object.freeze(row)));
  const BOARD_FLAT = Object.freeze(BOARD.flatMap((r) => r));
</script>

<script>
  import AutoCountdown from "$lib/AutoCountdown.svelte";
  import {
    drawNext,
    masterState,
    saveMaster,
    startNewGame,
  } from "$lib/master-store.svelte.js";
  import MasterEmptyState from "$lib/MasterEmptyState.svelte";
  import { settings } from "$lib/settings-store.svelte.js";
  import { cancelPlayback, playNumber } from "$lib/voice.js";

  let heroEl = $state(/** @type {HTMLDivElement | null} */ (null));
  let autoRunning = $state(false);
  // Bumped on each draw and on every (re-)arm of the auto-call interval —
  // signals AutoCountdown to reset its ring.
  let tickCount = $state(0);

  // Round is "active" once a game has been started (remaining filled or
  // called populated). `called.length === 0 && remaining.length === 0`
  // means no game in progress → show empty state.
  const hasGame = $derived(
    masterState.called.length > 0 || masterState.remaining.length > 0,
  );
  const lastCalled = $derived(
    masterState.called.length > 0
      ? masterState.called[masterState.called.length - 1]
      : null,
  );

  // Master state hydration lives in `+layout.svelte` so it runs before
  // any panel mounts — otherwise PlayerBoard would baseline its cursor
  // against an empty `masterState.called` and replay the back-history
  // when MasterPanel later loads.
  $effect(() => {
    // Subscribe to both arrays so any mutation re-saves.
    masterState.called;
    masterState.remaining;
    saveMaster();
  });

  // Map number -> 1-based draw order for fast Kinh! verification.
  const callOrder = $derived(
    new Map(masterState.called.map((n, i) => [n, i + 1])),
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
    // No tickCount bump here on purpose: `tickCount++` would read tickCount
    // and turn this effect into its own dependency (effect_update_depth_exceeded).
    // AutoCountdown's reset effect already re-baselines on `running` rising
    // edge and on `duration` change, so re-arms are covered without our help.
    const ms = settings.autoCallSpeed * 1000;
    const id = setInterval(() => {
      if (masterState.remaining.length === 0) {
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
    if (hasGame && !confirm("Bạn có muốn tạo ván mới không?")) return;
    cancelPlayback();
    autoRunning = false;
    startNewGame();
  }

  function handleDrawNext() {
    const next = drawNext();
    if (next === null) return;
    scrollOnNextDraw = true;
    tickCount++;
    if (settings.voiceEnabledMaster) playNumber(next);
  }

  function toggleAuto() {
    if (masterState.remaining.length === 0) return;
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
  {#if hasGame && masterState.remaining.length > 0}
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

{#if settings.autoCallEnabled && hasGame && masterState.remaining.length > 0}
  <div
    class="text-center text-sm text-slate-600 dark:text-slate-300 mb-4 tabular-nums"
  >
    Tự động: {settings.autoCallSpeed}s/số
  </div>
{/if}

{#if autoRunning && masterState.remaining.length > 0}
  <div class="flex justify-center mb-4">
    <AutoCountdown
      running={autoRunning}
      duration={settings.autoCallSpeed}
      tickKey={tickCount}
    />
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
    {#if hasGame}
      <div class="mt-2.5 text-sm text-slate-600 dark:text-slate-300 tabular-nums">
        Đã xổ: <strong class="font-semibold">{masterState.called.length}</strong>/90
        &middot; Còn lại: <strong class="font-semibold">{masterState.remaining.length}</strong>
      </div>
    {/if}
  </div>
{/if}

<!-- Called history -->
{#if masterState.called.length > 0}
  <div class="mb-6 px-1">
    <div class="text-sm font-medium text-slate-600 dark:text-slate-300 mb-1.5">
      Thứ tự đã xổ:
    </div>
    <div class="flex flex-wrap gap-1.5">
      {#each masterState.called as num, i (i)}
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
{#if hasGame}
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
