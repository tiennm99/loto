#!/usr/bin/env node
/**
 * Postbuild step: replace `'unsafe-inline'` in build/_headers script-src
 * with the SHA-256 hash(es) of every inline <script> in
 * build/index.html. Hash changes per build are expected (the
 * SvelteKit bootstrap embeds a timestamped registration call), so
 * this script must run on every build.
 *
 * If no inline scripts are present (future SvelteKit could go
 * src-only), the script removes `'unsafe-inline'` entirely so the
 * tightest possible CSP ships.
 */
import { readFileSync, writeFileSync } from "node:fs";
import { createHash } from "node:crypto";

const HEADERS = "build/_headers";
const HTML = "build/index.html";
const MARKER = `script-src 'self' 'unsafe-inline'`;

const html = readFileSync(HTML, "utf8");
const inlineScripts = [
  ...html.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g),
];

const hashes = inlineScripts.map((m) => {
  const body = m[1];
  const digest = createHash("sha256").update(body, "utf8").digest("base64");
  return `'sha256-${digest}'`;
});

const replacement =
  hashes.length > 0
    ? `script-src 'self' ${hashes.join(" ")}`
    : `script-src 'self'`;

const headers = readFileSync(HEADERS, "utf8");
if (!headers.includes(MARKER)) {
  console.error(
    `inject-csp-hashes: marker not found in ${HEADERS}.\nLooking for: ${MARKER}\n` +
      `Either the previous build already replaced it (re-run \`npm run build\` from clean) ` +
      `or static/_headers no longer contains the relaxed script-src directive.`,
  );
  process.exit(1);
}

writeFileSync(HEADERS, headers.replace(MARKER, replacement), "utf8");
console.log(
  `inject-csp-hashes: replaced 'unsafe-inline' with ${hashes.length} hash(es) in ${HEADERS}.`,
);
