/**
 * Global UI settings, persisted to localStorage and reactive via Svelte 5
 * runes. Each key is validated independently on load so adding new keys
 * doesn't break old saved data.
 *
 * @module lib/settings-store
 */

const STORAGE_KEY = "loto_settings";
const HEX6 = /^#[0-9a-fA-F]{6}$/;
const VALID_THEMES = /** @type {const} */ (["auto", "light", "dark"]);

export const DEFAULT_SETTINGS = Object.freeze({
  /** Excel "Standard Color: Purple". */
  emptyCellColor: "#7030A0",
  /** "auto" follows OS prefers-color-scheme; "light"/"dark" overrides it. */
  theme: /** @type {"auto"|"light"|"dark"} */ ("auto"),
  /** When true, the master panel renders inline below the player board on `/`. */
  masterMode: false,
  /** When true, the master "Xổ số" button becomes "Bắt đầu/Dừng" + auto interval. */
  autoCallEnabled: false,
  /** Auto-call interval, seconds per number. Integer 1..10. */
  autoCallSpeed: 5,
});

export const settings = $state({ ...DEFAULT_SETTINGS });

/** @param {unknown} v */
function validColor(v) {
  return typeof v === "string" && HEX6.test(v) ? v : null;
}
/** @param {unknown} v */
function validTheme(v) {
  return typeof v === "string" && VALID_THEMES.includes(/** @type {any} */ (v))
    ? /** @type {"auto"|"light"|"dark"} */ (v)
    : null;
}
/** @param {unknown} v */
function validBool(v) {
  return typeof v === "boolean" ? v : null;
}
/** @param {unknown} v */
function validSpeed(v) {
  return typeof v === "number" && Number.isInteger(v) && v >= 1 && v <= 10
    ? v
    : null;
}

/* ------------------------------------------------------------------ */
/* DOM apply                                                          */
/* ------------------------------------------------------------------ */

/** @type {MediaQueryList | null} */
let mql = null;
/** @type {((e: MediaQueryListEvent) => void) | null} */
let mqlListener = null;

function applyEmptyCellColor() {
  if (typeof document === "undefined") return;
  document.documentElement.style.setProperty(
    "--empty-cell-bg",
    settings.emptyCellColor,
  );
}

function applyTheme() {
  if (typeof document === "undefined") return;
  // Tear down any previous auto listener before reapplying.
  if (mql && mqlListener) {
    mql.removeEventListener("change", mqlListener);
    mql = null;
    mqlListener = null;
  }
  const root = document.documentElement;
  const set = (/** @type {boolean} */ dark) =>
    root.classList.toggle("dark", dark);
  if (settings.theme === "dark") {
    set(true);
  } else if (settings.theme === "light") {
    set(false);
  } else {
    // auto: track the OS preference and re-apply on changes.
    mql = window.matchMedia("(prefers-color-scheme: dark)");
    set(mql.matches);
    mqlListener = (e) => set(e.matches);
    mql.addEventListener("change", mqlListener);
  }
}

function applyAll() {
  applyEmptyCellColor();
  applyTheme();
}

/* ------------------------------------------------------------------ */
/* Public API                                                         */
/* ------------------------------------------------------------------ */

export function loadSettings() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const parsed = JSON.parse(raw) ?? {};
      settings.emptyCellColor =
        validColor(parsed.emptyCellColor) ?? DEFAULT_SETTINGS.emptyCellColor;
      settings.theme =
        validTheme(parsed.theme) ?? DEFAULT_SETTINGS.theme;
      settings.masterMode =
        validBool(parsed.masterMode) ?? DEFAULT_SETTINGS.masterMode;
      settings.autoCallEnabled =
        validBool(parsed.autoCallEnabled) ?? DEFAULT_SETTINGS.autoCallEnabled;
      settings.autoCallSpeed =
        validSpeed(parsed.autoCallSpeed) ?? DEFAULT_SETTINGS.autoCallSpeed;
    }
  } catch {
    /* private mode / quota / corrupt JSON — fall back to defaults */
  }
  applyAll();
}

export function saveSettings() {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify({ ...settings }));
  } catch {
    /* see loadSettings */
  }
  applyAll();
}

export function resetSettings() {
  settings.emptyCellColor = DEFAULT_SETTINGS.emptyCellColor;
  settings.theme = DEFAULT_SETTINGS.theme;
  settings.masterMode = DEFAULT_SETTINGS.masterMode;
  settings.autoCallEnabled = DEFAULT_SETTINGS.autoCallEnabled;
  settings.autoCallSpeed = DEFAULT_SETTINGS.autoCallSpeed;
  saveSettings();
}
