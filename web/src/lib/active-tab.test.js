// @vitest-environment happy-dom

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

/** Minimal stub matching the BroadcastChannel surface we use. */
class FakeBC {
  /** @type {Map<string, FakeBC[]>} */
  static channels = new Map();
  /** @param {string} name */
  constructor(name) {
    this.name = name;
    /** @type {((e: { data: any }) => void) | null} */
    this.onmessage = null;
    this.closed = false;
    const peers = FakeBC.channels.get(name) ?? [];
    peers.push(this);
    FakeBC.channels.set(name, peers);
  }
  /**
   * Defers delivery to a microtask and re-reads the peer list at delivery
   * time (not at call time), so two channels opened in the same tick — the
   * "two tabs mount simultaneously" case under test — both see each other's
   * claim. This matches real `BroadcastChannel`, which queues a task and
   * resolves the receiving set when that task runs, not when postMessage
   * is called.
   * @param {any} data
   */
  postMessage(data) {
    if (this.closed) return;
    const name = this.name;
    const sender = this;
    queueMicrotask(() => {
      const peers = FakeBC.channels.get(name) ?? [];
      for (const p of peers) {
        if (p === sender || p.closed || !p.onmessage) continue;
        p.onmessage({ data });
      }
    });
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

/** Flush FakeBC's queued microtask message deliveries. */
async function deliverBcMessages() {
  await Promise.resolve();
}

beforeEach(() => {
  FakeBC.channels.clear();
  vi.stubGlobal("BroadcastChannel", /** @type {any} */ (FakeBC));
  vi.resetModules();
  // Default: a strictly increasing "clock" so claim order matches call
  // order without every test having to stub a value. Individual tests
  // override a single call with `mockReturnValueOnce` when they need an
  // exact (e.g. equal or stale) timestamp.
  let clock = 0;
  vi.spyOn(Date, "now").mockImplementation(() => {
    clock += 1;
    return clock;
  });
});

afterEach(() => {
  vi.unstubAllGlobals();
  vi.restoreAllMocks();
});

describe("active-tab", () => {
  it("watchActiveTab returns no-op cleanup when BroadcastChannel is missing", async () => {
    vi.stubGlobal("BroadcastChannel", undefined);
    // @ts-expect-error - "?tag" is a real relative import at runtime (Vite/
    // Vitest cache-busting for module-singleton isolation between tests);
    // TS can't resolve the literal query-string specifier.
    const mod = await import("./active-tab.svelte.js?nobc");
    const cleanup = mod.watchActiveTab();
    expect(typeof cleanup).toBe("function");
    expect(mod.activeTab.inactive).toBe(false);
    cleanup();
  });

  it("second tab's claim marks the first tab inactive", async () => {
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabA = await import("./active-tab.svelte.js?a");
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabB = await import("./active-tab.svelte.js?b");
    tabA.watchActiveTab();
    expect(tabA.activeTab.inactive).toBe(false);
    tabB.watchActiveTab();
    await deliverBcMessages();
    expect(tabA.activeTab.inactive).toBe(true);
    expect(tabB.activeTab.inactive).toBe(false);
  });

  it("claimActiveTab reactivates self and inactivates the other", async () => {
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabA = await import("./active-tab.svelte.js?ra-a");
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabB = await import("./active-tab.svelte.js?ra-b");
    tabA.watchActiveTab();
    tabB.watchActiveTab();
    await deliverBcMessages();
    expect(tabA.activeTab.inactive).toBe(true);
    tabA.claimActiveTab();
    expect(tabA.activeTab.inactive).toBe(false);
    await deliverBcMessages();
    expect(tabB.activeTab.inactive).toBe(true);
  });

  it("watchActiveTab is idempotent within a single module", async () => {
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tab = await import("./active-tab.svelte.js?idem");
    const c1 = tab.watchActiveTab();
    const c2 = tab.watchActiveTab();
    expect(typeof c1).toBe("function");
    expect(typeof c2).toBe("function");
    c1();
    c2();
  });

  it("simultaneous mount with unequal ts: older claim freezes, newer stays active", async () => {
    vi.mocked(Date.now).mockReturnValueOnce(1000);
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabOlder = await import("./active-tab.svelte.js?ts-older");
    tabOlder.watchActiveTab();

    vi.mocked(Date.now).mockReturnValueOnce(2000);
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabNewer = await import("./active-tab.svelte.js?ts-newer");
    tabNewer.watchActiveTab();
    await deliverBcMessages();

    expect(tabOlder.activeTab.inactive).toBe(true);
    expect(tabNewer.activeTab.inactive).toBe(false);
  });

  it("simultaneous mount with equal ts: exactly one tab wins via id tie-break", async () => {
    const idLow = "11111111-1111-4111-8111-111111111111";
    const idHigh = "99999999-9999-4999-8999-999999999999";

    vi.spyOn(crypto, "randomUUID").mockReturnValueOnce(
      /** @type {`${string}-${string}-${string}-${string}-${string}`} */ (
        idLow
      ),
    );
    vi.mocked(Date.now).mockReturnValueOnce(5000);
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabLow = await import("./active-tab.svelte.js?tie-low");
    tabLow.watchActiveTab();

    vi.spyOn(crypto, "randomUUID").mockReturnValueOnce(
      /** @type {`${string}-${string}-${string}-${string}-${string}`} */ (
        idHigh
      ),
    );
    vi.mocked(Date.now).mockReturnValueOnce(5000);
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabHigh = await import("./active-tab.svelte.js?tie-high");
    tabHigh.watchActiveTab();
    await deliverBcMessages();

    // Equal claim ts: the lexicographically greater id wins, regardless of
    // mount order — both tabs receive each other's claim and independently
    // reach the same conclusion.
    expect(tabLow.activeTab.inactive).toBe(true);
    expect(tabHigh.activeTab.inactive).toBe(false);
  });

  it("reclaim updates myClaimTs so a stale peer claim can't re-freeze the reclaimed tab", async () => {
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabA = await import("./active-tab.svelte.js?stale-a");
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabB = await import("./active-tab.svelte.js?stale-b");
    tabA.watchActiveTab();
    tabB.watchActiveTab();
    await deliverBcMessages();
    expect(tabA.activeTab.inactive).toBe(true);

    // A reclaims — this records a fresh, later claim ts and reactivates
    // immediately.
    tabA.claimActiveTab();
    expect(tabA.activeTab.inactive).toBe(false);
    await deliverBcMessages();
    expect(tabB.activeTab.inactive).toBe(true);

    // A stale claim from B (older than A's reclaim — e.g. a delayed
    // message) must not re-freeze A. A ignores it as a loss for B and
    // echoes its own winning claim back, so B (the stale claimer) freezes
    // instead of staying active locally — exactly one tab ends up active.
    vi.mocked(Date.now).mockReturnValueOnce(1);
    tabB.claimActiveTab();
    await deliverBcMessages(); // B's stale claim reaches A; A echoes
    await deliverBcMessages(); // A's echo reaches B; B freezes
    expect(tabA.activeTab.inactive).toBe(false);
    expect(tabB.activeTab.inactive).toBe(true);
  });

  it("a losing claimer is echoed at and freezes instead of leaving both tabs active", async () => {
    // Reproduces the asymmetric-delivery race: tabA's own initial claim is
    // flushed (delivered to nobody) before tabB even mounts, so tabB never
    // sees it — matching real BroadcastChannel, which doesn't replay past
    // messages to a channel opened later. Both claims then carry the same
    // ts, so only the id tie-break decides the winner, and only tabA (the
    // one still open when tabB's claim arrives) evaluates it.
    const idLow = "11111111-1111-4111-8111-111111111111";
    const idHigh = "99999999-9999-4999-8999-999999999999";

    vi.spyOn(crypto, "randomUUID").mockReturnValueOnce(
      /** @type {`${string}-${string}-${string}-${string}-${string}`} */ (
        idHigh
      ),
    );
    vi.mocked(Date.now).mockReturnValueOnce(3000);
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabA = await import("./active-tab.svelte.js?echo-a");
    tabA.watchActiveTab();
    await deliverBcMessages(); // tabA's own claim is flushed with no peers

    vi.spyOn(crypto, "randomUUID").mockReturnValueOnce(
      /** @type {`${string}-${string}-${string}-${string}-${string}`} */ (
        idLow
      ),
    );
    vi.mocked(Date.now).mockReturnValueOnce(3000);
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabB = await import("./active-tab.svelte.js?echo-b");
    tabB.watchActiveTab();
    await deliverBcMessages(); // tabB's claim reaches only tabA
    await deliverBcMessages(); // tabA's echo (it strictly wins the tie) reaches tabB

    // tabA has the greater id, so on the tie it wins and never freezes;
    // without the echo fix tabB would also stay active (both active).
    expect(tabA.activeTab.inactive).toBe(false);
    expect(tabB.activeTab.inactive).toBe(true);
  });

  it("the echo does not loop: message traffic settles after one echo round", async () => {
    const postSpy = vi.spyOn(FakeBC.prototype, "postMessage");

    vi.mocked(Date.now).mockReturnValueOnce(2000);
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabWinner = await import("./active-tab.svelte.js?loop-winner");
    tabWinner.watchActiveTab();
    await deliverBcMessages(); // winner's own claim, delivered to nobody yet

    vi.mocked(Date.now).mockReturnValueOnce(1000);
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tabLoser = await import("./active-tab.svelte.js?loop-loser");
    tabLoser.watchActiveTab();
    await deliverBcMessages(); // loser's stale claim reaches winner; winner echoes
    await deliverBcMessages(); // winner's echo reaches loser; loser freezes

    expect(tabWinner.activeTab.inactive).toBe(false);
    expect(tabLoser.activeTab.inactive).toBe(true);

    // Winner's initial claim + loser's stale claim + one echo from the
    // winner. The loser never echoes back (it's frozen and always
    // strictly loses the winner's unchanged ts), so traffic must stop here.
    const settledCount = postSpy.mock.calls.length;
    expect(settledCount).toBe(3);

    // Flushing further must not produce any more messages.
    await deliverBcMessages();
    await deliverBcMessages();
    expect(postSpy.mock.calls.length).toBe(settledCount);
  });

  it("a peer claim with no ts (legacy tab) always wins, matching old always-freeze behavior", async () => {
    // @ts-expect-error - "?tag" cache-busting import, see the note above.
    const tab = await import("./active-tab.svelte.js?legacy");
    tab.watchActiveTab();
    await deliverBcMessages(); // this tab's own claim, delivered to nobody

    // A pre-fix peer broadcasts {type, id} with no `ts` at all.
    const legacyPeer = new /** @type {any} */ (BroadcastChannel)(
      "loto_active_tab",
    );
    legacyPeer.postMessage({ type: "claim", id: "legacy-peer" });
    await deliverBcMessages();

    expect(tab.activeTab.inactive).toBe(true);
    legacyPeer.close();
  });
});
