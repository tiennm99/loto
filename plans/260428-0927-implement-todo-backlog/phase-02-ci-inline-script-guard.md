---
name: CI inline-script guard
phase: 2
status: todo
priority: high
effort: 30m
---

# Phase 2 — CI inline-script guard

## Context
- TODO: SvelteKit emits one inline bootstrap script. CSP relaxed to
  `'unsafe-inline'` to accommodate. If a future SvelteKit upgrade adds
  another inline block, we want CI to fail loudly.
- `static/_headers:2` — current CSP includes `script-src 'self' 'unsafe-inline'`.
- No CI build job today (Cloudflare Pages builds on its own; only
  `.github/workflows/deploy-github-pages.yml` is a redirect-only job).

## Decision
Add an `npm run verify:build` script that:
1. Counts `<script>` tags in `build/index.html`
2. Fails if count > 1 (current expected: 1 inline bootstrap)
3. Wires into `npm run build` as a sanity post-step (or kept manual via
   `package.json` scripts so dev builds aren't slowed).

Then add a minimal GH Actions job that runs `npm ci && npm run build &&
npm run verify:build`.

## Files
- Create: `scripts/verify-build-inline-scripts.mjs`
- Create: `.github/workflows/verify-build.yml`
- Modify: `package.json` — add `"verify:build"` script

## Script (`scripts/verify-build-inline-scripts.mjs`)

```js
#!/usr/bin/env node
import { readFileSync } from "node:fs";

const EXPECTED_INLINE = 1;
const html = readFileSync("build/index.html", "utf8");
const inline = (html.match(/<script(?![^>]*\bsrc=)[^>]*>/g) || []).length;

if (inline > EXPECTED_INLINE) {
  console.error(
    `verify-build: found ${inline} inline <script> tags in build/index.html (expected ${EXPECTED_INLINE}).\n` +
    `If this is intentional, update EXPECTED_INLINE in scripts/verify-build-inline-scripts.mjs ` +
    `AND add the SHA-256 hash(es) to static/_headers script-src.`
  );
  process.exit(1);
}
console.log(`verify-build: ${inline} inline <script> tag(s) — OK.`);
```

## Workflow (`.github/workflows/verify-build.yml`)

```yaml
name: Verify build

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]

jobs:
  verify:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-node@v4
        with:
          node-version: 20
          cache: npm
      - run: npm ci
      - run: npm test
      - run: npm run build
      - run: npm run verify:build
```

## Steps
1. Write `scripts/verify-build-inline-scripts.mjs` (executable not required, run via node).
2. Add `"verify:build": "node scripts/verify-build-inline-scripts.mjs"` to `package.json`.
3. Build locally and run — confirm passes with `EXPECTED_INLINE = 1`.
4. Tweak EXPECTED to 0 temporarily, confirm script fails. Reset to 1.
5. Add `.github/workflows/verify-build.yml`.
6. Push, observe green action.

## Success
- `npm run verify:build` exits 0 on current build.
- Tampering EXPECTED to 0 causes exit 1 (proves guard works).
- GH Actions runs on push/PR to main.

## Edge cases
- Module scripts (`<script type="module" src="...">` with src) excluded by `(?![^>]*\bsrc=)` — only inline counted.
- Whitespace before `>` handled by `[^>]*`.
