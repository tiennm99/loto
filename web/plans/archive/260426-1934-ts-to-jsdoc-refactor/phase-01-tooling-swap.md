---
phase: 1
title: Tooling swap
priority: high
effort: S
status: planned
---

# Phase 1 — Tooling swap

Replace TS toolchain with JS + JSDoc equivalents. No source-file changes yet.

## Steps

1. **Create `jsconfig.json`** at repo root, mirroring `tsconfig.json`'s essentials:

```json
{
  "compilerOptions": {
    "target": "ES2017",
    "module": "esnext",
    "moduleResolution": "bundler",
    "checkJs": true,
    "allowJs": true,
    "jsx": "react-jsx",
    "lib": ["dom", "dom.iterable", "esnext"],
    "strict": false,
    "paths": { "@/*": ["./*"] }
  },
  "include": ["**/*.js", "**/*.jsx", "**/*.mjs"],
  "exclude": ["node_modules", ".next", "out"]
}
```

   Notes: `checkJs: true` enables JSDoc type checking. `strict: false` because JSDoc strict mode trips on plain values; keep narrowing opt-in.

2. **Delete `tsconfig.json`** and `next-env.d.ts`.

3. **Edit `package.json`** — remove from `devDependencies`:
   - `typescript`
   - `@types/node`
   - `@types/react`
   - `@types/react-dom`

   Keep `eslint-config-next` (works with both).

4. **`eslint.config.mjs`** — read it; if it imports TS-specific parser/rules, swap to JS equivalents. Most likely no change needed.

5. **Run `npm install`** to prune the TS deps from `node_modules` and update `package-lock.json`.

## Files affected

- create: `jsconfig.json`
- delete: `tsconfig.json`, `next-env.d.ts`, `tsconfig.tsbuildinfo` (already gitignored)
- modify: `package.json`, `package-lock.json`
- maybe modify: `eslint.config.mjs`

## Verify

- `ls *.ts *.tsx` returns nothing
- `npm run lint` doesn't fail because of missing TS parser
- `npm run dev` doesn't error on the now-removed `tsconfig`

## Out of scope

Source file conversion (Phase 2).

## Status: planned
