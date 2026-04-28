---
name: Audio cache LRU rule
phase: 8
status: todo
priority: low
effort: 30m
---

# Phase 8 — Audio cache LRU rule

## Context
- `vite.config.js:55-69` — workbox runtime cache `loto-audio`:
  - `maxEntries: 400`
  - `maxAgeSeconds: 60 * 60 * 24 * 30` (30 days, age-based)
- TODO: prefer "drop voices not used in 30 days" (LRU on access) over
  pure age-on-cache.

## Decision
Workbox's `expiration.maxAgeSeconds` is age-on-cache, not LRU. Workbox
ships **no built-in true LRU** plugin — but `purgeOnQuotaError: true`
plus `matchOptions.ignoreSearch` and `cacheName` is what we have.

Pragmatic options:
- **A)** Lower `maxAgeSeconds` to 7 days, accept some re-fetches.
- **B)** Set `purgeOnQuotaError: true`, leave 30d, ride out quota
  pressure when storage hits caps.
- **C)** Custom workbox plugin that re-touches the cache entry on each
  match (workaround for missing LRU).

**Pick A + B** combined. Voices < 200KB each; 30d → 7d won't hurt
offline UX meaningfully (only matters for voices NEVER played in 30d,
which means user has ignored that voice — they can refetch on next
play). Add `purgeOnQuotaError: true` as a belt-and-suspenders.

Skip C — TODO says "only matters at voices > 10" and we have 2.
Workbox plugin scope is unjustified now.

## Files
- Modify: `vite.config.js:55-69`

## Changes

```js
runtimeCaching: [
  {
    urlPattern: /\/audio\/.*\.mp3$/,
    handler: "CacheFirst",
    options: {
      cacheName: "loto-audio",
      expiration: {
        maxEntries: 400,
        // 7 days — clip stays cached as long as it gets played at least
        // once a week. If a voice is unused for 7d, it falls out and
        // re-fetches on next play. Low impact: each clip <200KB.
        maxAgeSeconds: 60 * 60 * 24 * 7,
        purgeOnQuotaError: true,
      },
      cacheableResponse: { statuses: [200] },
    },
  },
],
```

## Steps
1. Edit `vite.config.js`.
2. Update the comment in the block above the `additionalManifestEntries`
   line if it still says "30d" or implies long retention.
3. `npm run build` — confirm SW generates without errors.
4. Hard-refresh in DevTools → Application → Cache Storage → `loto-audio`.
   Trigger a play; entry appears with current timestamp.
5. Bump device clock 8 days, refresh, verify entry is purged on next
   workbox housekeeping (or simulate via `caches.delete('loto-audio')`).

## Success
- `maxAgeSeconds` = 7 days.
- `purgeOnQuotaError: true`.
- Build passes.
- Default voice still precached (unchanged).

## Risks
- Users who only play once a month will see a re-fetch. Acceptable —
  audio is small and same-origin.
- 7d × 184 default-voice precache entries: precache survives via
  `additionalManifestEntries` (separate flow), not the runtime cache,
  so 7d doesn't affect the default voice.
