<script>
  import {
    DEFAULT_SETTINGS,
    resetSettings,
    saveSettings,
    settings,
  } from "$lib/settings-store.svelte.js";

  let open = $state(false);

  // Curated swatches: brown (default Minh Tân), warm cream, slate,
  // amber, emerald, indigo, plum, deep pink — covers the common asks
  // (paper-feel, neutral, vivid).
  const PRESETS = [
    DEFAULT_SETTINGS.emptyCellColor,
    "#f5e6c8",
    "#475569",
    "#f59e0b",
    "#10b981",
    "#6366f1",
    "#9333ea",
    "#e11d48",
  ];

  /** @param {string} hex */
  function pick(hex) {
    settings.emptyCellColor = hex;
    saveSettings();
  }

  function onPickerInput(/** @type {Event} */ e) {
    const v = /** @type {HTMLInputElement} */ (e.currentTarget).value;
    settings.emptyCellColor = v;
    saveSettings();
  }

  function close() {
    open = false;
  }

  // Escape-to-close while open. Window listener avoids relying on focus
  // staying inside the modal — the dialog div may not receive keydown
  // when the trigger button still holds focus on initial open.
  $effect(() => {
    if (!open) return;
    /** @param {KeyboardEvent} e */
    const onKey = (e) => {
      if (e.key === "Escape") close();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  });
</script>

<button
  type="button"
  aria-label="Cài đặt"
  title="Cài đặt"
  onclick={() => (open = true)}
  class="inline-flex items-center justify-center w-9 h-9 rounded-full
         text-slate-500 dark:text-slate-400
         hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors
         focus:outline-none focus:ring-2 focus:ring-indigo-400"
>
  <svg
    xmlns="http://www.w3.org/2000/svg"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="2"
    stroke-linecap="round"
    stroke-linejoin="round"
    class="w-5 h-5"
    aria-hidden="true"
  >
    <circle cx="12" cy="12" r="3"></circle>
    <path
      d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.65 1.65 0 0 0-1-1.51 1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.65 1.65 0 0 0 1.51-1 1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33h0a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51h0a1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82v0a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"
    ></path>
  </svg>
</button>

{#if open}
  <div
    role="dialog"
    aria-modal="true"
    aria-labelledby="settings-title"
    class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 backdrop-blur-sm animate-fade-in"
  >
    <!-- Backdrop: click-to-close, hidden from a11y tree (the "Xong" button is the labelled close). -->
    <div
      aria-hidden="true"
      onclick={close}
      onkeydown={(e) => e.key === "Enter" && close()}
      role="presentation"
      class="absolute inset-0"
    ></div>
    <div
      class="relative mx-4 max-w-sm w-full rounded-3xl bg-white dark:bg-slate-800 p-6 shadow-2xl animate-pop-in"
    >
      <h2
        id="settings-title"
        class="text-xl font-bold text-slate-800 dark:text-slate-100 mb-1"
      >
        Cài đặt
      </h2>
      <p class="text-xs text-slate-500 dark:text-slate-400 mb-5">
        Tuỳ chỉnh giao diện bảng lô tô
      </p>

      <fieldset class="mb-5">
        <legend
          class="text-sm font-semibold text-slate-700 dark:text-slate-200 mb-2"
        >
          Màu ô trống
        </legend>

        <div class="flex items-center gap-3 mb-3">
          <input
            type="color"
            aria-label="Chọn màu tuỳ ý"
            value={settings.emptyCellColor}
            oninput={onPickerInput}
            class="w-12 h-10 rounded-lg border border-slate-300 dark:border-slate-600 cursor-pointer bg-transparent"
          />
          <code
            class="text-xs font-mono text-slate-500 dark:text-slate-400 tabular-nums"
          >
            {settings.emptyCellColor}
          </code>
        </div>

        <div class="grid grid-cols-8 gap-2">
          {#each PRESETS as hex (hex)}
            {@const selected = settings.emptyCellColor.toLowerCase() === hex.toLowerCase()}
            <button
              type="button"
              aria-label="Đặt màu {hex}"
              aria-pressed={selected}
              onclick={() => pick(hex)}
              style:background-color={hex}
              class="aspect-square rounded-lg border-2 transition-all
                     {selected
                       ? 'border-indigo-500 dark:border-indigo-400 ring-2 ring-indigo-300 dark:ring-indigo-600 scale-110'
                       : 'border-slate-200 dark:border-slate-600 hover:scale-105'}"
            ></button>
          {/each}
        </div>
      </fieldset>

      <div class="flex justify-between gap-2">
        <button
          type="button"
          onclick={() => resetSettings()}
          class="px-4 py-2 rounded-full text-sm font-medium
                 text-slate-600 dark:text-slate-300
                 hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors"
        >
          Mặc định
        </button>
        <button
          type="button"
          onclick={close}
          class="px-6 py-2 rounded-full text-sm font-semibold text-white
                 bg-gradient-to-r from-indigo-500 to-purple-500
                 hover:from-indigo-600 hover:to-purple-600
                 active:scale-95 transition-all shadow-lg"
        >
          Xong
        </button>
      </div>
    </div>
  </div>
{/if}
