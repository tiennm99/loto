/**
 * Shared reactive state for the master draw deck. Lifted out of
 * MasterPanel.svelte so the player side can read `called[]` directly
 * (instead of relying on a single-slot bus that loses history).
 *
 * Persisted to localStorage `loto_master`. Mutations replace whole
 * arrays so Svelte's proxy fires reactively for every consumer.
 *
 * @module lib/master-store
 */

const STORAGE_KEY = "loto_master";
/** Hard cap so a poisoned origin can't stall the UI on mount.
 *  90-number called list serializes to ~500 bytes; 16 KB has 30× headroom. */
const MAX_STORAGE_BYTES = 16_384;

export const masterState = $state({
  /** @type {number[]} */
  called: [],
  /** @type {number[]} */
  remaining: [],
});

function shuffled1to90() {
  const all = Array.from({ length: 90 }, (_, i) => i + 1);
  for (let i = all.length - 1; i > 0; i--) {
    const j = Math.floor(Math.random() * (i + 1));
    [all[i], all[j]] = [all[j], all[i]];
  }
  return all;
}

/** @param {unknown} v */
function isValidNumberArray(v) {
  return (
    Array.isArray(v) &&
    v.every((n) => Number.isInteger(n) && n >= 1 && n <= 90)
  );
}

export function loadMaster() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw || raw.length > MAX_STORAGE_BYTES) return;
    const parsed = JSON.parse(raw, (k, v) =>
      k === "__proto__" || k === "constructor" ? undefined : v,
    );
    if (
      !parsed ||
      !isValidNumberArray(parsed.called) ||
      !isValidNumberArray(parsed.remaining)
    ) {
      return;
    }
    masterState.called = parsed.called;
    masterState.remaining = parsed.remaining;
  } catch {
    /* private mode / corrupt JSON — leave defaults */
  }
}

export function saveMaster() {
  try {
    localStorage.setItem(
      STORAGE_KEY,
      JSON.stringify({
        called: masterState.called,
        remaining: masterState.remaining,
      }),
    );
  } catch {
    /* see loadMaster */
  }
}

/** Start a fresh round: empty called, full shuffled remaining. */
export function startNewGame() {
  masterState.called = [];
  masterState.remaining = shuffled1to90();
}

/**
 * Draw the next number from `remaining` into `called`.
 * @returns {number | null} the drawn number, or null if exhausted
 */
export function drawNext() {
  if (masterState.remaining.length === 0) return null;
  const next = masterState.remaining[0];
  masterState.called = [...masterState.called, next];
  masterState.remaining = masterState.remaining.slice(1);
  return next;
}

/** Wipe everything — used by tests and when leaving a game permanently. */
export function resetMaster() {
  masterState.called = [];
  masterState.remaining = [];
}
