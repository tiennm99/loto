---
phase: 3
title: Update docs and TODO
status: completed
priority: P2
effort: 1h
dependencies:
  - 1
  - 2
---

# Phase 3: Update docs and TODO

## Overview

Sweep all docs and the residual TODO list to remove CF references and
describe GH Pages as the sole deploy target. Update the README build snippet
since `build:gh` becomes the canonical build (or `build` stays as the GH
build — we keep both scripts for now since they're aliased to the same
output via env).

## Requirements

- Functional: docs accurately describe the new deploy flow.
- Non-functional: no stale `loto.miti99.com` references except where
  intentional (e.g. PageFooter's `miti99.com` is the author site, not the
  deploy URL — leave alone).

## Related Code Files

- Modify: `README.md`
- Modify: `docs/deployment-guide.md` (heaviest rewrite — currently CF-centric)
- Modify: `docs/codebase-summary.md`
- Modify: `docs/system-architecture.md`
- Modify: `docs/code-standards.md`
- Modify: `docs/development-roadmap.md`
- Modify: `docs/project-overview-pdr.md`
- Modify: `plans/todo.md` (drop CF Lighthouse entries; keep GH Pages ones)
- Read for context: `src/lib/PageFooter.svelte` (no change — `miti99.com` link is unrelated)

## Implementation Steps

1. **`README.md`**
   - Replace the Build section's two-script table with a single `npm run build:gh`
     line OR keep both but mark `build:gh` as the deployed one.
   - Replace `Deployed to Cloudflare Pages from main (set up via the CF
     dashboard — see docs/deployment-guide.md).` with: `Deployed to GitHub
     Pages from main via .github/workflows/deploy-github-pages.yml — see
     docs/deployment-guide.md.`
2. **`docs/deployment-guide.md`** — substantial rewrite:
   - Build Profiles table: drop CF row, keep GH Pages row as the only target.
   - Replace "Production Deployment — Cloudflare Pages" section with
     "Production Deployment — GitHub Pages": describe the workflow, GH repo
     Settings → Pages → Source: GitHub Actions, URL `https://tiennm99.github.io/loto/`.
   - Delete "GitHub Pages (redirect-only)" subsection.
   - Delete "Manual GH Pages Build (still available)" subsection (the build
     IS the canonical build now).
   - "Build & Output" section: remove mention of `_headers`/`_redirects` and
     CSP injection.
   - "Environment Variables → Build-Time": `BUILD_PROFILE=gh` is now the
     default for the deploy workflow; document it as such.
   - "CI/CD Pipeline" section: drop the Cloudflare bullet; keep only GH Pages.
   - "Security Considerations": drop CSP/headers bullets that no longer apply;
     a one-liner that GH Pages serves HTTPS by default is enough.
   - "Troubleshooting" table: drop the `BUILD_PROFILE` row that mentions
     Cloudflare; reword the basePath row for `/loto` only.
   - Update "Last reviewed" date to 2026-05-09.
3. **`docs/codebase-summary.md`** — find Cloudflare/CF/wrangler/_headers
   mentions, replace with GH Pages descriptions or remove.
4. **`docs/system-architecture.md`** — same sweep; if it has a deployment
   diagram or section, replace CF box with GH Pages.
5. **`docs/code-standards.md`** — likely just a passing CF mention; replace
   or remove. If it references `inject-csp-hashes.mjs`, drop that.
6. **`docs/development-roadmap.md`** — replace CF references with GH Pages.
7. **`docs/project-overview-pdr.md`** — replace CF references with GH Pages.
8. **`plans/todo.md`** — under "PWA install verification":
   - Delete "Lighthouse — Cloudflare Pages (root base)" subsection entirely.
   - Keep "Lighthouse — GitHub Pages (`/loto/` base)" as the sole production
     check.
   - Delete the "CSP + headers (production)" subsection (no longer applicable).
   - In "Common gotchas", remove the CSP / `_headers` references.
   - Drop the "CSP hash brittleness" entry under "Tech debt".

## Success Criteria

- [ ] `grep -ri 'cloudflare\|wrangler\|_headers\|_redirects\|loto\.miti99\.com' docs/ README.md plans/todo.md` returns nothing (or only intentional leftovers documented in this plan).
- [ ] `docs/deployment-guide.md` describes only GH Pages.
- [ ] `plans/todo.md` no longer has CF-specific Lighthouse / CSP entries.
- [ ] `docs/deployment-guide.md` "Last reviewed" updated.

## Risk Assessment

- **Risk:** Doc sweeps miss a reference and downstream readers get confused.
  **Mitigation:** The grep success-criterion is the safety net.
- **Risk:** `docs/code-standards.md` or `docs/system-architecture.md` describe
  the CSP hash injection as a code-standard. Removing without reading
  context could leave a dangling concept (e.g. "we ship strict CSP" claims).
  **Mitigation:** Read each doc fully before editing; rewrite affected
  paragraphs rather than deleting sentences mid-thought.
