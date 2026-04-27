// @vitest-environment happy-dom
import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  DEFAULT_SETTINGS,
  loadSettings,
  resetSettings,
  saveSettings,
  settings,
} from "./settings-store.svelte.js";

const KEY = "loto_settings";

/** Minimal MediaQueryList stub. matches=false unless we override. */
function mockMatchMedia(matches = false) {
  /** @type {Set<(e: MediaQueryListEvent) => void>} */
  const listeners = new Set();
  const mql = {
    matches,
    media: "(prefers-color-scheme: dark)",
    addEventListener: (/** @type {string} */ _t, fn) => listeners.add(fn),
    removeEventListener: (/** @type {string} */ _t, fn) => listeners.delete(fn),
    addListener: () => {},
    removeListener: () => {},
    onchange: null,
    dispatchEvent: () => true,
  };
  /** @param {boolean} newMatches */
  const fire = (newMatches) => {
    mql.matches = newMatches;
    listeners.forEach((fn) =>
      fn(/** @type {any} */ ({ matches: newMatches })),
    );
  };
  vi.stubGlobal(
    "matchMedia",
    vi.fn(() => mql),
  );
  // happy-dom puts matchMedia on window; stub there too for safety.
  window.matchMedia = /** @type {any} */ (vi.fn(() => mql));
  return { mql, fire, listenerCount: () => listeners.size };
}

beforeEach(() => {
  localStorage.clear();
  for (const k of /** @type {const} */ ([
    "emptyCellColor",
    "theme",
    "mode",
    "autoCallEnabled",
    "autoCallSpeed",
    "voiceEnabledMaster",
    "voiceEnabledPlayer",
    "voiceWaitingNumber",
    "voice",
  ])) {
    /** @type {any} */ (settings)[k] = /** @type {any} */ (DEFAULT_SETTINGS)[k];
  }
  document.documentElement.style.removeProperty("--empty-cell-bg");
  document.documentElement.classList.remove("dark");
  mockMatchMedia(false);
});

describe("settings-store — defaults", () => {
  it("DEFAULT_SETTINGS is frozen", () => {
    expect(Object.isFrozen(DEFAULT_SETTINGS)).toBe(true);
  });

  it("default empty-cell color is Excel Standard Purple (#7030A0)", () => {
    expect(DEFAULT_SETTINGS.emptyCellColor).toBe("#7030A0");
  });

  it("default theme is auto", () => {
    expect(DEFAULT_SETTINGS.theme).toBe("auto");
  });

  it("mode defaults to player; autoCallEnabled false; autoCallSpeed 5", () => {
    expect(DEFAULT_SETTINGS.mode).toBe("player");
    expect(DEFAULT_SETTINGS.autoCallEnabled).toBe(false);
    expect(DEFAULT_SETTINGS.autoCallSpeed).toBe(5);
  });

  it("master voice ON, player voice + waiting-number OFF by default", () => {
    expect(DEFAULT_SETTINGS.voiceEnabledMaster).toBe(true);
    expect(DEFAULT_SETTINGS.voiceEnabledPlayer).toBe(false);
    expect(DEFAULT_SETTINGS.voiceWaitingNumber).toBe(false);
    expect(typeof DEFAULT_SETTINGS.voice).toBe("string");
    expect(DEFAULT_SETTINGS.voice.length).toBeGreaterThan(0);
  });

  it("voice round-trips through localStorage and survives reload", () => {
    settings.voice = "nam-minh";
    settings.voiceEnabledMaster = false;
    saveSettings();
    settings.voice = "hoai-my";
    settings.voiceEnabledMaster = true;
    loadSettings();
    expect(settings.voice).toBe("nam-minh");
    expect(settings.voiceEnabledMaster).toBe(false);
  });

  it("invalid voice id falls back to default; other settings survive", () => {
    localStorage.setItem(
      KEY,
      JSON.stringify({
        ...DEFAULT_SETTINGS,
        voice: "made-up-voice-id",
        autoCallSpeed: 7,
      }),
    );
    loadSettings();
    expect(settings.voice).toBe(DEFAULT_SETTINGS.voice);
    expect(settings.autoCallSpeed).toBe(7);
  });

  it("non-boolean voiceEnabled* falls back to default", () => {
    localStorage.setItem(
      KEY,
      JSON.stringify({
        ...DEFAULT_SETTINGS,
        voiceEnabledMaster: "yes",
        voiceEnabledPlayer: 1,
      }),
    );
    loadSettings();
    expect(settings.voiceEnabledMaster).toBe(
      DEFAULT_SETTINGS.voiceEnabledMaster,
    );
    expect(settings.voiceEnabledPlayer).toBe(
      DEFAULT_SETTINGS.voiceEnabledPlayer,
    );
  });
});

describe("settings-store — loadSettings (color)", () => {
  it("uses defaults when localStorage is empty and applies CSS var", () => {
    loadSettings();
    expect(settings.emptyCellColor).toBe(DEFAULT_SETTINGS.emptyCellColor);
    expect(
      document.documentElement.style.getPropertyValue("--empty-cell-bg"),
    ).toBe(DEFAULT_SETTINGS.emptyCellColor);
  });

  it("reads a previously-saved valid color", () => {
    localStorage.setItem(KEY, JSON.stringify({ emptyCellColor: "#abcdef" }));
    loadSettings();
    expect(settings.emptyCellColor).toBe("#abcdef");
    expect(
      document.documentElement.style.getPropertyValue("--empty-cell-bg"),
    ).toBe("#abcdef");
  });

  it("ignores an invalid stored color and keeps the default", () => {
    localStorage.setItem(KEY, JSON.stringify({ emptyCellColor: "not-a-color" }));
    loadSettings();
    expect(settings.emptyCellColor).toBe(DEFAULT_SETTINGS.emptyCellColor);
  });

  it("ignores a wrong-shape JSON payload", () => {
    localStorage.setItem(KEY, JSON.stringify({ unrelated: 42 }));
    loadSettings();
    expect(settings.emptyCellColor).toBe(DEFAULT_SETTINGS.emptyCellColor);
  });

  it("survives corrupt JSON without throwing", () => {
    localStorage.setItem(KEY, "{not json");
    expect(() => loadSettings()).not.toThrow();
    expect(settings.emptyCellColor).toBe(DEFAULT_SETTINGS.emptyCellColor);
  });

  it("rejects 3-digit hex shorthand (#fff)", () => {
    localStorage.setItem(KEY, JSON.stringify({ emptyCellColor: "#fff" }));
    loadSettings();
    expect(settings.emptyCellColor).toBe(DEFAULT_SETTINGS.emptyCellColor);
  });

  it("accepts uppercase hex", () => {
    localStorage.setItem(KEY, JSON.stringify({ emptyCellColor: "#ABCDEF" }));
    loadSettings();
    expect(settings.emptyCellColor).toBe("#ABCDEF");
  });
});

describe("settings-store — loadSettings (per-key fallback)", () => {
  it("preserves a single old key (color only) without wiping it", () => {
    localStorage.setItem(KEY, JSON.stringify({ emptyCellColor: "#1e88e5" }));
    loadSettings();
    expect(settings.emptyCellColor).toBe("#1e88e5");
    expect(settings.theme).toBe(DEFAULT_SETTINGS.theme);
    expect(settings.mode).toBe(DEFAULT_SETTINGS.mode);
  });

  it("loads valid theme values", () => {
    for (const t of /** @type {const} */ (["auto", "light", "dark"])) {
      localStorage.setItem(KEY, JSON.stringify({ theme: t }));
      loadSettings();
      expect(settings.theme).toBe(t);
    }
  });

  it("falls back to default theme on invalid value", () => {
    localStorage.setItem(KEY, JSON.stringify({ theme: "neon" }));
    loadSettings();
    expect(settings.theme).toBe(DEFAULT_SETTINGS.theme);
  });

  it("loads autoCallEnabled boolean", () => {
    localStorage.setItem(KEY, JSON.stringify({ autoCallEnabled: true }));
    loadSettings();
    expect(settings.autoCallEnabled).toBe(true);
  });

  it("loads valid mode values", () => {
    for (const m of /** @type {const} */ (["player", "master", "both"])) {
      localStorage.setItem(KEY, JSON.stringify({ mode: m }));
      loadSettings();
      expect(settings.mode).toBe(m);
    }
  });

  it("rejects invalid mode value", () => {
    localStorage.setItem(KEY, JSON.stringify({ mode: "spectator" }));
    loadSettings();
    expect(settings.mode).toBe(DEFAULT_SETTINGS.mode);
  });

  it("migrates legacy masterMode=true → mode='both'", () => {
    localStorage.setItem(KEY, JSON.stringify({ masterMode: true }));
    loadSettings();
    expect(settings.mode).toBe("both");
  });

  it("legacy masterMode=false maps to default mode (player)", () => {
    localStorage.setItem(KEY, JSON.stringify({ masterMode: false }));
    loadSettings();
    expect(settings.mode).toBe("player");
  });

  it("explicit mode wins over legacy masterMode", () => {
    localStorage.setItem(
      KEY,
      JSON.stringify({ mode: "master", masterMode: true }),
    );
    loadSettings();
    expect(settings.mode).toBe("master");
  });

  it("loads autoCallSpeed in range 1..10", () => {
    for (const n of [1, 5, 10]) {
      localStorage.setItem(KEY, JSON.stringify({ autoCallSpeed: n }));
      loadSettings();
      expect(settings.autoCallSpeed).toBe(n);
    }
  });

  it("rejects out-of-range and non-integer autoCallSpeed", () => {
    for (const bad of [0, 11, 5.5, -1, "5", null]) {
      localStorage.setItem(KEY, JSON.stringify({ autoCallSpeed: bad }));
      loadSettings();
      expect(settings.autoCallSpeed).toBe(DEFAULT_SETTINGS.autoCallSpeed);
    }
  });
});

describe("settings-store — saveSettings", () => {
  it("persists ALL keys, not just color", () => {
    settings.emptyCellColor = "#112233";
    settings.theme = "dark";
    settings.mode = "both";
    settings.autoCallEnabled = true;
    settings.autoCallSpeed = 7;
    settings.voiceEnabledMaster = false;
    settings.voiceEnabledPlayer = true;
    settings.voiceWaitingNumber = true;
    settings.voice = "nam-minh";
    saveSettings();
    const raw = localStorage.getItem(KEY);
    expect(raw).not.toBeNull();
    expect(JSON.parse(/** @type {string} */ (raw))).toEqual({
      emptyCellColor: "#112233",
      theme: "dark",
      mode: "both",
      autoCallEnabled: true,
      autoCallSpeed: 7,
      voiceEnabledMaster: false,
      voiceEnabledPlayer: true,
      voiceWaitingNumber: true,
      voice: "nam-minh",
    });
  });

  it("pushes the color to the document CSS var", () => {
    settings.emptyCellColor = "#445566";
    saveSettings();
    expect(
      document.documentElement.style.getPropertyValue("--empty-cell-bg"),
    ).toBe("#445566");
  });
});

describe("settings-store — resetSettings", () => {
  it("returns ALL settings to defaults and persists", () => {
    settings.emptyCellColor = "#000000";
    settings.theme = "dark";
    settings.mode = "both";
    saveSettings();
    resetSettings();
    expect(settings.emptyCellColor).toBe(DEFAULT_SETTINGS.emptyCellColor);
    expect(settings.theme).toBe(DEFAULT_SETTINGS.theme);
    expect(settings.mode).toBe(DEFAULT_SETTINGS.mode);
    const stored = JSON.parse(
      /** @type {string} */ (localStorage.getItem(KEY)),
    );
    expect(stored.emptyCellColor).toBe(DEFAULT_SETTINGS.emptyCellColor);
    expect(stored.theme).toBe(DEFAULT_SETTINGS.theme);
  });
});

describe("settings-store — applyTheme via load/save", () => {
  it('theme="light" removes dark class', () => {
    document.documentElement.classList.add("dark"); // pretend dark was active
    settings.theme = "light";
    saveSettings();
    expect(document.documentElement.classList.contains("dark")).toBe(false);
  });

  it('theme="dark" adds dark class', () => {
    settings.theme = "dark";
    saveSettings();
    expect(document.documentElement.classList.contains("dark")).toBe(true);
  });

  it('theme="auto" mirrors matchMedia.matches=true', () => {
    mockMatchMedia(true);
    settings.theme = "auto";
    saveSettings();
    expect(document.documentElement.classList.contains("dark")).toBe(true);
  });

  it('theme="auto" mirrors matchMedia.matches=false', () => {
    mockMatchMedia(false);
    settings.theme = "auto";
    saveSettings();
    expect(document.documentElement.classList.contains("dark")).toBe(false);
  });

  it('theme="auto" re-applies on matchMedia change event', () => {
    const { fire } = mockMatchMedia(false);
    settings.theme = "auto";
    saveSettings();
    expect(document.documentElement.classList.contains("dark")).toBe(false);
    fire(true);
    expect(document.documentElement.classList.contains("dark")).toBe(true);
    fire(false);
    expect(document.documentElement.classList.contains("dark")).toBe(false);
  });

  it('switching auto → dark detaches matchMedia listener', () => {
    const { fire, listenerCount } = mockMatchMedia(false);
    settings.theme = "auto";
    saveSettings();
    expect(listenerCount()).toBe(1);
    settings.theme = "dark";
    saveSettings();
    expect(listenerCount()).toBe(0);
    // After detach, OS pref change must NOT toggle the class anymore.
    fire(false);
    expect(document.documentElement.classList.contains("dark")).toBe(true);
  });
});
