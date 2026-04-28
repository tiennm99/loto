---
name: Strict CSP via hashed inline script
phase: 7
status: todo
priority: medium
effort: 1.5h
---

# Phase 7 — Strict CSP via hashed inline script

## Context
- `static/_headers:2` — current CSP: `script-src 'self' 'unsafe-inline'`.
- The `'unsafe-inline'` is a relaxation to admit SvelteKit's bootstrap
  inline `<script>` block in built `index.html`.
- Goal: replace `'unsafe-inline'` with `'sha256-…'` hash of the actual
  inline. Brittle — hash changes per build — so build pipeline must
  regenerate the hash and rewrite `_headers` on every build.

## Approach

### Option A — vite plugin (preferred)
A Vite plugin `closeBundle` hook reads `build/index.html`, extracts
inline scripts, computes SHA-256 of each, and rewrites `build/_headers`
with the hashes injected into `script-src`.

### Option B — postbuild npm script
Same logic but as `scripts/inject-csp-hashes.mjs` invoked via
`"build": "vite build && node scripts/inject-csp-hashes.mjs"`.

**Pick Option B** — keeps `vite.config.js` simpler and the script is
isolated. `_headers` lives in `static/` and is copied to `build/_headers`
by the static adapter, so we rewrite the *built* copy.

## Files
- Create: `scripts/inject-csp-hashes.mjs`
- Modify: `package.json` — change `"build"` and `"build:gh"` to chain
- Modify: `static/_headers` — change marker for replacement
- Verify: `Phase 2 verify-build` script still passes (still 1 inline,
  just now hashed not unsafe-inline)

## Script (`scripts/inject-csp-hashes.mjs`)

```js
#!/usr/bin/env node
import { readFileSync, writeFileSync } from "node:fs";
import { createHash } from "node:crypto";

const HEADERS = "build/_headers";
const HTML = "build/index.html";

const html = readFileSync(HTML, "utf8");
const inlineScripts = [...html.matchAll(/<script(?![^>]*\bsrc=)[^>]*>([\s\S]*?)<\/script>/g)];

if (inlineScripts.length === 0) {
  console.log("inject-csp-hashes: no inline scripts found, leaving CSP as-is.");
  process.exit(0);
}

const hashes = inlineScripts.map((m) => {
  const body = m[1];
  const h = createHash("sha256").update(body, "utf8").digest("base64");
  return `'sha256-${h}'`;
});

let headers = readFileSync(HEADERS, "utf8");
const before = `script-src 'self' 'unsafe-inline'`;
const after = `script-src 'self' ${hashes.join(" ")}`;

if (!headers.includes(before)) {
  console.error(`inject-csp-hashes: marker not found in ${HEADERS}.\nLooking for: ${before}`);
  process.exit(1);
}

headers = headers.replace(before, after);
writeFileSync(HEADERS, headers, "utf8");
console.log(`inject-csp-hashes: injected ${hashes.length} hash(es) into ${HEADERS}.`);
```

## Package.json

```json
"scripts": {
  "build": "vite build && node scripts/inject-csp-hashes.mjs",
  "build:gh": "BUILD_PROFILE=gh vite build && node scripts/inject-csp-hashes.mjs"
}
```

## Steps
1. Confirm static adapter copies `static/_headers` → `build/_headers`
   (it does — same as `static/_redirects`).
2. Write the script.
3. `npm run build` — confirm `build/_headers` now has
   `script-src 'self' 'sha256-…'` (no `'unsafe-inline'`).
4. Local serve `build/` (e.g. `npx serve build`) → verify SW registers,
   no CSP errors in DevTools console.
5. Wire `verify:build` (Phase 2) to also assert `'unsafe-inline'`
   absent from `build/_headers` script-src — extend that script:

   ```js
   const headers = readFileSync("build/_headers", "utf8");
   if (/script-src[^;]*'unsafe-inline'/.test(headers)) {
     console.error("verify-build: script-src still contains 'unsafe-inline'");
     process.exit(1);
   }
   ```

## Success
- `build/_headers` contains hashed `script-src` instead of `'unsafe-inline'`.
- App still loads, SW registers, no CSP violations in browser console.
- `verify:build` from Phase 2 enforces no-unsafe-inline going forward.

## Risks
- Each build rewrites the hash → `_headers` shipped to Cloudflare changes
  on every deploy. That's fine; `_headers` is treated as build artifact.
- If SvelteKit upgrades and adds another inline script, the regex
  catches it automatically (multi-hash). Phase 2's `EXPECTED_INLINE`
  may need bumping.
- `style-src 'unsafe-inline'` stays — Svelte's `style:` directives are
  attribute-level, no nonce/hash escape. Documented in
  `plans/reports/security-260427-2047-pass2-full.md`.

## Edge cases
- Whitespace inside `<script>` — `.update(body, "utf8")` digests the
  exact bytes; minor formatting changes shift the hash. Keep
  vite/svelte versions pinned.
- If a future SvelteKit release injects script via `<script src=…>`
  only, `inlineScripts.length === 0` → no rewrite needed; CSP stays
  `script-src 'self'`. The early `process.exit(0)` handles that.
