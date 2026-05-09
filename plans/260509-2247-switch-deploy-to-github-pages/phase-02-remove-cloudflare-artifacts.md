---
phase: 2
title: Remove Cloudflare artifacts
status: completed
priority: P2
effort: 1h
dependencies:
  - 1
---

# Phase 2: Remove Cloudflare artifacts

## Overview

Delete CF-only files (`wrangler.toml`, `static/_headers`, `static/_redirects`)
and the CSP-hash machinery that exists solely to patch `_headers`. GH Pages
ignores these files (or wouldn't have them) and the user opted to keep it
simple for a static site.

## Requirements

- Functional: `npm run build` and `npm run build:gh` produce a deployable
  `build/` without invoking CSP-hash injection or relying on `_headers` /
  `_redirects`.
- Non-functional: no dead scripts in `package.json`; no orphan files in
  `static/` or repo root.

## Architecture

The chain `vite build → inject-csp-hashes.mjs → verify-build-inline-scripts.mjs`
exists only because CF Pages reads `static/_headers` and we wanted to ship a
strict CSP without `'unsafe-inline'`. None of that survives the move:

- GH Pages can't set HTTP headers from a `_headers` file.
- User accepted dropping CSP/security-headers machinery.

So the simplification is:
- `npm run build` → `vite build` (no postbuild step).
- `npm run build:gh` → `BUILD_PROFILE=gh vite build` (no postbuild step).
- Delete `verify:build` script entry; CI step that called it gets removed.

## Related Code Files

- Delete: `wrangler.toml`
- Delete: `static/_headers`
- Delete: `static/_redirects`
- Delete: `scripts/inject-csp-hashes.mjs`
- Delete: `scripts/verify-build-inline-scripts.mjs`
- Modify: `package.json` (drop CSP postbuild from `build` and `build:gh`; drop `verify:build` script)
- Modify: `.github/workflows/verify-build.yml` (drop `npm run verify:build` step)

## Implementation Steps

1. Delete `wrangler.toml` (CF-only manifest).
2. Delete `static/_headers` and `static/_redirects` (CF-only routing/headers).
3. Delete `scripts/inject-csp-hashes.mjs` and `scripts/verify-build-inline-scripts.mjs`.
4. Edit `package.json` scripts:
   - `"build": "vite build && node scripts/inject-csp-hashes.mjs"` →
     `"build": "vite build"`
   - `"build:gh": "BUILD_PROFILE=gh vite build && node scripts/inject-csp-hashes.mjs"` →
     `"build:gh": "BUILD_PROFILE=gh vite build"`
   - Remove the entire `"verify:build": "node scripts/verify-build-inline-scripts.mjs"` line.
5. Edit `.github/workflows/verify-build.yml`:
   - Remove the trailing `- run: npm run verify:build` step.
6. Run locally to confirm:
   - `npm run build` exits 0, produces `build/index.html` and assets.
   - `npm run build:gh` exits 0, produces `build/` with basePath `/loto`
     visible in the rendered HTML (`grep -q '/loto/_app/' build/index.html`).
   - `npm test` still passes (no test should reference `_headers`/`_redirects`).

## Success Criteria

- [ ] Deleted files no longer present (`git status` shows them as deletions).
- [ ] `npm run build` and `npm run build:gh` both succeed locally.
- [ ] `package.json` has no reference to `inject-csp-hashes` or `verify:build`.
- [ ] `verify-build.yml` does not invoke `npm run verify:build`.
- [ ] CI `Verify build` workflow stays green on PR.

## Risk Assessment

- **Risk:** Some test or doc depends on `static/_headers` content.
  **Mitigation:** Phase 3 sweeps docs. Tests under `src/` don't reference
  these files; sanity-check with `grep -r '_headers\|_redirects\|wrangler' src/ tests/ 2>/dev/null` before merging.
- **Risk:** `inject-csp-hashes.mjs` referenced from somewhere besides
  `package.json` (e.g. a husky hook, a doc snippet someone copy-pastes).
  **Mitigation:** `grep -r 'inject-csp-hashes\|verify-build-inline-scripts' .`
  before deletion to confirm only `package.json` references them.
