// @vitest-environment happy-dom

import { beforeEach, describe, expect, it } from "vitest";
import {
  drawNext,
  loadMaster,
  masterState,
  saveMaster,
  startNewGame,
} from "./master-store.svelte.js";

beforeEach(() => {
  localStorage.clear();
  masterState.called = [];
  masterState.remaining = [];
  masterState.hydrated = false;
});

describe("master-store", () => {
  it("starts empty", () => {
    expect(masterState.called).toEqual([]);
    expect(masterState.remaining).toEqual([]);
  });

  it("startNewGame fills remaining with 90 unique 1..90", () => {
    startNewGame();
    expect(masterState.called).toEqual([]);
    expect(masterState.remaining).toHaveLength(90);
    const set = new Set(masterState.remaining);
    expect(set.size).toBe(90);
    for (let n = 1; n <= 90; n++) expect(set.has(n)).toBe(true);
  });

  it("drawNext appends called and shifts remaining; returns drawn", () => {
    startNewGame();
    const first = masterState.remaining[0];
    const drawn = drawNext();
    expect(drawn).toBe(first);
    expect(masterState.called).toEqual([first]);
    expect(masterState.remaining).toHaveLength(89);
    expect(masterState.remaining.includes(first)).toBe(false);
  });

  it("drawNext returns null when exhausted", () => {
    startNewGame();
    for (let i = 0; i < 90; i++) drawNext();
    expect(drawNext()).toBeNull();
    expect(masterState.called).toHaveLength(90);
    expect(masterState.remaining).toEqual([]);
  });

  it("save + load round-trip preserves state", () => {
    startNewGame();
    drawNext();
    drawNext();
    const calledBefore = [...masterState.called];
    const remainingBefore = [...masterState.remaining];
    saveMaster();
    masterState.called = [];
    masterState.remaining = [];
    expect(masterState.called).toEqual([]);
    loadMaster();
    expect(masterState.called).toEqual(calledBefore);
    expect(masterState.remaining).toEqual(remainingBefore);
  });

  it("loadMaster ignores corrupt JSON", () => {
    localStorage.setItem("loto_master", "{not valid");
    loadMaster();
    expect(masterState.called).toEqual([]);
    expect(masterState.remaining).toEqual([]);
  });

  it("loadMaster ignores non-int range entries", () => {
    localStorage.setItem(
      "loto_master",
      JSON.stringify({ called: [1, 91], remaining: [] }),
    );
    loadMaster();
    expect(masterState.called).toEqual([]);
  });

  it("loadMaster rejects payloads exceeding the size cap", () => {
    const big = "x".repeat(20_000);
    localStorage.setItem("loto_master", big);
    loadMaster();
    expect(masterState.called).toEqual([]);
  });

  describe("hydrated flag (reclaim-a-frozen-tab fix, H2)", () => {
    it("starts false and flips true after the first loadMaster()", () => {
      expect(masterState.hydrated).toBe(false);
      loadMaster();
      expect(masterState.hydrated).toBe(true);
    });

    it("flips true even when there is nothing to load", () => {
      // No localStorage entry at all — still counts as "a load happened",
      // so a consumer's save-effect gate (MasterPanel) is safe to write.
      loadMaster();
      expect(masterState.hydrated).toBe(true);
    });

    it("flips true even when the stored payload is corrupt", () => {
      localStorage.setItem("loto_master", "{not valid");
      loadMaster();
      expect(masterState.hydrated).toBe(true);
    });

    it("re-loading picks up a peer tab's newer writes instead of this tab's stale copy", () => {
      // Simulates the reclaim sequence: this tab drew a couple of numbers,
      // froze (its in-memory masterState is untouched but stale), a peer
      // tab drew more and persisted them, then this tab reclaims and must
      // re-read localStorage before anything re-saves.
      startNewGame();
      drawNext();
      drawNext();
      saveMaster();
      const staleCalled = [...masterState.called];
      const thirdDraw = masterState.remaining[0];

      // Peer tab: independently draws one more and persists.
      const peerCalled = [...staleCalled, thirdDraw];
      localStorage.setItem(
        "loto_master",
        JSON.stringify({
          called: peerCalled,
          remaining: masterState.remaining.slice(1),
        }),
      );

      // Reclaim: claimActiveTab()'s fix re-hydrates before re-enabling.
      loadMaster();
      expect(masterState.called).toEqual(peerCalled);
      expect(masterState.called).not.toEqual(staleCalled);
    });
  });
});
