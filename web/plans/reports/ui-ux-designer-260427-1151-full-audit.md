# Lô tô — Full UI/UX Audit (260427)

Stack: SvelteKit + Tailwind 4. Mobile-first, single-page. Audit scope: app.css, +page.svelte, PlayerBoard, MasterPanel, SettingsButton, PageFooter.

Severity: P0 = breaks UX/a11y, P1 = noticeable polish/clarity, P2 = nice-to-have.

---

## P0

### 1. Master-only mode looks abandoned on first load
**Where:** `+page.svelte:37-52`, `MasterPanel.svelte:323-327`.
Mode=master + no game started = page shows only "Ván mới" button + "Nhấn 'Ván mới' để bắt đầu" text. No header context, no preview, no hero. Feels like a broken page, not "the master tool".
**Fix sketch:** add empty-state hero (decorative 11x9 ghost board mock like PlayerBoard's preview, or a "Chuẩn bị xổ số" illustration). Surface a "Đang ở chế độ Quản trò" pill near top so the role is explicit when no player board sits above.

### 2. `cell-crossed` red diagonal — contrast on green winning row
**Where:** `app.css:184-196`, `PlayerBoard.svelte:281`.
Winning row uses `bg-emerald-100`/`text-emerald-700` + `#ef4444` diagonal. Red-on-mint is OK, but the `text-emerald-300` dark variant + red slash + dim emerald-900/40 bg = ~2.5:1 text contrast in dark. Below WCAG AA.
**Fix sketch:** for dark winning cells, lift text to `emerald-200` and bump bg to `emerald-900/60`; or use a lighter slash color (rose-300) on dark.

### 3. `tan-tan-num` font stack omits Vietnamese-safe fallback
**Where:** `app.css:131-142`. "Arial Narrow", "Avenir Next Condensed", "Roboto Condensed" — none are guaranteed to ship Vietnamese diacritics consistently on Android (esp. older WebView). Player numbers won't render diacritics, but the same class is reused for fallback rendering and any future label. Still, Roboto Condensed isn't loaded — relies on system. On Linux/Android without it, you get default sans, defeating the "tall + tight" aesthetic.
**Fix sketch:** load a self-hosted Vietnamese-supporting condensed display face (e.g. Bebas Neue Vie, Oswald subset latin-ext+vietnamese) via @font-face; keep current as fallback.

### 4. Toast overlays board content — blocks the cell underneath
**Where:** `PlayerBoard.svelte:321-336`. Toast is `absolute inset-0` flex-centered, pointer-events-none on wrapper but the button itself eats clicks for ~5s. Lands smack on the middle row.
**Fix sketch:** anchor toast to top-edge or below grid (translate-y above the grid) — never centered over playable cells.

---

## P1

### 5. Three-mode picker has no icon/visual cue
**Where:** `SettingsButton.svelte:241-256`. "Người chơi / Quản trò / Cả hai" are text-only pills. Discoverability OK once modal opens but doesn't communicate role at a glance. "Cả hai" is ambiguous (both *what*?).
**Fix sketch:** add a tiny glyph above each label (player card icon / megaphone / split). Add a 1-line description under the active selection ("Hiển thị bảng người chơi" etc.) so users know *what changes*.

### 6. Light-mode `--section-band-bg` (`#e3f2fd 70%`) over amber radial = muddy edge
**Where:** `app.css:24, 13-17`. Cool blue band on warm amber glow at top → visible color clash on the topmost section label.
**Fix sketch:** either warm the band tint (cream or rose tinge) or darken the section accent slightly so band reads on either glow. Test against amber and indigo variants.

### 7. Master hero number circle — fixed `w-40 h-40` clips on 320px screens with border-[8px]
**Where:** `MasterPanel.svelte:220-237`. 160px circle + 16px ring + page padding (`px-2`=8) + `flex flex-col items-center` = fits, but the called-history scrolls awkwardly because hero never collapses. On scroll-into-view, header gets pushed off, host loses orientation.
**Fix sketch:** sticky top mini-pill of last-called when hero is scrolled offscreen. Or shrink hero to `w-32 h-32` ≤375px.

### 8. Auto-call sub-fieldset density unclear
**Where:** `SettingsButton.svelte:260-294`. Slider sits inside the same fieldset as the toggle, no visual separation, label "Tốc độ" floats. When toggle off, fieldset shrinks abruptly with no transition — jarring.
**Fix sketch:** indent slider with left border (mirror voice-waiting nesting at line 319). Add slide transition. Add tick labels at 1s/5s/10s.

### 9. Voice nesting reads: "Quản trò đọc số" + sub-text "Đọc số đã xổ + báo Chờ/Kinh khi ở Cả hai"
**Where:** `SettingsButton.svelte:307-314`. The hint text only renders when mode≠player. So in mode=player, "Quản trò đọc số" toggle is shown with no explanation — confusing because there is no master visible.
**Fix sketch:** hide the master voice toggle entirely in mode=player (it has no effect there) or add a hint that explains it's for "Cả hai" mode.

### 10. Color-picker: presets are 5×2 grid, hex code is tiny + no swatch label
**Where:** `SettingsButton.svelte:373-389`. 10 color squares with no name tooltip. Selected state is a 110% scale + indigo ring — but the *current custom* hex shown next to the native picker doesn't update its visual when a preset is clicked (it does, but the `<input type="color">` and `<code>` block are visually disconnected from the preset row).
**Fix sketch:** wrap picker + hex + presets in one card with a "Tuỳ chỉnh" / "Mẫu sẵn" sub-divide. Show hex *inside* the selected swatch overlay.

### 11. Settings modal: "Mặc định" button is barely visible (slate-600 text, no border)
**Where:** `SettingsButton.svelte:392-401`. Reset is a destructive-ish action. Currently looks like a footer link, easily missed or accidentally hit.
**Fix sketch:** ghost-button style with subtle border, or move under a divider line with smaller "Đặt lại tất cả" text + confirm dialog.

### 12. Header brand "Lô tô" + sub "Hội chợ TN1" — sub is muted slate, italic, all-caps tracking-[0.28em]
**Where:** `+page.svelte:26-30`. Reads more "fintech subhead" than fairground. Off-brand for festive.
**Fix sketch:** swap to a hand-drawn or display Vietnamese script font for sub; or wrap in decorative dashes/dots ("· Hội chợ TN1 ·"). Lower tracking (.18em) and add a tiny string-light or paper-lantern emoji.

### 13. "Kinh!" celebration modal — purple/pink gradient, generic
**Where:** `PlayerBoard.svelte:389-439`. Confetti overlay only triggers on tier 2 (3+ bingos). First bingo modal is plain-ish for a fairground game. No row number callout in an oversized way.
**Fix sketch:** for first bingo use a single confetti burst (not the rain), and make `Hàng X` huge (text-5xl) — that's the actual win info.

---

## P2

### 14. Footer dual cross-hatch label "Made by miti99" — visually heavy near grid bottom
**Where:** `PlayerBoard.svelte:291-314`. Reuses `.section-label` (which has flanking ✚✚✚). Then `PageFooter.svelte` adds a *second* "Made by miti99" line. Duplicate attribution.
**Fix sketch:** drop the in-card credit (it duplicates the footer); keep just the cross-hatch decorative band with no text, or replace with "TN1 · Lô tô" branding.

### 15. Confetti emoji set "🎊✨🎉🥳" — same chunkiness, all sit at 2rem
**Where:** `PlayerBoard.svelte:34, 175-182`. Visual variety low; no rotation in fall start.
**Fix sketch:** add Vietnamese-flavored bits: 🥢 🎋 🏮 (lantern!) and randomize size 1.5–2.5rem.

### 16. Master called-history pills — pink/green border with cream fill, OK contrast but identical shape to hero
**Where:** `MasterPanel.svelte:253-268`. Mini repeats hero styling. `tabular-nums` good. But `border-[3px]` on 36px pill = ~31px usable. Numbers like 88 cramped.
**Fix sketch:** drop border to 2px on pills; reserve 3px-ring treatment for hero only.

### 17. `cell-crossed` slash uses fixed `#ef4444` red — same on green winning bg as on red losing bg
**Where:** `app.css:184-196`. Green winning row deserves a green or gold slash to communicate "complete" not "marked".
**Fix sketch:** add a `.cell-crossed-win` modifier with emerald slash, applied when `rowComplete`.

### 18. `aria-live="assertive"` on hero number
**Where:** `MasterPanel.svelte:218`. SR users get every draw spammed assertively. Should be polite — assertive is reserved for warnings.
**Fix sketch:** `aria-live="polite"` is sufficient; lastCalled change is informational not urgent.

### 19. Reduced-motion not respected
**Where:** `app.css:144-182`. confetti-fall, bounce-slow, spin-slow, pop-in, toast — none gated by `@media (prefers-reduced-motion: reduce)`.
**Fix sketch:** wrap animation declarations in a media query that disables transforms/opacity loops, falling back to instant fade-in.

### 20. Empty cells use a single solid color across light & dark
**Where:** `app.css:20`, `PlayerBoard.svelte:259-266`. Default `#7030A0` purple over near-black `#050813` is fine but on light theme the same purple looks office-clipart-ish next to amber glow. The `bg-black/15` overlay in dark is a band-aid.
**Fix sketch:** allow per-theme cell color or auto-shift the saved color toward a more festive Tân Tân red/blue when theme=light and color=default.

### 21. No reset/clear keyboard shortcut, no swipe gestures on mobile
**Where:** general. Power user pain — tapping "Xổ số" 90 times.
**Fix sketch:** spacebar to draw next; long-press hero to undo last call. Nice-to-have only.

---

## Unresolved questions

1. Is the rose lower-glow in dark mode visible on tall pages? It's `at 50% 110%` but `background-attachment: fixed` — confirm intent: glow should track the viewport bottom, not the document.
2. Default mode is `"player"` — is that final? "Cả hai" might be the natural at-a-table default. Check telemetry/intent.
3. Is the in-card "Made by miti99" supposed to live there alongside footer attribution, or was it left over from before PageFooter existed?
4. Tier-2 confetti threshold = 3+ bingos per session. Is this per-card or per-page? Code says `celebratedRows.size` on a 9-row card — only triggers if 3 of 9 rows complete, which is rare in a single round.
5. Is there appetite for a per-row column highlight when "Chờ" toast fires (point at the row that's waiting)?

---

**Status:** DONE
**Summary:** 21 findings — 4 P0 (master empty state, dark winning-cell contrast, font Vietnamese fallback, toast blocks cells), 9 P1 (mode picker discoverability, modal density, brand mood, hero scaling), 8 P2.
