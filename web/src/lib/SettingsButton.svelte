<script>
  import { VOICES } from "$lib/audio-manifest.js";
  import {
    resetSettings,
    saveSettings,
    settings,
  } from "$lib/settings-store.svelte.js";

  let open = $state(false);

  // Office "Standard Colors" palette (10 swatches). Default is Purple — index 9.
  const PRESETS = /** @type {const} */ ([
    "#C00000", // Dark Red
    "#FF0000", // Red
    "#FFC000", // Orange
    "#FFFF00", // Yellow
    "#92D050", // Light Green
    "#00B050", // Green
    "#00B0F0", // Light Blue
    "#0070C0", // Blue
    "#002060", // Dark Blue
    "#7030A0", // Purple (default)
  ]);

  const THEMES = /** @type {const} */ ([
    ["auto", "Tự động"],
    ["light", "Sáng"],
    ["dark", "Tối"],
  ]);

  /** @param {string} hex */
  function pickColor(hex) {
    settings.emptyCellColor = hex;
    saveSettings();
  }

  function onPickerInput(/** @type {Event} */ e) {
    settings.emptyCellColor = /** @type {HTMLInputElement} */ (
      e.currentTarget
    ).value;
    saveSettings();
  }

  /** @param {"auto"|"light"|"dark"} t */
  function pickTheme(t) {
    settings.theme = t;
    saveSettings();
  }

  function toggleMaster() {
    settings.masterMode = !settings.masterMode;
    saveSettings();
  }

  function toggleAuto() {
    settings.autoCallEnabled = !settings.autoCallEnabled;
    saveSettings();
  }

  function onSpeedInput(/** @type {Event} */ e) {
    const n = Number(
      /** @type {HTMLInputElement} */ (e.currentTarget).value,
    );
    if (Number.isInteger(n) && n >= 1 && n <= 10) {
      settings.autoCallSpeed = n;
      saveSettings();
    }
  }

  function toggleVoiceMaster() {
    settings.voiceEnabledMaster = !settings.voiceEnabledMaster;
    saveSettings();
  }

  function toggleVoicePlayer() {
    settings.voiceEnabledPlayer = !settings.voiceEnabledPlayer;
    saveSettings();
  }

  /** @param {string} id */
  function pickVoice(id) {
    settings.voice = id;
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
    <!-- Backdrop: click-to-close, hidden from a11y tree. Window-level
         Escape handler in $effect covers keyboard close. -->
    <div
      aria-hidden="true"
      onclick={close}
      class="absolute inset-0"
    ></div>
    <div
      class="relative mx-4 max-w-sm sm:max-w-md w-full max-h-[90vh] overflow-y-auto rounded-3xl bg-white dark:bg-slate-800 p-6 shadow-2xl animate-pop-in"
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

      <!-- Theme -->
      <fieldset class="mb-5">
        <legend
          class="text-sm font-semibold text-slate-700 dark:text-slate-200 mb-2"
        >
          Giao diện
        </legend>
        <div class="grid grid-cols-3 gap-2">
          {#each THEMES as [v, label] (v)}
            {@const selected = settings.theme === v}
            <button
              type="button"
              aria-pressed={selected}
              onclick={() => pickTheme(v)}
              class="px-3 py-2 rounded-lg border-2 text-sm font-medium transition-all
                     {selected
                       ? 'border-indigo-500 dark:border-indigo-400 bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300'
                       : 'border-slate-200 dark:border-slate-600 text-slate-600 dark:text-slate-300 hover:border-slate-300 dark:hover:border-slate-500'}"
            >
              {label}
            </button>
          {/each}
        </div>
      </fieldset>

      {#snippet switchRow(label, isOn, onToggle)}
        <label
          class="flex items-center justify-between gap-3 px-3 py-2 rounded-lg
                 border-2 border-slate-200 dark:border-slate-600 cursor-pointer
                 hover:border-slate-300 dark:hover:border-slate-500 transition-colors"
        >
          <span class="text-sm text-slate-700 dark:text-slate-200">{label}</span>
          <span
            role="switch"
            tabindex="0"
            aria-checked={isOn}
            onclick={onToggle}
            onkeydown={(/** @type {KeyboardEvent} */ e) => {
              if (e.key === " " || e.key === "Enter") {
                e.preventDefault();
                onToggle();
              }
            }}
            class="relative inline-block w-10 h-6 rounded-full transition-colors flex-none
                   focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-indigo-400
                   focus:ring-offset-white dark:focus:ring-offset-slate-800
                   {isOn ? 'bg-emerald-500' : 'bg-slate-300 dark:bg-slate-600'}"
          >
            <span
              class="absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow-sm transition-transform
                     {isOn ? 'translate-x-4' : 'translate-x-0'}"
            ></span>
          </span>
        </label>
      {/snippet}

      <!-- Master mode -->
      <fieldset class="mb-5">
        <legend
          class="text-sm font-semibold text-slate-700 dark:text-slate-200 mb-2"
        >
          Chế độ quản trò
        </legend>
        <p class="text-xs text-slate-400 dark:text-slate-500 mb-2">
          Hiện bảng quản trò bên dưới bảng người chơi
        </p>
        {@render switchRow("Hiện bảng quản trò", settings.masterMode, toggleMaster)}
      </fieldset>

      <!-- Auto-call (only relevant when master mode is on) -->
      {#if settings.masterMode}
      <fieldset class="mb-5">
        <legend
          class="text-sm font-semibold text-slate-700 dark:text-slate-200 mb-2"
        >
          Tự động xổ
        </legend>
        <p class="text-xs text-slate-400 dark:text-slate-500 mb-2">
          Tự xổ số liên tiếp khi đang ở chế độ quản trò
        </p>
        <div class="mb-3">
          {@render switchRow("Tự xổ số", settings.autoCallEnabled, toggleAuto)}
        </div>
        {#if settings.autoCallEnabled}
          <label class="block">
            <span class="text-xs text-slate-600 dark:text-slate-300">
              Tốc độ: <strong class="tabular-nums"
                >{settings.autoCallSpeed}</strong
              > giây/số
            </span>
            <input
              type="range"
              min="1"
              max="10"
              step="1"
              value={settings.autoCallSpeed}
              oninput={onSpeedInput}
              aria-label="Tốc độ tự động xổ"
              aria-valuetext="{settings.autoCallSpeed} giây mỗi số"
              class="w-full mt-1 accent-indigo-500"
            />
          </label>
        {/if}
      </fieldset>
      {/if}

      <!-- Voice / sound -->
      <fieldset class="mb-5">
        <legend
          class="text-sm font-semibold text-slate-700 dark:text-slate-200 mb-2"
        >
          Âm thanh
        </legend>
        <p class="text-xs text-slate-400 dark:text-slate-500 mb-2">
          Đọc số bằng tiếng Việt
        </p>

        <div class="mb-2">
          {@render switchRow("Quản trò đọc số", settings.voiceEnabledMaster, toggleVoiceMaster)}
        </div>

        {@render switchRow("Báo Chờ / Kinh", settings.voiceEnabledPlayer, toggleVoicePlayer)}

        {#if VOICES.length > 1}
          <p class="text-xs text-slate-500 dark:text-slate-400 mt-3 mb-1">
            Giọng đọc
          </p>
          <div class="grid grid-cols-2 gap-2">
            {#each VOICES as v (v.id)}
              {@const selected = settings.voice === v.id}
              <button
                type="button"
                aria-pressed={selected}
                onclick={() => pickVoice(v.id)}
                class="px-3 py-2 rounded-lg border-2 text-sm transition-all
                       {selected
                         ? 'border-indigo-500 dark:border-indigo-400 bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300'
                         : 'border-slate-200 dark:border-slate-600 text-slate-600 dark:text-slate-300 hover:border-slate-300 dark:hover:border-slate-500'}"
              >
                {v.label}
              </button>
            {/each}
          </div>
        {/if}
      </fieldset>

      <!-- Empty cell color -->
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

        <div class="grid grid-cols-5 gap-2">
          {#each PRESETS as hex (hex)}
            {@const selected =
              settings.emptyCellColor.toLowerCase() === hex.toLowerCase()}
            <button
              type="button"
              aria-label="Đặt màu {hex}"
              aria-pressed={selected}
              onclick={() => pickColor(hex)}
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
