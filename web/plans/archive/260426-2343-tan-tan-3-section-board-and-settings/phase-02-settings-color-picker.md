# Phase 2 — Settings Modal + Empty-Cell Color Picker

## Context

- Plan: [plan.md](plan.md)
- Depends on Phase 1 only loosely (works either way; better when Phase 1
  shipped because the section background also picks up the color).
- Touches: new `settings-store.svelte.js` + `SettingsButton.svelte`,
  edits in `PlayerBoard.svelte`, `master/+page.svelte`, `+page.svelte`,
  `app.css`.

## Overview

- **Priority**: P1 (user-requested feature)
- **Status**: TODO
- **Effort**: ~45 min
- **Description**: Gear icon → modal panel with a color picker. Selected
  color paints empty/blank cells across player card *and* master tracking
  grid. Persists in localStorage. Default: brown (matches physical Tân Tân).

## Key Insights

- One **global** color, not per-card. Simpler, matches physical sheet
  (one paper color).
- Use **CSS custom property** (`--empty-cell-bg`) on `:root` so a single
  store update repaints every empty cell with no per-component prop drilling.
- Settings store goes in `src/lib/settings-store.svelte.js` — the
  `.svelte.js` extension lets it use Svelte 5 runes (`$state`) at module
  scope. Components import the reactive `settings` object.
- The color picker can be a **native** `<input type="color">` to avoid
  pulling in a UI library. Add a few **preset swatches** (brown, slate,
  amber, emerald, indigo, neutral white) for one-tap defaults.
- Keep the modal a11y-clean: `role="dialog"`, `aria-modal="true"`,
  `Escape` to close, focus trap-ish (focus the close button on open).

## Requirements

### Functional
- A gear button (⚙) sits in the player page header (`/`) and master page
  header (`/master`).
- Clicking the gear opens a modal panel.
- Modal contains:
  - A native color picker (`<input type="color">`) bound to the current
    color.
  - 6 preset swatches as quick-pick buttons.
  - A "Reset to default" button.
  - A "Close" button.
- Color change is **live** — empty cells repaint as user picks.
- Setting persists in localStorage under key `loto_settings`.
- Default color: a brown matching the physical card (`#7a4a2b` or close).

### Non-functional
- No new third-party deps.
- Settings store must support adding more keys later (extensible shape).
- Works offline (no network).
- Dark mode: the picker UI itself must be readable in dark mode; the
  user-chosen color obviously stays as-is.

## Architecture

### Store shape

```js
// settings-store.svelte.js
export const DEFAULT_SETTINGS = Object.freeze({
  emptyCellColor: '#7a4a2b', // brown — matches physical Minh Tân card
});

const STORAGE_KEY = 'loto_settings';

export const settings = $state({ ...DEFAULT_SETTINGS });

// load + persist helpers
export function loadSettings() { /* read localStorage, merge into settings */ }
export function saveSettings() { /* write current settings */ }
export function resetSettings() { /* reset to DEFAULT_SETTINGS */ }
```

A root effect (set up once in `+layout.svelte` or in each page that
imports the store) calls `loadSettings()` on mount and persists on change.
Simpler: do it inside the store module via a top-level `$effect.root` in
the `.svelte.js` file — Svelte 5 supports this.

### CSS variable wiring

In `app.css`, define a default:
```css
:root { --empty-cell-bg: #7a4a2b; }
```

Bind it from the store at the page level (whichever component first
mounts the store):
```svelte
<div style:--empty-cell-bg={settings.emptyCellColor}>...</div>
```

OR set it directly on `document.documentElement` in `loadSettings()` /
on store change via an effect. The latter avoids prop drilling.

### Empty-cell consumption

Replace existing empty-cell backgrounds:

| File | Current | New |
|---|---|---|
| `PlayerBoard.svelte` (empty cell) | `bg-slate-50 dark:bg-slate-900/60` | `style:background-color="var(--empty-cell-bg)"` |
| `master/+page.svelte` (empty cell, `!hasNumber`) | `bg-slate-100 dark:bg-slate-900/60` | same |

Filled cells keep their existing styles. The chosen color only repaints
**`hasNumber === false`** cells.

### Settings button placement

A small floating gear button in the top-right of each page's header
section. Reuse the same `SettingsButton.svelte` component on both pages.

### Modal

`SettingsButton.svelte` owns its own `open` state. When open, renders the
modal as a sibling overlay. Same dismiss patterns as the existing Kinh
modal in `PlayerBoard.svelte` (backdrop click + Escape).

## Related Code Files

### Create
- `src/lib/settings-store.svelte.js` — rune-based reactive store +
  load/save/reset. ~50 LOC.
- `src/lib/SettingsButton.svelte` — gear button + modal + color picker
  + 6 preset swatches + reset/close. ~80 LOC.

### Modify
- `src/lib/PlayerBoard.svelte` — empty-cell background uses CSS var.
- `src/routes/master/+page.svelte` — empty-cell background uses CSS var;
  mount `<SettingsButton />` in header.
- `src/routes/+page.svelte` — mount `<SettingsButton />` in header.
- `src/app.css` — add `:root { --empty-cell-bg: ...; }` default.

### Don't touch
- `src/lib/game-logic.js` (no game state changes)

## Implementation Steps

1. Create `src/lib/settings-store.svelte.js` with `settings` rune object,
   `DEFAULT_SETTINGS`, `loadSettings`, `saveSettings`, `resetSettings`.
2. In the store module, set up a root effect that:
   - Reads localStorage on first call.
   - Writes localStorage on change.
   - Pushes `settings.emptyCellColor` to
     `document.documentElement.style.setProperty('--empty-cell-bg', …)`.
3. Add `:root { --empty-cell-bg: #7a4a2b; }` to `src/app.css` as fallback.
4. Create `src/lib/SettingsButton.svelte`:
   - `let { } = $props();` (no props)
   - Local `open = $state(false)`
   - Imports `settings` + `resetSettings` from the store
   - Renders gear button; on click toggles `open`
   - Modal: `<input type="color" bind:value={settings.emptyCellColor}>`,
     6 preset swatches as buttons setting `settings.emptyCellColor` directly,
     reset button calling `resetSettings()`, close button.
   - `Escape` keydown closes modal.
5. In `PlayerBoard.svelte`, change empty-cell background from
   `bg-slate-50 dark:bg-slate-900/60` to inline
   `style:background-color="var(--empty-cell-bg)"`.
6. In `master/+page.svelte`, do the same for `!hasNumber` cells; also
   import and mount `<SettingsButton />` in the header (next to the
   "Về trang người chơi" link).
7. In `+page.svelte`, mount `<SettingsButton />` in the player page header.
8. Run `npx svelte-check`.
9. Manual test: open gear, change color, see player card + master grid
   empty cells update live. Reload page → color persists.
10. Reset button → returns to brown default.

## Todo

- [ ] Create `settings-store.svelte.js` with rune store + persistence
- [ ] Create `SettingsButton.svelte` with gear + modal + picker + presets
- [ ] Add CSS var `--empty-cell-bg` default in `app.css`
- [ ] Wire CSS var update from store via effect
- [ ] Update `PlayerBoard.svelte` empty cells to use CSS var
- [ ] Update `master/+page.svelte` empty cells + mount SettingsButton
- [ ] Update `+page.svelte` to mount SettingsButton
- [ ] Test: change color → repaints both boards live
- [ ] Test: reload → persists
- [ ] Test: reset → back to brown
- [ ] Test: Escape closes modal
- [ ] `npx svelte-check` clean
- [ ] Update `docs/codebase-summary.md` (new files, new storage key)
- [ ] Update `docs/project-overview-pdr.md` (note settings feature)

## Success Criteria

- Gear icon visible on `/` and `/master`.
- Modal opens, picker bound to current color.
- Picking a color via picker OR swatch repaints empty cells live on both
  boards.
- Reload preserves the chosen color.
- Reset button returns to brown default.
- No svelte-check errors.

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| `$state` at module scope misuse | Medium | High | Use `.svelte.js` extension; verify with svelte-check; reference Svelte 5 docs if unsure (`docs-seeker` skill / context7). |
| CSS var doesn't apply across light/dark mode cleanly | Low | Cosmetic | Set var on `:root`, no media-query gating. User picked color overrides the brand-default for both modes — that's the user's call. |
| localStorage unavailable (private mode) | Low | Setting just doesn't persist | Wrap reads/writes in try/catch (mirror existing `saveGrid`/`loadGrid` pattern). |
| Race: store loads after first paint, brief flash of fallback color | Low | Cosmetic | Acceptable; default fallback in CSS is the same brown, so no flash for default users. |
| New storage key collides with existing | None | – | `loto_settings` is unused. |

## Security Considerations

- `<input type="color">` returns a 7-char `#rrggbb` string, no injection
  vector via CSS variable. Still: validate `/^#[0-9a-fA-F]{6}$/` before
  applying, fall back to default on mismatch.
- localStorage data is per-origin; no concerns.

## Next Steps

After this phase the plan is complete. Optional follow-ups (NOT in this plan):
- Add more settings (font size, sound on Kinh, etc.) — store is already
  shaped to allow it.
- Persist user-defined preset swatches.

## Storage Keys (added)

| Key | Shape | Purpose |
|---|---|---|
| `loto_settings` | `{ emptyCellColor: "#rrggbb" }` | global UI settings |
