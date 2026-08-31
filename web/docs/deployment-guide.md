# Deployment Guide

## Build Profiles

| Script | basePath | Target |
|---|---|---|
| `npm run build` | `""` (root) | Local preview / generic static host |
| `npm run build:gh` | `/loto` | GitHub Pages → `https://tiennm99.github.io/loto` (canonical) |

Implementation: `svelte.config.js` reads `BUILD_PROFILE` env. Default is empty
basePath; `BUILD_PROFILE=gh npm run build` switches to `/loto`.

Internal links use `import { base } from '$app/paths'` so they survive
either profile without code changes.

## Production Deployment — GitHub Pages

Canonical deploy. Wired via the `deploy-pages` job in
`.github/workflows/ci.yml`: on push to `main`, the `build (gh)` job runs
`npm run build:gh` and uploads `build/`; `deploy-pages` downloads that artifact
and publishes it. Both are gated on the `test` job.

One-time setup (already done; documented for restoration):
1. Repo → Settings → Pages → Source: **GitHub Actions**.
2. Push to `main` triggers the workflow; the deploy job posts the
   live URL on completion.

URL: `https://tiennm99.github.io/loto/`

No external secrets; the workflow uses GitHub's built-in `pages` and
`id-token` permissions (declared in the workflow YAML).

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
npm run build:gh
```

Generates:
- `build/` — Complete static HTML + JS export with `/loto` basePath
- `.svelte-kit/` — Build cache (not needed for deployment)

### Export Settings
- `adapter-static` in `svelte.config.js`
- No server-side rendering (SSR disabled via `ssr: false`)
- All pages pre-rendered to HTML + JS bundles

### Asset Hosting
- `base` path matches deployment target (GH: `/loto`, root for local preview, codeserver: `/absproxy/{port}`)
- CSS, JS, fonts all prefixed correctly
- GitHub Pages serves the project at `/loto`, so `/loto/_app/*` paths resolve correctly

## Environment Variables

### Development (code-server only)
- `VITE_DEV_PROFILE` — set to "codeserver" to enable proxy mode
- `CODESERVER_HOST` — hostname for HMR proxy
- `CODESERVER_PORT` — port (default 3000)

### Build-Time
- `BUILD_PROFILE` — set to `gh` for GitHub Pages build (basePath `/loto`). The
  deploy workflow sets this via `npm run build:gh`. Default empty (root
  basePath) is for local preview / non-GH static hosts.

### Not Used at Runtime
- No database URL, API keys, or secrets (all client-side, localStorage)
- `.env.local` is `.gitignore`d and safe for local config

## Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| 404 on assets after deploy | basePath mismatch | Workflow runs `npm run build:gh` — check the deploy job log emits `/loto/_app/...` URLs |
| HMR not connecting (code-server) | CODESERVER_HOST not set | Add `CODESERVER_HOST=...` to `.env.local` |
| Assets 404 (code-server) | Wrong proxy URL | Use `/absproxy/{port}`, not `/proxy/{port}` |
| Page blank after refresh | State not persisted | Check browser localStorage is enabled |
| Stale CSS (code-server) | HMR failed | Manually refresh page (F5) |

## CI/CD Pipeline

One workflow, `.github/workflows/ci.yml`, covers PRs and pushes to `main`:

- **`test`** — `npm test`. Every other job depends on it, so a red suite
  blocks all deploys.
- **`build`** — a two-entry matrix producing the only two web builds in the
  run: `npm run build` (base `""`, artifact `web-build`) and `npm run build:gh`
  (base `/loto`, artifact `web-build-gh`).
- **`deploy-pages`** — canonical deploy; publishes `web-build-gh`.
- **`deploy-firebase`** / **`preview-firebase`** — Firebase live channel on
  `main`, preview channel on same-repo PRs; both consume `web-build`.
- **`android-debug`** — independent of the web build: lints, tests and
  assembles the native app's unsigned APK straight from the checkout.

Tags run `.github/workflows/android-release.yml` instead, which builds
standalone because no `ci` run exists to take artifacts from.

## Performance Checklist

- [x] Static export via adapter-static (no server overhead)
- [x] Tailwind 4 purged for production size
- [x] localStorage reduces bundle—no API calls
- [x] Images minimal (mostly CSS gradients + emojis)
- [x] Fonts: Roboto Condensed self-hosted via @fontsource

Bundle analysis: Run `npm run build && ls -lh build/` to inspect file sizes.

## Security Considerations

- No sensitive data in code (no API keys, secrets)
- `.env.local` is local-only, not committed
- localStorage scoped to origin
- No external API calls (offline-capable)
- GitHub Pages serves HTTPS by default

Last reviewed: 2026-05-09
