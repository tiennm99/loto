<script>
  import { VOICES } from "$lib/audio-manifest.js";
  import {
    resetSettings,
    saveSettings,
    settings,
  } from "$lib/settings-store.svelte.js";
  import { clearAudioCache } from "$lib/voice.js";

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

  const MODES = /** @type {const} */ ([
    ["player", "Người chơi"],
    ["master", "Quản trò"],
    ["both", "Cả hai"],
  ]);

  const MODE_HINTS = /** @type {const} */ ({
    player: "Chỉ bảng người chơi",
    master: "Chỉ bảng quản trò (xổ số, theo dõi)",
    both: "Cả hai bảng — quản trò tự đánh dấu cho người chơi",
  });

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

  /** @param {"player"|"master"|"both"} m */
  function pickMode(m) {
    settings.mode = m;
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

  function toggleVoiceWaitingNumber() {
    settings.voiceWaitingNumber = !settings.voiceWaitingNumber;
    saveSettings();
  }

  /** @param {string} id */
  function pickVoice(id) {
    if (settings.voice !== id) clearAudioCache();
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
         focus:outline-none focus:ring-2 focus:ring-rose-400"
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
      class="relative mx-4 max-w-sm sm:max-w-md w-full max-h-[90vh] overflow-y-auto rounded-3xl bg-white dark:bg-slate-800 shadow-2xl animate-pop-in"
    >
      <!-- Sticky title — pinned so the modal stays anchored on small
           viewports (iPhone SE: 375x667) where the body would otherwise
           push the heading off-screen. -->
      <div class="sticky top-0 z-10 bg-white dark:bg-slate-800 px-6 pt-6 pb-3 rounded-t-3xl">
        <h2
          id="settings-title"
          class="text-2xl font-bold text-slate-800 dark:text-slate-100 mb-1"
        >
          Cài đặt
        </h2>
        <p class="text-sm text-slate-500 dark:text-slate-400">
          Tuỳ chỉnh giao diện bảng lô tô
        </p>
      </div>

      <div class="px-6 pt-2">

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
                       ? 'border-rose-600 dark:border-rose-400 bg-rose-50 dark:bg-rose-950/40 text-rose-700 dark:text-rose-300'
                       : 'border-slate-200 dark:border-slate-600 text-slate-600 dark:text-slate-300 hover:border-slate-300 dark:hover:border-slate-500'}"
            >
              {label}
            </button>
          {/each}
        </div>
      </fieldset>

      {#snippet switchRow(label, isOn, onToggle)}
        <!-- Whole row is the switch — role/tabindex/click here, not on the
             inner pill, so tapping the label text also toggles. -->
        <div
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
          class="flex items-center justify-between gap-3 px-3 py-2 rounded-lg
                 border-2 border-slate-200 dark:border-slate-600 cursor-pointer
                 hover:border-slate-300 dark:hover:border-slate-500 transition-colors
                 focus:outline-none focus:ring-2 focus:ring-rose-400"
        >
          <span class="text-sm text-slate-700 dark:text-slate-200">{label}</span>
          <span
            aria-hidden="true"
            class="relative inline-block w-10 h-6 rounded-full transition-colors flex-none
                   {isOn ? 'bg-emerald-500' : 'bg-slate-300 dark:bg-slate-600'}"
          >
            <span
              class="absolute top-0.5 left-0.5 w-5 h-5 bg-white rounded-full shadow-sm transition-transform
                     {isOn ? 'translate-x-4' : 'translate-x-0'}"
            ></span>
          </span>
        </div>
      {/snippet}

      <!-- Display mode: player / master / both -->
      <fieldset class="mb-5">
        <legend
          class="text-sm font-semibold text-slate-700 dark:text-slate-200 mb-2"
        >
          Chế độ hiển thị
        </legend>
        <p class="text-sm text-slate-500 dark:text-slate-400 mb-2">
          Chọn vai trò để chỉ hiện phần liên quan
        </p>
        <div class="grid grid-cols-3 gap-2">
          {#each MODES as [v, label] (v)}
            {@const selected = settings.mode === v}
            <button
              type="button"
              aria-pressed={selected}
              onclick={() => pickMode(v)}
              class="flex flex-col items-center gap-1 px-2 py-2 rounded-lg border-2 text-sm font-medium transition-all
                     {selected
                       ? 'border-rose-600 dark:border-rose-400 bg-rose-50 dark:bg-rose-950/40 text-rose-700 dark:text-rose-300'
                       : 'border-slate-200 dark:border-slate-600 text-slate-600 dark:text-slate-300 hover:border-slate-300 dark:hover:border-slate-500'}"
            >
              {#if v === "player"}
                <svg viewBox="0 0 24 16" class="w-7 h-5" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                  <rect x="1" y="1" width="22" height="14" rx="1.5" />
                  <path d="M1 6h22M1 11h22M8 1v14M16 1v14" />
                </svg>
              {:else if v === "master"}
                <svg viewBox="0 0 24 24" class="w-6 h-6" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linejoin="round" aria-hidden="true">
                  <path d="M3 11l14-6v14L3 13z" />
                  <path d="M3 11v2" />
                  <path d="M19 8a4 4 0 0 1 0 8" />
                </svg>
              {:else}
                <!-- "Both": mini grid (player) + mini megaphone (master)
                     side-by-side, so the glyph reads as the two roles
                     combined rather than two stacked windows. -->
                <svg viewBox="0 0 28 16" class="w-7 h-5" fill="none" stroke="currentColor" stroke-width="1.5" aria-hidden="true">
                  <rect x="1" y="2" width="11" height="12" rx="1" />
                  <path d="M1 6h11M1 10h11M5 2v12M9 2v12" />
                  <path d="M16 6l9-3v10l-9-3z" stroke-linejoin="round" />
                  <path d="M16 6v4" />
                </svg>
              {/if}
              <span>{label}</span>
            </button>
          {/each}
        </div>
        <p class="text-xs text-slate-500 dark:text-slate-400 mt-2 italic">
          {MODE_HINTS[settings.mode]}
        </p>
      </fieldset>

      <!-- Auto-call (only relevant when master panel is visible) -->
      {#if settings.mode !== "player"}
      <fieldset class="mb-5">
        <legend
          class="text-sm font-semibold text-slate-700 dark:text-slate-200 mb-2"
        >
          Tự động xổ
        </legend>
        <p class="text-sm text-slate-500 dark:text-slate-400 mb-2">
          Tự xổ số liên tiếp khi đang ở chế độ quản trò
        </p>
        <div class="mb-3">
          {@render switchRow("Tự xổ số", settings.autoCallEnabled, toggleAuto)}
        </div>
        {#if settings.autoCallEnabled}
          <label class="block mt-2 pl-3 border-l-2 border-slate-200 dark:border-slate-600">
            <span class="text-sm text-slate-700 dark:text-slate-200">
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
              class="w-full mt-1 accent-rose-600"
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
        <p class="text-sm text-slate-500 dark:text-slate-400 mb-2">
          Đọc số bằng tiếng Việt
        </p>

        {#if settings.mode !== "player"}
          <div class="mb-2">
            {@render switchRow("Quản trò đọc số", settings.voiceEnabledMaster, toggleVoiceMaster)}
            {#if settings.mode === "both"}
              <p class="text-xs text-slate-500 dark:text-slate-400 mt-1.5 px-1">
                Đọc số đã xổ + báo Chờ/Kinh.
              </p>
            {:else}
              <p class="text-xs text-slate-500 dark:text-slate-400 mt-1.5 px-1">
                Đọc số đã xổ.
              </p>
            {/if}
          </div>
        {/if}

        {#if settings.mode !== "master"}
          {@render switchRow("Báo Chờ / Kinh", settings.voiceEnabledPlayer, toggleVoicePlayer)}

          {#if settings.voiceEnabledPlayer && settings.mode !== "both"}
            <div class="mt-2 pl-3 border-l-2 border-slate-200 dark:border-slate-600">
              {@render switchRow("Đọc thêm số đang chờ", settings.voiceWaitingNumber, toggleVoiceWaitingNumber)}
              <p class="text-xs text-slate-500 dark:text-slate-400 mt-1.5 px-1">
                Tắt: chỉ nói "Chờ". Bật: nói "Chờ {`{số}`}".
              </p>
            </div>
          {/if}
        {/if}

        {#if VOICES.length > 1}
          <p class="text-sm font-medium text-slate-600 dark:text-slate-300 mt-3 mb-1.5">
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
                         ? 'border-rose-600 dark:border-rose-400 bg-rose-50 dark:bg-rose-950/40 text-rose-700 dark:text-rose-300'
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

        <div class="rounded-xl border border-slate-200 dark:border-slate-700 p-3 space-y-3">
          <div>
            <p class="text-xs uppercase tracking-wider font-semibold text-slate-500 dark:text-slate-400 mb-1.5">
              Tuỳ chỉnh
            </p>
            <div class="flex items-center gap-3">
              <input
                type="color"
                aria-label="Chọn màu tuỳ ý"
                value={settings.emptyCellColor}
                oninput={onPickerInput}
                class="w-12 h-10 rounded-lg border border-slate-300 dark:border-slate-600 cursor-pointer bg-transparent"
              />
              <code
                class="text-sm font-mono text-slate-600 dark:text-slate-300 tabular-nums"
              >
                {settings.emptyCellColor}
              </code>
            </div>
          </div>

          <div>
            <p class="text-xs uppercase tracking-wider font-semibold text-slate-500 dark:text-slate-400 mb-1.5">
              Mẫu sẵn
            </p>
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
                           ? 'border-rose-600 dark:border-rose-400 ring-2 ring-rose-300 dark:ring-rose-600 scale-110'
                           : 'border-slate-200 dark:border-slate-600 hover:scale-105'}"
                ></button>
              {/each}
            </div>
          </div>
        </div>
      </fieldset>

      </div>

      <!-- Sticky action row: keep "Xong" reachable without scrolling
           back when the body overflows (iPhone SE). Border-t separates
           visually from content slipping behind. -->
      <div class="sticky bottom-0 z-10 bg-white dark:bg-slate-800 px-6 py-4 border-t border-slate-200 dark:border-slate-700 flex justify-between gap-2 rounded-b-3xl">
        <button
          type="button"
          onclick={() => resetSettings()}
          class="px-4 py-2 rounded-full text-sm font-medium
                 text-slate-600 dark:text-slate-300
                 border border-slate-300 dark:border-slate-600
                 hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors"
        >
          Đặt lại
        </button>
        <button
          type="button"
          onclick={close}
          class="px-6 py-2 rounded-full text-sm font-semibold text-white
                 bg-rose-600 hover:bg-rose-700
                 active:scale-95 transition-all shadow-md"
        >
          Xong
        </button>
      </div>
    </div>
  </div>
{/if}
