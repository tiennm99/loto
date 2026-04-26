---
phase: 4
title: Port routes + layout
priority: high
effort: M
status: planned
---

# Phase 4 — Routes + layout

Set up the SvelteKit route tree and port the two pages.

## Target tree

```
src/
├── app.html
├── app.css
├── routes/
│   ├── +layout.svelte           ← global wrapper (replaces app/layout.tsx)
│   ├── +layout.js               ← export const prerender = true
│   ├── +page.svelte             ← player page (replaces app/page.tsx)
│   └── master/
│       └── +page.svelte         ← host page (replaces app/master/page.tsx)
└── lib/
    ├── game-logic.js            ← from Phase 2
    └── PlayerBoard.svelte       ← from Phase 3
```

## `+layout.js` — enable static prerendering for every route

```js
export const prerender = true;
export const ssr = false;       // pure SPA, no HTML pre-render of dynamic state
export const trailingSlash = 'never';
```

`ssr = false` is the right move because the game state is entirely
client-side. Without it, SvelteKit tries to prerender HTML and the
localStorage code paths get awkward guards.

## `+layout.svelte` — global shell

```svelte
<script>
  import '../app.css';
  let { children } = $props();
</script>

{@render children()}
```

That's the whole layout. The Next-side `app/layout.tsx` set `<html lang="vi">`
with the Geist font — that lives in `src/app.html` now (Phase 1).

## `+page.svelte` — player route

Replicate `app/page.jsx` 1:1:

```svelte
<script>
  import PlayerBoard from '$lib/PlayerBoard.svelte';

  let showInstructions = $state(false);
</script>

<div class="flex flex-col flex-1 items-center px-3 py-8 sm:py-12">
  <div class="w-full max-w-lg">
    <header class="text-center mb-8">
      <h1 class="text-4xl sm:text-5xl font-extrabold tracking-tight bg-gradient-to-r from-indigo-500 to-purple-500 bg-clip-text text-transparent">
        Lô tô
      </h1>
      <p class="mt-2 text-sm text-slate-500 dark:text-slate-400">
        Lấy cảm hứng từ những buổi họp lớp thiếu giấy chơi lô tô
        <br class="hidden sm:block" /> của TN1 (2014–2017)
      </p>
      <div class="mt-3 flex items-center justify-center gap-3 text-xs">
        <button
          onclick={() => (showInstructions = !showInstructions)}
          class="text-indigo-500 dark:text-indigo-400 hover:underline"
        >
          {showInstructions ? 'Ẩn hướng dẫn' : 'Hướng dẫn'}
        </button>
        <span class="text-slate-300 dark:text-slate-600">|</span>
        <a href="/master" class="text-orange-500 dark:text-orange-400 hover:underline">
          Trang quản trò →
        </a>
      </div>
    </header>

    {#if showInstructions}
      <div class="mb-6 rounded-xl bg-indigo-50 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900 p-4 text-sm text-slate-600 dark:text-slate-400">
        <ul class="space-y-1 list-disc list-inside">
          <li>Nhấn <strong class="text-slate-800 dark:text-slate-200">Tạo bảng mới</strong> để tạo bảng</li>
          <li>Nhấn vào ô số để đánh dấu khi số được xổ</li>
          <li>Nhấn lại để bỏ đánh dấu</li>
          <li>Bảng được lưu tự động</li>
        </ul>
      </div>
    {/if}

    <PlayerBoard />

    <footer class="mt-10 text-center text-xs text-slate-400 dark:text-slate-600">
      Made with ❤️ by
      <a href="https://miti99.com" target="_blank" rel="noopener noreferrer" class="text-indigo-500 hover:underline">miti99</a>
    </footer>
  </div>
</div>
```

## `master/+page.svelte` — host route

Replicate `app/master/page.jsx`. Key differences from React:

- `useState` → `$state` runes
- `useEffect(load on mount)` → `$effect(...)` (runs once at mount because
  no reactive deps tracked inside)
- `useEffect(save on change)` → `$effect(() => { saveState(state); })`
- `useCallback` → plain functions
- `BOARD = Object.freeze(...)` — module-scope, declared in `<script module>`
  so it computes once across all instances:

  ```svelte
  <script module>
    const STORAGE_KEY = 'loto_master';
    function buildBoard() { /* same body */ }
    const BOARD = Object.freeze(buildBoard().map(Object.freeze));
    const BOARD_FLAT = Object.freeze(BOARD.flatMap((r) => r));
  </script>

  <script>
    import PlayerBoard from '$lib/PlayerBoard.svelte';
    /* state, handlers, effects */
  </script>

  <!-- markup -->
  ```

The "host's own player card" piece becomes:
```svelte
<PlayerBoard storagePrefix="loto_master_card" />
```

— same prop pattern.

## Behavior to preserve

- "Ván mới" button confirms before reset if game in progress
- "Xổ số" button hidden when `remaining.length === 0`
- Last drawn number badge with size animation
- Called numbers history strip (chip-style)
- 9×10 master board lighting up on draw
- Master's own player card below, with separate localStorage prefix

## Files affected

- create: `src/routes/+layout.svelte`, `src/routes/+layout.js`,
  `src/routes/+page.svelte`, `src/routes/master/+page.svelte`

## Verify

- `npm run dev` — both `/` and `/master` render without errors
- Tab switching between routes preserves localStorage independently
  (player at `loto_*`, master state at `loto_master`, master's card at
  `loto_master_card_*`)
- Browser back/forward navigates correctly (SvelteKit client router)
- View source on prerendered build (Phase 5 verifies) shows the static
  HTML shell with `ssr: false` skipping content render

## Out of scope

Codeserver dev profile (Phase 5), CF deploy (Phase 5), docs (Phase 6).

## Status: planned
