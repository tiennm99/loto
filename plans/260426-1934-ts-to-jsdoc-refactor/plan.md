---
slug: ts-to-jsdoc-refactor
created: 2026-04-26
status: completed
completedAt: 2026-04-26
mode: fast
blockedBy: []
blocks: []
---

# Refactor TypeScript → JavaScript + JSDoc

Convert all `.ts` / `.tsx` source to `.js` / `.jsx`. Replace inline TS types with JSDoc `@type` / `@param` / `@returns`. Drop the TS toolchain.

## Why (per user request)

- Reduce config surface (no `tsconfig.json`, no TS deps)
- Author types in comments rather than syntax

## Why this is risky (read before starting)

| Concern | Reality |
|---|---|
| Lost compile-time type safety | JSDoc types are checked **only** if `// @ts-check` (or `checkJs: true`) is on, and even then catch a subset of what TS catches (no const generics, no template literal types, weaker inference). |
| Recent hardening regresses | `lib/game-logic.ts`'s `isNumberMatrix` / `isBoolMatrix` runtime guards stay, but the surrounding type narrowing weakens. |
| Verbosity | JSDoc blocks add 3-7 lines per non-trivial function vs. inline TS. |
| Next.js examples assume TS | Snippets in `docs/code-standards.md` need rewrites; future Stack-Overflow copy-paste fits less cleanly. |
| Tooling friction | Some IDE refactors (Rename Symbol across files, find-references) are weaker without TS server backing TS files. |

If after reading this you don't have a concrete reason JSDoc is better for *this* project, stop and don't run the plan.

## Scope

- 4 source files: `app/page.tsx`, `app/master/page.tsx`, `app/layout.tsx`, `components/player-board.tsx`
- 1 logic file: `lib/game-logic.ts`
- 1 config file: `next.config.ts`
- 1 generated file to delete: `next-env.d.ts`
- `tsconfig.json` → `jsconfig.json` (Next reads jsconfig for path aliases)
- `package.json` — drop `typescript`, `@types/*` deps
- `eslint.config.mjs` — already JS, may need rule tweaks
- `docs/code-standards.md` — update snippets

Out of scope: behavior changes, new features, test addition.

## Phases

| # | Phase | File | Effort |
|---|---|---|---|
| 1 | Tooling swap | `phase-01-tooling-swap.md` | S |
| 2 | Source conversion + JSDoc | `phase-02-source-conversion.md` | M |
| 3 | Verify, docs, commit | `phase-03-verify-and-docs.md` | S |

Total: ~1-2h focused. Mechanical work, no architecture decisions.

## Acceptance criteria

- [ ] No `.ts` or `.tsx` files remain in `app/`, `components/`, `lib/`
- [ ] `next.config.mjs` replaces `next.config.ts`
- [ ] `jsconfig.json` replaces `tsconfig.json` with the same `@/*` path alias
- [ ] `npm run build` produces same `Route (app)` table as before (`/`, `/_not-found`, `/master`, all static)
- [ ] `npm run lint` produces no NEW errors (3 pre-existing `react-hooks/set-state-in-effect` allowed)
- [ ] `npm run dev` and `npm run dev:codeserver` both start cleanly
- [ ] `package.json` `dependencies` and `devDependencies` no longer reference `typescript`, `@types/node`, `@types/react`, `@types/react-dom`
- [ ] All public functions in `lib/game-logic.js` have JSDoc with `@param` / `@returns`
- [ ] All component prop interfaces have a `@typedef` block
- [ ] `docs/code-standards.md` snippets use JS+JSDoc
- [ ] Single commit (or coherent series) on `dev` branch

## Risks / mitigations

1. **`output: "export"` + JSX in `next.config.mjs`** — Next supports `next.config.mjs`, no concern.
2. **`@/*` alias** — Works in `jsconfig.json` identically.
3. **`eslint-config-next`** — Works with both; remove TS-specific rules if any.
4. **Type narrowing in `safeParse<T>`** — Generic stays expressible in JSDoc as `@template`. Verify the validators still narrow correctly via `// @ts-check`.
5. **`React.FC` vs function declaration** — Codebase uses plain function declarations; no change.
6. **Vendor `next-env.d.ts`** — Pure-JS Next projects don't need it; delete and verify.

## Rollback

Single revert of the conversion commit. The previous TS state is preserved on `master` and earlier `dev` commits.
