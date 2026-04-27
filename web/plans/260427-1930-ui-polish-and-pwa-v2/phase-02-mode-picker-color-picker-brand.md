# Phase 2 — Mode picker icons, color picker, brand polish

Cleans up the medium-priority audit items in `SettingsButton.svelte`
plus the header subline. Each is small and visual; the phase is one
focused commit.

**Status**: not started
**Priority**: P1
**Effort**: ~50 min
**Depends on**: nothing (independent of phase 1)

## Files to modify

- `src/lib/SettingsButton.svelte` — mode picker SVG glyphs, color
  picker layout, "Mặc định" button styling
- `src/routes/+page.svelte` — header subline replacement

## Mode picker — inline SVG glyphs

Replace text-only buttons with text + tiny glyph above. No icon
library; ~15 lines of inline SVG total.

```svelte
{#snippet modeIcon(v)}
  {#if v === "player"}
    <!-- Card with cells -->
    <svg viewBox="0 0 24 16" class="w-6 h-4 mx-auto" fill="none"
         stroke="currentColor" stroke-width="1.5">
      <rect x="1" y="1" width="22" height="14" rx="1.5" />
      <path d="M1 6h22M1 11h22M8 1v14M16 1v14" />
    </svg>
  {:else if v === "master"}
    <!-- Megaphone -->
    <svg viewBox="0 0 24 24" class="w-5 h-5 mx-auto" fill="none"
         stroke="currentColor" stroke-width="1.8">
      <path d="M3 11l14-6v14L3 13z M3 11v2 M19 8a4 4 0 0 1 0 8" />
    </svg>
  {:else}
    <!-- Two stacked cards -->
    <svg viewBox="0 0 24 20" class="w-6 h-5 mx-auto" fill="none"
         stroke="currentColor" stroke-width="1.5">
      <rect x="1" y="1" width="18" height="11" rx="1" />
      <rect x="5" y="7" width="18" height="11" rx="1" />
    </svg>
  {/if}
{/snippet}
```

Inside the existing `{#each MODES}` loop:

```svelte
<button ...>
  {@render modeIcon(v)}
  <span class="block mt-0.5">{label}</span>
</button>
```

Add `flex flex-col items-center` to the button class so glyph and
label stack cleanly. Existing `MODE_HINTS[settings.mode]` line stays.

## Color picker layout

Wrap in a single bordered card with sub-headers, native input on the
left, presets on the right. Replace the current loose flex/grid:

```svelte
<fieldset class="mb-5">
  <legend class="text-sm font-semibold text-slate-700 dark:text-slate-200 mb-2">
    Màu ô trống
  </legend>
  <div class="rounded-xl border border-slate-200 dark:border-slate-700 p-3 space-y-3">
    <!-- Custom -->
    <div>
      <p class="text-xs uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">
        Tuỳ chỉnh
      </p>
      <div class="flex items-center gap-3">
        <input type="color" .../>
        <code class="text-sm font-mono ...">{settings.emptyCellColor}</code>
      </div>
    </div>
    <!-- Presets -->
    <div>
      <p class="text-xs uppercase tracking-wider text-slate-500 dark:text-slate-400 mb-1.5">
        Mẫu sẵn
      </p>
      <div class="grid grid-cols-5 gap-2">
        {#each PRESETS as hex (hex)}
          ...
        {/each}
      </div>
    </div>
  </div>
</fieldset>
```

Adds visual structure; no new logic.

## "Mặc định" button — clearer affordance

Currently a near-invisible footer link. Keep it understated but make
it look like a button:

```svelte
<button
  type="button"
  onclick={() => resetSettings()}
  class="px-4 py-2 rounded-full text-sm font-medium
         text-slate-600 dark:text-slate-300
         border border-slate-300 dark:border-slate-600
         hover:bg-slate-100 dark:hover:bg-slate-700 transition-colors"
>
  Đặt lại
</button>
```

Rename "Mặc định" → "Đặt lại" (clearer = "reset" not "default").

## Header subline — fairground mood

Current:

```svelte
<p class="text-xs sm:text-sm uppercase tracking-[0.28em] text-slate-600 dark:text-slate-300 italic mt-1.5 font-medium">
  Hội chợ TN1
</p>
```

New: drop the heavy uppercase tracking; flank with hairline dashes;
add a tiny lantern emoji to anchor mood. Keep `text-sm`, drop italic.

```svelte
<p class="flex items-center justify-center gap-2 text-xs sm:text-sm
          text-slate-600 dark:text-slate-300 mt-1.5 font-medium">
  <span aria-hidden="true" class="block w-6 h-px bg-slate-400/60 dark:bg-slate-500/50"></span>
  <span>🏮 Hội chợ TN1</span>
  <span aria-hidden="true" class="block w-6 h-px bg-slate-400/60 dark:bg-slate-500/50"></span>
</p>
```

## Todo

- [ ] Add inline mode-picker SVG glyphs (snippet pattern)
- [ ] Restructure mode picker buttons for vertical icon+label layout
- [ ] Wrap color picker in a single bordered card with sub-headers
- [ ] Style "Mặc định" → "Đặt lại" with bordered chip look
- [ ] Replace header subline with dash-flanked lantern band
- [ ] `npm test && npm run build` — green
- [ ] Manual smoke: mode picker glyphs read on light + dark; color
      picker visually grouped; reset button clearly clickable.

## Success criteria

- Mode picker communicates role at a glance without reading the label.
- Color picker no longer feels disconnected (single card vs loose).
- Reset button clearly affordant but not destructive-looking.
- Header subline reads as fairground decor, not SaaS subhead.

## Risks

- **Glyph stroke width on dark mode**: `currentColor` inherits from
  the button text, so it follows the indigo-active vs slate-resting
  states automatically. Test contrast on both.
- **Color picker height growth**: bordered card adds ~16px vertical;
  the modal scrolls anyway, no real cost.
- **"Đặt lại" wording**: "Mặc định" is the current Vietnamese
  convention; "Đặt lại" is clearer but slightly less polite. Both
  work — defer to user if they push back.
