// @vitest-environment happy-dom
import { beforeEach, describe, expect, it } from "vitest";
import { broadcastDraw, bus, resetBus } from "./call-bus.svelte.js";

beforeEach(() => {
  resetBus();
});

describe("call-bus", () => {
  it("starts with lastDrawn=null", () => {
    expect(bus.lastDrawn).toBeNull();
  });

  it("broadcastDraw stores the number", () => {
    broadcastDraw(42);
    expect(bus.lastDrawn?.num).toBe(42);
    expect(typeof bus.lastDrawn?.at).toBe("number");
  });

  it("broadcasting the same number twice creates a fresh object", () => {
    broadcastDraw(7);
    const first = bus.lastDrawn;
    broadcastDraw(7);
    const second = bus.lastDrawn;
    expect(second?.num).toBe(7);
    expect(second).not.toBe(first);
  });

  it("resetBus clears lastDrawn", () => {
    broadcastDraw(15);
    expect(bus.lastDrawn).not.toBeNull();
    resetBus();
    expect(bus.lastDrawn).toBeNull();
  });
});
