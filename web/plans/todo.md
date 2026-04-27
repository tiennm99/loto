# Next-session TODO

Hand-off list as of 2026-04-27 (commit `ee71bf0`). Two pass-2 reviews
finished; below are items deferred for future cuts. None are blocking.

## Highest leverage (start here)

- **Test the auto-tick effect.** No integration test for
  `PlayerBoard.svelte` bus-driven marking. The dedup-by-`at` fix is the
  highest-risk uncovered code path — caught one P0 already, easy to
  regress. See `plans/archive/260427-1036-three-mode-and-master-auto-tick/`
  for context.
- **CI smoke check for inline scripts in built `index.html`.** SvelteKit
  emits one inline bootstrap script; we relaxed CSP to `'unsafe-inline'`
  to accommodate it. If a future SvelteKit change adds another inline
  block, we want to notice — `grep -c '<script>' build/index.html` in CI.
- **Verify PWA install live.** Lighthouse PWA = 100/100 + manual install
  test on Android Chrome and iOS Safari. Splash + theme color flow.
  `BUILD_PROFILE=gh` deploy under `/loto/` base — confirm SW + manifest
  paths still resolve.

## UX polish (queued from pass-2)

- **Mode picker glyphs read unevenly.** Player rect = clear. Megaphone
  = abstract. "Both" two stacked rectangles = looks like windows, not
  roles. Redesign or add tiny role labels under each glyph.
- **`MasterEmptyState` ↔ PlayerBoard ghost-grid duplication.** Two
  near-identical decorative components — extract a shared
  `<GhostBoardPreview rows={N} />` if a third use appears.
- **Settings modal on iPhone SE.** Title + footer scroll off — make the
  `<h2>` and the bottom button row sticky so the user can always close
  the modal without scrolling all the way back.
- **Tier-2 confetti threshold.** 3+ row bingos on a 9-row card is rare;
  consider triggering tier 2 on the 2nd bingo, or after a bingo + Chờ.
- **Confetti emoji variety.** Add 🥢 🎋 🏮 to mix the all-celebration
  set. Randomize size 1.5–2.5rem.
- **Per-row "Chờ" visual indicator.** Subtle ring/glow on the
  `section-label` band when a Chờ row exists in that section — reduces
  reliance on the toast.

## Tech debt

- **Strict CSP via hashed inline script.** Today we ship
  `script-src 'self' 'unsafe-inline'`. Computing the SHA-256 of
  SvelteKit's bootstrap inline at build time and adding it to CSP
  would close the relaxation. Brittle: hash changes per build. Worth
  tooling if we want a real CSP grade.
- **LRU on audio cache.** Workbox already enforces `maxEntries: 400`,
  but a "drop voices not used in 30 days" rule would be nicer than
  age-only. Only matters at voices > 10.
- **`cookie` override is a temporary patch.** Remove the override
  block in `package.json` once `@sveltejs/kit` ships a release with
  `cookie >= 0.7.0` upstream. Same for `serialize-javascript@^7.0.5`
  once `workbox-build` updates.
- **Maskable icon at 70% safe-zone.** Verify in Chrome DevTools
  "Show maskable preview" before announcing PWA. May need to drop to
  65% if Android shape masks crop too tight.
- **Voice list growth.** If we add voices > 2 (esp. > 10), revisit the
  precache strategy — currently we precache only the default voice.

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
addressed in pass-2.
