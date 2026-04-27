/**
 * Bundled-MP3 playback for Vietnamese number calls. No runtime TTS, no
 * API. Clips live under `static/audio/{voiceId}/{1..90,cho,kinh}.mp3`
 * and are picked by `settings.voice`.
 * @module lib/voice
 */
import { base } from "$app/paths";
import { settings } from "$lib/settings-store.svelte.js";

/** @type {Map<string, HTMLAudioElement>} */
const cache = new Map();

/** @type {HTMLAudioElement | null} */
let activeClip = null;

/** @type {symbol | null} */
let activeToken = null;

/** @type {(() => void) | null} — resolver of the in-flight playClip promise */
let activeResolver = null;

function isBrowser() {
  return typeof window !== "undefined" && typeof Audio !== "undefined";
}

/** @param {string} url */
function getAudio(url) {
  let a = cache.get(url);
  if (!a) {
    a = new Audio(url);
    a.preload = "auto";
    cache.set(url, a);
  }
  return a;
}

/** @param {string} name — clip basename without extension */
function clipUrl(name) {
  return `${base}/audio/${settings.voice}/${name}.mp3`;
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
 */
function playClip(url, token) {
  return new Promise((resolve) => {
    if (!isBrowser() || activeToken !== token) return resolve();
    const a = getAudio(url);
    a.currentTime = 0;
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
  (async () => {
    await playClip(clipUrl("cho"), token);
    await playClip(clipUrl(String(n)), token);
  })();
}

export function playBingo() {
  if (!isBrowser()) return;
  cancelPlayback();
  const token = Symbol("playBingo");
  activeToken = token;
  void playClip(clipUrl("kinh"), token);
}
