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

console.log(`verify-build: ${inline} inline <script> tag(s) — OK.`);
