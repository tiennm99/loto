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

describe("active-tab", () => {
  it("watchActiveTab returns no-op cleanup when BroadcastChannel is missing", async () => {
    vi.stubGlobal("BroadcastChannel", undefined);
    const mod = await import("./active-tab.svelte.js?nobc");
    const cleanup = mod.watchActiveTab();
    expect(typeof cleanup).toBe("function");
    expect(mod.activeTab.inactive).toBe(false);
    cleanup();
  });

  it("second tab's claim marks the first tab inactive", async () => {
    const tabA = await import("./active-tab.svelte.js?a");
    const tabB = await import("./active-tab.svelte.js?b");
    tabA.watchActiveTab();
    expect(tabA.activeTab.inactive).toBe(false);
    tabB.watchActiveTab();
    expect(tabA.activeTab.inactive).toBe(true);
    expect(tabB.activeTab.inactive).toBe(false);
  });

  it("claimActiveTab reactivates self and inactivates the other", async () => {
    const tabA = await import("./active-tab.svelte.js?ra-a");
    const tabB = await import("./active-tab.svelte.js?ra-b");
    tabA.watchActiveTab();
    tabB.watchActiveTab();
    expect(tabA.activeTab.inactive).toBe(true);
    tabA.claimActiveTab();
    expect(tabA.activeTab.inactive).toBe(false);
    expect(tabB.activeTab.inactive).toBe(true);
  });

  it("watchActiveTab is idempotent within a single module", async () => {
    const tab = await import("./active-tab.svelte.js?idem");
    const c1 = tab.watchActiveTab();
    const c2 = tab.watchActiveTab();
    expect(typeof c1).toBe("function");
    expect(typeof c2).toBe("function");
    c1();
    c2();
  });
});
