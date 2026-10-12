#!/usr/bin/env node
/**
 * Build-time regression guard for the PWA layer.
 *
 * `adapter-static` renders page HTML during `adapt()`, which runs AFTER the
 * PWA plugin's `closeBundle` — so `@vite-pwa/sveltekit` never gets to inject
 * its own registration tag into the built HTML, and a plain grep on
 * `build/index.html` proves nothing either way. `+layout.svelte` registers
 * the service worker itself via a dynamically-imported `virtual:pwa-register`
 * chunk instead; this script asserts that wiring survived the build rather
 * than trusting a future refactor not to quietly drop it.
 *
 * Also asserts the audio precache manifest in `build/sw.js` is prefixed
 * with the SAME base path SvelteKit itself resolved for this build — a
 * mismatch here 404s every precached clip and fails the SW `install` event
 * on that origin (this exact bug shipped once already).
 *
 * Usage: run AFTER `npm run build` / `npm run build:gh` (see package.json's
 * `verify:pwa` script), with the same `BUILD_PROFILE`/`NEXT_BASE_PATH` env
 * the build used.
 */
import { existsSync, readdirSync, readFileSync, statSync } from "node:fs";
import { join } from "node:path";

import { resolveBase } from "../base-path.js";

const BUILD_DIR = "build";
let ok = true;

/** @param {string} msg */
function fail(msg) {
  console.error(`✗ ${msg}`);
  ok = false;
}

/** @param {string} dir */
function walkJsFiles(dir) {
  /** @type {string[]} */
  const files = [];
  for (const entry of readdirSync(dir)) {
    const full = join(dir, entry);
    if (statSync(full).isDirectory()) files.push(...walkJsFiles(full));
    else if (entry.endsWith(".js")) files.push(full);
  }
  return files;
}

// --- sw.js was actually generated ---------------------------------------
const swPath = join(BUILD_DIR, "sw.js");
let swSrc = "";
try {
  swSrc = readFileSync(swPath, "utf8");
} catch {
  fail(`${swPath} not found — did the PWA plugin run?`);
  process.exit(1);
}

// --- the layout's virtual:pwa-register import survived as its own chunk --
// Located through Vite's client manifest rather than by grepping chunk
// contents for "serviceWorker" + "sw.js" together: Rollup is free to split
// those strings across chunks, which would fail this gate with no real
// regression. The manifest names the chunk by its source id regardless.
const appDir = join(BUILD_DIR, "_app");
const jsFiles = walkJsFiles(appDir);
const CLIENT_MANIFEST = join(".svelte-kit", "output", "client", ".vite", "manifest.json");
/** @type {Record<string, { file: string, dynamicImports?: string[] }>} */
let clientManifest = {};
try {
  clientManifest = JSON.parse(readFileSync(CLIENT_MANIFEST, "utf8"));
} catch {
  fail(`${CLIENT_MANIFEST} not found or unreadable — run this right after the build.`);
}
const registerKey = Object.keys(clientManifest).find((k) =>
  k.endsWith("virtual:pwa-register"),
);
if (!registerKey) {
  fail(
    "The client build has no virtual:pwa-register chunk — the dynamic " +
      "import in +layout.svelte that registers the service worker is gone.",
  );
} else {
  const registerFile = join(BUILD_DIR, clientManifest[registerKey].file);
  if (!existsSync(registerFile)) {
    fail(`virtual:pwa-register chunk ${registerFile} is missing from ${BUILD_DIR}.`);
  }
  const importedBy = Object.values(clientManifest).some((entry) =>
    entry.dynamicImports?.includes(registerKey),
  );
  if (!importedBy) {
    fail("No client chunk dynamically imports virtual:pwa-register.");
  }
}
if (!jsFiles.some((f) => readFileSync(f, "utf8").includes("sw.js"))) {
  fail(`No built JS under ${appDir} references "sw.js" — nothing registers it.`);
}

// --- H1: precached audio entries carry the resolved base path -----------
const base = resolveBase();
const audioUrls = [...swSrc.matchAll(/"([^"]*\/audio\/[^"]*\.mp3)"/g)].map(
  (m) => m[1],
);
if (audioUrls.length === 0) {
  fail(`${swPath} has no precached audio entries — expected the default voice's clips.`);
} else {
  const badUrls = audioUrls.filter((u) => !u.startsWith(`${base}/audio/`));
  if (badUrls.length > 0) {
    fail(
      `${badUrls.length}/${audioUrls.length} precached audio URL(s) don't ` +
        `start with "${base}/audio/" (resolved base "${base}") — e.g. "${badUrls[0]}". ` +
        "additionalManifestEntries in vite.config.js and svelte.config.js's " +
        "paths.base have drifted apart again.",
    );
  }
}

if (!ok) process.exit(1);
console.log(
  `✓ PWA build check passed — base "${base}", ${jsFiles.length} client JS files, ` +
    `${audioUrls.length} precached audio entries.`,
);
