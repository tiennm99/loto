---
phase: 6
title: Update docs, verify end-to-end, commit
priority: high
effort: S
status: planned
---

# Phase 6 — Finalize

End-to-end verification, docs sync, commit + push.

## Steps

1. **End-to-end smoke test (manual)**
   ```bash
   npm run dev
   ```
   - Visit `/`, generate a card, click 4 cells in row 0 → see "Chờ X" toast
   - Click the 5th → see "Kinh!" popup with "Hàng 1 đã đầy đủ!"
   - Press Esc — popup dismisses
   - Reload — card persists
   - Visit `/master`, click "Ván mới", click "Xổ số" several times — numbers
     light up on the 9×10 board, history strip grows
   - Generate a card on `/master` (the host's own card) — confirm it's a
     different card than `/`'s (separate localStorage prefixes)

2. **Build check**
   ```bash
   npm run build
   ```
   Expect `build/` populated, no errors. `build/index.html`,
   `build/master.html` (or `build/master/index.html` depending on
   `trailingSlash` config).

3. **Codeserver dev smoke**
   ```bash
   npm run dev:codeserver
   ```
   Open the proxy URL; verify page + HMR.

4. **Lint**
   ```bash
   npm run lint
   ```
   No new errors.

5. **Update docs.** Files to rewrite (re-generate from new code state, don't
   edit the Next-era versions):

   - `docs/codebase-summary.md` — file table now lists `src/routes/...`,
     `src/lib/...`, `svelte.config.js`, `vite.config.js`
   - `docs/system-architecture.md` — diagram of SvelteKit route flow,
     Svelte 5 runes pattern, drop "use client" client-only architecture
     section (no longer relevant — `ssr: false` in `+layout.js`)
   - `docs/code-standards.md` — Svelte 5 runes (`$state`, `$derived`,
     `$effect`), `<script>` vs `<script module>`, JSDoc style for `$props()`
   - `docs/design-guidelines.md` — Tailwind classes unchanged, but the
     "use Tailwind in className" examples become `class=`. Animation CSS
     stays in `app.css`.
   - `docs/deployment-guide.md` — output dir `build/` (not `out/`),
     framework preset SvelteKit
   - `docs/development-roadmap.md` — drop any roadmap items that
     SvelteKit unlocks (e.g. "consider migration to lighter framework"
     if listed)
   - `docs/project-overview-pdr.md` — tech stack section: Next.js → SvelteKit

   Set `Last reviewed: 2026-04-26` on each.

6. **Update `README.md`** — entry point sentences:

   ```md
   # Lô tô

   Bàn số của trò chơi "Lô tô" — SvelteKit app.

   Two routes: `/` for players, `/master` for the host.

   See `docs/` for architecture, code standards, and deployment.
   ```

7. **Update active plan + sync ts-to-jsdoc plan if not already.**
   - `plans/260426-1934-ts-to-jsdoc-refactor/plan.md` — already marked
     `completed` (done in Phase 0 of this refactor).
   - `plans/260426-2033-sveltekit-refactor/plan.md` — change `status:
     planned` → `status: completed` after this phase finishes.

8. **Commit on `dev`** in coherent slices:

   ```
   refactor: scaffold sveltekit, drop next/react

   Replace Next.js 16 + React 19 with SvelteKit + Svelte 5 runes. New
   svelte.config.js / vite.config.js / src/app.html, all Next config files
   removed. Adapter-static for CF Pages compatibility.

   refactor: port game logic + player board to svelte

   src/lib/game-logic.js — verbatim copy of the JSDoc-only utilities.
   src/lib/PlayerBoard.svelte — runes-based reactivity replaces useState/
   useRef/useMemo/useEffect ceremony. Bingo and waiting detection use a
   two-pass $effect to mirror the React-side fix. <button> cells with
   aria-label/aria-pressed/focus ring kept.

   refactor: port routes; wire deploy profiles

   src/routes/+layout.{svelte,js} sets prerender + ssr=false. + page.svelte
   and master/+page.svelte mirror the Next pages 1:1, using $app/paths base
   so internal links survive /loto and /absproxy/{port} rewrites.
   svelte.config.js routes BUILD_PROFILE=gh and codeserver dev to the right
   basePath; default empty for CF Pages.

   docs: refresh for sveltekit
   ```

   Or one squashed commit if you prefer — your call.

9. **Push** — `git push`. Already tracking `origin/dev`.

## Acceptance gates (must pass before commit)

- [ ] Manual smoke covered all paths
- [ ] `npm run build` succeeds
- [ ] `npm run lint` no new errors
- [ ] Both dev profiles boot
- [ ] Zero references to Next, React, or `app/`/`components/`/`lib/` in
      source
- [ ] `docs/` updated to SvelteKit terminology
- [ ] No leftover `next.config.*`, `next-env.d.ts`, `tsconfig.tsbuildinfo`,
      `out/` directory

## Rollback

`git revert` of the phase commits restores Next state. Old code preserved
on `master` branch + earlier `dev` commits.

## Status: planned
