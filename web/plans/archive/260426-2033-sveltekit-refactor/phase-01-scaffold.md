---
phase: 1
title: Scaffold SvelteKit project + tooling
priority: high
effort: M
status: planned
---

# Phase 1 — Scaffold SvelteKit + tooling

In-place rewrite. Replace the Next-flavored top-level config and source dirs
with SvelteKit equivalents.

## Steps

1. **Delete Next artefacts** (preserve `docs/`, `plans/`, `.gitignore`,
   `.env.example`, `README.md`):
   - `package.json` will be rewritten — back up scripts mentally:
     `dev`, `dev:codeserver`, `build`, `build:gh`, `start`, `lint`
   - Remove: `next.config.mjs`, `app/`, `components/`, `lib/`,
     `eslint.config.mjs` (will be rewritten), `postcss.config.mjs`
   - Keep `.env.local` (gitignored)

2. **`package.json` — new dependencies:**

   ```json
   {
     "name": "loto",
     "private": true,
     "type": "module",
     "scripts": {
       "dev": "vite dev",
       "dev:codeserver": "VITE_DEV_PROFILE=codeserver vite dev --host 0.0.0.0",
       "build": "vite build",
       "build:gh": "BUILD_PROFILE=gh vite build",
       "preview": "vite preview",
       "lint": "eslint ."
     },
     "devDependencies": {
       "@sveltejs/adapter-static": "^3",
       "@sveltejs/kit": "^2",
       "@sveltejs/vite-plugin-svelte": "^4",
       "@tailwindcss/vite": "^4",
       "eslint": "^9",
       "eslint-plugin-svelte": "^2",
       "svelte": "^5",
       "tailwindcss": "^4",
       "vite": "^5"
     }
   }
   ```

   Note: SvelteKit's `dev` defaults to localhost; codeserver profile binds
   `0.0.0.0`. Read latest versions during install (`npm install`).

3. **`svelte.config.js`** at root:

   ```js
   import adapter from '@sveltejs/adapter-static';
   import { vitePreprocess } from '@sveltejs/vite-plugin-svelte';

   export default {
     preprocess: vitePreprocess(),
     kit: {
       adapter: adapter({
         pages: 'build',
         assets: 'build',
         fallback: undefined,
         precompress: false,
         strict: true,
       }),
       paths: {
         // Filled by Phase 5; default empty (CF Pages root)
         base: process.env.BUILD_PROFILE === 'gh' ? '/loto' : '',
       },
     },
   };
   ```

4. **`vite.config.js`** at root:

   ```js
   import { sveltekit } from '@sveltejs/kit/vite';
   import tailwindcss from '@tailwindcss/vite';

   const isCodeserver = process.env.VITE_DEV_PROFILE === 'codeserver';
   const host = process.env.CODESERVER_HOST;
   const port = Number(process.env.CODESERVER_PORT ?? 3000);

   export default {
     plugins: [tailwindcss(), sveltekit()],
     server: isCodeserver
       ? {
           port,
           host: true,
           allowedHosts: host ? [host] : true,
           hmr: host
             ? {
                 host,
                 protocol: 'wss',
                 clientPort: 443,
                 path: `/absproxy/${port}/`,
               }
             : true,
         }
       : { port: 3000 },
   };
   ```

   Codeserver profile mirrors `sokoban/vite/config.codeserver.mjs` —
   absproxy preserves the prefix, HMR client connects through `wss://...:443`.

5. **`jsconfig.json`** — JSDoc-aware path aliases (SvelteKit auto-generates
   one referencing `./.svelte-kit/tsconfig.json`, but we don't want TS
   inheritance; write a minimal version):

   ```json
   {
     "compilerOptions": {
       "checkJs": false,
       "module": "esnext",
       "moduleResolution": "bundler",
       "target": "es2022",
       "allowJs": true,
       "paths": { "$lib": ["./src/lib"], "$lib/*": ["./src/lib/*"] }
     },
     "include": ["src/**/*", "vite.config.js", "svelte.config.js"]
   }
   ```

   `checkJs: false` because user wants no TS-flavored validation. SvelteKit
   may warn about an auto-generated `.svelte-kit/tsconfig.json` — ignore;
   it lives inside the gitignored build cache.

6. **`eslint.config.mjs`** — minimal flat config:

   ```js
   import js from '@eslint/js';
   import svelte from 'eslint-plugin-svelte';
   import globals from 'globals';

   export default [
     js.configs.recommended,
     ...svelte.configs['flat/recommended'],
     {
       languageOptions: {
         globals: { ...globals.browser, ...globals.node },
       },
     },
     { ignores: ['build/', '.svelte-kit/', 'node_modules/'] },
   ];
   ```

   Add `globals` and `@eslint/js` to devDeps as discovered during install.

7. **Tailwind 4 setup** — add `src/app.css` with:

   ```css
   @import 'tailwindcss';
   ```

   Replace the Next-side `app/globals.css`. Custom keyframes (`fade-in`,
   `pop-in`, `bounce-slow`, `spin-slow`, `toast`) and `.cell-crossed`
   diagonal class will move here in Phase 4 — verbatim copy.

8. **`.gitignore` additions:**
   ```
   .svelte-kit
   build
   ```
   Remove now-stale entries: `.next/`, `out/`, `next-env.d.ts`,
   `tsconfig.tsbuildinfo`, `repomix-output.xml` (keep — still relevant).

9. **`src/app.html`** — minimal SvelteKit HTML shell:

   ```html
   <!doctype html>
   <html lang="vi">
     <head>
       <meta charset="utf-8" />
       <meta name="viewport" content="width=device-width, initial-scale=1" />
       <title>Lô tô</title>
       <meta name="description" content="Bàn số của trò chơi Lô tô" />
       %sveltekit.head%
     </head>
     <body class="min-h-full flex flex-col">
       <div style="display: contents">%sveltekit.body%</div>
     </body>
   </html>
   ```

   Vietnamese `lang="vi"` matches the Next layout. Geist font import will
   move to `app.css` (`@import` from `next/font/google` doesn't exist
   outside Next; either bundle Geist via `@fontsource/geist-sans` or use
   the system stack and accept a font swap).

10. **`npm install`** to materialize the new dep tree. Verify
    `@sveltejs/kit`, `svelte`, `@tailwindcss/vite` resolve.

## Files affected

- delete: `next.config.mjs`, `app/`, `components/`, `lib/`, `postcss.config.mjs`
- create: `svelte.config.js`, `vite.config.js`, `src/app.html`, `src/app.css`,
  `jsconfig.json`, `eslint.config.mjs` (rewritten)
- modify: `package.json`, `.gitignore`

## Verify

- `ls package.json svelte.config.js vite.config.js src/app.html src/app.css`
  all present
- `npm install` completes with 0 errors
- No leftover `app/` or `components/` or `lib/` from Next at repo root
- `grep -r "next\|react" package.json` returns nothing

## Out of scope

Source code (Phases 2-4), deploy wiring (Phase 5), docs (Phase 6).

## Status: planned
