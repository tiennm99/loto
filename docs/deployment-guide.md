# Deployment Guide

## Build Profiles

| Script | basePath | Target |
|---|---|---|
| `npm run build` | `""` (root) | Cloudflare Pages → `https://loto.miti99.com` |
| `npm run build:gh` | `/loto` | GitHub Pages → `https://tiennm99.github.io/loto` (manual fallback) |

Implementation: `svelte.config.js` reads `BUILD_PROFILE` env. Default is empty
basePath; `BUILD_PROFILE=gh npm run build` switches to `/loto`.

Internal links use `import { base } from '$app/paths'` so they survive
either profile without code changes.

## Production Deployment — Cloudflare Pages

Primary deploy. Set up via the Cloudflare dashboard once; subsequent
pushes to `main` trigger automatic builds + deploys.

1. dash.cloudflare.com → Workers & Pages → Create → Pages → Connect to Git
   → pick the repo
2. Build settings:
   - Framework preset: SvelteKit
   - Build command: `npm run build`
   - Build output directory: `build`
   - Production branch: `main`
3. After first deploy, add the custom domain:
   Project → Custom domains → `loto.miti99.com`. Cloudflare gives DNS
   records to add at your registrar (or auto-configures if `miti99.com` is
   on Cloudflare DNS).

No GitHub Actions involved; no repo secrets needed.

## GitHub Pages (redirect-only)

`.github/workflows/deploy-github-pages.yml` no longer builds the app.
It generates two tiny HTML pages (`/loto/index.html` and
`/loto/master/index.html`) that immediately redirect to the canonical
URL on Cloudflare Pages.

The redirect uses both `<meta http-equiv="refresh">` (no-JS fallback)
and a tiny inline script that preserves path / query / hash:

```js
location.replace("https://loto.miti99.com" + path + search + hash);
```

So `tiennm99.github.io/loto/` → `loto.miti99.com/` and
`tiennm99.github.io/loto/master` → `loto.miti99.com/master`.

The redirect runs on every push to `main`. If you want full GH Pages
serving back (instead of the redirect), restore the prior version of
`.github/workflows/deploy-github-pages.yml` from git history — it ran
`npm run build:gh` and uploaded the `build/` artifact.

### Manual GH Pages Build (still available)

If you ever want to build a real GH Pages export by hand:

```bash
npm run build:gh
# upload build/ to the GH Pages target manually
```

`build:gh` script kept as an escape hatch — not used by the automated
redirect workflow.

## Development Environment

### Local Dev
```bash
npm install
npm run dev
```

Access at `http://localhost:3000` (no basePath).

HMR works automatically.

### Code-Server Dev

For browser-based development (VS Code in browser):

**1. Start code-server** with Node.js environment:
```bash
code-server --no-auth
```

**2. Create `.env.local`** in project root:
```
VITE_DEV_PROFILE=codeserver
CODESERVER_HOST=your-machine.example.com
CODESERVER_PORT=3000
```

Replace `your-machine.example.com` with your actual hostname/IP (must match the proxy URL you'll access).

**3. Run dev server**:
```bash
npm run dev:codeserver
```

This reads `vite.config.js` codeserver config (basePath `/absproxy/{PORT}`, HMR proxy).

**4. Access via browser**:
Navigate to:
```
https://your-codeserver-host/absproxy/3000/
```

**Key Points**:
- `/absproxy/{port}` (NOT `/proxy/{port}`) preserves basePath through the proxy.
- HMR socket connects to `CODESERVER_HOST` for live reload.
- If HMR fails, manually refresh the page (Vite HMR still compiles server-side).

### Manual Refresh Workaround
If HMR over proxy is unreliable:
1. Make code changes
2. Manually refresh browser (F5)
3. Dev server has already compiled the changes

This is normal in proxy environments.

## Build & Output

### Build Command
```bash
npm run build
```

Generates:
- `build/` — Complete static HTML + JS export
- `.svelte-kit/` — Build cache (not needed for deployment)

### Export Settings
- `adapter-static` in `svelte.config.js`
- No server-side rendering (SSR disabled via `ssr: false`)
- All pages pre-rendered to HTML + JS bundles

### Asset Hosting
- `base` path matches deployment target (prod: `""`, GH: `/loto`, codeserver: `/absproxy/{port}`)
- CSS, JS, fonts all prefixed correctly
- GitHub Pages serves from repository root, so `/loto` paths resolve correctly

## Environment Variables

### Development (code-server only)
- `VITE_DEV_PROFILE` — set to "codeserver" to enable proxy mode
- `CODESERVER_HOST` — hostname for HMR proxy
- `CODESERVER_PORT` — port (default 3000)

### Build-Time
- `BUILD_PROFILE` — set to "gh" for GitHub Pages build (basePath `/loto`). Default empty (Cloudflare).

### Not Used at Runtime
- No database URL, API keys, or secrets (all client-side, localStorage)
- `.env.local` is `.gitignore`d and safe for local config

## Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| 404 on subpages after deploy | basePath mismatch | Verify `BUILD_PROFILE=gh` for GitHub Pages; default for Cloudflare |
| HMR not connecting (code-server) | CODESERVER_HOST not set | Add `CODESERVER_HOST=...` to `.env.local` |
| Assets 404 (code-server) | Wrong proxy URL | Use `/absproxy/{port}`, not `/proxy/{port}` |
| Page blank after refresh | State not persisted | Check browser localStorage is enabled |
| Stale CSS (code-server) | HMR failed | Manually refresh page (F5) |

## CI/CD Pipeline

Two pipelines run on push to `main`:
- **Cloudflare Pages** (canonical) — wired via the CF dashboard, builds with
  `npm run build`, publishes to `loto.miti99.com`.
- **GitHub Pages** (redirect-only) — wired via
  `.github/workflows/deploy-github-pages.yml`, deploys static redirect HTML
  that forwards `tiennm99.github.io/loto/*` to `loto.miti99.com/*`.

## Performance Checklist

- [x] Static export via adapter-static (no server overhead)
- [x] Tailwind 4 purged for production size
- [x] localStorage reduces bundle—no API calls
- [x] Images minimal (mostly CSS gradients + emojis)
- [x] Fonts: Geist via Google Fonts CDN

Bundle analysis: Run `npm run build && ls -lh build/` to inspect file sizes.

## Security Considerations

- No sensitive data in code (no API keys, secrets)
- `.env.local` is local-only, not committed
- localStorage scoped to origin
- No external API calls (offline-capable)
- GitHub Pages HTTPS by default

Last reviewed: 2026-04-26
