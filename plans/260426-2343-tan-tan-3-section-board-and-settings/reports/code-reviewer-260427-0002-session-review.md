# Code Review — Tân Tân 3-section board + Settings color picker

**Date:** 2026-04-27
**Reviewer:** code-reviewer
**Scope:** uncommitted working tree, two features

---

## TL;DR

Both features are essentially correct and ship-worthy. Row-index math is right. Color validation regex is safe enough for a CSS custom property sink. A handful of small bugs/papercuts — none block ship — and one **must-fix** focus/a11y issue worth 5 minutes before deploy.

Verdict: **DONE_WITH_CONCERNS** — ship today, fix the must-fix list before merging the PR.

---

## Must-fix (block ship until done)

### 1. Settings modal: `<button>` overlay swallows the dialog's keydown handler — Escape doesn't close

`SettingsButton.svelte:81` puts `onkeydown={onKeydown}` on the dialog `<div>` with `tabindex="-1"`, but you never `.focus()` it on open. Combined with the full-screen `<button class="absolute inset-0">` overlay (line 84) that takes focus away, **pressing Escape after opening the modal does nothing** until the user manually clicks a focusable element inside the dialog. The trigger gear button still has focus when the modal opens, so Escape there hits document — and your handler is on the dialog div, not document.

Fix one of:
- Add a `window`/document keydown listener inside an `$effect` while `open` is true, OR
- `bind:this={dialogEl}` on the dialog div and call `dialogEl.focus()` in `$effect(() => { if (open) dialogEl.focus(); })`.

Recommend the first — simpler, also handles "user clicked into the color input then hit Esc" since the input doesn't bubble keydown to the dialog div in some browsers.

```js
$effect(() => {
  if (!open) return;
  const handler = (/** @type {KeyboardEvent} */ e) => { if (e.key === "Escape") close(); };
  window.addEventListener("keydown", handler);
  return () => window.removeEventListener("keydown", handler);
});
```

### 2. Settings modal: clicking the backdrop `<button>` triggers form-submit semantics inside any future `<form>` ancestor

`SettingsButton.svelte:84-89` — bare `<button>` defaults to `type="submit"`. You set `type="button"` (good) but it's a global a11y/HTML linter complaint pattern. Confirm: yes you have `type="button"`. Skip — just noting the area.

Real concern here: the backdrop `<button>` exists purely to make the backdrop clickable. Screen readers will announce it as "Đóng, button" duplicating the close action that already exists on `Xong`. Better:

```svelte
<div role="presentation" onclick={close} class="absolute inset-0"></div>
```

with `<svelte:options ... />` not needed; Svelte will warn about a11y_click_events_have_key_events on a div, but this is a backdrop and `aria-hidden` semantics + the modal having Escape covers it. Or keep the button but add `aria-hidden="true"` and `tabindex="-1"` so it doesn't appear in the tab order or AT tree.

---

## Nice-to-fix (do soon, not blocking)

### 3. Layout `$effect(() => loadSettings())` runs on every reactive re-render

`+layout.svelte:11-13` — `loadSettings()` reads/writes `settings.emptyCellColor` (a `$state`). That mutation is itself a tracked dependency. Today the effect has no dependencies it reads (mutation isn't reading), so it runs once. But this is fragile: if anyone later adds a read like `console.log(settings.emptyCellColor)` in the effect or inside `loadSettings`, you'll get an infinite-loop warning or extra localStorage hits per keystroke when the picker fires.

Tighten intent with `$effect.pre(() => { loadSettings(); }, []);` is **not** valid in Svelte 5. The idiomatic single-shot is:

```js
import { onMount } from "svelte";
onMount(() => { loadSettings(); });
```

`onMount` is the right tool when you want once-on-mount semantics with no reactivity. Effect is overkill here.

### 4. `applyToDom()` in tests removes the `--empty-cell-bg` property in `beforeEach` but `loadSettings` only **sets** it when localStorage is empty OR valid

Race-free in tests, but in the browser there is a brief flash window: between first paint of `+layout.svelte` (CSS var = the `:root` default `#7a4a2b`) and `$effect`/`onMount` running with a saved value (`#000000`, say). User sees brown for 1 frame then black. For this app, fine — call it out, no fix needed. If you cared: emit the saved color via SvelteKit `<svelte:head>` synchronously, but that's massive overkill.

### 5. `/^#[0-9a-fA-F]{6}$/` validation rejects valid CSS colors that round-trip from `<input type="color">`

The native picker always emits 7-char `#rrggbb` lowercase, so practically you're fine. But you reject:
- `#fff` (3-digit shorthand) — your test asserts this; intentional.
- `#fff8` / `#ffffff80` (alpha hex) — fine to reject; CSS var wouldn't differ visibly behind opaque cells anyway.
- Named colors, `rgb()`, `hsl()` — also fine to reject; matches your "hex picker only" UX.

Verdict on regex sufficiency for **security** sink (`document.documentElement.style.setProperty('--empty-cell-bg', x)`): **safe**. The 6-hex regex blocks any character that could close the CSS declaration (`;`, `}`, `/*`, whitespace, parens). Even if it didn't, `style.setProperty` value sanitization in modern engines drops `;` and `{}`. No CSS-injection / XSS risk. Good.

### 6. `master/+page.svelte:217` — `style:background-color={hasNumber ? null : "var(--empty-cell-bg)"}`

Works, but you removed the `bg-slate-100 dark:bg-slate-900/60` Tailwind class from the conditional and didn't add a fallback for the non-`hasNumber` path beyond the inline style. If somehow `--empty-cell-bg` fails to apply (CSS var unsupported, blocked extension, etc.), empty cells render transparent showing the parent's `bg-white`. Defensive option: keep `bg-slate-100 dark:bg-slate-900/60` as a Tailwind fallback for empty cells; the inline `style:background-color` will win when present. Not blocking — `--empty-cell-bg` has a default in `:root`.

### 7. PlayerBoard `aria-label` redundancy

`PlayerBoard.svelte:164` has `aria-label="Bảng lô tô"` on the outer wrapper, **and** each of the 3 inner `.loto-grid` divs has `aria-label="Bảng lô tô — phần N"`. Screen reader will read "Bảng lô tô, group" then "Bảng lô tô — phần 1, group". Drop the inner labels or change them to `aria-labelledby` pointing at the section-label `<div>` (which would itself need an id and `role="heading"` semantics).

Cheapest fix: remove the inner `aria-label` and add `id="section-{sectionIdx}"` + `role="heading" aria-level="3"` to `.section-label`, then `aria-labelledby="section-{sectionIdx}"` on the grid. Or just delete the redundant labels — the visible label text is enough for sighted users and the outer landmark is enough for AT.

---

## Noted, not blocking

### 8. Row-index math is correct — verified

`PlayerBoard.svelte:173-174`:
- `grid.slice(startRow, startRow + 3)` returns 3 rows of 9 = 27 cells when flattened.
- `startRow + Math.floor(idx / 9)` for `idx in [0..26]`: `Math.floor(idx/9) ∈ {0,1,2}` → row ∈ `{startRow, startRow+1, startRow+2}`. Exactly correct for all three sections (start 0, 3, 6).
- `idx % 9` gives col 0..8. Correct.
- `crossed[row]?.[col]` — optional chain handles initial state where `crossed = []`. Fine.

`{#each ... as num, idx (idx)}` keying by `idx` is OK because the underlying number at each position is stable for the lifetime of the grid. If you ever regenerate the board without remounting `PlayerBoard`, Svelte will reuse DOM nodes by index — which is what you want here. No keying bug.

### 9. `$state` at module scope inside `.svelte.js`

Legal in Svelte 5 — module-scope `$state` creates a singleton reactive state. Both `/` and `/master` import the same `settings` object so the picker on either page reflects on the other. Working as intended. Note: SSR with module-scope state can leak between requests in SvelteKit (one server process serves many users with shared module state). For a static-export site (`adapter-static`), this is irrelevant — there is no server. Keep an eye if you ever add a non-static adapter.

### 10. `DEFAULT_SETTINGS = Object.freeze({ ... })` then `$state({ ...DEFAULT_SETTINGS })`

Spread copies only own enumerable props. With one key today, fine. When you add a nested key later (e.g., `{ theme: { primary: '#xxx' } }`), the spread is shallow — `settings.theme` and `DEFAULT_SETTINGS.theme` would alias. `resetSettings` would mutate the frozen default through `settings.theme.primary = ...`. Add a structured-clone or per-key reset before introducing nested keys. YAGNI for now.

### 11. Dark mode parity — yes, mismatch is real but harmless

The rest of the app uses Tailwind v4 `dark:` utilities which (per Tailwind v4 default) compile to `@media (prefers-color-scheme: dark)`. Your new CSS in `app.css` uses the same media query. Parity is **fine**.

`:global(.dark)` selector at `app.css:48` is **dead code** — nothing in this codebase ever sets a `.dark` class on root. Either remove it (cleanest) or leave it as future-proofing. I'd remove it; YAGNI is in force.

### 12. SVG complexity in `SettingsButton.svelte`

Inline 6-line SVG path is harmless but the file is now ~70% boilerplate gear icon. Fine for one-off. If you add more iconography, consider an icon component or `iconify-svelte`. Don't pre-optimize.

### 13. `PRESETS` includes `DEFAULT_SETTINGS.emptyCellColor` as the first swatch

Means `pick(PRESETS[0])` is identical to `resetSettings()`. Two ways to achieve the same thing — fine, since the "Mặc định" button has its own footer placement. Just noting.

### 14. `localStorage.setItem` in `saveSettings` runs synchronously per keystroke during `<input type="color">` `oninput`

Native color picker fires `input` events continuously while dragging. Each event → `JSON.stringify` + `localStorage.setItem` + `documentElement.style.setProperty`. On modern hardware this is sub-ms; on a low-end Android, you might see jank. If you observe it, debounce the localStorage write only — keep `applyToDom` synchronous so the preview is live. Don't pre-optimize until you measure.

### 15. SettingsButton modal lacks initial focus

When opened, focus stays on the gear button. Sighted keyboard users tab into the modal; screen reader users may not realize a dialog opened. Combined with must-fix #1, the cleanest answer is: on open, focus the dialog container (then your Escape handler also works because it's on a focused element). See must-fix #1.

### 16. No focus-trap

When the modal is open, Tab can escape to underlying page elements (header link, "Tạo bảng mới" button, cells). For a small static-site settings dialog, **acceptable**. WAI-ARIA APG recommends a trap; pragmatically, you can ship without one. Don't build one yourself — use a library or accept the gap.

### 17. `style:background-color="var(--empty-cell-bg)"` (PlayerBoard) vs `style:background-color={hasNumber ? null : "var(--empty-cell-bg)"}` (master)

Inconsistent style. PlayerBoard's empty-cell branch always renders the inline style; master's renders it conditionally. Both work because PlayerBoard's inline-style div is in the `{#if !hasNumber}` branch already. Just inconsistent. Pick one pattern.

---

## Security check — passed

- CSS-injection via `--empty-cell-bg`: regex blocks all special chars; `style.setProperty` provides defense-in-depth. **Safe.**
- localStorage tampering: malicious user editing `loto_settings` in DevTools — worst case is they pick a hex color the picker doesn't permit. No privilege escalation, no XSS. **Safe.**
- No PII / secrets in any new file. **Confirmed.**
- No new network calls. **Confirmed.**

---

## Plan TODO completion

Did **not** verify per-checkbox status of `phase-01-three-section-player-board.md` and `phase-02-settings-color-picker.md`. Not in scope of this review per request. Recommend the orchestrator runs `ck plan check` for completed phases.

---

## Recommended actions (priority order)

1. **(must-fix #1)** Move SettingsButton Escape handler to a `window` listener inside `$effect` gated on `open`. ~3 lines.
2. **(must-fix #2)** Mark backdrop `<button>` as `aria-hidden="true" tabindex="-1"` or convert to non-interactive div. ~1 line.
3. **(nice #3)** Swap `$effect(() => loadSettings())` in `+layout.svelte` for `onMount(() => loadSettings())`. ~2 lines.
4. **(nice #7)** Drop redundant inner `aria-label` on each `.loto-grid` in `PlayerBoard.svelte`. ~3 lines.
5. **(noted #11)** Delete `:global(.dark) .section-divider` selector — dead code. ~1 line.

Total: ~10 lines, ~5 minutes. After this, ship.

---

## Metrics

- Files reviewed: 8 (5 modified, 2 new components, 1 new store)
- New LOC: ~230 (SettingsButton 167 + settings-store 63)
- Modified LOC: ~80 (PlayerBoard, master, layout, app.css)
- Critical issues: 0
- Must-fix: 2 (a11y, both in SettingsButton)
- Nice-to-fix: 5
- Noted: 10
- Security issues: 0

---

## Unresolved questions

- Does the project intentionally support OS-driven dark mode only, or is a future user toggle planned? If the latter, the dead `:global(.dark)` selector becomes useful and the dark-mode parity comment changes.
- Should the picker support alpha (RGBA) for see-through paper effect? Currently rejected by regex; would be a 1-char regex change + shape decision.

---

**Status:** DONE_WITH_CONCERNS
**Summary:** Both features correct and shippable. Two small a11y bugs in the settings modal (Escape key, backdrop button semantics) should be patched before merge — total ~5 minutes of fixes.
**Concerns/Blockers:** Modal Escape doesn't close when trigger retains focus; backdrop button is announced redundantly to screen readers.
