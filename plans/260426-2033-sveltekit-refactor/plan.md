---
slug: sveltekit-refactor
created: 2026-04-26
status: completed
completedAt: 2026-04-26
mode: fast
blockedBy: []
blocks: []
---

# Refactor Next.js → SvelteKit

Replace the Next.js 16 + React 19 stack with SvelteKit + Svelte 5 runes. User
chose SvelteKit over plain Svelte to leave room for future routes / load
functions / a possible backend. Static export only for now (no SSR target).

## Why (per user request)

- Bundle size — Svelte compiles, no runtime; ~75% smaller payload
- Reactivity ergonomics — runes (`$state`, `$derived`, `$effect`) replace the
  React `useRef<Set>` / two-pass `useEffect` / `useMemo` ceremony in
  `components/player-board.jsx`
- Consistency with `sokoban` / `rplace` (already Svelte 5)
- Room to extend — file-based routing, layouts, future server endpoints

## Constraints (must preserve)

- **JSDoc only** — no TypeScript (explicit user preference, just shipped)
- **Static export** to Cloudflare Pages at `loto.miti99.com` (root basePath)
- **Codeserver dev profile** equivalent to current `npm run dev:codeserver`
- **Gameplay 1:1** — bingo popup ("Kinh!"), waiting toast ("Chờ X"), master
  9×10 tracking board, host's own player card via `storagePrefix`
- **Visual identity** — indigo→purple for player, orange→red for host,
  emerald for completed rows, amber for waiting toast
- **Tailwind 4** — keep
- **Two routes** — `/` and `/master`
- **localStorage prefix pattern** — `loto_grid` / `loto_crossed` /
  `loto_master` / `loto_master_card_*`
- **Plans + docs preserved** — update content; don't delete

## Stack target

| Concern | Choice |
|---|---|
| Framework | SvelteKit (latest stable) |
| Language | Svelte 5 (runes mode) + JS + JSDoc |
| Adapter | `@sveltejs/adapter-static` (CF Pages target) |
| Styling | Tailwind 4 (Vite plugin) |
| Build | Vite (under SvelteKit) |
| Routing | SvelteKit file-based (`src/routes/+page.svelte`) |
| State | Svelte 5 `$state` runes; localStorage in `$effect` |
| Lint | ESLint 9 + `eslint-plugin-svelte` (no TS rules) |

## Out of scope (deliberately deferred)

- Tests — none exist; not adding now
- New features (sound effects, undo, multiplayer, theme switcher) — see
  `docs/development-roadmap.md`
- TypeScript — explicitly rejected
- Backend / API — extension surface, not this refactor

## Risk callouts

| Risk | Mitigation |
|---|---|
| Rewrite reopens fixed bugs (`isRowComplete` empty-row, two-pass row scan) | Phase 5 has explicit checklist mapping each Next-side fix to its Svelte equivalent |
| Codeserver proxy in Vite uses different config keys than Next | Copy proven config from sibling `sokoban` repo verbatim |
| `adapter-static` requires `prerender = true` on every page | One-line `export const prerender = true` in root `+layout.js` covers all routes |
| Tailwind 4 + SvelteKit setup is newer; less StackOverflow coverage | Use the Tailwind official Vite plugin; pinned in package.json |
| Plans dir + reports inside repo make `git mv` from old paths messy | Don't move plans; the new SvelteKit tree replaces `app/`, `components/`, `lib/` |

## Phases

| # | Phase | File | Effort |
|---|---|---|---|
| 1 | Scaffold + tooling | `phase-01-scaffold.md` | M |
| 2 | Port game logic | `phase-02-game-logic.md` | S |
| 3 | Port player-board component | `phase-03-player-board.md` | M |
| 4 | Port routes + layout | `phase-04-routes.md` | M |
| 5 | Wire codeserver + CF deploy | `phase-05-deploy-profiles.md` | S |
| 6 | Update docs + verify + commit | `phase-06-finalize.md` | S |

Total: ~3-4h focused.

## Acceptance criteria

- [ ] `npm run dev` boots SvelteKit dev server, `/` and `/master` render
- [ ] `npm run build` produces static export in `build/` (SvelteKit default)
- [ ] `npm run dev:codeserver` works under `/absproxy/{port}`
- [ ] Generate card → click cells → bingo popup fires once per completed row
- [ ] Waiting toast ("Chờ X") fires on 4/5 and clears on row completion
- [ ] localStorage state survives reload for both player and master cards
- [ ] Master draws numbers, tracking board lights up cells, master's player
      card plays independently
- [ ] No `.tsx`, `.ts`, `.jsx`, `next.config.*`, `next-env.d.ts`, or
      `app/`, `components/`, `lib/` dirs remain
- [ ] No `react`, `react-dom`, `next` in `package.json`
- [ ] `docs/` updated to describe SvelteKit structure
- [ ] `plans/260426-2033-sveltekit-refactor/` complete; old plan dir
      preserved as historical record
- [ ] CF Pages dashboard build command updated to match the new output dir

## Rollback

`git revert` of the squash-merge commit (or the series of phase commits)
restores Next state. Old code is preserved in `master` branch + earlier `dev`
commits.

## Reference projects

- `tiennm99/sokoban` — Svelte 5 + Vite + codeserver dev config (template for
  Phase 5)
- `tiennm99/rplace` — Svelte 5 + Hono on Cloudflare Workers (extension hint
  for future backend work; not used here)
