// @vitest-environment happy-dom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

// Track every Audio instance the module constructs. Since voice.js now
// keeps a single reusable <audio> element for the module's lifetime (M6
// fix — one element, `.src` swapped per clip, so it stays iOS-unlocked
// across every subsequent play()), this array should never grow past 1
// within a test.
/** @type {any[]} */
let audios = [];

class FakeAudio {
  constructor() {
    this.src = "";
    this.preload = "";
    this.currentTime = 0;
    this.onended = null;
    this.onerror = null;
    this.paused = true;
    audios.push(this);
  }
  play() {
    this.paused = false;
    return Promise.resolve();
  }
  pause() {
    this.paused = true;
  }
}

/** @type {typeof import("./voice.js")} */
let voice;
/** @type {typeof import("./settings-store.svelte.js").settings} */
let settings;

beforeEach(async () => {
  audios = [];
  vi.stubGlobal("Audio", /** @type {any} */ (FakeAudio));
  // voice.js keeps module-level singleton state (the shared element, the
  // unlock flag) by design — reset the module registry so each test gets
  // a fresh instance instead of reusing a prior test's primed element.
  vi.resetModules();
  ({ settings } = await import("./settings-store.svelte.js"));
  voice = await import("./voice.js");
  settings.voice = "test-voice";
  settings.voiceWaitingNumber = false;
  settings.mode = "player";
});

afterEach(() => {
  voice.cancelPlayback();
  vi.unstubAllGlobals();
});

describe("voice — playback cancellation", () => {
  it("cancelPlayback pauses and rewinds the active clip", () => {
    voice.playNumber(7);
    const a = audios[audios.length - 1];
    a.currentTime = 0.42;
    a.paused = false;
    voice.cancelPlayback();
    expect(a.paused).toBe(true);
    expect(a.currentTime).toBe(0);
    expect(a.onended).toBeNull();
    expect(a.onerror).toBeNull();
  });

  it("a second playNumber cancels the first and reuses the same element", () => {
    voice.playNumber(3);
    const a = audios[audios.length - 1];
    expect(a.src).toMatch(/\/3\.mp3$/);
    voice.playNumber(5);
    // Single shared element (M6 fix) — src swapped in place, no 2nd
    // Audio instance constructed.
    expect(audios.length).toBe(1);
    expect(a.src).toMatch(/\/5\.mp3$/);
    expect(a.paused).toBe(false);
  });

  it("playWaiting plays only 'cho' when voiceWaitingNumber is off", async () => {
    settings.voiceWaitingNumber = false;
    voice.playWaiting(42);
    // Let the async chain schedule + start the cho clip.
    await Promise.resolve();
    expect(audios.length).toBe(1);
    expect(audios[0].src).toMatch(/\/cho\.mp3$/);
    audios[0].onended?.();
    await Promise.resolve();
    await Promise.resolve();
    // Number clip should NOT be queued — src stays on "cho".
    expect(audios.length).toBe(1);
    expect(audios[0].src).toMatch(/\/cho\.mp3$/);
  });

  it("playWaiting chains 'cho' → number when voiceWaitingNumber is on", async () => {
    settings.voiceWaitingNumber = true;
    voice.playWaiting(42);
    await Promise.resolve();
    expect(audios[0].src).toMatch(/\/cho\.mp3$/);
    audios[0].onended?.();
    await Promise.resolve();
    await Promise.resolve();
    expect(audios.length).toBe(1);
    expect(audios[0].src).toMatch(/\/42\.mp3$/);
  });

  it("playWaiting suppresses trailing number in both mode even when flag is on", async () => {
    settings.voiceWaitingNumber = true;
    settings.mode = "both";
    voice.playWaiting(42);
    await Promise.resolve();
    expect(audios[0].src).toMatch(/\/cho\.mp3$/);
    audios[0].onended?.();
    await Promise.resolve();
    await Promise.resolve();
    // Master is the announcer in both mode — overlapping "Chờ N" right
    // after a master call confuses listeners, so we drop it.
    expect(audios.length).toBe(1);
    expect(audios[0].src).toMatch(/\/cho\.mp3$/);
  });
});

describe("voice — iOS unlock (M6)", () => {
  it("unlockAudio primes the shared element exactly once", () => {
    voice.unlockAudio();
    expect(audios.length).toBe(1);
    const primed = audios[0];
    voice.unlockAudio();
    // Idempotent — no 2nd element created on a repeat call (e.g. both
    // "Bắt đầu" and a later "Xổ số" click in the same session).
    expect(audios.length).toBe(1);
    expect(audios[0]).toBe(primed);
  });

  it("playback after unlockAudio reuses the already-primed element", () => {
    voice.unlockAudio();
    const primed = audios[0];
    // Simulates auto-call's setInterval callback, which iOS does not
    // treat as a user gesture — playback must land on the element that
    // was unlocked earlier inside a real click, not a fresh one.
    voice.playNumber(9);
    expect(audios.length).toBe(1);
    expect(audios[0]).toBe(primed);
    expect(audios[0].src).toMatch(/\/9\.mp3$/);
  });
});
