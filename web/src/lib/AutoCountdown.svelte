<script>
  /**
   * Visual countdown for the master panel's auto-call.
   * Props-driven: parent owns the timer, this component just renders.
   * @module lib/AutoCountdown
   */

  /**
   * @typedef {Object} Props
   * @property {boolean} running    - master is currently auto-calling
   * @property {number}  duration   - seconds per tick (1..10)
   * @property {number}  tickKey    - bump to reset the ring
   */
  /** @type {Props} */
  let { running, duration, tickKey } = $props();

  let tickStart = $state(performance.now());
  let now = $state(performance.now());

  // One-time read; reduce-motion users see a static ring + ticking number.
  const reduceMotion =
    typeof window !== "undefined" &&
    window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches === true;

  // Re-baseline whenever the parent bumps tickKey, running flips on, or
  // duration changes mid-run (e.g. host moves the speed slider).
  // Use a local snapshot so we don't read `tickStart` after writing it —
  // that would make this effect depend on its own write and infinite-loop.
  $effect(() => {
    tickKey; // subscribe
    duration; // subscribe — keeps the contract explicit, not parent-coupled
    if (running) {
      const t = performance.now();
      tickStart = t;
      now = t;
    }
  });

  // rAF loop is the only writer of `now` while running. Cleanup cancels it
  // on running=false / unmount, so no leaks across master mode toggles.
  $effect(() => {
    if (!running) return;
    let raf = requestAnimationFrame(function loop() {
      now = performance.now();
      raf = requestAnimationFrame(loop);
    });
    return () => cancelAnimationFrame(raf);
  });

  const elapsedMs = $derived(Math.max(0, now - tickStart));
  const totalMs = $derived(Math.max(1, duration * 1000));
  const progress = $derived(Math.min(1, elapsedMs / totalMs));
  // Clamp to [1, duration] while running — avoids flashing 0 between the
  // interval edge and the parent's tickKey bump.
  const secondsRemaining = $derived(
    running ? Math.max(1, Math.ceil(duration - elapsedMs / 1000)) : duration,
  );

  const SIZE = 100;
  const RADIUS = 44;
  const STROKE = 8;
  const CIRCUMFERENCE = 2 * Math.PI * RADIUS;
  const dashOffset = $derived(
    reduceMotion ? 0 : CIRCUMFERENCE * progress,
  );
</script>

<div
  class="relative w-20 h-20 sm:w-24 sm:h-24"
  role="timer"
  aria-live="off"
  aria-label="Đếm ngược: {secondsRemaining} giây"
>
  <svg viewBox="0 0 {SIZE} {SIZE}" class="w-full h-full -rotate-90">
    <circle
      cx={SIZE / 2}
      cy={SIZE / 2}
      r={RADIUS}
      fill="none"
      stroke="currentColor"
      stroke-width={STROKE}
      class="text-slate-200 dark:text-slate-700"
    />
    <circle
      cx={SIZE / 2}
      cy={SIZE / 2}
      r={RADIUS}
      fill="none"
      stroke="currentColor"
      stroke-width={STROKE}
      stroke-linecap="round"
      stroke-dasharray={CIRCUMFERENCE}
      stroke-dashoffset={dashOffset}
      class="text-amber-500 dark:text-amber-400"
    />
  </svg>
  <span
    class="absolute inset-0 flex items-center justify-center
           text-2xl sm:text-3xl font-black tabular-nums
           text-slate-700 dark:text-slate-100"
  >
    {secondsRemaining}
  </span>
</div>
