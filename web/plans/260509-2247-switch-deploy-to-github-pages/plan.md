---
title: Switch deploy target from Cloudflare Pages to GitHub Pages
description: >-
  Make GitHub Pages the canonical deploy at tiennm99.github.io/loto. Drop CF
  Pages, _headers, _redirects, CSP-hash injection. Keep it simple — static site,
  no security-headers machinery.
status: completed
priority: P2
created: 2026-05-09T00:00:00.000Z
---

# Switch deploy target from Cloudflare Pages to GitHub Pages

## Overview

Today CF Pages is canonical (`loto.miti99.com`) and GH Pages serves a redirect HTML
to it. Flip that: make GH Pages do a real build of `npm run build:gh` (basePath
`/loto`) and serve the app at `https://tiennm99.github.io/loto/`. Remove CF
artifacts (`wrangler.toml`, `static/_headers`, `static/_redirects`, CSP-hash
injection scripts) since GH Pages can't honor them and the user opted to keep
it simple for a static site.

## Phases

| Phase | Name | Status |
|-------|------|--------|
| 1 | [Wire GH Pages build into CI](./phase-01-wire-gh-pages-build-into-ci.md) | Completed |
| 2 | [Remove Cloudflare artifacts](./phase-02-remove-cloudflare-artifacts.md) | Completed |
| 3 | [Update docs and TODO](./phase-03-update-docs-and-todo.md) | Completed |

## Dependencies

None. Sequential within plan: phase 1 → 2 → 3 (CI must work before docs declare
the new flow). Phase 2 can land in same PR as phase 1 since they touch
different files.

## Post-merge manual step

Disable the Cloudflare Pages project from the CF dashboard so it stops
auto-building from `main`. Optionally remove the `loto.miti99.com` DNS record
or repoint it (out of scope for this plan).
