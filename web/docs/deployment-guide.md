# Deployment Guide

## Build Profiles

Two named profiles select the basePath for the deploy target:

| Script | basePath | Target URL |
|---|---|---|
| `npm run build:gh` | `/loto` | `https://tiennm99.github.io/loto` |
| `npm run build:cf` | `""` (root) | `https://loto.miti99.com` |
| `npm run build` | `/loto` | legacy default — same as `build:gh` |

Implementation: `next.config.mjs` reads `BUILD_PROFILE` (`gh` or `cf`) and
picks the basePath. `NEXT_BASE_PATH` overrides everything for one-off
custom-domain builds.

## Production Deployment

### GitHub Pages

Auto-deploys from `master` via `.github/workflows/deploy.yml`. Runs
`npm run build:gh`, then uploads `out/` as a Pages artifact.

URL: `https://tiennm99.github.io/loto`. basePath `/loto` because GH Pages
serves the project under that path prefix.

### Cloudflare Pages

Custom domain target: `https://loto.miti99.com` (basePath `""`).

**Option A — Dashboard:**
1. dash.cloudflare.com → Workers & Pages → Create → Pages → Connect to Git
   → pick the repo
2. Build settings:
   - Framework preset: `Next.js (Static HTML Export)`
   - Build command: `npm run build:cf`
   - Build output directory: `out`
   - Production branch: `master`
3. After first deploy, add the custom domain:
   Project → Custom domains → `loto.miti99.com`. Cloudflare gives DNS
   records to add at your registrar (or auto-configures if `miti99.com` is
   on Cloudflare DNS).

**Option B — GitHub Actions (`.github/workflows/deploy-cloudflare-pages.yml`):**

Add two repo secrets:
- `CLOUDFLARE_API_TOKEN` — dash → My Profile → API Tokens → Create with the
  `Pages — Edit` template
- `CLOUDFLARE_ACCOUNT_ID` — visible in the dash sidebar

The workflow runs `npm run build:cf`, then publishes `out/` via
`cloudflare/wrangler-action@v3` to a Pages project named `loto`. Custom
domain setup is the same dashboard step as Option A — done once.

The two providers can run in parallel — different artifacts, different
URLs. Drop whichever you stop wanting by deleting its workflow file.

### Manual Deploy

```bash
npm run build:gh   # for GH Pages
npm run build:cf   # for CF Pages / any root-served host
# out/ directory ready for upload to any static host
```

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
CODESERVER_HOST=your-machine.example.com
CODESERVER_PORT=3000
```

Replace `your-machine.example.com` with your actual hostname/IP (must match the proxy URL you'll access).

**3. Run dev server**:
```bash
npm run dev:codeserver
```

This sets `NEXT_DEV_PROFILE=codeserver`, triggering the code-server config path in `next.config.mjs`.

**4. Access via browser**:
Navigate to:
```
https://your-codeserver-host/absproxy/3000/
```

**Key Points**:
- `/absproxy/{port}` (NOT `/proxy/{port}`) preserves basePath through the proxy.
- HMR socket connects to `CODESERVER_HOST` for live reload.
- If HMR fails, manually refresh the page (CSS/JS changes still apply server-side).

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
- `out/` — Complete static HTML export
- `.next/` — Build cache (not needed for deployment)

### Export Settings
- `output: "export"` in `next.config.mjs`
- No server-side rendering
- All pages pre-rendered to HTML + JS bundles

### Asset Hosting
- `assetPrefix` matches `basePath` (prod: `/loto`, dev/codeserver: empty or `/absproxy/{port}`)
- CSS, JS, fonts all prefixed correctly
- GitHub Pages serves from repository root, so `/loto` paths resolve correctly

## Environment Variables

### Required (code-server only)
- `CODESERVER_HOST` — hostname for HMR proxy
- `CODESERVER_PORT` — port (default 3000)

### Optional
- `NEXT_DEV_PROFILE` — set to "codeserver" to enable proxy mode (usually set by `npm run dev:codeserver`)

### Not Used at Runtime
- No database URL, API keys, or secrets (all client-side, localStorage)
- `.env.local` is `.gitignore`d and safe for local config

## Troubleshooting

| Issue | Cause | Fix |
|-------|-------|-----|
| 404 on subpages after deploy | basePath mismatch | Verify `basePath="/loto"` in prod; local dev should be empty |
| HMR not connecting (code-server) | CODESERVER_HOST not set | Add `CODESERVER_HOST=...` to `.env.local` |
| Assets 404 (code-server) | Wrong proxy URL | Use `/absproxy/{port}`, not `/proxy/{port}` |
| Page blank after refresh | State not persisted | Check browser localStorage is enabled |
| Stale CSS (code-server) | HMR failed | Manually refresh page (F5) |

## CI/CD Pipeline

`.github/workflows/deploy.yml`:
- Triggers on `push` to `master`
- Installs dependencies (`npm install`)
- Builds app (`npm run build`)
- Deploys `out/` folder to GitHub Pages

No manual steps required; push to master and GitHub Pages updates automatically.

## Performance Checklist

- [x] Static export (no server overhead)
- [x] Tailwind purged for production size
- [x] localStorage reduces bundle—no API calls
- [x] Images minimal (mostly CSS gradients)
- [x] Fonts: Geist via Google Fonts CDN

Bundle analysis: Run `npm run build && ls -lh out/` to inspect file sizes.

## Security Considerations

- No sensitive data in code (no API keys, secrets)
- `.env.local` is local-only, not committed
- localStorage scoped to origin
- No external API calls (offline-capable)
- GitHub Pages HTTPS by default

Last reviewed: 2026-04-26
