<!-- Empty-state hero for master mode. Replaces the bare line of text
     when no game has been started; uses a decorative ghost mock of
     the 11×9 master tracking grid so the page silhouette stays
     consistent with what the user sees mid-game. -->

<div class="text-center py-10">
  <div
    aria-hidden="true"
    class="mx-auto max-w-xs opacity-30 pointer-events-none mb-6
           rounded-md overflow-hidden border border-slate-300 dark:border-slate-600"
  >
    <div class="grid grid-cols-9 gap-px bg-slate-300 dark:bg-slate-700">
      {#each Array(99) as _, i (i)}
        {@const filled = i % 11 < 9 && i % 7 === 0}
        <div
          class="aspect-square text-[0.5rem] flex items-center justify-center
                 text-black {filled ? 'bg-white dark:bg-slate-800 dark:text-slate-100' : ''}"
          style:background-color={filled ? undefined : "var(--empty-cell-bg)"}
        >
          {filled ? ((i * 13) % 90) + 1 : ""}
        </div>
      {/each}
    </div>
  </div>
  <span
    class="inline-block px-3 py-1 rounded-full text-xs font-semibold
           tracking-wider uppercase
           bg-amber-100 dark:bg-amber-950/40
           text-amber-700 dark:text-amber-300"
  >
    Chế độ Quản trò
  </span>
  <p class="mt-3 text-base text-slate-600 dark:text-slate-300 italic">
    Nhấn
    <span class="font-semibold text-amber-700 dark:text-amber-400 not-italic">
      "Ván mới"
    </span>
    để bắt đầu xổ số
  </p>
  <p class="mt-1.5 text-sm text-slate-500 dark:text-slate-400">
    🎤 Đã sẵn sàng
  </p>
</div>
