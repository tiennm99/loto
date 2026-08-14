// @vitest-environment happy-dom
import { afterEach, beforeEach, describe, expect, it } from "vitest";
import { _resetWakeLockForTest, setWakeLock } from "./wake-lock.js";

class FakeSentinel {
  constructor() {
    this.released = false;
    /** @type {Record<string, () => void>} */
    this.listeners = {};
  }
  release() {
    this.released = true;
    this.listeners.release?.();
    return Promise.resolve();
  }
  /** @param {string} type @param {() => void} fn */
  addEventListener(type, fn) {
    this.listeners[type] = fn;
  }
}

/** @type {FakeSentinel[]} */
let sentinels = [];
/** @type {() => Promise<any>} */
let requestImpl;
let requestCount = 0;

/** @param {"visible" | "hidden"} state */
function setVisibility(state) {
  Object.defineProperty(document, "visibilityState", {
    configurable: true,
    get: () => state,
  });
  document.dispatchEvent(new Event("visibilitychange"));
}

function grantWakeLock() {
  Object.defineProperty(navigator, "wakeLock", {
    configurable: true,
    value: {
      request: () => {
        requestCount++;
        return requestImpl();
      },
    },
  });
}

beforeEach(() => {
  sentinels = [];
  requestCount = 0;
  requestImpl = () => {
    const s = new FakeSentinel();
    sentinels.push(s);
    return Promise.resolve(s);
  };
  grantWakeLock();
  setVisibility("visible");
});

afterEach(() => {
  _resetWakeLockForTest();
  Reflect.deleteProperty(navigator, "wakeLock");
});

describe("wake-lock", () => {
  it("acquires a screen lock when turned on", async () => {
    setWakeLock(true);
    await Promise.resolve();
    expect(requestCount).toBe(1);
    expect(sentinels[0].released).toBe(false);
  });

  it("releases the lock when turned off", async () => {
    setWakeLock(true);
    await Promise.resolve();
    setWakeLock(false);
    expect(sentinels[0].released).toBe(true);
  });

  it("is idempotent — repeat calls do not stack locks", async () => {
    setWakeLock(true);
    setWakeLock(true);
    await Promise.resolve();
    setWakeLock(true);
    await Promise.resolve();
    expect(requestCount).toBe(1);
  });

  it("re-acquires after the system drops the lock and the app returns", async () => {
    setWakeLock(true);
    await Promise.resolve();
    expect(requestCount).toBe(1);

    // Android releases the lock on hide; the sentinel fires "release".
    sentinels[0].release();
    setVisibility("hidden");
    setVisibility("visible");
    await Promise.resolve();

    expect(requestCount).toBe(2);
    expect(sentinels[1].released).toBe(false);
  });

  it("does not re-acquire once turned off", async () => {
    setWakeLock(true);
    await Promise.resolve();
    setWakeLock(false);
    setVisibility("hidden");
    setVisibility("visible");
    await Promise.resolve();
    expect(requestCount).toBe(1);
  });

  it("releases a lock that resolves after the caller turned it off", async () => {
    /** @type {(s: FakeSentinel) => void} */
    let resolveRequest = () => {};
    requestImpl = () =>
      new Promise((resolve) => {
        resolveRequest = resolve;
      });

    setWakeLock(true);
    setWakeLock(false);

    const late = new FakeSentinel();
    resolveRequest(late);
    await Promise.resolve();
    await Promise.resolve();

    // Must not leak a lock the UI already considers dropped.
    expect(late.released).toBe(true);
  });

  it("no-ops without navigator.wakeLock", () => {
    Reflect.deleteProperty(navigator, "wakeLock");
    expect(() => setWakeLock(true)).not.toThrow();
    expect(() => setWakeLock(false)).not.toThrow();
  });

  it("swallows a rejected request", async () => {
    requestImpl = () => Promise.reject(new Error("battery saver"));
    setWakeLock(true);
    await Promise.resolve();
    await Promise.resolve();
    expect(() => setWakeLock(false)).not.toThrow();
  });
});
