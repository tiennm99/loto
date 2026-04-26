// @vitest-environment happy-dom
import { beforeEach, describe, expect, it } from "vitest";
import {
  DEFAULT_SETTINGS,
  loadSettings,
  resetSettings,
  saveSettings,
  settings,
} from "./settings-store.svelte.js";

const KEY = "loto_settings";

beforeEach(() => {
  localStorage.clear();
  // Restore in-memory state to defaults between tests so order doesn't matter.
  settings.emptyCellColor = DEFAULT_SETTINGS.emptyCellColor;
  document.documentElement.style.removeProperty("--empty-cell-bg");
});

describe("settings-store — defaults", () => {
  it("DEFAULT_SETTINGS is frozen", () => {
    expect(Object.isFrozen(DEFAULT_SETTINGS)).toBe(true);
  });

  it("default empty-cell color is brown (#7a4a2b)", () => {
    expect(DEFAULT_SETTINGS.emptyCellColor).toBe("#7a4a2b");
  });
});

describe("settings-store — loadSettings", () => {
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

  it("ignores an invalid stored color and keeps the in-memory default", () => {
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

  it("rejects 3-digit hex shorthand (#fff is not valid per regex)", () => {
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

describe("settings-store — saveSettings", () => {
  it("persists current in-memory settings to localStorage", () => {
    settings.emptyCellColor = "#112233";
    saveSettings();
    const raw = localStorage.getItem(KEY);
    expect(raw).not.toBeNull();
    expect(JSON.parse(/** @type {string} */ (raw))).toEqual({
      emptyCellColor: "#112233",
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
  it("returns settings to defaults and persists the reset", () => {
    settings.emptyCellColor = "#000000";
    saveSettings();
    resetSettings();
    expect(settings.emptyCellColor).toBe(DEFAULT_SETTINGS.emptyCellColor);
    expect(JSON.parse(/** @type {string} */ (localStorage.getItem(KEY)))).toEqual({
      emptyCellColor: DEFAULT_SETTINGS.emptyCellColor,
    });
  });
});
