# Next-session TODO

Hand-off list as of 2026-04-28 (commit `9f24b6d`). The
`260428-0927-implement-todo-backlog` plan shipped 8 of 9 phases — only
the manual PWA verification checklist remains. Below are residual /
new items deferred from that pass.

## Highest leverage (start here)

- **Run Phase 9 — PWA install verification.** Manual checklist in
  `plans/260428-0927-implement-todo-backlog/phase-09-pwa-verify-install.md`.
  Needs production deploy on Cloudflare Pages + physical Android
  Chrome + iOS Safari. Lighthouse PWA = 100/100, install flow,
  airplane-mode offline, `curl -I` header check (script-src now
  hashed, no longer `'unsafe-inline'`).

## UX polish (carried over from pass-2)

- **`MasterEmptyState` ↔ PlayerBoard ghost-grid duplication.** Two
  near-identical decorative components — extract a shared
  `<GhostBoardPreview rows={N} />` only if a third use appears.
  (Rule-of-three not met yet.)
- **Maskable icon at 70% safe-zone.** Verify in Chrome DevTools "Show
  maskable preview" before announcing PWA. May need to drop to 65% if
  Android shape masks crop too tight. Roll into Phase 9 verification.

## Tech debt

- **`cookie` and `serialize-javascript` overrides are temporary.**
  Remove the `overrides` block in `package.json` once
  `@sveltejs/kit` and `workbox-build` ship releases that pull
  `cookie >= 0.7.0` and `serialize-javascript >= 7.0.5` upstream.
- **CSP hash brittleness.** `inject-csp-hashes.mjs` regenerates the
  SvelteKit-bootstrap hash per build. If the bootstrap changes
  format (e.g. SvelteKit moves to script-src-elem with nonce), the
  marker `script-src 'self' 'unsafe-inline'` won't be present and
  the script will exit 1. Watch for that on SvelteKit major bumps.
- **Voice list growth.** If we add voices > 2 (esp. > 10), revisit
  the precache strategy — currently we precache only the default
  voice. The 7d runtime cache covers the rest, but cold-start cost
  on alternate voices grows linearly.

## New features (parking lot)

- Multiple cards per player.
- Long-press hero to undo last call (master).
- Spacebar to draw next number (master power-user shortcut).
- Internationalization (English UI). Vi default; en strings in a flat
  map. Maybe pick `paraglide-js` or roll our own minimal map.
- "New version available" reload toast for SW autoUpdate. Today the
  swap is silent; if users complain about content jumping mid-game,
  add a non-blocking notice.

## Reports archive

- Pass 1 (260427-1151): `code-reviewer-`, `ui-ux-designer-`, `security-`
- Pass 2 (260427-2047): `code-reviewer-pass2-full`,
  `ui-ux-designer-pass2-full`, `security-pass2-full`
- Phase-specific: `code-reviewer-260427-1036-three-mode`,
  `code-reviewer-260427-2030-polish-pwa`

All under `plans/reports/`. Pass-1 unresolved items were verified
addressed in pass-2; pass-2 items were addressed across the
`260428-0927-implement-todo-backlog` phases.

## Recently shipped (260428-0927-implement-todo-backlog)

- ✅ Phase 1 — auto-tick integration test (8 cases, helper extracted)
- ✅ Phase 2 — CI inline-script guard (`npm run verify:build`)
- ✅ Phase 3 — mode picker "Both" glyph: grid + megaphone composite
- ✅ Phase 4 — settings modal sticky title/footer on small screens
- ✅ Phase 5 — per-section Chờ ring (amber, reduced-motion aware)
- ✅ Phase 6 — confetti tier-2 threshold + 🥢🎋🏮 + size jitter
- ✅ Phase 7 — strict CSP: SHA-256 hash of inline bootstrap, no
  `'unsafe-inline'` in `script-src`
- ✅ Phase 8 — audio cache 30d → 7d + purgeOnQuotaError
