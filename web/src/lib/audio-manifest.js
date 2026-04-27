/**
 * Voice manifest produced by `scripts/generate-audio.py`. Imported as a
 * static asset so Vite bakes it into the bundle — no runtime fetch.
 * @module lib/audio-manifest
 */
import manifest from "../../static/audio/manifest.json";

/**
 * @typedef {Object} VoiceEntry
 * @property {string} id          — folder slug (e.g. "hoai-my")
 * @property {string} edgeName    — original edge-tts ShortName
 * @property {string} label       — display label
 * @property {string} gender
 */

/** @type {VoiceEntry[]} */
export const VOICES = manifest.voices;

/** Set of valid voice ids for fast membership checks. */
export const VOICE_IDS = new Set(VOICES.map((v) => v.id));

/** First entry is the default — the script writes them in edge-tts order. */
export const DEFAULT_VOICE = VOICES[0]?.id ?? "hoai-my";
