# Lô tô

Bàn số của trò chơi "Lô tô" — SvelteKit app.

Two routes: `/` for players, `/master` for the host (quản trò) — calls
numbers, shows a tracking board, and has its own player card to play along.

See `docs/` for architecture, code standards, and deployment.

## Development

```bash
npm install
npm run dev
```

### Inside code-server (reverse proxy)

```bash
cp .env.example .env.local
# edit .env.local: set CODESERVER_HOST and CODESERVER_PORT
npm run dev:codeserver
```

Open `https://<CODESERVER_HOST>/absproxy/<CODESERVER_PORT>/`.

Use `/absproxy/{port}/`, **not** `/proxy/{port}/` — the latter strips the
path prefix and breaks the SvelteKit base path.

## Build

```bash
npm run build         # default — root basePath, for Cloudflare Pages
npm run build:gh      # /loto basePath, for tiennm99.github.io/loto manual export
```

Static export to `build/`. Deployed to Cloudflare Pages from `main`
(set up via the CF dashboard — see `docs/deployment-guide.md`).
