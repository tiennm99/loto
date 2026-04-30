/**
 * Single-tab guard for the Lô tô app. When the user opens the app in a
 * second tab, the second tab broadcasts a claim and the first tab
 * freezes itself until the user explicitly takes it back. Prevents
 * double auto-call intervals, double localStorage writers, and
 * overlapping audio across tabs of the same origin.
 *
 * No-op in browsers without `BroadcastChannel` (legacy iOS Safari).
 *
 * @module lib/tab-lock
 */

const CHANNEL = "loto_tab_lock";

const TAB_ID =
  typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
    ? crypto.randomUUID()
    : `tab-${Math.random().toString(36).slice(2)}-${Date.now()}`;

export const tabLock = $state({
  /** True when another tab has taken over and this tab should pause. */
  frozen: false,
});

/** @type {BroadcastChannel | null} */
let bc = null;

/**
 * Start listening for tab-claim messages and immediately announce that
 * THIS tab is now active. Returns a cleanup that closes the channel.
 * Calling more than once is a no-op so layout HMR doesn't double-bind.
 */
export function startTabLock() {
  if (typeof BroadcastChannel === "undefined") return () => {};
  if (bc) return () => {};
  bc = new BroadcastChannel(CHANNEL);
  bc.onmessage = (e) => {
    if (e.data?.type === "claim" && e.data.id !== TAB_ID) {
      tabLock.frozen = true;
    }
  };
  bc.postMessage({ type: "claim", id: TAB_ID });
  return () => {
    if (bc) {
      bc.close();
      bc = null;
    }
  };
}

/**
 * Re-claim the lock from the frozen banner. Unfreezes this tab and
 * broadcasts a new claim — any other live tab freezes itself in turn.
 */
export function reclaimTab() {
  tabLock.frozen = false;
  if (bc) bc.postMessage({ type: "claim", id: TAB_ID });
}
