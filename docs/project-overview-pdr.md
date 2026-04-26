# Lô Tô — Project Overview & PDR

## What is Lô Tô?

Lô tô is a traditional Vietnamese bingo game. The app replicates the game digitally for players to generate their own 9×9 number cards and mark cells as a host calls numbers from 1–90. First player to complete an entire row wins and shouts "Kinh!" (the game's victory cheer).

The inspiration comes from TN1 class reunions (2014–2017) where players often ran build of physical bingo cards.

## Core Mechanics

- **Players**: Generate a randomized 9×9 card with 45 numbers (5 per row, weighted distribution across columns 1–90). Click cells to mark them as numbers are called.
- **Host**: Draws numbers randomly from a shuffled 1–90 deck, displays the current number on a large board, and tracks which numbers have been called.
- **Bingo**: When a row is complete, the player's card triggers a celebration popup showing "Kinh!" with confetti emojis. Before bingo, toast notifications prompt "Chờ X" (waiting for X) when only one number remains in a row.

## Tech Stack

- **Framework**: SvelteKit 2 with Svelte 5 (runes mode)
- **Runtime**: Svelte 5 runes ($state, $derived, $effect, $props)
- **Styling**: Tailwind CSS 4 (utility-first, animations)
- **Persistence**: localStorage (no backend)
- **Deploy**: Cloudflare Pages (root domain), GitHub Pages fallback (`/loto`)
- **Dev Profile**: code-server compatible via `/absproxy/{port}` basePath + HMR proxy config

## Architecture Overview

Two public pages:
1. **`/`** — Player page. Generate a card, click cells to mark them, see bingo popup and waiting toasts.
2. **`/master`** — Host page. Control number drawing, view 9×10 master board (tracking called vs uncalled), and host's own player card.

State is entirely client-side. Each page/card instance uses a unique localStorage prefix (e.g., `"loto"` for player, `"loto_master"` for host's state, `"loto_master_card"` for host's player card).

## Deployment

- **Production**: Cloudflare Pages auto-deploys from `main` branch (configured via the CF dashboard, no GitHub Actions). Custom domain `loto.miti99.com`. `npm run build:gh` is a manual fallback for GitHub Pages at the `/loto` basePath.
- **Development**: `npm run dev` (local), `npm run dev:codeserver` (code-server via proxy).
- **Build**: `npm run build` generates static export to `build/` directory.

## Key Acceptance Criteria

- [x] Player can generate a new 9×9 card with valid number distribution.
- [x] Player can click cells to toggle crossed state.
- [x] Bingo popup triggers when row is complete, shows row number and "Kinh!" message.
- [x] Toast notifications show "Chờ X" before bingo (one number remaining).
- [x] Host can draw numbers and see them on the 9×10 master board.
- [x] Host has their own player card (isolated by localStorage prefix).
- [x] Offline persistence via localStorage (grid and crossed state).
- [x] Dark mode support (Tailwind dark classes).
- [x] Mobile-responsive (base + sm breakpoints).
- [x] HMR works on code-server via proxy.

## Visual Language

- **Player gradient**: indigo → purple (primary brand, positive action).
- **Host gradient**: orange → red (higher-stakes, control action).
- **Completed rows**: emerald (success indicator).
- **Waiting toast**: amber (attention, ephemeral).
- **Emojis**: 🎉 ✨ 🎊 🥳 ❤️ (celebration, joy).

## Future Considerations (Not Committed)

- Undo last crossed cell
- Sound effects on bingo
- Theme switcher
- PWA install
- Multiplayer sync (real-time via WebSocket)
- i18n beyond Vietnamese

Last reviewed: 2026-04-26
