/**
 * Global UI settings, persisted to localStorage and reactive via Svelte 5
 * runes. Currently only `emptyCellColor` (paints blank cells across both
 * the player card and the master tracking grid). Shape is intentionally
 * an object so future keys can be added without breaking persistence.
 *
 * @module lib/settings-store
 */

const STORAGE_KEY = "loto_settings";
const HEX6 = /^#[0-9a-fA-F]{6}$/;

export const DEFAULT_SETTINGS = Object.freeze({
  /** Brown — matches a Minh Tân paper card. */
  emptyCellColor: "#7a4a2b",
});

export const settings = $state({ ...DEFAULT_SETTINGS });

/** @param {unknown} v */
function isValidColor(v) {
  return typeof v === "string" && HEX6.test(v);
}

function applyToDom() {
  if (typeof document === "undefined") return;
  document.documentElement.style.setProperty(
    "--empty-cell-bg",
    settings.emptyCellColor,
  );
}

export function loadSettings() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) {
      applyToDom();
      return;
    }
    const parsed = JSON.parse(raw);
    if (parsed && isValidColor(parsed.emptyCellColor)) {
      settings.emptyCellColor = parsed.emptyCellColor;
    }
  } catch {
    /* private mode / quota / corrupt JSON — fall back to defaults */
  }
  applyToDom();
}

export function saveSettings() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...settings }));
  } catch {
    /* see loadSettings */
  }
  applyToDom();
}

export function resetSettings() {
  settings.emptyCellColor = DEFAULT_SETTINGS.emptyCellColor;
  saveSettings();
}
