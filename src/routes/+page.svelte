<script>
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
        class="text-4xl sm:text-5xl font-extrabold tracking-tight bg-gradient-to-r from-indigo-500 to-purple-500 bg-clip-text text-transparent"
      >
        Lô tô
      </h1>
    </header>

    <PlayerBoard />

    {#if settings.masterMode}
      <section class="mt-10" aria-label="Bảng quản trò">
        <h2
          class="text-center text-xl sm:text-2xl font-extrabold tracking-tight mb-4 bg-gradient-to-r from-orange-500 to-red-500 bg-clip-text text-transparent"
        >
          Quản trò
        </h2>
        <MasterPanel />
      </section>
    {/if}

    <PageFooter />
  </div>
</div>
