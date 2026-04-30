/**
 * Single-active-tab coordinator. When the user opens the app in a
 * second tab, the second tab broadcasts a claim and the first tab
 * marks itself inactive until the user explicitly takes it back.
 * Prevents double auto-call intervals, double localStorage writers,
 * and overlapping audio across tabs of the same origin.
 *
 * Soft coordination only — relies on cooperating tabs, not OS-level
 * locks. No-op in browsers without `BroadcastChannel` (legacy iOS
 * Safari ≤15.4); the prior race remains in those edge environments.
 *
 * @module lib/active-tab
 */

const CHANNEL = "loto_active_tab";

const TAB_ID =
  typeof crypto !== "undefined" && typeof crypto.randomUUID === "function"
    ? crypto.randomUUID()
    : `tab-${Math.random().toString(36).slice(2)}-${Date.now()}`;

export const activeTab = $state({
  /** True when another tab has claimed; this tab should pause its UI. */
  inactive: false,
});

/** @type {BroadcastChannel | null} */
let bc = null;

/**
 * Open the channel, listen for peer claims, and announce that THIS tab
 * is now the active one. Returns a cleanup that closes the channel.
 * Calling more than once is a no-op so layout HMR doesn't double-bind.
 */
export function watchActiveTab() {
  if (typeof BroadcastChannel === "undefined") return () => {};
  if (bc) return () => {};
  bc = new BroadcastChannel(CHANNEL);
  bc.onmessage = (e) => {
    if (e.data?.type === "claim" && e.data.id !== TAB_ID) {
      activeTab.inactive = true;
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
 * Take active status back from the inactive banner. Marks this tab
 * active locally and broadcasts a claim — any peer freezes itself in
 * turn so the handover is symmetric.
 */
export function claimActiveTab() {
  activeTab.inactive = false;
  if (bc) bc.postMessage({ type: "claim", id: TAB_ID });
}
