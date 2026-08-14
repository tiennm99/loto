/**
 * Makes the back gesture close the topmost overlay instead of leaving the app.
 *
 * Each open overlay pushes one sentinel history entry. A back press pops it,
 * `popstate` fires, and we close the newest overlay. Only when nothing is left
 * to pop does back reach the native layer, which then asks before exiting
 * (see `MainActivity.handleOnBackPressed`).
 *
 * Browsers get the same behaviour for free — browser back closes the modal
 * rather than leaving the page.
 *
 * SvelteKit interop: its client router gates its own `popstate` handler on
 * `event.state?.[HISTORY_INDEX]`. Our sentinel state deliberately omits that
 * key, so the router ignores these entries and no navigation is triggered.
 * Don't spread `history.state` into the sentinel — that would reintroduce the
 * key and hand our events to the router.
 *
 * @module lib/overlay-history
 */

/** @typedef {{ id: number, close: () => void }} OverlayEntry */

/** Open overlays, oldest first. Closing is LIFO. */
/** @type {OverlayEntry[]} */
const stack = [];

let seq = 0;

/**
 * Number of `popstate` events to swallow because we caused them ourselves via
 * `history.back()` in `dispose()`. Without this, closing by button or Escape
 * would pop the sentinel AND re-run `close()`.
 */
let suppress = 0;

let listening = false;

function onPopState() {
  if (suppress > 0) {
    suppress--;
    return;
  }
  const entry = stack.pop();
  entry?.close();
}

/**
 * Installed on first use and left in place for the page lifetime. Removing it
 * when the stack empties would strand a pending suppressed `popstate`, leaking
 * a `suppress` count into the next overlay and swallowing a real back press.
 */
function startListening() {
  if (listening) return;
  window.addEventListener("popstate", onPopState);
  listening = true;
}

/** @param {OverlayEntry} entry */
function dispose(entry) {
  const i = stack.indexOf(entry);
  // Already gone — closed by a back press, which ran `close()` itself.
  if (i === -1) return;
  stack.splice(i, 1);
  suppress++;
  history.back();
}

/**
 * Register an open overlay. Call on open; call the returned function when the
 * overlay closes by any other route (button, Escape, backdrop tap) so the
 * sentinel is popped and history stays balanced.
 *
 * Nested overlays close LIFO. Each overlay owns exactly one history entry, so
 * disposing a non-top overlay still pops one entry and the counts stay level.
 *
 * @param {() => void} close — closes this overlay; must be idempotent-safe
 * @returns {() => void} dispose
 */
export function pushOverlay(close) {
  if (typeof window === "undefined") return () => {};
  const entry = { id: ++seq, close };
  stack.push(entry);
  startListening();
  history.pushState({ lotoOverlay: entry.id }, "");
  return () => dispose(entry);
}

/** Test-only teardown so module state doesn't leak across cases. */
export function _resetOverlayHistoryForTest() {
  stack.length = 0;
  suppress = 0;
}
