// @vitest-environment happy-dom
import { afterEach, beforeEach, describe, expect, it } from "vitest";

import { focusTrap } from "./focus-trap.js";

/** @type {HTMLElement} */
let outsideButton;
/** @type {HTMLDivElement} */
let dialog;
/** @type {HTMLButtonElement} */
let first;
/** @type {HTMLButtonElement} */
let last;

beforeEach(() => {
  document.body.innerHTML = "";
  document.body.style.overflow = "";

  outsideButton = document.createElement("button");
  outsideButton.textContent = "outside";
  document.body.appendChild(outsideButton);
  outsideButton.focus();

  dialog = document.createElement("div");
  first = document.createElement("button");
  first.textContent = "first";
  const middle = document.createElement("button");
  middle.textContent = "middle";
  last = document.createElement("button");
  last.textContent = "last";
  dialog.append(first, middle, last);
  document.body.appendChild(dialog);
});

afterEach(() => {
  document.body.innerHTML = "";
  document.body.style.overflow = "";
});

/** @param {HTMLElement} target @param {boolean} [shiftKey] */
function tab(target, shiftKey = false) {
  const event = new KeyboardEvent("keydown", {
    key: "Tab",
    shiftKey,
    cancelable: true,
    bubbles: true,
  });
  target.dispatchEvent(event);
  return event;
}

describe("focus-trap (M4)", () => {
  it("focuses the dialog container on mount", () => {
    const trap = focusTrap(dialog);
    expect(document.activeElement).toBe(dialog);
    expect(dialog.getAttribute("tabindex")).toBe("-1");
    trap.destroy();
  });

  it("wraps Tab from the last focusable back to the first", () => {
    const trap = focusTrap(dialog);
    last.focus();
    const event = tab(dialog);
    expect(event.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(first);
    trap.destroy();
  });

  it("wraps Shift+Tab from the first focusable back to the last", () => {
    const trap = focusTrap(dialog);
    first.focus();
    const event = tab(dialog, true);
    expect(event.defaultPrevented).toBe(true);
    expect(document.activeElement).toBe(last);
    trap.destroy();
  });

  it("locks body scroll while active and restores it on destroy", () => {
    document.body.style.overflow = "auto";
    const trap = focusTrap(dialog);
    expect(document.body.style.overflow).toBe("hidden");
    trap.destroy();
    expect(document.body.style.overflow).toBe("auto");
  });

  it("restores focus to the previously-focused element on destroy", () => {
    expect(document.activeElement).toBe(outsideButton);
    const trap = focusTrap(dialog);
    expect(document.activeElement).toBe(dialog);
    trap.destroy();
    expect(document.activeElement).toBe(outsideButton);
  });

  it("does not fight over the body scroll lock across two stacked traps", () => {
    const outer = focusTrap(dialog);
    const dialog2 = document.createElement("div");
    const btn2 = document.createElement("button");
    dialog2.appendChild(btn2);
    document.body.appendChild(dialog2);
    const inner = focusTrap(dialog2);

    expect(document.body.style.overflow).toBe("hidden");
    inner.destroy();
    // Outer trap is still active — lock must stay held.
    expect(document.body.style.overflow).toBe("hidden");
    outer.destroy();
    expect(document.body.style.overflow).toBe("");
  });
});
