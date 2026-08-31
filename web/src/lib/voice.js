/**
 * Bundled-MP3 playback for Vietnamese number calls. No runtime TTS, no
 * API. Clips live under `static/audio/{voiceId}/{1..90,cho,kinh}.mp3`
 * and are picked by `settings.voice`.
 * @module lib/voice
 */
import { base } from "$app/paths";
import { settings } from "$lib/settings-store.svelte.js";

/**
 * Single reusable <audio> element, `.src` swapped per clip. iOS Safari
 * only allows programmatic `play()` on an element that was actually
 * play()'d inside a user gesture; auto-call plays clips from a
 * `setInterval` callback, which is never a gesture. A single element
 * unlocked once via `unlockAudio()` (called from the "Bắt đầu"/"Xổ số"
 * click) stays unlocked for the rest of the page's life — the previous
 * per-URL-cached-element design created a fresh, never-unlocked element
 * for every clip and was silent on iOS under auto-call with no
 * diagnostic (also retained up to 92 `preload="auto"` elements per voice).
 * @type {HTMLAudioElement | null}
 */
let audio = null;

/** True once `unlockAudio()` has primed `audio` inside a user gesture. */
let unlocked = false;

/** @type {HTMLAudioElement | null} */
let activeClip = null;

/** @type {symbol | null} */
let activeToken = null;

/** @type {(() => void) | null} — resolver of the in-flight playClip promise */
let activeResolver = null;

function isBrowser() {
  return typeof window !== "undefined" && typeof Audio !== "undefined";
}

function getAudio() {
  if (!audio) {
    audio = new Audio();
    audio.preload = "auto";
  }
  return audio;
}

/**
 * Unlock the shared `<audio>` element for later programmatic playback on
 * iOS Safari. MUST be called synchronously from inside a real user
 * gesture (a click handler) — see the module doc comment on `audio`.
 * Idempotent and safe to call from every relevant click handler.
 */
export function unlockAudio() {
  if (!isBrowser() || unlocked) return;
  unlocked = true;
  const a = getAudio();
  const played = a.play();
  if (played && typeof played.then === "function") {
    played.then(() => a.pause()).catch(() => {
      /* The priming play() itself may reject (e.g. no src yet) — that's
         fine, the attempt still happened synchronously inside the
         gesture, which is what iOS actually checks. */
    });
  } else {
    a.pause();
  }
}

/** @param {string} name — clip basename without extension */
function clipUrl(name) {
  // `settings.voice` is allowlist-validated and `name` is internal-only,
  // but encode anyway as defense-in-depth in case either ever escapes.
  return `${base}/audio/${encodeURIComponent(settings.voice)}/${encodeURIComponent(name)}.mp3`;
}

/**
 * Stop and reset the shared element. Kept as its own export (rather than
 * folded into `cancelPlayback`) so callers that change `settings.voice`
 * keep a stable "drop anything voice-specific" hook even though there is
 * now only one element to reset.
 */
export function clearAudioCache() {
  cancelPlayback();
}

export function cancelPlayback() {
  if (!isBrowser()) return;
  if (activeClip) {
    activeClip.onended = null;
    activeClip.onerror = null;
    activeClip.pause();
    activeClip.currentTime = 0;
  }
  activeClip = null;
  activeToken = null;
  // Resolve any pending playClip promise so async chains exit cleanly
  // (the next await will see the token mismatch and short-circuit).
  if (activeResolver) {
    const r = activeResolver;
    activeResolver = null;
    r();
  }
}

/**
 * Play one clip; resolve when it ends, errors, or is canceled.
 * @param {string} url
 * @param {symbol} token — caller's session marker; mismatch = canceled
 * @returns {Promise<void>}
 */
function playClip(url, token) {
  return new Promise((resolve) => {
    if (!isBrowser() || activeToken !== token) return resolve();
    const a = getAudio();
    activeClip = a;
    activeResolver = resolve;
    const done = () => {
      a.onended = null;
      a.onerror = null;
      if (activeClip === a) activeClip = null;
      if (activeResolver === resolve) activeResolver = null;
      resolve();
    };
    a.onended = done;
    a.onerror = done;
    // Reassigning `.src` (even to the same URL) reloads and resets
    // playback to 0 — required because this element is reused across
    // every clip, unlike the old one-element-per-URL cache.
    a.src = url;
    a.play().catch(done);
  });
}

/** @param {number} n */
export function playNumber(n) {
  if (!isBrowser()) return;
  cancelPlayback();
  const token = Symbol("playNumber");
  activeToken = token;
  void playClip(clipUrl(String(n)), token);
}

/** @param {number} n */
export function playWaiting(n) {
  if (!isBrowser()) return;
  cancelPlayback();
  const token = Symbol("playWaiting");
  activeToken = token;
  // Suppress the trailing number in both mode — master is already
  // calling numbers aloud, so "Chờ 42" right after a "33" call confuses
  // listeners who can't tell which number is the active draw.
  const speakNumber =
    settings.voiceWaitingNumber && settings.mode !== "both";
  (async () => {
    await playClip(clipUrl("cho"), token);
    if (speakNumber) await playClip(clipUrl(String(n)), token);
  })();
}

export function playBingo() {
  if (!isBrowser()) return;
  cancelPlayback();
  const token = Symbol("playBingo");
  activeToken = token;
  void playClip(clipUrl("kinh"), token);
}
