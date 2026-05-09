---
phase: 1
title: Wire GH Pages build into CI
status: completed
priority: P2
effort: 1h
dependencies: []
---

# Phase 1: Wire GH Pages build into CI

## Overview

Replace the redirect-only `deploy-github-pages.yml` with a real build+deploy
pipeline that runs `npm run build:gh` (basePath `/loto`) and uploads `build/`
as the GH Pages artifact. Site lives at `https://tiennm99.github.io/loto/`.

## Requirements

- Functional: push to `main` builds and deploys the SvelteKit app to GH Pages.
- Non-functional: workflow uses `actions/configure-pages@v5`, `upload-pages-artifact@v3`, `deploy-pages@v4` (already present). Concurrency group `github-pages`. Caches npm.

## Architecture

Single workflow, two jobs (build → deploy). Build job runs Node 20, `npm ci`,
`npm run build:gh`, uploads `build/`. Deploy job consumes the artifact.

`build:gh` already exists in `package.json` and produces basePath `/loto` via
`BUILD_PROFILE=gh` in `svelte.config.js:23`. CSP-hash injection step in that
script (`node scripts/inject-csp-hashes.mjs`) gets removed in Phase 2 — for
this phase we leave it; the script no-ops cleanly if `_headers` is absent
after Phase 2 lands (will be revisited).

Note: Phase 1 + 2 should land in the same PR so the build script and the
files it touches stay consistent.

## Related Code Files

- Modify: `.github/workflows/deploy-github-pages.yml`
- Read for context: `package.json`, `svelte.config.js`, `.github/workflows/verify-build.yml`

## Implementation Steps

1. Rewrite `.github/workflows/deploy-github-pages.yml`:
   - Replace the `Generate redirect pages` step block with a real build:
     ```yaml
     - uses: actions/checkout@v4
     - uses: actions/setup-node@v4
       with:
         node-version: 20
         cache: npm
     - run: npm ci
     - run: npm run build:gh
     - uses: actions/configure-pages@v5
     - uses: actions/upload-pages-artifact@v3
       with:
         path: build
     ```
   - Keep deploy job as-is (`actions/deploy-pages@v4`, environment
     `github-pages`).
   - Rename workflow: `name: Deploy redirect to GitHub Pages` →
     `name: Deploy to GitHub Pages`.
2. After PR merges and the first run goes green:
   - GitHub repo → Settings → Pages → Source: GitHub Actions (should already
     be set; confirm).
   - Confirm `https://tiennm99.github.io/loto/` loads the app, not the old
     redirect HTML.

## Success Criteria

- [ ] `deploy-github-pages.yml` runs `npm run build:gh` and uploads `build/`.
- [ ] First post-merge run on `main` succeeds (both build + deploy jobs green).
- [ ] `https://tiennm99.github.io/loto/` serves the live app.
- [ ] Service worker registers at `/loto/sw.js`; manifest at
  `/loto/manifest.webmanifest`; icons at `/loto/icons/...` resolve.
- [ ] Audio clips load from `/loto/audio/{voice}/{n}.mp3`.

## Risk Assessment

- **Risk:** basePath mismatch causes 404s on assets.
  **Mitigation:** `build:gh` already wires basePath `/loto`; `import { base } from '$app/paths'` is used internally per `docs/deployment-guide.md:12`. Verify in Phase 1 success-criteria checks.
- **Risk:** Stale CF cache or DNS still points users to old `loto.miti99.com`.
  **Mitigation:** Out of scope for this plan; documented as post-merge manual step in `plan.md`.
- **Risk:** Service worker from previous CF deploy lingers in user browsers and
  serves stale paths. **Mitigation:** SW uses `registerType: "autoUpdate"`
  (`vite.config.js:42`). Users on `loto.miti99.com` won't see the new
  deployment anyway since URL changed; users on `tiennm99.github.io/loto` had
  only the redirect HTML before, no SW registered.
