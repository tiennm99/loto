---
phase: 3
title: Verify, docs, commit
priority: high
effort: S
status: planned
---

# Phase 3 — Verify, docs, commit

Final pass: end-to-end verify, update docs, commit + push.

## Steps

1. **Build check**
   ```bash
   npm run build
   ```
   Expect: same routes (`/`, `/_not-found`, `/master`), all static.

2. **Type check via JSDoc**
   ```bash
   npx tsc --noEmit
   ```
   `checkJs: true` makes tsc validate JSDoc types in `.js` / `.jsx`. Treat any new error as a regression — fix the JSDoc, don't disable the check.

3. **Lint**
   ```bash
   npx eslint app components lib next.config.mjs
   ```
   Expect ≤3 errors (the pre-existing `react-hooks/set-state-in-effect`).

4. **Smoke test both dev modes**
   ```bash
   npm run dev          # localhost:3000/
   npm run dev:codeserver  # /absproxy/{port}/
   ```
   Verify:
   - Generate a card, mark a row, see "Kinh!" popup
   - Master page draws numbers, player card on /master also works (own storage prefix)
   - localStorage persists across reload

5. **Update docs**
   - `docs/code-standards.md` — replace TS examples with JS+JSDoc, update language references
   - `docs/codebase-summary.md` — file extensions in tables (`.tsx` → `.jsx`, `.ts` → `.js`)
   - `docs/system-architecture.md` — same
   - `docs/development-roadmap.md` — mark "JSDoc migration" if listed; otherwise no change
   - `README.md` — if it mentions TypeScript, update

6. **Commit on `dev`**
   ```
   refactor: convert from TypeScript to JavaScript with JSDoc

   - Replace .ts/.tsx with .js/.jsx
   - Author types as JSDoc with checkJs: true so tsc still validates
   - Drop typescript and @types/* devDependencies
   - tsconfig.json -> jsconfig.json (same @/* alias)
   - next.config.ts -> next.config.mjs
   - Delete vendored next-env.d.ts (not needed for pure-JS Next projects)
   - Update docs to reflect new file extensions
   ```

7. **Push** — `git push`. Branch `dev` already tracks `origin/dev`.

## Acceptance gates (must pass before commit)

- [ ] `npm run build` succeeds
- [ ] `npx tsc --noEmit` succeeds (zero new errors vs. pre-refactor)
- [ ] Lint error count unchanged or lower
- [ ] Both dev profiles boot
- [ ] Manual smoke: generate card, mark row, see bingo
- [ ] No `*.ts` / `*.tsx` files remain in `app/`, `components/`, `lib/`, root config

## Rollback

Single `git revert` of the conversion commit recovers the TS state.

## Status: planned
