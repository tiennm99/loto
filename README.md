# Lô tô

Bàn số của trò chơi "Lô tô" — Next.js app.

Two pages: `/` for players, `/master` for the host (quản trò) — calls numbers, shows a tracking board, and has its own player card to play along.

See `docs/` for project overview, architecture, code standards, and deployment.

## Development

```bash
npm run dev
```

### Inside code-server (reverse proxy)

```bash
cp .env.example .env.local
# edit .env.local: set CODESERVER_HOST and CODESERVER_PORT
npm run dev:codeserver
```

Open `https://<CODESERVER_HOST>/absproxy/<CODESERVER_PORT>/`.

Use `/absproxy/{port}/`, **not** `/proxy/{port}/` — the latter strips the path prefix and breaks Next's `basePath`. HMR may not survive the proxy; refresh manually if it disconnects.

## Build

```bash
npm run build
```

Static export to `out/`. Deployed to Cloudflare Pages from `master` (set up via the CF dashboard — see `docs/deployment-guide.md`).
