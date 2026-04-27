# Phase 1 — Settings expansion (foundation)

## Context
- [plan.md](plan.md)
- All later phases consume settings from this store. Land first.

## Overview
- Priority: P0 (blocks 2, 5, 6)
- Status: TODO
- Effort: ~30 min
- Description: Extend `settings-store.svelte.js` shape, validate, persist;
  swap preset palette to Office Standard Colors; default empty-cell color
  to purple. Add tests.

## New settings shape

```ts
{
  emptyCellColor: string,    // hex, default "#7030A0" (Excel Standard Purple)
  theme: "auto"|"light"|"dark", // default "auto" — implementation in Phase 2
  masterMode: boolean,        // default false — wired in Phase 5
  autoCallEnabled: boolean,   // default false — wired in Phase 6
  autoCallSpeed: number,      // 1..10 seconds, default 5 — wired in Phase 6
}
```

## Excel Standard Colors palette (10 swatches, in order)

| # | Name | Hex |
|---|---|---|
| 1 | Dark Red | `#C00000` |
| 2 | Red | `#FF0000` |
| 3 | Orange | `#FFC000` |
| 4 | Yellow | `#FFFF00` |
| 5 | Light Green | `#92D050` |
| 6 | Green | `#00B050` |
| 7 | Light Blue | `#00B0F0` |
| 8 | Blue | `#0070C0` |
| 9 | Dark Blue | `#002060` |
| 10 | **Purple (default)** | `#7030A0` |

Layout in modal: `grid grid-cols-5 gap-2` so all 10 fit cleanly.

## Validation

- `emptyCellColor`: same `/^#[0-9a-fA-F]{6}$/` regex.
- `theme`: must be one of the literal strings; else fall back to `"auto"`.
- `masterMode` / `autoCallEnabled`: must be boolean; else default.
- `autoCallSpeed`: must be integer in `[1, 10]`; else default 5.

Use a small per-key validator function — don't pull in a schema lib.

## Files

| File | Change |
|---|---|
| `src/lib/settings-store.svelte.js` | extend `DEFAULT_SETTINGS`; add per-key validators; update `loadSettings` to validate each key independently and fall back per-key on miss; update `saveSettings` (no shape change). Apply-to-DOM stays for `--empty-cell-bg`. |
| `src/lib/settings-store.test.js` | add tests for the 4 new keys. |
| `src/lib/SettingsButton.svelte` | replace 8 ad-hoc swatches with the 10 Excel ones; first preset auto-tracks `DEFAULT_SETTINGS.emptyCellColor`. (Theme toggle, master mode, auto-call land in later phases — keep this phase scoped to color + foundation.) |

## Implementation Steps

1. Edit `settings-store.svelte.js`:
   - Update `DEFAULT_SETTINGS`:
     ```js
     export const DEFAULT_SETTINGS = Object.freeze({
       emptyCellColor: "#7030A0",
       theme: "auto",
       masterMode: false,
       autoCallEnabled: false,
       autoCallSpeed: 5,
     });
     ```
   - Add validators (one fn per key, returns coerced value or default).
   - Replace `loadSettings` body to apply each validator independently:
     ```js
     const parsed = JSON.parse(raw) ?? {};
     settings.emptyCellColor = validColor(parsed.emptyCellColor) ?? DEFAULT_SETTINGS.emptyCellColor;
     settings.theme         = validTheme(parsed.theme)         ?? DEFAULT_SETTINGS.theme;
     // ... etc
     ```
2. Edit `settings-store.test.js`:
   - Update default-color test to expect `#7030A0`.
   - Add tests for each new key: default, valid load, invalid load fall-back.
3. Edit `SettingsButton.svelte`:
   - Replace `PRESETS` array with the 10 Excel hex values.
   - Change swatch grid to `grid-cols-5`.

## Tests to add (in `settings-store.test.js`)

```
describe("settings-store — theme")
  it default is "auto"
  it loads "light" / "dark" valid
  it falls back on unknown string
describe("settings-store — masterMode")
  it default is false
  it loads true
  it falls back on non-boolean
describe("settings-store — autoCallEnabled")  // same shape as masterMode
describe("settings-store — autoCallSpeed")
  it default 5
  it loads 1..10
  it rejects 0 / 11 / non-int
  it rejects "5" string
```

## Risks

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Existing users have `loto_settings` with only `emptyCellColor` | High | None — per-key fallback handles it | Per-key validation, not whole-object |
| Default purple is too saturated against white grid cells | Med | Cosmetic | Reviewed: `#7030A0` reads fine; user picked purple; iterate if needed |

## Success criteria

- `npm test` adds new passing tests; existing 38 still pass.
- Settings modal shows 10 Excel swatches, default purple is the first one (auto-bound to `DEFAULT_SETTINGS.emptyCellColor`).
- Loading a settings JSON with old `{emptyCellColor: "#1e88e5"}` keeps the user's blue choice (no key wiped).

## Next
- Phase 2 consumes `settings.theme`.
- Phase 5 consumes `settings.masterMode`.
- Phase 6 consumes `settings.autoCallEnabled` + `settings.autoCallSpeed`.
