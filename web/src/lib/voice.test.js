// @vitest-environment happy-dom
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { settings } from "./settings-store.svelte.js";
import { cancelPlayback, clearAudioCache, playNumber, playWaiting } from "./voice.js";

// Track every Audio instance the module constructs so we can drive
// onended/onerror by hand and verify cancelPlayback's bookkeeping.
/** @type {any[]} */
let audios = [];

class FakeAudio {
  /** @param {string} url */
  constructor(url) {
    this.src = url;
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

beforeEach(() => {
  audios = [];
  vi.stubGlobal("Audio", /** @type {any} */ (FakeAudio));
  // Reset cache + active state so each test starts cleanly.
  clearAudioCache();
  settings.voice = "test-voice";
  settings.voiceWaitingNumber = false;
});

afterEach(() => {
  cancelPlayback();
  vi.unstubAllGlobals();
});

describe("voice — playback cancellation", () => {
  it("cancelPlayback pauses and rewinds the active clip", () => {
    playNumber(7);
    const a = audios[audios.length - 1];
    a.currentTime = 0.42;
    a.paused = false;
    cancelPlayback();
    expect(a.paused).toBe(true);
    expect(a.currentTime).toBe(0);
    expect(a.onended).toBeNull();
    expect(a.onerror).toBeNull();
  });

  it("a second playNumber cancels the first", () => {
    playNumber(3);
    const first = audios[audios.length - 1];
    playNumber(5);
    const second = audios[audios.length - 1];
    expect(first).not.toBe(second);
    expect(first.paused).toBe(true);
    expect(second.paused).toBe(false);
  });

  it("playWaiting plays only 'cho' when voiceWaitingNumber is off", async () => {
    settings.voiceWaitingNumber = false;
    playWaiting(42);
    // Let the async chain schedule + start the cho clip.
    await Promise.resolve();
    expect(audios.length).toBe(1);
    expect(audios[0].src).toMatch(/\/cho\.mp3$/);
    audios[0].onended?.();
    await Promise.resolve();
    await Promise.resolve();
    // Number clip should NOT be queued.
    expect(audios.length).toBe(1);
  });

  it("playWaiting chains 'cho' → number when voiceWaitingNumber is on", async () => {
    settings.voiceWaitingNumber = true;
    playWaiting(42);
    await Promise.resolve();
    expect(audios[0].src).toMatch(/\/cho\.mp3$/);
    audios[0].onended?.();
    await Promise.resolve();
    await Promise.resolve();
    expect(audios.length).toBe(2);
    expect(audios[1].src).toMatch(/\/42\.mp3$/);
  });
});
