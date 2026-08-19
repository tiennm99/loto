# Phase 03 — Solid-color palette refactor

## Context

Upstream commit `2fb35f2` (2026-04-29). New single-accent system:

- **rose-600** `#E11D48` — player + UI primary
- **amber-600** `#D97706` — host primary
- **emerald-600** `#059669` — draw / completion
- **sky-600** `#0284C7` — master ≤49 token (replaces pink-500)
- Background warmer cream `#fffbeb` (light); dark unchanged (`#050813`).
- All ad-hoc gradients dropped EXCEPT the wordmark (intentional brand
  signature; web also kept its rose→amber wordmark).

## Priority

P1 — visual cohesion.

## Status

pending.

## Key insights

- Android already has `BrandRoseLight/Dark`, `BrandAmberLight/Dark`,
  `BrandEmeraldLight/Dark`. We need the deeper `-600` shades AND new
  `BrandSky*` constants.
- `BrandPink*` and `BrandIndigo*` become unused after this phase. Delete
  them outright (`docs/development-rules.md`: "do not leave unused"),
  unless any test asserts on the old hex.
- "Ván mới" button currently uses `Brush.horizontalGradient(amber, rose)`
  — flatten to solid `BrandAmber600`.
- The crossed-cell red diagonal slash is a `Canvas` line in Compose
  already — no SVG migration needed; it's already a stroked line, not a
  gradient.

## Related code files

- `ui/theme/Color.kt` — add `BrandSky*`, `BrandRose600`, `BrandAmber600`,
  `BrandEmerald600`, `BackgroundCream`. Remove `BrandPink*`, `BrandIndigo*`
  once unused.
- `ui/theme/Theme.kt` — switch light `background` to `BackgroundCream`.
- `ui/master/MasterCell.kt`, `ui/master/CalledHistory.kt`,
  `ui/master/CurrentNumberHero.kt` — replace `BrandPinkLight` with
  `BrandSky600`.
- `ui/master/MasterControls.kt` — drop the gradient `Box` wrapper; use a
  plain `Button(containerColor = BrandAmber600)`.
- `ui/board/PlayerBoardScreen.kt` — replace `BrandIndigoLight` (clear
  button) with `BrandRose600`. Adjust `Generate` button to `BrandRose600`
  if currently `BrandRoseLight`.
- `ui/board/KinhModal.kt` — drop the gradient `Brush` on "Kinh!" title;
  use solid `BrandAmber600` (matches web's text-amber-600).

## Implementation steps

1. **Color.kt — add deeper shades**

   ```kotlin
   // Solid-palette shades (matches Tailwind -600 step)
   val BrandRose600 = Color(0xFFE11D48)
   val BrandAmber600 = Color(0xFFD97706)
   val BrandEmerald600 = Color(0xFF059669)
   val BrandSky600 = Color(0xFF0284C7)
   val BrandSky400 = Color(0xFF38BDF8)   // dark variant
   val BackgroundCream = Color(0xFFFFFBEB)
   ```

   After all consumers migrate, delete:

   ```kotlin
   // val BrandPinkLight, BrandPinkDark
   // val BrandIndigoLight, BrandIndigoDark
   ```

2. **Theme.kt — light background**

   ```kotlin
   private val LotoLightColorScheme = lightColorScheme(
       // ...
       background = BackgroundCream,    // was BackgroundLight
       // ...
   )
   ```

3. **Master ≤49 token color** — every `BrandPinkLight` → `BrandSky600`
   (light theme). For dark theme, use `BrandSky400`. Wire the dark
   variant via `if (darkTheme) BrandSky400 else BrandSky600` at the
   call site.

4. **MasterControls.kt — flatten "Ván mới"**

   Replace the `Box { Button(transparent) }` wrapper with:

   ```kotlin
   Button(
       onClick = onNewGame,
       colors = ButtonDefaults.buttonColors(
           containerColor = BrandAmber600,
           contentColor = Color.White,
       ),
       modifier = Modifier.weight(1f),
   ) {
       Text(stringResource(R.string.btn_new_game), fontWeight = FontWeight.Bold)
   }
   ```

   Remove the `Brush.horizontalGradient` import.

5. **PlayerBoardScreen.kt — clear button**

   Replace `BrandIndigoLight` with `BrandRose600`. Use `OutlinedButton`
   contentColor.

6. **KinhModal.kt — flatten title**

   Replace `Brush.horizontalGradient` with a solid `BrandAmber600` text
   color. Drop the gradient brush import.

## Todo

- [ ] Color.kt: add 6 new tokens
- [ ] Theme.kt: light background → cream
- [ ] MasterCell.kt: pink → sky (theme-aware)
- [ ] CalledHistory.kt: pink → sky
- [ ] CurrentNumberHero.kt: pink → sky
- [ ] MasterControls.kt: drop gradient on "Ván mới"
- [ ] PlayerBoardScreen.kt: indigo → rose on clear button
- [ ] KinhModal.kt: drop gradient on "Kinh!"
- [ ] Color.kt: delete BrandPink/Indigo once unused
- [ ] Compile clean

## Success criteria

- No `Brush.horizontalGradient` import outside `Wordmark.kt`.
- "Ván mới" renders as solid amber, no orange→red split.
- Master ≤49 tokens render sky-blue, not pink.
- Light-mode background is cream (`#fffbeb`).
- "Kinh!" title is solid amber.
- `grep -r BrandPink` and `grep -r BrandIndigo` return only theme-deletion
  candidates (or nothing after cleanup).

## Risks

- KinhModal Compose render previews may need re-snapshot if any exist.
- Dark-mode contrast: sky-600 on amber-cream cell could under-contrast.
  Use sky-400 in dark mode (we add `BrandSky400` for that).

## Next

Phase 04 (voice-master hint copy).
