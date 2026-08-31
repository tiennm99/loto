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
 * `Date.now()` of this tab's most recent claim (mount or {@link claimActiveTab}).
 * Used to resolve simultaneous claims: whichever tab claimed last stays
 * active, so two tabs opened in the same instant don't both freeze each
 * other. Equal timestamps (clock-resolution collisions) tie-break on
 * {@link TAB_ID} so exactly one tab wins deterministically.
 * @type {number}
 */
let myClaimTs = 0;

/**
 * Broadcast a claim carrying this tab's id and current claim timestamp.
 */
function postClaim() {
  myClaimTs = Date.now();
  if (bc) bc.postMessage({ type: "claim", id: TAB_ID, ts: myClaimTs });
}

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
    if (e.data?.type !== "claim" || e.data.id === TAB_ID) return;
    // A legacy peer (pre-echo build) broadcasts {type, id} with no `ts`.
    // Treat that as beating any real timestamp so this tab freezes instead
    // of silently ignoring it (matches the old always-freeze behavior).
    const peerTs = typeof e.data.ts === "number" ? e.data.ts : Infinity;
    // Newest claim wins. Ignore a peer claim that is older than this tab's
    // own claim; on an exact tie (same millisecond) the lexicographically
    // greater id wins so exactly one side freezes.
    const peerWins =
      peerTs > myClaimTs || (peerTs === myClaimTs && e.data.id > TAB_ID);
    if (peerWins) {
      activeTab.inactive = true;
    } else if (!activeTab.inactive && bc) {
      // This tab wins but the peer doesn't know it lost yet — it set
      // itself active locally before broadcasting. Echo this tab's own
      // winning claim so the peer re-evaluates and freezes. This can't
      // loop: the echo carries this tab's unchanged (ts, id), so the peer
      // strictly loses it and freezes without ever echoing back (a frozen
      // tab never claims).
      bc.postMessage({ type: "claim", id: TAB_ID, ts: myClaimTs });
    }
  };
  postClaim();
  return () => {
    if (bc) {
      bc.close();
      bc = null;
    }
  };
}

/**
 * Take active status back from the inactive banner. Marks this tab
 * active locally, records a fresh claim timestamp (so a stale peer claim
 * can't re-freeze it), and broadcasts the claim — any peer freezes itself
 * in turn so the handover is symmetric.
 */
export function claimActiveTab() {
  activeTab.inactive = false;
  postClaim();
}
