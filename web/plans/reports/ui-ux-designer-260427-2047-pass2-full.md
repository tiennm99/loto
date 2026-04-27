# Lô tô — UI/UX Audit Pass 2 (260427-2047)

Scope: re-audit since `f28279b` against current `main`. Read `app.css`, `app.html`, `manifest.webmanifest`, `+page.svelte`, `PlayerBoard`, `MasterPanel`, `MasterEmptyState`, `SettingsButton`, `PageFooter`. Severity P0/P1/P2.

Mostly clean follow-up. Most pass-1 items addressed well. Findings below are new or partial-fix regressions.

---

## P0

### 1. PWA splash + tab theme color hardcoded saturated blue, mismatches light page background
**Where:** `app.html:9` (`#1565c0` light) and `manifest.webmanifest:9-10` (`theme_color #1565c0`, `background_color #0a0f1f`).
Light app bg is `#f8fafc` (near-white) but Safari tab strip / Chrome top bar paints `#1565c0`. Hard color jump where the bg should bleed into the chrome. PWA splash on iOS uses `background_color` only — `#0a0f1f` (deep navy) → light-mode users get a dark-navy splash that flashes into a near-white app. Jarring on cold launch.
**Fix sketch:** light theme-color `#f3e9d7` or `#fff7ec` (warm off-white that echoes the amber top-glow). Manifest `background_color` to a neutral midpoint or fork by media: `background_color: #f8fafc`. iOS doesn't honor light/dark manifest yet, so pick the value matching the *more common* launch theme — `auto` defaults to user OS, so neutral cream is safer than near-black.

### 2. Tab title still bare "Lô tô" — PWA install card name mismatch
**Where:** `app.html:6` `<title>Lô tô</title>` vs `manifest.name "Lô tô — Hội chợ TN1"`.
Installed app shows full name; browser tab + history show only "Lô tô". Cold-share link previews lose the "Hội chợ TN1" context entirely. Also no Open Graph tags.
**Fix sketch:** title `Lô tô — Hội chợ TN1`; add `<meta property="og:title">`, `og:description`, `og:image` (use `icon-512.png`). One-time copy, immediate brand lift on share.

---

## P1

### 3. PlayerBoard empty-state and MasterEmptyState are near-clones with conflicting prompts
**Where:** `PlayerBoard.svelte:343-371` ghost grid + `MasterEmptyState.svelte:6-43` ghost grid.
Both render same opacity-30 monochrome ghost-grid pattern. Master mode in `both` shows player ghost AND master ghost stacked when no game started. Visual repetition; the page reads "two empty boxes" not "two distinct roles". Also: master grid shows 99 cells but real master board is 11×9=99 with last row = single cell at col 8 — ghost should mirror the actual silhouette.
**Fix sketch:** vary the ghosts visually — player ghost shows a subtle row-of-numbers stripe; master ghost shows scattered "called dots" pattern. Or hide the player ghost entirely in `both` mode pre-game (the master-mode hero CTA carries the call-to-action).

### 4. Mode picker glyphs read at first glance only for "player"; "master" megaphone is ambiguous, "both" looks like a stacked-window icon
**Where:** `SettingsButton.svelte:259-275`.
Player rect-with-grid-lines reads instantly = "card with rows". Master path `M3 11l14-6v14L3 13z` + arc is a megaphone but at 24×24 stroke 1.8 looks like an abstract triangle pointing right; not enough silhouette weight at 28px tall. "Both" stacked rectangles read as "two cards" not "player + master roles". Hint line below mitigates but the glyph itself doesn't sell.
**Fix sketch:** master = filled megaphone with sound waves (use stroke-width 2.2 + fill-on-active). "Both" = player-card-glyph layered with mini-megaphone badge in corner — composes the two prior glyphs. Keeps semantic continuity ("both = the two things above stacked").

### 5. "Đặt lại" reset chip — bordered now, but still no confirm dialog
**Where:** `SettingsButton.svelte:431-440`.
Pass-1 fix turned reset into a chip-with-border (good). But it still resets all settings (theme, mode, color, voice, auto-call) on a single tap. Easy mis-tap on mobile next to "Xong". No undo.
**Fix sketch:** `if (confirm("Đặt lại tất cả tuỳ chỉnh?"))` guard; or convert to two-step ("Tap to reset" → "Confirm reset" inline state for 3s). Native `confirm()` is fine here, low frequency.

### 6. Settings modal scroll on small viewports — sticky header/footer absent
**Where:** `SettingsButton.svelte:166-167`. `max-h-[90vh] overflow-y-auto` whole-modal scroll.
On 375×667 (iPhone SE) with both auto-call AND voice-waiting expanded, modal hits ~700px. User scrolls past "Cài đặt" title; "Đặt lại / Xong" footer scrolls off too — must scroll back to dismiss. Title and primary CTAs should be persistent.
**Fix sketch:** sticky title row (`sticky top-0 bg-white dark:bg-slate-800 -mx-6 px-6 pt-6 pb-3 z-10`), sticky footer row similarly. Inner content gets the scroll. Saves a scroll-trip per session.

### 7. Auto-call slider — still no tick labels at 1s/5s/10s
**Where:** `SettingsButton.svelte:306-316`.
Pass-1 noted this. Fixed: nesting + left-border indent (good). Not fixed: tick labels. Slider value floats free, user has no anchor for "what is fast vs slow".
**Fix sketch:** below slider, add `<div class="flex justify-between text-[10px] text-slate-400 mt-1"><span>1s</span><span>5s</span><span>10s</span></div>` aligned to track. Trivial.

### 8. Voice "Quản trò đọc số" hint copy still confusing in `both` mode
**Where:** `SettingsButton.svelte:336-338`. Pass-1 note: hide in player mode — done. But: hint says `Đọc số đã xổ + báo Chờ/Kinh khi ở "Cả hai".` In `master` mode the second clause ("báo Chờ/Kinh") is wrong — there's no player board to call Chờ/Kinh from in solo master.
**Fix sketch:** branch the hint by mode: master → `Đọc số đã xổ.`; both → `Đọc số đã xổ và báo Chờ/Kinh thay người chơi.`

### 9. Header subline `🏮 Hội chợ TN1` — lantern emoji renders monochrome on Windows/Linux, color on Apple/Android
**Where:** `+page.svelte:31`.
Cross-platform lantern inconsistency. On a Windows browser the lantern is line-art outlined, breaking the festive intent. Dashes-flank treatment is good though.
**Fix sketch:** ship a tiny SVG lantern inline (12×16, color: rose-500) in place of the emoji. Same byte cost, consistent across OS, theme-tintable.

### 10. Master "Số vừa xổ" hero — w-32 mobile is good (pass-1 fixed), but border-[6px] eats interior
**Where:** `MasterPanel.svelte:244-252`.
128px circle - 12px border (×2) = 104px interior for an 8xl number. Number renders fine but the ring feels chunky at this size; reads as "thick outlined badge", not "called number". Aspect feels token-y not announcer-y.
**Fix sketch:** `border-[4px] sm:border-[10px]`. Keep desktop chunk; trim mobile.

---

## P2

### 11. Section-divider hatch repeats under the bottom decorative band but with no label slot
**Where:** `PlayerBoard.svelte:316`. `<div class="section-label" aria-hidden="true"></div>` — empty label = just the cross-hatch flanks ::before/::after with `flex:1` and 0 gap. Visually thinner than divider above section-label rows.
**Fix sketch:** swap to `<div class="section-divider"></div>` — semantic match, consistent thickness. Tested: same color path via `--section-accent`.

### 12. `aria-live="assertive"` on hero number — still assertive in pass-1 had it as P2; updated to "polite" in code (good). No regression.

### 13. Confetti emoji set still `["🎊", "✨", "🎉", "🥳"]` — pass-1 P2 note re. lantern + Vietnamese-flavored set not addressed
**Where:** `PlayerBoard.svelte:35`.
Add `🏮 🎋 🥢` for fairground feel. Match the lantern in the header for cohesion.

### 14. `apple-touch-icon` only 192px (no 180px specifically; no `apple-touch-startup-image`)
**Where:** `app.html:11`. iOS scales 192→180 fine but loses sharpness. Missing splash image = bare-color flash on cold launch.
**Fix sketch:** generate `icon-180.png` and `apple-splash-{2048x2732,1668x2388,1170x2532}.png` from `source.svg`. Wire `<link rel="apple-touch-startup-image" media="...">`. Optional polish.

### 15. `MasterEmptyState` ghost grid uses `i % 11 < 9 && i % 7 === 0` — generates non-deterministic-looking sparse fill that doesn't mirror the master grid silhouette (col 8 row 10 only has 90)
**Where:** `MasterEmptyState.svelte:14`. Cosmetic — looks like a noise-ghost rather than a master-board ghost. Fine as decoration but pass on opportunity to communicate "tracking grid" affordance.
**Fix sketch:** mirror real `BOARD` shape: row 0 cols 1-8, rows 1-9 all cols, row 10 col 8. Then ghost-fill ~15-20% with a deterministic mod pattern.

### 16. Toast above grid (pass-1 fixed) — but on `both` mode the toast renders inside PlayerBoard's `relative` wrapper while master panel below pushes content; toast `-top-3` goes negative into the page-padding zone, can clip on very narrow screens (≤320px) where parent has only `px-2` (8px).
**Where:** `+page.svelte:11`, `PlayerBoard.svelte:330`.
Edge case (≤320 = older Android, rare). Toast still readable but center text near the screen edge.
**Fix sketch:** add `mx-2` on the toast button so it can compress safely; or move toast to `top-1` (positive offset, sits inside the rounded board chrome).

---

## Pass-1 fix verification

| Pass-1 item | Verdict |
|---|---|
| 1. Master empty state | ✅ MasterEmptyState added, role pill shown |
| 2. Dark winning row contrast | ✅ `bg-emerald-900/60 text-emerald-200` |
| 3. Vietnamese font fallback | ✅ self-hosted Roboto Condensed 700 |
| 4. Toast over cells | ✅ moved to `-top-3` (see P2 §16 edge case) |
| 5. Mode picker glyphs | ⚠️ added but readability mixed (P1 §4) |
| 7. Master hero w-40 mobile clip | ✅ w-32 sm:w-56 |
| 8. Auto-call slider density | ⚠️ partial — nesting fixed, no tick labels (P1 §7) |
| 9. Voice nesting wording | ⚠️ partial — hidden in player but copy still off in master (P1 §8) |
| 10. Color picker grouped | ✅ bordered card + sub-headers |
| 11. Reset button visibility | ⚠️ chip-bordered (good) but no confirm (P1 §5) |
| 12. Header subline brand mood | ✅ dash-flanked, lantern emoji (see P1 §9 cross-OS) |
| 13. Bingo modal row-number size | ✅ text-5xl/6xl |
| 18. aria-live polite on hero | ✅ |
| 19. Reduced-motion gating | ✅ media query added |
| 14. Footer dual attribution | ✅ in-card credit removed |

---

## Unresolved questions

1. Manifest `background_color: #0a0f1f` was chosen for dark; should we accept the light-mode PWA splash dark-flash, or pick a neutral cream? (P0 §1)
2. iOS Safari standalone — has it actually been tested on a device, or only DevTools simulated? Apple-status-bar `black-translucent` interacts with `safe-area-inset-top`; nothing in the layout reserves that inset.
3. Is there appetite to drop the `🏮` emoji entirely if the SVG-inline lantern is rejected? Plain dashes alone read fine and ship-stable. (P1 §9)
4. Auto-call max 10s — is that the right ceiling? Real-life Lô tô callers often pause 15-20s for call-and-response. Out of scope but data point.

---

**Status:** DONE
**Summary:** 16 findings — 2 P0 (PWA splash/theme color mismatch, tab title brand), 8 P1 (empty-state duplication, glyph readability, reset-confirm, sticky modal chrome, slider ticks, voice hint copy, lantern cross-OS, mobile hero ring), 6 P2. Pass-1 mostly addressed; partial-fixes on items 5/8/9/11.
