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
    aria-label="Phiên Lô tô đang chạy ở tab khác. Nhấn để tiếp tục tại đây."
  >
    <p class="text-2xl sm:text-3xl font-bold mb-3">
      Phiên Lô tô đang chạy ở tab khác
    </p>
    <p class="text-base text-slate-300 max-w-sm">
      Nhấn để tiếp tục tại đây. Tab kia sẽ tự động dừng.
    </p>
  </button>
{:else}
  {@render children()}
{/if}
