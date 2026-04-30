<script>
  import { onMount } from "svelte";
  import "../app.css";
  import { loadMaster } from "$lib/master-store.svelte.js";
  import { loadSettings } from "$lib/settings-store.svelte.js";
  import {
    reclaimTab,
    startTabLock,
    tabLock,
  } from "$lib/tab-lock.svelte.js";

  let { children } = $props();

  // Hydrate global stores once so children mount with consistent state.
  // Master state must hydrate before PlayerBoard reads `masterState.called`
  // length — see the mode-mount race fix in the consistency refactor.
  // The tab-lock cleanup is returned so HMR / route changes close the
  // BroadcastChannel cleanly.
  onMount(() => {
    loadSettings();
    loadMaster();
    return startTabLock();
  });
</script>

{#if tabLock.frozen}
  <button
    type="button"
    onclick={reclaimTab}
    class="fixed inset-0 z-50 flex flex-col items-center justify-center
           bg-slate-900/95 text-white text-center px-6
           cursor-pointer focus:outline-none"
    aria-label="Loto đã mở ở tab khác. Nhấn để chuyển về tab này."
  >
    <p class="text-2xl sm:text-3xl font-bold mb-3">
      🏮 Loto đã mở ở tab khác
    </p>
    <p class="text-base text-slate-300 max-w-sm">
      Tap để chuyển về tab này. Tab kia sẽ tự dừng.
    </p>
  </button>
{:else}
  {@render children()}
{/if}
