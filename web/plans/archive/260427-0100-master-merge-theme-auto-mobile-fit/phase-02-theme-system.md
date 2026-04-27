# Phase 2 — Theme system: explicit light/dark override

## Context
- [plan.md](plan.md) — depends on Phase 1.
- Currently dark mode follows `prefers-color-scheme` automatically via Tailwind v4's default.
- Goal: 3-state theme (`auto` / `light` / `dark`); user-pick lives in Settings.

## Overview
- Priority: P1
- Status: TODO
- Effort: ~30 min

## Architecture

**Tailwind v4 dark variant.** Tailwind v4 ships with `dark:` mapped to `@media (prefers-color-scheme: dark)` by default. To honor an explicit class, declare a custom variant in `app.css`:

```css
@import "tailwindcss";
@variant dark (&:where(.dark, .dark *));
```

After this, `dark:bg-slate-900` matches when ancestor `<html>` has `class="dark"`. We *replace* the OS-media behavior with class-based; OS preference is then mirrored into the class by JS, so the visual result is the same when `theme === "auto"`.

**JS apply.** `settings-store` decides:
- `"light"` → remove `dark` class from `<html>`.
- `"dark"` → add `dark` class to `<html>`.
- `"auto"` → apply class based on `window.matchMedia('(prefers-color-scheme: dark)').matches`, AND subscribe to its `change` event to re-apply.

When the user picks a non-auto theme, unsubscribe the media listener.

**CSS conversions.** `app.css` currently has 2 `@media (prefers-color-scheme: dark)` blocks (`.section-divider`, `.section-divider-vertical`, `.section-label`). Convert to `:global(.dark) .selector` instead, since the global theme system now drives the class. Without conversion these styles only kick in by OS pref, ignoring the user's explicit pick.

## Settings UI

In `SettingsButton.svelte`, add a fieldset above the color picker:

```svelte
<fieldset class="mb-5">
  <legend class="text-sm font-semibold ...">Giao diện</legend>
  <div class="grid grid-cols-3 gap-2">
    {#each [["auto","Tự động"],["light","Sáng"],["dark","Tối"]] as [v, label]}
      <button onclick={() => pickTheme(v)}
              class="px-3 py-2 rounded-lg border-2 ...
                     {settings.theme === v ? 'border-indigo-500 ...' : 'border-slate-200 ...'}">
        {label}
      </button>
    {/each}
  </div>
</fieldset>
```

`pickTheme(v)` updates `settings.theme = v` and calls `saveSettings()`. The store's apply effect handles the DOM class.

## Files

| File | Change |
|---|---|
| `src/app.css` | Add `@variant dark (&:where(.dark, .dark *));` after `@import`. Convert 2 dark `@media` blocks to `:global(.dark) .selector { ... }`. |
| `src/lib/settings-store.svelte.js` | Add `applyTheme()` helper called from `loadSettings()` and from the effect that runs when `settings.theme` changes. Manages the `prefers-color-scheme` media listener for auto mode. |
| `src/lib/SettingsButton.svelte` | Add theme tri-state selector. |
| `src/lib/settings-store.test.js` | Add tests for theme apply (manipulate `window.matchMedia` mock, verify `<html>` class). |
| `src/routes/+layout.svelte` | No change — `loadSettings()` on mount already triggers `applyTheme()`. |

## Implementation Steps

1. `app.css`:
   - Add `@variant dark (&:where(.dark, .dark *));` directly under `@import "tailwindcss";`.
   - Replace `@media (prefers-color-scheme: dark) { .section-divider, .section-divider-vertical { ... } }` with `:global(.dark) .section-divider, :global(.dark) .section-divider-vertical { ... }`.
   - Same conversion for `.section-label`.
2. `settings-store.svelte.js`:
   - Add `applyTheme()`:
     ```js
     /** @type {MediaQueryList | null} */
     let mql = null;
     /** @type {((e: MediaQueryListEvent) => void) | null} */
     let mqlListener = null;
     function applyTheme() {
       if (typeof document === "undefined") return;
       // tear down any previous auto listener
       if (mql && mqlListener) {
         mql.removeEventListener("change", mqlListener);
         mql = null; mqlListener = null;
       }
       const root = document.documentElement;
       const set = (dark) => root.classList.toggle("dark", dark);
       if (settings.theme === "dark") set(true);
       else if (settings.theme === "light") set(false);
       else { // auto
         mql = window.matchMedia("(prefers-color-scheme: dark)");
         set(mql.matches);
         mqlListener = (e) => set(e.matches);
         mql.addEventListener("change", mqlListener);
       }
     }
     ```
   - Call `applyTheme()` from `loadSettings()` (after settings are populated) and from `saveSettings()`.
3. `SettingsButton.svelte`:
   - Add the theme fieldset above the color one.
   - Wire to `settings.theme` + `saveSettings()`.
4. `settings-store.test.js`:
   - Tests for `applyTheme()`. Mock `window.matchMedia`. Assert class on `<html>`. Test `auto` listener attach/detach when switching modes.

## Edge cases

- SSR: app uses adapter-static + `ssr: false` (per docs). `applyTheme` early-returns if `document` is undefined — safe.
- Multiple components calling `applyTheme()` while listener already attached: helper tears down first → idempotent.
- User flips between `auto` and `dark` and `auto` again — listener correctly re-attaches.

## Tests

```
describe("applyTheme")
  beforeEach: clear classList; mock matchMedia (prefers-color-scheme: dark) → false
  it("theme=light removes dark class")
  it("theme=dark adds dark class")
  it("theme=auto applies based on matchMedia matches")
  it("theme=auto re-applies on matchMedia change event")
  it("switching auto → dark detaches matchMedia listener")
```

## Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| `@variant dark (.dark &)` doesn't compile in Tailwind v4 setup | Low | High | Verify in Phase 2 dev test. Fallback: add to a `@layer` |
| Existing dark CSS in `app.css` no longer triggers from OS | High by design | Low | After conversion, OS pref drives the class via JS; visual result identical |
| User in private browsing → no localStorage → theme reset every reload | Low | Annoying | Acceptable; same as today for color setting |

## Success criteria

- Toggling theme in Settings repaints the app live, no reload.
- Set theme=light, reload — stays light despite OS dark mode.
- Set theme=auto, change OS dark mode — app flips live.
- All existing 38+ tests pass; new theme tests pass.
- `npm run build` clean (no Tailwind variant errors).
