// @vitest-environment happy-dom

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

/** Minimal stub matching the BroadcastChannel surface we use. */
class FakeBC {
  /** @type {Map<string, FakeBC[]>} */
  static channels = new Map();
  /** @param {string} name */
  constructor(name) {
    this.name = name;
    this.onmessage = null;
    this.closed = false;
    const peers = FakeBC.channels.get(name) ?? [];
    peers.push(this);
    FakeBC.channels.set(name, peers);
  }
  /** @param {any} data */
  postMessage(data) {
    if (this.closed) return;
    const peers = FakeBC.channels.get(this.name) ?? [];
    for (const p of peers) {
      if (p === this || p.closed || !p.onmessage) continue;
      p.onmessage({ data });
    }
  }
  close() {
    this.closed = true;
    const peers = FakeBC.channels.get(this.name) ?? [];
    FakeBC.channels.set(
      this.name,
      peers.filter((p) => p !== this),
    );
  }
}

beforeEach(() => {
  FakeBC.channels.clear();
  vi.stubGlobal("BroadcastChannel", /** @type {any} */ (FakeBC));
  vi.resetModules();
});

afterEach(() => {
  vi.unstubAllGlobals();
});

describe("tab-lock", () => {
  it("startTabLock returns no-op cleanup when BroadcastChannel is missing", async () => {
    vi.stubGlobal("BroadcastChannel", undefined);
    const mod = await import("./tab-lock.svelte.js?nobc");
    const cleanup = mod.startTabLock();
    expect(typeof cleanup).toBe("function");
    expect(mod.tabLock.frozen).toBe(false);
    cleanup();
  });

  it("second tab's claim freezes the first tab", async () => {
    const tabA = await import("./tab-lock.svelte.js?a");
    const tabB = await import("./tab-lock.svelte.js?b");
    tabA.startTabLock();
    expect(tabA.tabLock.frozen).toBe(false);
    tabB.startTabLock();
    expect(tabA.tabLock.frozen).toBe(true);
    expect(tabB.tabLock.frozen).toBe(false);
  });

  it("reclaimTab unfreezes self and freezes the other", async () => {
    const tabA = await import("./tab-lock.svelte.js?ra-a");
    const tabB = await import("./tab-lock.svelte.js?ra-b");
    tabA.startTabLock();
    tabB.startTabLock();
    expect(tabA.tabLock.frozen).toBe(true);
    tabA.reclaimTab();
    expect(tabA.tabLock.frozen).toBe(false);
    expect(tabB.tabLock.frozen).toBe(true);
  });

  it("startTabLock is idempotent within a single module", async () => {
    const tab = await import("./tab-lock.svelte.js?idem");
    const c1 = tab.startTabLock();
    const c2 = tab.startTabLock();
    expect(typeof c1).toBe("function");
    expect(typeof c2).toBe("function");
    c1();
    c2();
  });
});
