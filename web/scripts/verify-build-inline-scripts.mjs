#!/usr/bin/env node
/**
 * Guard: count inline <script> tags in build/index.html. SvelteKit
 * currently emits exactly one inline bootstrap block — the relaxation
 * we ship in static/_headers (`script-src 'self' 'unsafe-inline'`)
 * is calibrated to that. If a future SvelteKit upgrade adds another
 * inline block, this guard fails CI so we either (a) hash the new
 * block into CSP or (b) bump EXPECTED_INLINE intentionally.
 *
 * Inline = no `src=` attribute. Module/external scripts are excluded.
 */
import { readFileSync } from "node:fs";

const EXPECTED_INLINE = 1;
const HTML_PATH = "build/index.html";
const HEADERS_PATH = "build/_headers";

let html;
try {
  html = readFileSync(HTML_PATH, "utf8");
} catch (e) {
  console.error(`verify-build: cannot read ${HTML_PATH} — run \`npm run build\` first.`);
  process.exit(2);
}

const inline = (html.match(/<script(?![^>]*\bsrc=)[^>]*>/g) || []).length;

if (inline > EXPECTED_INLINE) {
  console.error(
    `verify-build: found ${inline} inline <script> tags in ${HTML_PATH} (expected ${EXPECTED_INLINE}).\n` +
      `If this is intentional, update EXPECTED_INLINE in scripts/verify-build-inline-scripts.mjs\n` +
      `AND add the SHA-256 hash(es) of the new inline block(s) to static/_headers script-src.`,
  );
  process.exit(1);
}

if (inline < EXPECTED_INLINE) {
  console.warn(
    `verify-build: found ${inline} inline <script> tags but expected ${EXPECTED_INLINE}.\n` +
      `If SvelteKit changed its bootstrap strategy, lower EXPECTED_INLINE and tighten CSP.`,
  );
}

// Post-Phase 7: script-src must NOT contain 'unsafe-inline' anymore —
// `inject-csp-hashes` should have replaced it with sha256 hashes.
let headers;
try {
  headers = readFileSync(HEADERS_PATH, "utf8");
} catch {
  console.warn(`verify-build: ${HEADERS_PATH} not found — skipping CSP check.`);
  process.exit(0);
}

const scriptSrcLine = headers
  .split("\n")
  .find((l) => /script-src\b/.test(l) && /Content-Security-Policy/i.test(l));
// Single-line policy: Content-Security-Policy: ... script-src 'self' …
if (scriptSrcLine && /script-src[^;]*'unsafe-inline'/.test(scriptSrcLine)) {
  console.error(
    `verify-build: ${HEADERS_PATH} script-src still contains 'unsafe-inline'. ` +
      `inject-csp-hashes.mjs should have replaced it with SHA-256 hash(es).`,
  );
  process.exit(1);
}

console.log(`verify-build: ${inline} inline <script> tag(s), CSP hashed — OK.`);
