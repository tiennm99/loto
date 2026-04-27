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
      <!-- z-10: H1 has a CSS `drop-shadow` filter which creates a stacking
           context. Without z-index here, that filtered H1 (later in tree
           order) paints over the gear button and swallows clicks. -->
      <div class="absolute right-0 top-0 z-10">
        <SettingsButton />
      </div>
      <h1
        class="text-5xl sm:text-7xl font-black italic tracking-tight bg-gradient-to-r from-rose-500 via-amber-500 to-rose-500 bg-clip-text text-transparent drop-shadow-[0_2px_0_rgba(0,0,0,0.15)]"
      >
        Lô tô
      </h1>
      <p
        class="text-xs sm:text-sm uppercase tracking-[0.28em] text-slate-600 dark:text-slate-300 italic mt-1.5 font-medium"
      >
        Hội chợ TN1
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
          class="text-center text-lg sm:text-xl font-bold tracking-wider uppercase mb-4 text-orange-500 dark:text-orange-400"
        >
          Quản trò
        </h2>
        <MasterPanel />
      </section>
    {/if}

    <PageFooter />
  </div>
</div>
