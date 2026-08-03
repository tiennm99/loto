# Lô Tô

Web version of *Lô tô hội chợ* — Vietnamese fairground bingo. Built with SvelteKit
(static export). The caller draws numbers one at a time; a host panel manages the
game and called numbers are announced aloud in Vietnamese using bundled audio clips.
No runtime TTS or server required.

Live: [tiennm99.github.io/loto](https://tiennm99.github.io/loto)

Android port: [`android/`](../android) — a Capacitor wrapper that bundles this
app into a fully-offline APK.

---

## Features

- **Number board** — 1–90 grid; called numbers highlight as they are drawn
- **Host panel** — enable "Chế độ quản trò" (host mode) in settings to reveal the
  master draw panel inline below your player card
- **Vietnamese voice callouts** — pre-generated MP3 clips for every number using
  Microsoft Edge TTS (`edge-tts`); multiple voices selectable in settings; no API
  key or internet connection needed at runtime
- **Auto-cross player card** — optional automatic crossing of numbers on the player's
  bingo card as they are called
- **Auto-countdown** — configurable delay between number draws for unattended calling
- **PWA** — installable as a progressive web app; works offline after first load
- **Static export** — no server; deploys to any static host or GitHub Pages

---

## Requirements

[Node.js](https://nodejs.org) 18+ and [pnpm](https://pnpm.io).

---

## Quick Start

```sh
git clone https://github.com/tiennm99/loto
cd loto
pnpm install
pnpm dev        # dev server at http://localhost:5173
```

---

## Commands

| Command | Description |
|---|---|
| `pnpm dev` | Start development server |
| `pnpm build` | Production build (root base path) |
| `pnpm build:gh` | Production build for GitHub Pages (`/loto` base path) |
| `pnpm preview` | Preview production build locally |
| `pnpm test` | Run unit tests |
| `pnpm lint` | ESLint check |

---

## Configuration

Copy `.env.example` to `.env.local` and set:

| Variable | Description |
|---|---|
| `CODESERVER_HOST` | Hostname for code-server reverse-proxy dev (optional) |
| `CODESERVER_PORT` | Port for code-server proxy (optional) |

When running inside code-server use `pnpm dev:codeserver` and open
`https://<CODESERVER_HOST>/absproxy/<CODESERVER_PORT>/` (use `/absproxy/`, not
`/proxy/` — the latter strips the path prefix and breaks SvelteKit routing).

---

## Architecture

Single-page SvelteKit app, statically exported. No backend.

```
src/
├── routes/          # Single route: /
├── lib/
│   ├── game-logic.js          # Draw state, number pool, called-number tracking
│   ├── master-store.svelte.js # Host panel state (Svelte 5 runes store)
│   ├── settings-store.svelte.js
│   ├── voice.js               # Audio manifest loader + clip player
│   ├── player-auto-cross.js   # Auto-cross logic
│   ├── MasterPanel.svelte     # Host draw UI
│   ├── PlayerBoard.svelte     # Bingo card + number grid
│   └── AutoCountdown.svelte   # Countdown timer between draws
static/
└── audio/{voiceId}/   # Pre-generated MP3 clips (one per number per voice)
scripts/
└── generate-audio.py  # Regenerates audio clips via edge-tts
```

Game state lives entirely in Svelte 5 rune stores; no server round-trips.
Audio manifest is generated once at build prep time — the app reads a JSON manifest
on load and plays the appropriate clip for each drawn number.

---

## Regenerating Audio

Voice clips in `static/audio/` were generated with
[edge-tts](https://github.com/rany2/edge-tts) (free, no API key). To regenerate
(e.g. to add a new Microsoft `vi-*` voice):

```sh
pip install edge-tts
python3 scripts/generate-audio.py
```

The script auto-discovers all available `vi-*` voices and writes an updated manifest.

---

## Deployment

Deployed to GitHub Pages via `.github/workflows/web-deploy-github-pages.yml` on push
to `main`. Build command: `pnpm build:gh` (sets `/loto` base path). Static output in
`build/`.

---

## License

Apache-2.0 — see [`LICENSE`](../LICENSE) at the repository root.
