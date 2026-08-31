/**
 * Update-prompt coordinator for the PWA service worker.
 *
 * `vite.config.js` sets `registerType: "prompt"` so vite-plugin-pwa NEVER
 * force-reloads a live tab when a new build activates — an unattended
 * reload would drop in-memory round state (`autoRunning`, `showCongrats`,
 * the auto-call countdown) mid-fairground. Instead `+layout.svelte` calls
 * `registerSW({ onNeedRefresh: showUpdatePrompt })`, which routes here so a
 * small dismissible banner can ask the host before reloading. The actual
 * reload only ever happens from an explicit tap on the banner's button.
 *
 * @module lib/update-prompt
 */

export const updatePrompt = $state({
  /** True once vite-plugin-pwa's `onNeedRefresh` has fired this session. */
  visible: false,
});

/** @type {((reloadPage?: boolean) => Promise<void>) | null} */
let updateSW = null;

/**
 * Store the function `registerSW()` returns so `applyUpdate()` can trigger
 * the actual reload later without `+layout.svelte` holding its own copy.
 * @param {(reloadPage?: boolean) => Promise<void>} fn
 */
export function setUpdateSW(fn) {
  updateSW = fn;
}

/** Wired to `registerSW`'s `onNeedRefresh` callback. */
export function showUpdatePrompt() {
  updatePrompt.visible = true;
}

/** Host tapped "Tải lại" — activate the waiting worker and reload. */
export function applyUpdate() {
  updatePrompt.visible = false;
  if (updateSW) void updateSW(true);
}

/** Host tapped dismiss — keep the current version running for this visit. */
export function dismissUpdate() {
  updatePrompt.visible = false;
}

/** Test-only reset so module-singleton state doesn't leak between cases. */
export function _resetUpdatePromptForTest() {
  updatePrompt.visible = false;
  updateSW = null;
}
