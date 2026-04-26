---
phase: 5
title: Codeserver dev profile + CF Pages deploy
priority: high
effort: S
status: planned
---

# Phase 5 — Codeserver dev + CF Pages deploy

Wire the two operational profiles already proven on Next:
1. `npm run dev:codeserver` working through `/absproxy/{port}/`
2. `npm run build` producing static export for CF Pages at `loto.miti99.com`

## Codeserver dev profile

Already drafted in Phase 1 `vite.config.js`. Verify by running:

```bash
echo "CODESERVER_HOST=codeserver.sg.miti99.com" > .env.local
echo "CODESERVER_PORT=3000" >> .env.local
npm run dev:codeserver
```

Open `https://codeserver.sg.miti99.com/absproxy/3000/`. Page should load,
HMR socket should upgrade (Vite HMR config maps client → wss:443/...).

**Differences vs Next implementation:**
- Vite handles HMR explicitly (Phase 1 config). Next 16's HMR was
  finicky over the proxy; Vite's named-host config works because we tell
  the client exactly which URL to connect on (matches sokoban's setup).
- SvelteKit's `paths.base` config takes the basePath, not Vite's. Bridge
  with the same `BUILD_PROFILE` env var pattern, but route through
  `svelte.config.js`:

```js
// svelte.config.js
const profile = process.env.BUILD_PROFILE;
const isCodeserver = process.env.VITE_DEV_PROFILE === 'codeserver';

let base = '';
if (isCodeserver) {
  const port = process.env.CODESERVER_PORT ?? '3000';
  base = `/absproxy/${port}`;
} else if (profile === 'gh') {
  base = '/loto';
}
// else: empty (CF Pages root or local dev)

export default {
  preprocess: vitePreprocess(),
  kit: {
    adapter: adapter({ pages: 'build', assets: 'build', strict: true }),
    paths: { base },
  },
};
```

The dev server reads `VITE_DEV_PROFILE` early (set by `npm run dev:codeserver`),
so `paths.base` populates correctly during dev.

**Internal links** in `+page.svelte` etc. should use `base`:

```svelte
<script>
  import { base } from '$app/paths';
</script>

<a href="{base}/master">Trang quản trò →</a>
```

This keeps links working under any of the three modes (root, /loto,
/absproxy/3000).

## CF Pages deploy

Already covered by current setup. Just update the dashboard build command
when the project is rebuilt:

| Setting | Value |
|---|---|
| Framework preset | SvelteKit |
| Build command | `npm run build` |
| Build output directory | `build` (NOT `out` — SvelteKit + adapter-static default) |
| Production branch | `master` |
| Environment variables | none (root basePath is the default) |

Custom domain `loto.miti99.com` step unchanged.

## Manual GH Pages export (the optional path)

```bash
npm run build:gh   # BUILD_PROFILE=gh sets paths.base = '/loto'
# Upload build/ to GitHub Pages
```

## Files affected

- modify: `svelte.config.js` (paths.base logic)
- modify: `src/routes/+page.svelte` and `src/routes/master/+page.svelte`
  (use `$app/paths` `base` for internal links)
- create/keep: `.env.local` (gitignored; user fills in)

## Verify

1. **Local dev** — `npm run dev` → `http://localhost:3000/`, links work.
2. **Codeserver** — `npm run dev:codeserver` → page loads at the proxy URL,
   HMR works (no `wss` errors in console).
3. **CF Pages build** — `npm run build` → `build/` populated, asset URLs
   start with `/_app/...` (root-relative).
4. **GH Pages build** — `npm run build:gh` → asset URLs start with
   `/loto/_app/...`.

## Out of scope

Docs sync (Phase 6).

## Status: planned
