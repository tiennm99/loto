<script>
  import { onMount } from "svelte";
  import "../app.css";
  import { loadMaster } from "$lib/master-store.svelte.js";
  import { loadSettings } from "$lib/settings-store.svelte.js";
  import {
    activeTab,
    claimActiveTab,
    watchActiveTab,
  } from "$lib/active-tab.svelte.js";
  import {
    applyUpdate,
    dismissUpdate,
    setUpdateSW,
    showUpdatePrompt,
    updatePrompt,
  } from "$lib/update-prompt.svelte.js";

  let { children } = $props();

  // Hydrate global stores once so children mount with consistent state.
  // Master state must hydrate before PlayerBoard reads `masterState.called`
  // length — see the mode-mount race fix in the consistency refactor.
  // The active-tab cleanup is returned so HMR / route changes close the
  // BroadcastChannel cleanly.
  onMount(() => {
    loadSettings();
    loadMaster();
    registerServiceWorker();
    return watchActiveTab();
  });

  // `adapter-static` renders page HTML during `adapt()`, which runs AFTER
  // the PWA plugin's `closeBundle` — so `@vite-pwa/sveltekit` never gets a
  // chance to inject its own registration tag into the built HTML. We have
  // to call the plugin's registration entry point ourselves. Dynamic import
  // (not a static one) so this module is never evaluated during SSR/
  // prerendering — `virtual:pwa-register` reaches into `navigator` and
  // `workbox-window`, both browser-only.
  //
  // `registerType: "prompt"` (vite.config.js) means the new SW installs and
  // waits rather than force-reloading a live round; `onNeedRefresh` only
  // flips the banner's visibility (lib/update-prompt.svelte.js) — the
  // reload itself happens exclusively from that banner's explicit tap.
  function registerServiceWorker() {
    if (!import.meta.env.PROD) return;
    if (typeof navigator === "undefined" || !("serviceWorker" in navigator))
      return;
    import("virtual:pwa-register")
      .then(({ registerSW }) => {
        const updateSW = registerSW({ onNeedRefresh: showUpdatePrompt });
        setUpdateSW(updateSW);
      })
      .catch(() => {
        /* SW unsupported/blocked (e.g. private mode) — app still works online */
      });
  }

  // Reclaiming a frozen tab: while this tab was frozen, the peer tab may
  // have drawn more numbers and persisted them. Re-read localStorage BEFORE
  // flipping `inactive` back off, so the children remount with the peer's
  // authoritative state instead of this tab's stale in-memory snapshot —
  // otherwise MasterPanel's save effect would immediately roll the round
  // back to whatever this tab last held.
  function reclaimTab() {
    loadSettings();
    loadMaster();
    claimActiveTab();
  }
</script>

{#if activeTab.inactive}
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

  {#if updatePrompt.visible}
    <!-- Small corner banner, never a full-screen overlay — must never sit
         over the board or the master "Số vừa xổ" hero mid-round. Reload
         only ever happens from the explicit "Tải lại" tap (see
         lib/update-prompt.svelte.js's module doc for why). -->
    <div
      role="status"
      aria-live="polite"
      class="fixed z-40 bottom-4 inset-x-4 sm:inset-x-auto sm:right-4 sm:left-auto sm:max-w-xs
             flex items-center gap-3 rounded-2xl bg-white dark:bg-slate-800
             border border-slate-200 dark:border-slate-600
             px-4 py-3 shadow-2xl animate-fade-in"
    >
      <p class="flex-1 text-sm font-semibold text-slate-800 dark:text-slate-100">
        Có bản mới. Tải lại?
      </p>
      <button
        type="button"
        onclick={applyUpdate}
        class="shrink-0 px-4 py-1.5 rounded-full font-semibold text-white text-sm
               bg-rose-600 hover:bg-rose-700 active:scale-95 transition-all shadow-md"
      >
        Tải lại
      </button>
      <button
        type="button"
        onclick={dismissUpdate}
        aria-label="Đóng thông báo cập nhật"
        class="shrink-0 px-2 py-1.5 rounded-full text-sm font-medium
               text-slate-500 dark:text-slate-400
               hover:text-slate-700 dark:hover:text-slate-200 transition-colors"
      >
        Để sau
      </button>
    </div>
  {/if}
{/if}
