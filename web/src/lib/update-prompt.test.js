// @vitest-environment happy-dom

import { beforeEach, describe, expect, it, vi } from "vitest";

import {
  _resetUpdatePromptForTest,
  applyUpdate,
  dismissUpdate,
  setUpdateSW,
  showUpdatePrompt,
  updatePrompt,
} from "./update-prompt.svelte.js";

beforeEach(() => {
  _resetUpdatePromptForTest();
});

describe("update-prompt", () => {
  it("starts hidden", () => {
    expect(updatePrompt.visible).toBe(false);
  });

  it("showUpdatePrompt (onNeedRefresh) reveals the banner", () => {
    showUpdatePrompt();
    expect(updatePrompt.visible).toBe(true);
  });

  it("dismissUpdate hides the banner without calling updateSW", () => {
    const updateSW = vi.fn().mockResolvedValue(undefined);
    setUpdateSW(updateSW);
    showUpdatePrompt();
    dismissUpdate();
    expect(updatePrompt.visible).toBe(false);
    expect(updateSW).not.toHaveBeenCalled();
  });

  it("applyUpdate hides the banner and reloads via updateSW(true)", () => {
    const updateSW = vi.fn().mockResolvedValue(undefined);
    setUpdateSW(updateSW);
    showUpdatePrompt();
    applyUpdate();
    expect(updatePrompt.visible).toBe(false);
    expect(updateSW).toHaveBeenCalledTimes(1);
    expect(updateSW).toHaveBeenCalledWith(true);
  });

  it("applyUpdate before registerSW resolves is a safe no-op (no throw)", () => {
    showUpdatePrompt();
    expect(() => applyUpdate()).not.toThrow();
    expect(updatePrompt.visible).toBe(false);
  });
});
