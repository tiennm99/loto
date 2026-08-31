---
title: "Dual-platform review-fix cycle: web PWA + Android state races"
date: 2026-08-31
summary: "Review agents found dead PWA + state races on both platforms; three fix waves, pre-commit review caught regressions in our own fixes, all verified green"
---

# Dual-platform review-fix cycle: web PWA + Android state races

## What happened

Full review-fix cycle over `web/` (SvelteKit) and `android/` (Kotlin/Compose), driven by parallel review and fix subagents.

Review pass found: web PWA entirely dead (SW never registered — adapter-static prerenders after the PWA plugin's closeBundle; audio precache also missing the `/loto` base, 92 would-be 404s), frozen-tab reclaim rolling back the live round via a stale save effect, and on Android a restore/DataStore race that silently discarded a fresh card plus confetti rendered invisibly behind the kinh Dialog window.

Fix wave 1 closed all criticals/highs + clear-cut mediums (SW registration + `verify-pwa-build.mjs` CI guard, shared `base-path.js` resolver, hydration-gated saves, loading-gated actions with a delay-injecting `SlowDataStore` fake, confetti embedded in the dialog window). Wave 2: `registerType: "prompt"` reload banner (docs already promised a toast; config contradicted them), `checkJs: true` with 56 type-only fixes, Android L2–L6/L7/L10. Wave 3 (delegated decisions): newest-claim-wins multi-tab contract, `dataExtractionRules` excluding only `datastore/loto_game_state.preferences_pb`, settings-loaded signal closing the M4 reverse-ordering replay race.

## Key lesson

Independent pre-commit review of the accumulated diff caught real regressions **introduced by our own fixes**: `voicePlayer.release()` on `isFinishing` left the `by lazy` app-scoped ExoPlayer permanently dead after relaunch into a cached process (fixed with a recreatable `VoicePlayerHolder`), and the one-way claim protocol could leave two tabs active (fixed with winner-echo + missing-`ts` = `Infinity`). Also folded settings-loaded into the exposed `loading` state after the reviewer showed `generate()`/`clearMarks()` dodged the gate.

## Final state

Web 144/144 vitest, eslint + svelte-check (checkJs on) clean, both build profiles + PWA verification pass. Android 129/129 unit tests, lint clean. Uncommitted on `main`. Deferred: master-panel recomposition perf (needs profiled baseline), reviewer lows (loadMaster deck-validation parity, player-gesture audio unlock, loading timeouts, verify-pwa chunk heuristic).

## Next steps

- Commit (user decision pending).
- Profile master panel on the API 24 QA device before touching recomposition.
- Consider Robolectric for the VoicePlayerHolder/Activity glue, currently untestable on JVM.

> Historical work record — not durable authority. Prefer docs/specs/ADRs for current decisions.
