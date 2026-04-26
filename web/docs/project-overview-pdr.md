# Lô Tô — Project Overview & PDR

## What is Lô Tô?

This app replicates **Lô tô hội chợ** (Vietnamese fairground / carnival lô tô),
specifically the **Tân Tân** style most familiar from southern Vietnam class
reunions and Tết gatherings. Inspiration: TN1 class reunions (2014–2017)
where players ran build of physical bingo cards.

It is **not** the British Bingo 90 / Italian Tombola 3×9 / 15-number ticket.
That format is intentionally out of scope.

## Game Variant & Scope

### In scope (Lô tô hội chợ Tân Tân)
- **Card**: 9 rows × 9 columns. Exactly **5 numbers per row** AND exactly
  **5 numbers per column** (45 numbers per card; 36 blanks).
- **Column ranges**: col 0 = 1–9, col 1 = 10–19, …, col 7 = 70–79,
  col 8 = 80–90.
- **Within a column**: numbers placed top-to-bottom in **ascending** order.
- **Number pool**: 1–90.
- **Win condition**: 1 row complete = **"Kinh!"**. After winning, the player
  may keep playing further rows (game does not end).
- **Waiting state**: when a row needs only 1 number, app announces **"Chờ N"**.
- **Host (quản trò)** draws numbers from a shuffled 1–90 deck, tracks them on
  a master board, and may also play their own card.

### Out of scope
- 3×9 / 15-number Bingo 90 / Tombola tickets (European format).
- Two-line and full-house win tiers.
- Custom number ranges (1–75 American bingo, etc.).
- Stake / pot / payout logic.
- Multiplayer real-time sync.

## Core Mechanics

- **Players**: Generate a randomized 9×9 card. The generator guarantees 5 per
  row and 5 per column (constraint-aware picker, not loose weighted random).
  Numbers within each column ascend top-to-bottom. Click cells to mark them.
- **Host**: Draws numbers randomly from a shuffled 1–90 deck, shows the
  current number, and tracks all called numbers on an **11×9 master board
  aligned by ones-digit** (col 0 = 1–9, col 8 = 80–90; 1 / 11 / 21 / … / 81
  share row 1, etc.). Each called cell shows its 1-based **draw order** as a
  small superscript so the host can quickly verify a "Kinh!" claim by reading
  the order across the winning row.
- **Bingo**: When a row is complete, a celebration popup shows "Kinh!" with
  confetti. Player keeps playing afterward — no game-end gate.
- **Waiting**: "Chờ N" toast when a row is one number away.

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

- **Production**: Cloudflare Pages at `loto.miti99.com` (canonical, CF dashboard, root basePath). GitHub Pages serves only a redirect to the canonical URL via `.github/workflows/deploy-github-pages.yml`.
- **Development**: `npm run dev` (local), `npm run dev:codeserver` (code-server via proxy).
- **Build**: `npm run build` generates static export to `build/` directory.

## Key Acceptance Criteria

- [x] Player card is 9×9 with **exactly 5 per row and 5 per column**.
- [x] Numbers within each column are **ascending top-to-bottom**.
- [x] Player can click cells to toggle crossed state.
- [x] Bingo popup triggers when row is complete, shows row number and "Kinh!" message.
- [x] Player may keep marking after a Kinh — no game-end lock.
- [x] Toast notifications show "Chờ X" before bingo (one number remaining).
- [x] Host can draw numbers and see them on the **11×9 last-digit-aligned master board**.
- [x] Master board shows **draw order** on each called cell for Kinh verification.
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

Last reviewed: 2026-04-26 (scope locked: Lô tô hội chợ Tân Tân)
