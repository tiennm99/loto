/**
 * Tiny one-slot bus to coordinate master draws → player auto-tick.
 * Each draw publishes a fresh object so even repeat numbers fire a
 * fresh reactive change. Consumers read `bus.lastDrawn?.num` in an
 * effect.
 *
 * @module lib/call-bus
 */

export const bus = $state({
  /** @type {{ num: number, at: number } | null} */
  lastDrawn: null,
});

/** @param {number} num */
export function broadcastDraw(num) {
  bus.lastDrawn = { num, at: Date.now() };
}

export function resetBus() {
  bus.lastDrawn = null;
}
