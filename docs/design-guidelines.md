# Design Guidelines

## Visual Identity

### Color Palette

**Primary (Player)**
- Gradient: indigo-500 → purple-500
- Use for: Main headings, generate button, hover states
- Semantic: Calm, welcoming, player-friendly

**Secondary (Host)**
- Gradient: orange-500 → red-500
- Use for: Host headings, new game button, high-stakes actions
- Semantic: Energetic, commanding, host authority

**Success (Completed Rows)**
- Background: emerald-100 (light) / emerald-900/40 (dark)
- Text: emerald-500 (light) / emerald-400 (dark)
- Use for: Crossed cells in completed rows

**Attention (Waiting Toast)**
- Background: amber-500/90
- Text: white
- Use for: "Chờ X" toast notifications

**Neutral (Grid & Text)**
- Borders: slate-200 (light) / slate-700 (dark)
- Background: white (light) / slate-800 (dark)
- Text: slate-600 (light) / slate-400 (dark)

### Dark Mode
All colors have corresponding `dark:` variants. Tailwind's `prefers-color-scheme: dark` media query is enabled (set in `app.css`).

## Typography

- **Font**: Geist Sans (Google Fonts), fallback to Arial/Helvetica.
- **Headings**: Extrabold (font-extrabold) for main title, bold (font-bold) for secondary.
- **Body**: Regular weight, slate-600 (light) / slate-400 (dark).
- **Emphasis**: `<strong>` for important copy (e.g., button labels in instructions).

### Sizing
- **Page title**: `text-4xl sm:text-5xl` (responsive)
- **Subheading**: `text-3xl sm:text-4xl`
- **Body**: `text-base sm:text-lg`
- **Small**: `text-xs text-slate-400`

## Layout & Spacing

### Breakpoints
- **Mobile first**: Base styles for mobile, then `sm:` (640px+), `md:` (768px+), `lg:` (1024px+).
- **Padding**: `px-3 py-8 sm:py-12` (horizontal, vertical responsive).
- **Gaps**: `gap-3`, `gap-6`, `gap-1.5` (consistent spacing scale).

### Grid Layout
- **Player/Host grid**: `grid-template-columns: repeat(9, 1fr)` (equal cells, no gap).
- **Master board**: Same 9-column layout (9×10 = 90 cells).
- **Cell sizing**: `aspect-square` for perfect squares.
- **Borders**: `border-r border-b` (right & bottom edges only, for clean lines).

### Containers
- **Max width**: `max-w-lg` (player), `max-w-2xl` (host) — centered with `mx-auto`.
- **Flex wrap**: Use `flex flex-wrap gap-1.5` for number history chips.

## Settings & UI Toggles

### Mode Picker (SettingsButton)
Three-way toggle in settings modal:
- **Player mode** (`mode: "player"`) — PlayerBoard only, default UX for players
- **Master mode** (`mode: "master"`) — MasterPanel only, host-only view (e.g., projector)
- **Both mode** (`mode: "both"`) — PlayerBoard + MasterPanel stacked inline, master auto-ticks player board on draw

Mode selection is persisted to localStorage and applied immediately.

## Component Patterns

### Button Styles
```html
<!-- Primary Action (Indigo/Purple) -->
<button class="px-8 py-3 rounded-full font-semibold text-white
               bg-gradient-to-r from-indigo-500 to-purple-500
               hover:from-indigo-600 hover:to-purple-600
               active:scale-95 transition-all shadow-lg shadow-indigo-500/25">
  Tạo bảng mới
</button>

<!-- Host Action (Orange/Red) -->
<button class="px-8 py-4 rounded-full font-semibold text-white text-lg
               bg-gradient-to-r from-orange-500 to-red-500
               hover:from-orange-600 hover:to-red-600
               active:scale-95 transition-all shadow-lg shadow-orange-500/25">
  Ván mới
</button>

<!-- Secondary (Emerald, Draw Action) -->
<button class="px-10 py-4 rounded-full font-semibold text-white text-lg
               bg-gradient-to-r from-emerald-500 to-teal-500
               hover:from-emerald-600 hover:to-teal-600
               active:scale-95 transition-all shadow-lg shadow-emerald-500/25">
  Xổ số
</button>
```

### Card Styling
- Border: `border border-slate-200 dark:border-slate-700`
- Rounded: `rounded-2xl`
- Shadow: `shadow-xl shadow-slate-200/50 dark:shadow-black/30`
- Overflow: `overflow-hidden` (clip content to rounded corners)

### Text Links
- Color: `text-indigo-500 dark:text-indigo-400` (player page)
- Color: `text-orange-500 dark:text-orange-400` (host page)
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

### Interactive
- **active:scale-95**: Button press response
- **transition-all**: Smooth color/shadow changes on hover

## Emoji Usage

Intentional emojis (matching traditional bingo celebration):
- 🎉 ✨ 🎊 🥳 ❤️ (bingo popup)
- ❤️ (footer)

Keep emojis rare; use for celebration only.

## Vietnamese Copy

- **Player instructions**: Simple, directive tone. "Nhấn" (press), "để" (to).
- **Toast messages**: Short. "Chờ X" (waiting for X).
- **Popups**: Celebratory. "Kinh!" (victory cry), "Tuyệt vời!" (awesome!).
- **Links**: Clear CTA. "Trang quản trò →" (host page), "← Về trang người chơi" (back to player).

## Mobile Responsiveness

- Stack vertically on mobile (`flex flex-col`).
- Reduce padding on small screens (`py-8` → `sm:py-12`).
- Enlarge text for readability on mobile (`text-base sm:text-lg`).
- Buttons stay full-width or flex-wrap on mobile.
- Images/grids use `aspect-square` to maintain proportion.

## Accessibility

- Alt text: Not critical (no images), but grid cells have semantic ARIA if needed.
- Focus states: Implicit via Tailwind (`focus:ring-2`), add if extending components.
- Contrast: Tailwind slate/indigo/emerald combos meet WCAG AA.
- Dark mode: Respects `prefers-color-scheme`, not forced.

Last reviewed: 2026-04-26
