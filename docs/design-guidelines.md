# Design Guidelines

## Visual Identity

Clean, flat, single-accent system — no gradients anywhere. Color carries
hierarchy via solid weights; emphasis via type weight + scale.

### Color Palette

**Primary (Player + UI accent)**
- Solid: rose-600 `#E11D48` (light) / rose-400 `#FB7185` (dark)
- Use for: Title, "Tạo bảng mới", "Tuyệt vời!", "Xong", focus rings,
  selected pickers, link in footer
- Semantic: warm, lucky-red Vietnamese-fair feel

**Secondary (Host)**
- Solid: amber-600 `#D97706` (light) / amber-400 `#FBBF24` (dark)
- Use for: "Quản trò" subhead, "Ván mới", host chips, bingo "Kinh!" h2
- Semantic: festive, energetic

**Draw / Go**
- Solid: emerald-600 `#059669` (light) / emerald-400 `#34D399` (dark)
- Use for: "Xổ số", "Bắt đầu", master grid token ring (≥50)

**Stop / Active-running**
- Solid: rose-600 (same as primary). Distinguished by context (only
  appears as the running auto-call toggle).

**Master low-half marker (≤49)**
- Solid: sky-600 `#0284C7` (light) / sky-400 `#38BDF8` (dark)
- Paired with emerald-600 (≥50) for an elegant cool/warm binary
- Replaces the previous pink — kept clearly distinct from the rose
  primary so the eye doesn't read "low-number" and "primary action" as
  the same brand color.

**Success (Completed Rows)**
- Background: emerald-100 (light) / emerald-900/60 (dark)
- Text: emerald-700 (light) / emerald-200 (dark)
- Slash: solid emerald-600 stroke

**Attention (Waiting Toast / "Chờ" ring)**
- Background: amber-500/95 (toast); amber-500/45→0.85 inset ring (label)
- Text: white
- Use for: "Chờ X" toast and persistent section-band cue

**Neutral (Grid & Text)**
- Page background: warm cream `#FFFBEB` (light) / near-black navy
  `#050813` (dark) — flat, no radial glow
- Borders: slate-200 (light) / slate-700 (dark)
- Body text: slate-600 (light) / slate-300 (dark)
- Heading text: slate-800/foreground (light) / slate-100 (dark)

### Dark Mode
All colors have corresponding `dark:` variants. Tailwind's
`prefers-color-scheme: dark` media query is enabled (set in `app.css`).
Light/dark mode contrast pairs verified at WCAG AA minimum.

## Typography

- **Font**: Geist Sans (Google Fonts), fallback to Arial/Helvetica.
- **Grid numbers**: Roboto Condensed (self-hosted via
  @fontsource/roboto-condensed, font-display: swap) in `tan-tan-num`
  stack, bold weight.
- **Headings**: Italic + `font-black` for the page title, `font-bold`
  for secondary headings (e.g. "Quản trò").
- **Body**: Regular weight, slate-600 (light) / slate-300 (dark).
- **Emphasis**: `<strong>` for important copy (e.g., button labels).

### Sizing
- **Page title**: `text-5xl sm:text-7xl`
- **Subheading**: `text-lg sm:text-xl uppercase tracking-wider`
- **Body**: `text-base sm:text-lg`
- **Small**: `text-xs text-slate-500 dark:text-slate-400`

## Layout & Spacing

### Breakpoints
- **Mobile first**: Base styles for mobile, then `sm:` (640px+),
  `md:` (768px+), `lg:` (1024px+).
- **Padding**: `px-2 py-4 sm:px-3 sm:py-12` (page).
- **Gaps**: `gap-3`, `gap-6`, `gap-1.5` (consistent spacing scale).

### Grid Layout
- **Player/Host grid**: `grid-template-columns: repeat(9, 1fr)` (equal
  cells, no gap).
- **Master board**: Same 9-column layout (9×11 = 99 cells, of which 90
  hold numbers).
- **Cell sizing**: `aspect-[3/4] sm:aspect-[3/5]` (player) /
  `aspect-square` (master).
- **Borders**: `border-r border-b` (right & bottom edges only).

### Containers
- **Max width**: `max-w-2xl` (page wrapper) — centered with `mx-auto`.
- **Flex wrap**: `flex flex-wrap gap-1.5` for number history chips.

### Section Frame (Tân Tân lô tô identity)
- **Outer frame**: 4px solid bars (top/bottom/left/right) in
  `--section-accent` (`#1565c0` light, `#64b5f6` dark).
- **Section label**: italic uppercase text on a pale section band, with
  thin 2px solid horizontal rules on each side via `::before`/`::after`.
- **Waiting cue**: `box-shadow: inset 0 0 0 2px amber-500/0.6` plus a
  2.4s pulse animation on the label band.

## Settings & UI Toggles

### Mode Picker (SettingsButton)
Three-way segmented picker in settings modal with inline SVG glyphs:
- **Player mode** (rectangle icon)
- **Master mode** (megaphone icon)
- **Both mode** (mini grid + mini megaphone side-by-side)

SVG icons hand-drawn from primitives (no icon library). Selected state
uses solid rose-600 border + rose-50 fill. Selection persisted to
localStorage.

### Color Picker (SettingsButton)
Grouped card with two sub-sections:
- **Tuỳ chỉnh** (Custom): native `<input type="color">` for hex entry
- **Mẫu sẵn** (Presets): 10 Excel "Standard Colors" swatches (purple
  default)

Card has rounded border, padding. Compact layout fits mobile.

## Component Patterns

### Button Styles
All buttons are solid-color, `rounded-full`, with a soft shadow
(`shadow-md`) and `active:scale-95` press feedback. Hover transitions
to a darker step (`-700`).

```html
<!-- Player primary -->
<button class="px-8 py-3 rounded-full font-semibold text-white
               bg-rose-600 hover:bg-rose-700
               active:scale-95 transition-all shadow-md">
  Tạo bảng mới
</button>

<!-- Host primary -->
<button class="px-8 py-4 rounded-full font-semibold text-white text-lg
               bg-amber-600 hover:bg-amber-700
               active:scale-95 transition-all shadow-md">
  Ván mới
</button>

<!-- Draw / Go -->
<button class="px-10 py-4 rounded-full font-semibold text-white text-lg
               bg-emerald-600 hover:bg-emerald-700
               active:scale-95 transition-all shadow-md">
  Xổ số
</button>

<!-- Secondary outline -->
<button class="px-6 py-3 rounded-full font-semibold
               text-slate-700 dark:text-slate-200
               bg-white dark:bg-slate-800
               border-2 border-slate-300 dark:border-slate-600
               hover:bg-slate-50 dark:hover:bg-slate-700
               active:scale-95 transition-all">
  Xoá đánh dấu
</button>
```

### Card Styling
- Border: `border border-slate-200 dark:border-slate-700`
- Rounded: `rounded-md` (player sheet) / `rounded-2xl` (master grid,
  modal)
- Shadow: `shadow-xl shadow-slate-200/50 dark:shadow-black/30`
- Overflow: `overflow-hidden` (clip content to rounded corners)

### Cell Cross Slash
Drawn as an SVG `<line>` overlay (no gradients). `viewBox="0 0 100 100"`
with `preserveAspectRatio="none"` stretches the line corner-to-corner
on any cell aspect ratio; `vector-effect="non-scaling-stroke"` keeps
the visual width constant.

- Standard slash: `stroke: #ef4444` (red-500)
- Winning-row slash (in `.cross-slash-win`): `stroke: #10b981` (emerald-500)
- Draw animation: `stroke-dashoffset` 200→0 over 200ms (skipped under
  `prefers-reduced-motion`)

### Text Links
- Color: `text-rose-600 dark:text-rose-400`
- Hover: `hover:underline`

## Animations

### Entrance
- **fade-in** (0.2s): Modal background, instant attention
- **pop-in** (0.4s): Bingo popup, celebratory scale bounce

### Continuous
- **bounce-slow** (1.5s): Emoji on bingo popup (translateY oscillation)
- **spin-slow** (3s): ✨ emoji (360° rotation)
- **spin-slow-reverse** (3s): 🎊 emoji (counter-rotation)

### Ephemeral
- **toast** (5s): "Chờ X" notification (scale 0.8→1.05→1, fade build)
- **cross-draw** (200ms): SVG slash stroke-dashoffset reveal
- **section-pulse** (2.4s): Amber inset-ring pulse on waiting section
- **confetti-fall** (1.6s): Tier-2 bingo emoji confetti

### Interactive
- **active:scale-95**: Button press response
- **active:scale-90**: Cell tap response
- **transition-all**: Smooth color/shadow changes on hover

All animations respect `prefers-reduced-motion: reduce`.

## Emoji Usage

Intentional emojis (matching traditional bingo + Vietnamese fair):
- 🎉 ✨ 🎊 🥳 ❤️ (bingo popup)
- 🏮 (header subtitle)
- 🎫 🎤 🎶 (empty/celebration messages)
- 🥢 🎋 🏮 (tier-2 confetti, alongside 🎊 ✨ 🎉 🥳)
- ❤️ (footer)

Keep emojis decorative only. Functional UI uses SVG glyphs.

## Vietnamese Copy

- **Player instructions**: Simple, directive tone. "Nhấn" (press),
  "để" (to).
- **Toast messages**: Short. "Chờ X" (waiting for X).
- **Popups**: Celebratory. "Kinh!" (victory cry),
  "Tuyệt vời!" (awesome!).
- **Mode hints**: One-liner per option, italic, slate-500.

## Mobile Responsiveness

- Stack vertically on mobile (`flex flex-col`).
- Reduce padding on small screens (`py-4` → `sm:py-12`).
- Enlarge text for readability on mobile (`text-base sm:text-lg`).
- Buttons stay full-width or flex-wrap on mobile.
- Cells use `aspect-[3/4] sm:aspect-[3/5]` (taller than wide) so
  numbers read large on phones.

## Accessibility

- Alt text: Not critical (no images). Grid cells expose
  `aria-label="Số {n}, đã đánh dấu"` and `aria-pressed`.
- Focus states: `focus:ring-2 focus:ring-rose-400` (interactive
  controls).
- Contrast: Tailwind slate/rose/amber/emerald combos meet WCAG AA;
  text-on-button pairs at 4.5:1 minimum.
- Dark mode: respects `prefers-color-scheme`, not forced.
- SVG glyphs: Simple, high-contrast shapes; keyboard-navigable via
  fieldset + label.
- Reduced motion: all animations disabled; SVG slash stays drawn.

## PWA Features

- **Install Prompt**: Native browser-driven (no custom banner).
  Manifest enables Android/iOS install.
- **Offline**: App shell precache enables instant load. Audio cached
  on first play.
- **Auto-Update**: SW detects new version; user sees reload toast
  (non-intrusive).

Last reviewed: 2026-04-29
