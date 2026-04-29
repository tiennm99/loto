<script>
  import { slide } from "svelte/transition";
  import MasterPanel from "$lib/MasterPanel.svelte";
  import PageFooter from "$lib/PageFooter.svelte";
  import PlayerBoard from "$lib/PlayerBoard.svelte";
  import SettingsButton from "$lib/SettingsButton.svelte";
  import { settings } from "$lib/settings-store.svelte.js";
</script>

<div
  class="flex flex-col flex-1 items-center px-2 py-4 sm:px-3 sm:py-12"
>
  <div class="w-full max-w-2xl">
    <header class="relative text-center mb-6">
      <div class="absolute right-0 top-0">
        <SettingsButton />
      </div>
      <h1
        class="text-5xl sm:text-7xl font-black italic tracking-tight text-rose-600 dark:text-rose-400"
      >
        Lô tô
      </h1>
      <p
        class="flex items-center justify-center gap-2 text-xs sm:text-sm
               text-slate-600 dark:text-slate-300 mt-1.5 font-medium"
      >
        <span aria-hidden="true" class="block w-6 sm:w-8 h-px bg-slate-400/60 dark:bg-slate-500/50"></span>
        <span>🏮 Hội chợ TN1</span>
        <span aria-hidden="true" class="block w-6 sm:w-8 h-px bg-slate-400/60 dark:bg-slate-500/50"></span>
      </p>
    </header>

    {#if settings.mode !== "master"}
      <PlayerBoard />
    {/if}

    {#if settings.mode !== "player"}
      <section
        class={settings.mode === "both" ? "mt-10" : ""}
        aria-label="Bảng quản trò"
        transition:slide={{ duration: 250 }}
      >
        <h2
          class="text-center text-lg sm:text-xl font-bold tracking-wider uppercase mb-4 text-amber-700 dark:text-amber-400"
        >
          Quản trò
        </h2>
        <MasterPanel />
      </section>
    {/if}

    <PageFooter />
  </div>
</div>
