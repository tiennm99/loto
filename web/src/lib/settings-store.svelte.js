/**
 * Global UI settings, persisted to localStorage and reactive via Svelte 5
 * runes. Each key is validated independently on load so adding new keys
 * doesn't break old saved data.
 *
 * @module lib/settings-store
 */

import { DEFAULT_VOICE, VOICE_IDS } from "$lib/audio-manifest.js";

const STORAGE_KEY = "loto_settings";
const HEX6 = /^#[0-9a-fA-F]{6}$/;
const VALID_THEMES = /** @type {const} */ (["auto", "light", "dark"]);
const VALID_MODES = /** @type {const} */ (["player", "master", "both"]);
/**
 * Board number sizes, as a multiplier on the responsive base size.
 * The Android wrapper pins the WebView's textZoom to 100 so the system font
 * setting can't break the fixed 9-column grid — this is the in-app
 * replacement for that control, so the rungs need to reach genuinely large.
 */
export const BOARD_TEXT_SCALES = /** @type {const} */ ([0.9, 1, 1.15, 1.3]);
/** Hard cap on stored settings JSON to keep poisoned origins from
 *  stalling the UI on mount. Real settings serialize to ~200 bytes. */
const MAX_STORAGE_BYTES = 8_192;

export const DEFAULT_SETTINGS = Object.freeze({
  /** Excel "Standard Color: Purple". */
  emptyCellColor: "#7030A0",
  /** "auto" follows OS prefers-color-scheme; "light"/"dark" overrides it. */
  theme: /** @type {"auto"|"light"|"dark"} */ ("auto"),
  /** Which panels are visible: player only, master only, or both inline. */
  mode: /** @type {"player"|"master"|"both"} */ ("player"),
  /** When true, the master "Xổ số" button becomes "Bắt đầu/Dừng" + auto interval. */
  autoCallEnabled: false,
  /** Auto-call interval, seconds per number. Integer 1..10. */
  autoCallSpeed: 5,
  /** Speak the called number aloud when master draws. */
  voiceEnabledMaster: true,
  /** Speak "Chờ" / "Kinh" on player events. */
  voiceEnabledPlayer: false,
  /** When voiceEnabledPlayer is on, also speak the awaited number after "Chờ". */
  voiceWaitingNumber: false,
  /** Active voice id; matches an entry in audio manifest. */
  voice: DEFAULT_VOICE,
  /** Multiplier on board number size; one of BOARD_TEXT_SCALES. */
  boardTextScale: 1,
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
function validMode(v) {
  return typeof v === "string" &&
    VALID_MODES.includes(/** @type {any} */ (v))
    ? /** @type {"player"|"master"|"both"} */ (v)
    : null;
}
/** @param {unknown} v */
function validSpeed(v) {
  return typeof v === "number" && Number.isInteger(v) && v >= 1 && v <= 10
    ? v
    : null;
}
/** @param {unknown} v */
function validVoiceId(v) {
  return typeof v === "string" && VOICE_IDS.has(v) ? v : null;
}
/** @param {unknown} v */
function validBoardTextScale(v) {
  return typeof v === "number" &&
    BOARD_TEXT_SCALES.includes(/** @type {any} */ (v))
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

function applyBoardTextScale() {
  if (typeof document === "undefined") return;
  document.documentElement.style.setProperty(
    "--board-text-scale",
    String(settings.boardTextScale),
  );
}

function applyAll() {
  applyEmptyCellColor();
  applyTheme();
  applyBoardTextScale();
}

/* ------------------------------------------------------------------ */
/* Public API                                                         */
/* ------------------------------------------------------------------ */

export function loadSettings() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw && raw.length <= MAX_STORAGE_BYTES) {
      // Strip `__proto__`/`constructor` as defense-in-depth in case
      // future code spreads/assigns the parsed object.
      const parsed = JSON.parse(raw, (k, v) =>
        k === "__proto__" || k === "constructor" ? undefined : v,
      ) ?? {};
      settings.emptyCellColor =
        validColor(parsed.emptyCellColor) ?? DEFAULT_SETTINGS.emptyCellColor;
      settings.theme =
        validTheme(parsed.theme) ?? DEFAULT_SETTINGS.theme;
      // Migration: legacy `masterMode: true` → `mode: "both"`. Anything
      // else (false / missing / invalid) falls through to the default.
      const parsedMode = validMode(parsed.mode);
      settings.mode = parsedMode
        ? parsedMode
        : parsed.masterMode === true
          ? "both"
          : DEFAULT_SETTINGS.mode;
      settings.autoCallEnabled =
        validBool(parsed.autoCallEnabled) ?? DEFAULT_SETTINGS.autoCallEnabled;
      settings.autoCallSpeed =
        validSpeed(parsed.autoCallSpeed) ?? DEFAULT_SETTINGS.autoCallSpeed;
      settings.voiceEnabledMaster =
        validBool(parsed.voiceEnabledMaster) ??
        DEFAULT_SETTINGS.voiceEnabledMaster;
      settings.voiceEnabledPlayer =
        validBool(parsed.voiceEnabledPlayer) ??
        DEFAULT_SETTINGS.voiceEnabledPlayer;
      settings.voiceWaitingNumber =
        validBool(parsed.voiceWaitingNumber) ??
        DEFAULT_SETTINGS.voiceWaitingNumber;
      settings.voice =
        validVoiceId(parsed.voice) ?? DEFAULT_SETTINGS.voice;
      settings.boardTextScale =
        validBoardTextScale(parsed.boardTextScale) ??
        DEFAULT_SETTINGS.boardTextScale;
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
  settings.mode = DEFAULT_SETTINGS.mode;
  settings.autoCallEnabled = DEFAULT_SETTINGS.autoCallEnabled;
  settings.autoCallSpeed = DEFAULT_SETTINGS.autoCallSpeed;
  settings.voiceEnabledMaster = DEFAULT_SETTINGS.voiceEnabledMaster;
  settings.voiceEnabledPlayer = DEFAULT_SETTINGS.voiceEnabledPlayer;
  settings.voiceWaitingNumber = DEFAULT_SETTINGS.voiceWaitingNumber;
  settings.voice = DEFAULT_SETTINGS.voice;
  settings.boardTextScale = DEFAULT_SETTINGS.boardTextScale;
  saveSettings();
}
