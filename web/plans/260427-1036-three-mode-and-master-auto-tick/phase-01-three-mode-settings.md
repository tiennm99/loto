# Phase 1 — Three-mode settings (player / master / both)

## Overview

Replace `masterMode: boolean` with `mode: "player" | "master" | "both"`.
Migrate existing saved state. Update settings UI from a single switch
to a 3-button segmented picker. Conditionally render PlayerBoard
and/or MasterPanel on `/` based on the new mode.

**Status**: not started
**Priority**: P0 (foundational for phases 2 & 3)
**Effort**: ~25 min

## Files to modify

- `src/lib/settings-store.svelte.js` — replace key, add validator,
  migration in `loadSettings`, update `resetSettings`
- `src/lib/settings-store.test.js` — update assertions
- `src/lib/SettingsButton.svelte` — replace switch with 3-button picker
- `src/routes/+page.svelte` — `mode === "master"` hides PlayerBoard;
  `mode === "player"` hides MasterPanel; `mode === "both"` renders both

## Key design

```js
// settings-store.svelte.js
export const DEFAULT_SETTINGS = Object.freeze({
  // ...existing keys...
  mode: /** @type {"player"|"master"|"both"} */ ("player"),
  // ...
});

const VALID_MODES = ["player", "master", "both"];

function validMode(v) {
  return typeof v === "string" && VALID_MODES.includes(v) ? v : null;
}

// In loadSettings:
const parsedMode = validMode(parsed.mode);
if (parsedMode) {
  settings.mode = parsedMode;
} else if (parsed.masterMode === true) {
  settings.mode = "both"; // legacy migration
} else {
  settings.mode = DEFAULT_SETTINGS.mode;
}
```

Drop `masterMode` from the saved JSON shape in `saveSettings` (which
spreads `{ ...settings }`, so just don't keep `masterMode` on
`settings`). Keep validator/load fallback for one release in case any
future user reverts — actually no, YAGNI. Drop cleanly.

## UI — segmented picker

In `SettingsButton.svelte`, replace the single `switchRow` for
`masterMode` with a 3-button group, mirroring the theme picker pattern
already in the file:

```svelte
<fieldset class="mb-5">
  <legend class="text-sm font-semibold text-slate-700 dark:text-slate-200 mb-2">
    Chế độ hiển thị
  </legend>
  <p class="text-sm text-slate-500 dark:text-slate-400 mb-2">
    Chọn vai trò để chỉ hiện phần liên quan
  </p>
  <div class="grid grid-cols-3 gap-2">
    {#each MODES as [v, label] (v)}
      {@const selected = settings.mode === v}
      <button
        type="button"
        aria-pressed={selected}
        onclick={() => pickMode(v)}
        class="px-2 py-2 rounded-lg border-2 text-sm font-medium transition-all
               {selected
                 ? 'border-indigo-500 dark:border-indigo-400 bg-indigo-50 dark:bg-indigo-950/40 text-indigo-700 dark:text-indigo-300'
                 : 'border-slate-200 dark:border-slate-600 text-slate-600 dark:text-slate-300 hover:border-slate-300 dark:hover:border-slate-500'}"
      >
        {label}
      </button>
    {/each}
  </div>
</fieldset>
```

```js
const MODES = /** @type {const} */ ([
  ["player", "Người chơi"],
  ["master", "Quản trò"],
  ["both", "Cả hai"],
]);

function pickMode(m) {
  settings.mode = m;
  saveSettings();
}
```

The "Tự động xổ" sub-fieldset, currently gated by `settings.masterMode`,
becomes gated by `settings.mode !== "player"` (i.e. master is visible).

## +page.svelte — conditional render

Replace the existing `{#if settings.masterMode}` MasterPanel block and
the unconditional `<PlayerBoard />` with:

```svelte
{#if settings.mode !== "master"}
  <PlayerBoard />
{/if}

{#if settings.mode !== "player"}
  <section class="mt-10" aria-label="Bảng quản trò"
           transition:slide={{ duration: 250 }}>
    <h2 class="...">Quản trò</h2>
    <MasterPanel />
  </section>
{/if}
```

Keep the section heading only when there's a player board above it
(otherwise it's redundant). Conditional:

```svelte
{#if settings.mode === "both"}
  <h2 class="...">Quản trò</h2>
{/if}
```

In `mode === "master"`, the page still has the brand header — the
master panel becomes the main content area.

## Tests

Update `settings-store.test.js`:
- `mode` defaults to `"player"`.
- Persistence round-trip includes `mode`.
- Legacy `masterMode: true` in stored JSON migrates to `mode: "both"`.
- Legacy `masterMode: false` (or missing) → `mode: "player"`.
- Invalid `mode` value falls back to default.
- Drop the old `masterMode` assertions in the "persists ALL keys" test.

## Todo

- [ ] Add `mode` to `DEFAULT_SETTINGS`, drop `masterMode`
- [ ] Add `validMode` helper
- [ ] Implement migration in `loadSettings`
- [ ] Update `resetSettings`
- [ ] Update tests
- [ ] Replace `masterMode` switch with 3-button picker in
      `SettingsButton.svelte`
- [ ] Re-gate "Tự động xổ" fieldset on `mode !== "player"`
- [ ] Update `+page.svelte` conditional rendering
- [ ] `npm test && npm run build` — green

## Success criteria

- `npm test` passes.
- `npm run build` clean.
- Manual: refresh with `localStorage.loto_settings = '{"masterMode":true}'`
  → after load, settings panel shows "Cả hai" selected, both panels
  visible.
- Manual: pick "Quản trò" → PlayerBoard disappears, MasterPanel shows
  alone.
- Manual: pick "Người chơi" → MasterPanel disappears.

## Risks

- **Touching shared settings store breaks other consumers**: low —
  voice flags and color settings are independent.
- **Slide transition jank** when MasterPanel appears alone: the
  existing `transition:slide` is on the `<section>`. Will visually
  test in `master` mode where it's the only thing rendering.
