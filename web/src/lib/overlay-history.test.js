// @vitest-environment happy-dom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  _resetOverlayHistoryForTest,
  pushOverlay,
} from "./overlay-history.js";

// happy-dom's History does not reliably emit popstate for back(), so we drive
// the event by hand. That also keeps these tests about our own bookkeeping
// rather than the polyfill's fidelity.
function firePopState() {
  window.dispatchEvent(new Event("popstate"));
}

/** @type {ReturnType<typeof vi.spyOn>} */
let back;
/** @type {ReturnType<typeof vi.spyOn>} */
let pushState;

beforeEach(() => {
  _resetOverlayHistoryForTest();
  back = vi.spyOn(history, "back").mockImplementation(() => {});
  pushState = vi.spyOn(history, "pushState").mockImplementation(() => {});
});

afterEach(() => {
  vi.restoreAllMocks();
});

describe("overlay-history", () => {
  it("pushes one history entry per overlay", () => {
    pushOverlay(() => {});
    expect(pushState).toHaveBeenCalledTimes(1);
  });

  it("omits SvelteKit's history index so the router ignores the entry", () => {
    pushOverlay(() => {});
    const state = pushState.mock.calls[0][0];
    // SvelteKit gates its popstate handler on this key; if we ever leak it,
    // back presses would trigger router navigation instead of closing a modal.
    expect(Object.keys(state)).toEqual(["lotoOverlay"]);
  });

  it("closes the overlay on back", () => {
    const close = vi.fn();
    pushOverlay(close);
    firePopState();
    expect(close).toHaveBeenCalledTimes(1);
  });

  it("programmatic close pops the sentinel without re-closing", () => {
    const close = vi.fn();
    const dispose = pushOverlay(close);

    dispose();
    expect(back).toHaveBeenCalledTimes(1);

    // The back() above produces a popstate; it must be swallowed.
    firePopState();
    expect(close).not.toHaveBeenCalled();
  });

  it("closes nested overlays LIFO", () => {
    const closeA = vi.fn();
    const closeB = vi.fn();
    pushOverlay(closeA);
    pushOverlay(closeB);

    firePopState();
    expect(closeB).toHaveBeenCalledTimes(1);
    expect(closeA).not.toHaveBeenCalled();

    firePopState();
    expect(closeA).toHaveBeenCalledTimes(1);
  });

  it("disposing a non-top overlay keeps the remaining one closable", () => {
    const closeA = vi.fn();
    const closeB = vi.fn();
    const disposeA = pushOverlay(closeA);
    pushOverlay(closeB);

    disposeA();
    expect(back).toHaveBeenCalledTimes(1);
    firePopState(); // swallowed — caused by disposeA
    expect(closeB).not.toHaveBeenCalled();

    firePopState(); // real back press
    expect(closeB).toHaveBeenCalledTimes(1);
    expect(closeA).not.toHaveBeenCalled();
  });

  it("disposing twice is harmless", () => {
    const close = vi.fn();
    const dispose = pushOverlay(close);
    dispose();
    dispose();
    expect(back).toHaveBeenCalledTimes(1);
  });

  it("a back press after the overlay already closed is a no-op", () => {
    const close = vi.fn();
    const dispose = pushOverlay(close);
    dispose();
    firePopState(); // swallowed
    expect(() => firePopState()).not.toThrow();
    expect(close).not.toHaveBeenCalled();
  });
});
