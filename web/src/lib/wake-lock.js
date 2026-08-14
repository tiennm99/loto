/**
 * Screen wake lock for the caller's device.
 *
 * Auto-call advances on a timer with no touch input, so a full round (up to
 * 90 numbers at 1–10s each) can run for 15 minutes without the user ever
 * touching the screen. Without a lock the display sleeps, the WebView
 * throttles its timers, and the round stalls.
 *
 * Android releases the lock whenever the page hides, so a one-shot
 * `request()` is dead after the first backgrounding — returning to the app
 * has to re-acquire. That re-acquire is why this module owns a
 * `visibilitychange` listener instead of just handing back a sentinel.
 *
 * @module lib/wake-lock
 */

/** @type {any} — WakeLockSentinel; lib.dom in this project predates the type. */
let sentinel = null;

/** True while a caller wants the screen held awake. */
let wanted = false;

/**
 * Guards the async gap inside `acquire()`. A `request()` that resolves after
 * the caller flipped to `false` must release immediately rather than leak a
 * lock the UI thinks it dropped. Same idea as `activeToken` in `voice.js`.
 */
let generation = 0;

/** Prevents two concurrent `acquire()` calls from each taking a sentinel. */
let acquiring = false;

let listening = false;

function supported() {
  return (
    typeof navigator !== "undefined" &&
    typeof document !== "undefined" &&
    !!(/** @type {any} */ (navigator).wakeLock)
  );
}

function onVisibilityChange() {
  if (document.visibilityState === "visible") void acquire();
}

function startListening() {
  if (listening || !supported()) return;
  document.addEventListener("visibilitychange", onVisibilityChange);
  listening = true;
}

function stopListening() {
  if (!listening) return;
  document.removeEventListener("visibilitychange", onVisibilityChange);
  listening = false;
}

async function acquire() {
  if (!supported() || !wanted || sentinel || acquiring) return;
  acquiring = true;
  const token = generation;
  try {
    const next = await /** @type {any} */ (navigator).wakeLock.request(
      "screen",
    );
    if (token !== generation || !wanted) {
      // Turned off while we awaited — drop it rather than hold a lock
      // nobody asked for.
      void next.release?.();
      return;
    }
    sentinel = next;
    // The system drops the lock on its own (screen off, battery saver).
    // Clear our handle so the next visibilitychange can re-acquire.
    next.addEventListener?.("release", () => {
      if (sentinel === next) sentinel = null;
    });
  } catch {
    /* hidden page, battery saver, permissions policy — nothing to do */
  } finally {
    acquiring = false;
  }
}

function releaseNow() {
  const held = sentinel;
  sentinel = null;
  if (held) {
    try {
      void Promise.resolve(held.release?.()).catch(() => {});
    } catch {
      /* already released by the system */
    }
  }
}

/**
 * Hold or drop the screen wake lock. Idempotent — safe to call from a
 * reactive effect that re-runs on unrelated state changes.
 *
 * No-ops entirely where `navigator.wakeLock` is missing (older WebViews on
 * minSdk 24 devices).
 *
 * @param {boolean} on
 */
export function setWakeLock(on) {
  if (on === wanted) return;
  wanted = on;
  // Invalidate any in-flight acquire from the previous state.
  generation++;
  if (on) {
    startListening();
    void acquire();
  } else {
    stopListening();
    releaseNow();
  }
}

/** Test-only teardown so module state doesn't leak across cases. */
export function _resetWakeLockForTest() {
  wanted = false;
  generation++;
  acquiring = false;
  stopListening();
  sentinel = null;
}
