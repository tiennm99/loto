# Code Standards

## File Naming & Structure

- **Kebab-case** for all files: `player-board.jsx`, `game-logic.js`.
- **Descriptive names**: Long names are preferred for self-documentation. Avoid ambiguity.
- **Single responsibility**: Each file has one primary export (component or utilities).
- **Max 200 lines per file**: Split larger components into smaller focused ones.

## React & JavaScript Conventions

### Hooks
- **useState**: For local UI state (grid, crossed, form inputs).
- **useEffect**: For side effects (load from localStorage, save to localStorage, detect changes).
- **useCallback**: For event handlers to stabilize function identity across renders.
- **useRef**: For mutable values that don't trigger renders (timers, celebratedRows set).

### Typing (JSDoc)
- Author types in JSDoc comments — `jsconfig.json` has `checkJs: true`, so `tsc --noEmit` validates them.
- Component props: `@typedef {Object} Props { ... }` followed by `@param {Props} props` on the function.
- Layout children: `@param {{ children: React.ReactNode }} props`.
- Avoid `any`; use precise types (e.g., `number[][]` for grid).
- Generics: `@template T` then reference `T` in `@param` / `@returns`.

### Client-Only Constraint
- All interactive pages must have `"use client"` at the top.
- No server-side data fetching; use localStorage instead.
- No async Server Components.

## CSS & Tailwind 4 Patterns

### Utilities
- Utility-first: `className="px-4 py-2 rounded-lg text-white"`.
- Responsive: `sm:`, `md:`, `lg:` prefixes for breakpoints.
- Dark mode: `dark:bg-slate-800`, `dark:text-white`.
- Animations: Custom keyframes in `globals.css`, apply via `animate-fade-in`.

### Layout
- Flexbox for alignment: `flex flex-col items-center justify-center`.
- Grid for game boards: `.loto-grid { grid-template-columns: repeat(9, 1fr); }`.
- Aspect ratio for square cells: `aspect-square`.

### Gradients
- Player page: `from-indigo-500 to-purple-500`.
- Host page: `from-orange-500 to-red-500`.
- Completed rows: `bg-emerald-100` + `text-emerald-500`.
- Shadows: `shadow-lg shadow-indigo-500/25`.

## localStorage Patterns

### Saving
```js
/**
 * @param {number[][]} grid
 * @param {string} [prefix]
 */
function saveGrid(grid, prefix = "loto") {
  localStorage.setItem(`${prefix}_grid`, JSON.stringify(grid));
}
```

### Loading
```js
/**
 * @param {string} [prefix]
 * @returns {number[][] | null}
 */
function loadGrid(prefix = "loto") {
  const data = localStorage.getItem(`${prefix}_grid`);
  if (!data) return null;
  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
}
```

**Key Pattern**: `{prefix}_{key}` enables multiple independent boards per component reuse.

## Error Handling

- **Silent fallback**: JSON parse errors return null; caller checks for null.
- **No try-catch in render**: Keep logic in useEffect or event handlers.
- **Confirmation dialogs**: `confirm("Bạn có muốn...")` for destructive actions.

## Naming Conventions

| Pattern | Example | Usage |
|---------|---------|-------|
| camelCase | `handleCellClick`, `storagePrefix` | variables, functions, props |
| PascalCase | `PlayerBoard`, `MasterPage` | components, types |
| UPPER_SNAKE | `STORAGE_KEY`, `NUM_ROWS` | constants |
| kebab-case | `player-board.jsx` | file names |

## Comment Style

- Document **why**, not **what**. The code shows what it does.
- Use `/** JSDoc */` for exported functions.
- Inline comments for complex logic (e.g., weighted random selection in `lib/game-logic.js:19–28`).

### Example
```js
/**
 * Weighted random selection of a column index.
 * @param {number[]} weights
 * @returns {number}
 */
function randomANumberInRow(weights) {
  // Convert weights to cumulative distribution for O(n) lookup
  const tempWeight = [...weights];
  for (let i = 1; i < tempWeight.length; i++) {
    tempWeight[i] += tempWeight[i - 1];
  }
  // ...
}
```

## Import Organization

1. React/Next imports
2. Third-party imports
3. Local component/utility imports

```js
import { useCallback, useState } from "react";
import Link from "next/link";
import PlayerBoard from "@/components/player-board";
import { generateGrid } from "@/lib/game-logic";
```

## Testing (Not Currently Implemented)

Future tests should follow:
- Unit: test game logic (generateGrid, isRowComplete, getWaitingNumber) in isolation.
- Component: mock localStorage, render PlayerBoard with different props.
- E2E: player flow (generate → click → bingo).

## Configuration

### Environment Variables
- **NEXT_DEV_PROFILE**: "codeserver" triggers proxy config.
- **CODESERVER_HOST**: Hostname for HMR (required if NEXT_DEV_PROFILE=codeserver).
- **CODESERVER_PORT**: Port (defaults to 3000).

Set in `.env.local` (not committed).

### Build Targets
- **output: "export"**: Static HTML export (no Node.js server needed).
- **basePath**: Configurable per deployment environment.

Last reviewed: 2026-04-26
