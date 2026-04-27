# Phase 1 — Vietnamese-safe font + master empty-state hero

Closes the two highest-impact deferred P0s from the audit:
1. `tan-tan-num` font stack relies on system fonts that don't ship
   Vietnamese diacritics consistently on Android.
2. `mode === "master"` with no game started shows just a button + line
   of text — feels broken.

**Status**: not started
**Priority**: P0
**Effort**: ~40 min
**Depends on**: nothing (independent)

## Files to add

- `static/fonts/roboto-condensed-700-vietnamese.woff2` — subset font
  asset (download once, commit). Or comparable face. ~30-50KB.
- `src/lib/MasterEmptyState.svelte` (new) — small, isolated component.
  Reuses `tan-tan-num` and existing styles.

## Files to modify

- `src/app.css` — add `@font-face` declaration; promote new face to
  the front of the `tan-tan-num` stack; ensure `font-display: swap`.
- `src/lib/MasterPanel.svelte` — render `<MasterEmptyState />` when
  `state === null`, replacing the current line of text.

## Font choice

**Recommended**: Roboto Condensed Bold (700), Vietnamese-extended
subset. Free under Apache-2.0. Already in `tan-tan-num` fallback list,
so promotion is non-breaking. 28KB gz at 700 weight only.

Alternative: Oswald — bolder feel, narrower, more "fairground sign"
mood. ~25KB gz. Pick Roboto unless we want a stronger brand shift.

Generate subset:

```bash
npx glyphhanger \
  --formats=woff2 \
  --subset=src/**/*.svelte,src/**/*.js \
  --LATIN \
  --whitelist=U+0102,U+0103,U+1EA0-1EF9 \
  fonts/RobotoCondensed-Bold.ttf
```

Or use `google-webfonts-helper` with manual subset selection.

## CSS

```css
@font-face {
  font-family: "Roboto Condensed";
  src: url("/fonts/roboto-condensed-700-vietnamese.woff2") format("woff2");
  font-weight: 700;
  font-style: normal;
  font-display: swap;
  unicode-range: U+0020-007F, U+00A0-024F, U+1E00-1EFF, U+0102-0103,
    U+1EA0-1EF9;
}
```

`tan-tan-num` already lists "Roboto Condensed" — promote it to the
front (or leave as-is now that the @font-face declaration registers
the family for the renderer). `font-display: swap` ensures Inter or
similar renders first while the woff2 downloads.

## MasterEmptyState component

```svelte
<script>
  // Decorative 11×9 ghost board mock — same proportions as the real
  // master tracking grid so the page silhouette stays consistent.
</script>

<div class="text-center py-10">
  <div
    aria-hidden="true"
    class="mx-auto max-w-xs opacity-30 pointer-events-none mb-6
           rounded-md overflow-hidden border border-slate-300 dark:border-slate-600"
  >
    <div class="grid grid-cols-9 gap-px bg-slate-300 dark:bg-slate-700">
      {#each Array(99) as _, i (i)}
        {@const filled = (i % 11) < 9 && i % 7 === 0}
        <div
          class="aspect-square text-[0.5rem] flex items-center justify-center
                 text-black {filled ? 'bg-white dark:bg-slate-800 dark:text-slate-100' : ''}"
          style:background-color={filled ? undefined : "var(--empty-cell-bg)"}
        >
          {filled ? ((i * 13) % 90) + 1 : ""}
        </div>
      {/each}
    </div>
  </div>
  <span
    class="inline-block px-3 py-1 rounded-full text-xs font-semibold
           tracking-wider uppercase
           bg-orange-100 dark:bg-orange-950/40
           text-orange-700 dark:text-orange-300"
  >
    Chế độ Quản trò
  </span>
  <p class="mt-3 text-base text-slate-600 dark:text-slate-300 italic">
    Nhấn
    <span class="font-semibold text-orange-500 dark:text-orange-400 not-italic">
      "Ván mới"
    </span>
    để bắt đầu xổ số
  </p>
  <p class="mt-1.5 text-sm text-slate-500 dark:text-slate-400">
    🎤 Đã sẵn sàng
  </p>
</div>
```

Replace the current `{:else} <div>Nhấn "Ván mới" ...</div>` block in
`MasterPanel.svelte`.

## Todo

- [ ] Subset Roboto Condensed Bold to Vietnamese, save as
      `static/fonts/roboto-condensed-700-vietnamese.woff2`
- [ ] Add `@font-face` in `app.css` (with `unicode-range`, `swap`)
- [ ] Verify `tan-tan-num` resolves the new face on Android Chrome
      (DevTools → Computed → Rendered fonts)
- [ ] Create `src/lib/MasterEmptyState.svelte`
- [ ] Wire into `MasterPanel.svelte` `{:else}` branch
- [ ] Update CSP `font-src` in `static/_headers` to allow `'self'` (already does, just confirm)
- [ ] `npm test && npm run build` — green
- [ ] Manual: master mode alone, no game → empty-state hero shows;
      diacritics on "Chờ", "Đã" render correctly

## Success criteria

- Numbers + Vietnamese-text-bearing labels render with the new face
  on Linux/Android (no fallback to default sans).
- master-only mode no longer feels broken — hero ghost grid + role
  pill make the page feel intentional.
- Lighthouse perf score doesn't drop more than 2 points (font is
  preloaded? Probably not needed — it's only used by the masthead.)

## Risks

- **Font license check**: confirm Apache-2.0 redistribution OK in the
  static export. (Yes — Roboto family is Apache-2.0.)
- **FOUT flash on slow connection**: `font-display: swap` is the right
  tradeoff. Don't switch to `block` — it's worse UX.
- **Subset miss**: if a Vietnamese diacritic combination isn't in the
  subset, the renderer falls back per-codepoint to the next family
  in the stack — visible but not broken. Mitigation: full
  Vietnamese subset > attempting smart trimming.
