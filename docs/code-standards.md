# Code Standards

## File Naming & Structure

- **Kebab-case** for all files: `PlayerBoard.svelte`, `game-logic.js`.
- **Descriptive names**: Long names are preferred for self-documentation. Avoid ambiguity.
- **Single responsibility**: Each file has one primary export (component or utilities).
- **Max 200 lines per file**: Split larger components into smaller focused ones.

## Svelte 5 Runes & JavaScript

### Rune Globals & eslint
Svelte 5 runes (`$state`, `$derived`, `$effect`, `$props`, `$bindable`, `$inspect`, `$host`) are declared as readonly globals in `eslint.config.mjs` (lines 16–22) to suppress "undefined variable" linter errors. They are usable in `.svelte` and `.svelte.js` files without import.

### State & Reactivity
- **$state**: For mutable local UI state (grid, crossed, booleans). `let grid = $state(...)`
- **$derived**: For computed values that auto-update when dependencies change. `const rowCompleteness = $derived(grid && crossed.length ? [...] : [])`
- **$effect**: For side effects (localStorage sync, detecting completed rows). Replaces React useEffect.
- **$props**: For destructuring component props. `let { storagePrefix = "loto" } = $props()`

### Reactive Store Files (`.svelte.js`)
Use the `.svelte.js` extension for modules that export rune-based reactive state (e.g., `settings-store.svelte.js`). This signals to bundlers and linters that the file uses Svelte 5 reactivity at module scope.

### Plain References (No Reactivity)
- Use regular `let` or `const` for timers, Sets, Maps (not reactive). Example: `let toastTimer = null; const celebratedRows = new Set();`

### Typing (JSDoc)
- Use JSDoc `@typedef {Object}` for prop types, JSDoc type annotations for functions.
- `jsconfig.json` has `checkJs: false` (not enabled), so types are documentation-only.
- Component props: `@typedef {Object} Props { ... }` then `/** @type {Props} */ let { ... } = $props()`
- Avoid `any`; use precise types (e.g., `number[][]` for grid).
- Generics: `@template T` for utility functions.

### Client-Only Architecture
- SvelteKit's `ssr: false` in `+layout.js` disables server rendering.
- All pages are client-only; no server-side data fetching.
- Use localStorage exclusively for persistence.

## CSS & Tailwind 4 Patterns

### Utilities
- Utility-first: `className="px-4 py-2 rounded-lg text-white"`.
- Responsive: `sm:`, `md:`, `lg:` prefixes for breakpoints.
- Dark mode: Explicit `@variant dark (.dark *)` in `app.css` declares dark-mode selector; use `dark:bg-slate-800 dark:text-white`. Settings store toggles `<html class="dark">` rather than relying on `@media (prefers-color-scheme: dark)`.
- Animations: Custom keyframes in `app.css`, apply via `animate-fade-in`.

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
- **No try-catch in render**: Keep logic in $effect or event handlers.
- **Confirmation dialogs**: `confirm("Bạn có muốn...")` for destructive actions.

## Naming Conventions

| Pattern | Example | Usage |
|---------|---------|-------|
| camelCase | `handleCellClick`, `storagePrefix` | variables, functions, props |
| PascalCase | `PlayerBoard`, `MasterPage` | components, types |
| UPPER_SNAKE | `STORAGE_KEY`, `NUM_ROWS` | constants |
| kebab-case | `PlayerBoard.svelte` | file names |

## Comment Style

- Document **why**, not **what**. The code shows what it does.
- Use `/** JSDoc */` for exported functions.
- Inline comments for complex logic (e.g., weighted random selection in `src/lib/game-logic.js:19–28`).

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

## Internal Navigation

- Use `import { base } from '$app/paths'` for internal links to preserve basePath across deployments.
- Example: `<a href="{base}/master">Host page</a>` works on root (`""`) and subpath (`/loto`) equally.

## Import Organization

1. SvelteKit imports (`$app/paths`, `$lib/...`)
2. Third-party imports
3. Local component/utility imports

```js
import { base } from "$app/paths";
import { generateGrid, isRowComplete } from "$lib/game-logic.js";
import PlayerBoard from "$lib/PlayerBoard.svelte";
```

## Component Patterns

### Props Pattern
```svelte
<script>
  /**
   * @typedef {Object} Props
   * @property {string} [storagePrefix]
   */
  /** @type {Props} */
  let { storagePrefix = "loto" } = $props();
</script>
```

### Event Handlers
Use inline event handlers (`onclick`, `onkeydown`). Svelte 5 handles click delegation. Event objects automatically passed:
```svelte
<button onclick={() => handleCellClick(row, col)}>Click</button>
<div onkeydown={(e) => e.key === "Escape" && dismiss()}>Dialog</div>
```

## Testing

### Unit Tests (Implemented)
- **Framework**: Vitest 4.1.5 with happy-dom
- **Test Files**: `src/lib/game-logic.test.js` (26 tests), `src/lib/settings-store.test.js` (27 tests) — 53 total passing
- **Coverage**: Game logic (generateGrid shape/constraints, isRowComplete, getWaitingNumber, persistence with validators), settings (load/save/reset with error handling, theme detection, master mode toggle, auto-call speed, color validation)
- **Scripts**: `npm test` (run once), `npm run test:watch` (continuous)
- **Pattern**: Use vitest's `describe` / `it` blocks, `expect()` assertions. Test both happy path and error cases (corrupt JSON, missing localStorage).

### Component Tests (Planned)
Future: render PlayerBoard, mock localStorage, verify toast/popup behaviors.

### E2E Tests (Planned)
Future: Playwright for player flow (generate → click → bingo) and host flow (draw → master board updates).

## Configuration

### Environment Variables (code-server only)
- **VITE_DEV_PROFILE**: "codeserver" triggers proxy config.
- **CODESERVER_HOST**: Hostname for HMR.
- **CODESERVER_PORT**: Port (defaults to 3000).

Set in `.env.local` (not committed).

### Build Targets
- **adapter-static**: Generates static HTML + JS in `build/`.
- **basePath**: Dual-mode: `""` (Cloudflare, dev) or `/loto` (GitHub Pages via `BUILD_PROFILE=gh`).

Last reviewed: 2026-04-27
Last synced: 2026-04-27 (6-phase refactor)
