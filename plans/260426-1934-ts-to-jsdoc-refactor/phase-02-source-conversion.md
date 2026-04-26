---
phase: 2
title: Source conversion + JSDoc
priority: high
effort: M
status: planned
---

# Phase 2 — Source conversion + JSDoc

Rename source files and replace TS syntax with JSDoc.

## Files

| From | To |
|---|---|
| `next.config.ts` | `next.config.mjs` |
| `app/layout.tsx` | `app/layout.jsx` |
| `app/page.tsx` | `app/page.jsx` |
| `app/master/page.tsx` | `app/master/page.jsx` |
| `components/player-board.tsx` | `components/player-board.jsx` |
| `lib/game-logic.ts` | `lib/game-logic.js` |

Use `git mv` so history follows. Then strip TS-only syntax: type annotations on params/returns, `interface`, `type` aliases, generics on calls (`useState<T>` → `useState`), `as` assertions, `!` non-null. Replace each with JSDoc.

## JSDoc patterns to apply

### Function with simple types
```js
/**
 * @param {number[][]} grid
 * @param {boolean[][]} crossed
 * @param {number} row
 * @returns {boolean}
 */
export function isRowComplete(grid, crossed, row) { ... }
```

### Generic helper (was `safeParse<T>`)
```js
/**
 * @template T
 * @param {string | null} raw
 * @param {(v: unknown) => v is T} validate
 * @returns {T | null}
 */
function safeParse(raw, validate) { ... }
```

### Type predicate (validators)
Keep them; JSDoc has `v is T` syntax inside `@param` parens.

### Component props (was `interface PlayerBoardProps`)
```js
/**
 * @typedef {Object} PlayerBoardProps
 * @property {string} [storagePrefix] localStorage key prefix
 */

/** @param {PlayerBoardProps} props */
export default function PlayerBoard({ storagePrefix = "loto" }) { ... }
```

### `useState` initial
Inferred from initial value — usually no JSDoc needed. For nullable state, type the initial:
```js
/** @type {[number[][] | null, (v: number[][] | null) => void]} */
const [grid, setGrid] = useState(null);
```
Or simpler: just trust inference, only annotate when checkJs complains.

### `useRef` for Set
```js
/** @type {React.MutableRefObject<Set<number>>} */
const celebratedRows = useRef(new Set());
```

### `next.config.mjs`
```js
/** @type {import('next').NextConfig} */
const nextConfig = { ... };
export default nextConfig;
```

### Layout child types
```js
/** @param {{ children: React.ReactNode }} props */
export default function RootLayout({ children }) { ... }
```

## Per-file checklist

- [ ] **`next.config.mjs`** — add `@type` import. Remove `import type { NextConfig }`. Keep all logic.
- [ ] **`lib/game-logic.js`** — add `// @ts-check` at top. JSDoc on every exported function. Type predicates for `isNumberMatrix`, `isBoolMatrix`. `@template T` on `safeParse`. Remove explicit `: number[][]`, `: boolean`, `void`, etc.
- [ ] **`components/player-board.jsx`** — `@typedef` for props. JSDoc on `useRef<Set<number>>`. JSDoc on `useState` only where inference fails (e.g. nullable grid).
- [ ] **`app/layout.jsx`** — strip `Readonly<{...}>`, replace `Metadata` import with JSDoc.
- [ ] **`app/page.jsx`** — minimal; mostly remove `useState<boolean>` etc.
- [ ] **`app/master/page.jsx`** — same; the `MasterState` interface becomes a `@typedef`. The `BOARD: ReadonlyArray<...>` annotation drops; runtime `Object.freeze` keeps the immutability guarantee.

## Strategy

1. Convert `lib/game-logic.ts` first — it has the most TS-heavy code and proves the pattern.
2. Run `npx tsc --noEmit` (still works on `.js` with checkJs) after each file to catch JSDoc syntax mistakes.
3. Convert UI files top-down: layout → page → master/page → player-board.
4. Convert `next.config.ts` last; verify `npm run build` survives.

## Verify

- `npx tsc --noEmit` passes (with `checkJs: true` in jsconfig, this still type-checks via JSDoc)
- `npm run build` produces same routes table
- `npm run dev` boots without warnings about missing types
- `grep -rn ":\s*\(string\|number\|boolean\|void\|any\)" app components lib` returns no TS-style annotations

## Out of scope

- Adding new types or improving existing ones
- Refactoring component structure
- Tests

## Status: planned
