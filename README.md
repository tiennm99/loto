# Lô tô

Bàn số của trò chơi "Lô tô" — SvelteKit app.

Single page (`/`). The host enables "Chế độ quản trò" in settings to
reveal the master panel inline below their player card; called numbers
get spoken aloud in Vietnamese using bundled MP3 clips (no runtime TTS).

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

## Regenerating audio

Vietnamese voice clips live in `static/audio/{voiceId}/`. Generated
once with [edge-tts](https://github.com/rany2/edge-tts) (free, no API
key) — runtime never calls TTS. To regenerate (e.g., to add a new voice
that Microsoft ships, or to change wording):

```bash
pip install edge-tts
python3 scripts/generate-audio.py
```

The script auto-discovers every `vi-*` voice and writes a manifest the
app reads on next reload.
