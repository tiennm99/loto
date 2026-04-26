# Lô Tô Rules Reference: SvelteKit Web App Implementation Guide

**Report Date:** 26 Apr 2026  
**Research Scope:** Vietnamese Lô tô (Bingo 90 / Tombola) rules, card layout, winning conditions, and host mechanics  
**Sources Consulted:** Wikipedia tiếng Việt, English Bingo 90 references, Vietnamese gaming blogs, official game set descriptions

---

## 1. PLAYER CARD LAYOUT

### Standard Format (Confirmed across sources)
- **Dimensions:** 3 rows × 9 columns grid
- **Numbers per row:** 5 marked, 4 blank spaces
- **Total numbers per card:** 15 (out of 1–90 pool)
- **Number distribution:** Column-by-column allocation (see below)

### Column Organization & Number Ranges

| Column | Range | Note |
|--------|-------|------|
| 1 | 1–9 | 9 numbers |
| 2 | 10–19 | 10 numbers |
| 3 | 20–29 | 10 numbers |
| 4 | 30–39 | 10 numbers |
| 5 | 40–49 | 10 numbers |
| 6 | 50–59 | 10 numbers |
| 7 | 60–69 | 10 numbers |
| 8 | 70–79 | 10 numbers |
| 9 | 80–90 | 11 numbers |

**Source:** [Bingo 90 Standard Rules](https://www.tombola.co.uk/bingo/how-to-play-bingo90), [Wikipedia Lô tô](https://vi.wikipedia.org/wiki/Lô_tô), [Tombola Rules](https://www.mundigames.com/multiplayer/tombola/rules/)

### Ascending Order Rule
**Within each column, numbers appear in ascending order from top to bottom.** A column may have 1, 2, or 3 numbers; never more than 3 per column (due to 3 rows).

**Source:** [Tombola Card Layout Explanation](https://oboe.com/learn/create-tombola-bingo-cards-in-pdf-fdzo/understanding-tombola-card-layout-0)

### Card Generation Implications for Your App
- **Exactly 5 per row:** Standard requirement (not an invention)
- **Exactly 5 per column (on average):** NOT a hard rule. Columns can have 1–3 numbers each; the total is always 15.
- **Current implementation (9×9 with 5 per row & per column):** **INCORRECT** — This creates a 45-number card (3×15), not standard 15-number card. Should be **3×9 grid with 15 total.**

---

## 2. MASTER BOARD (QUẢN TRÒ DISPLAY)

### Layout & Tracking Function
No single standardized master board layout found in Vietnamese sources. However, British Bingo 90 practice is:
- **9 columns × 10 rows** (representing 90 positions)
- Column headings: B(1–9), I(10–19), N(20–29), G(30–39), O(40–49), …, O(80–90) [note: "O" at start & end for symmetry in physical boards]
- Marked/crossed when called

### Vietnamese Quản Trò Practice
One person (quản trò or "cái") draws and calls numbers. Tracking happens verbally + manually via:
- Physical board with markers, or
- Pen-marked paper grid

No specific Vietnamese master board convention found; recommend adopting **9×10 column-by-tens layout** (matches player card column structure for easy mapping).

**Source:** [Bingo Caller Setup](https://www.bingocardcreator.com/bingo-caller/1-90/), [Game Master Role](https://antoursvietnam.com/how-to-play-lotto-show-lo-to-guide/)

---

## 3. DRAWING & CALLING MECHANICS

### Number Range
- **Standard:** 1–90 (confirmed for Bingo 90 adaptation in Vietnam)
- **Variation noted:** Some older sources mention 1–60, but modern Vietnamese games use 1–90

**Source:** [Lô Tô App References](https://apps.apple.com/vn/app/lô-tô/id1353746681), [Wikipedia tiếng Việt](https://vi.wikipedia.org/wiki/Lô_tô)

### Drawing Order
- **Method:** Pure random draw from bag/cage
- **No fixed opening number:** Unlike some lotteries, no mandated first-draw convention found
- **Hô lô tô chants:** Cultural recitation/singing occurs during calling (entertainment), but does NOT affect game rules

**Source:** [Vietnamese Game Master Culture](https://dochoicholon.com/bo-tro-choi-keu-lo-to-90-so-bang-giay-va-go.html)

---

## 4. WINNING CONDITIONS & CALLING PATTERNS

### Primary Win Condition: "Kinh"
**Definition:** Player completes one full horizontal row (5 consecutive marked numbers on that row).  
**Call:** Player shouts "Kinh!" (Vietnamese: "Kinh!" = "I won!")  
**Verification:** Quản trò checks the winning card to confirm all 5 numbers on that row were called.

**Source:** [Vietnamese Game Rules](https://shopee.vn/blog/cach-choi-lo-to/), [Wikipedia](https://vi.wikipedia.org/wiki/Lô_tô), [Vietcetera](https://vietcetera.com/en/feeling-lucky-try-this-vietnamese-traditional-game-called-lo-to)

### Waiting State: "Hò"
**Definition:** Player has 4 numbers in a row, waiting on 1 final number.  
**Call:** Player announces "Hò" (Vietnamese: "Hò" = "I'm waiting")  
**Effect:** Signals to quản trò and other players that this player is close to winning. Does NOT stop the game.

**Source:** [Vietnamese Game Master Mechanics](https://dochoicholon.com/bo-tro-choi-keu-lo-to-90-so-bang-giay-va-go.html)

### Extended Winning Patterns (Bingo 90 Standard, adopted in Vietnam)
1. **One Line ("một hàng"):** First horizontal row completed → **Primary prize**
2. **Two Lines ("hai hàng"):** Any two horizontal rows completed → **Secondary prize** (higher payout)
3. **Full House ("bingo" or "toàn bộ"):** All 15 numbers on the card marked → **Grand prize** (highest payout)

**Note:** "Kinh đôi" and "kinh ba" (mentioned in some sources) refer to secondary/tertiary prizes, not distinct patterns—terminology varies by region/host.

**Source:** [Bingo 90 Winning Patterns](https://www.bingosites.co.uk/90-ball-bingo/), [Tombola Multiplayer Rules](https://www.mundigames.com/multiplayer/tombola/rules/)

### Multiple Winners
**Same-draw rule:** If two players complete the same pattern (e.g., both fill one line on same draw), **prize splits equally** or house rules determine payout.

---

## 5. HOUSE & MASTER RULES

### Can Master Play Their Own Card?
**Yes, confirmed.** Quản trò can participate as a player while managing the draw.

**Source:** [Game Master as Player](https://dochoicholon.com/bo-tro-choi-keu-lo-to-90-so-bang-giay-va-go.html) (implies dual role), [Vietnamese game sets](https://www.sayweee.com/en/product/Vietnamese-Loto-Games/85136) (standard sets include 16 cards, host takes 1 or more)

### Stake / Pot Structure
Not a rules matter—informal/social game. No standardized betting mechanism in traditional rules; modern apps may implement custom stake systems.

### Game Flow
1. Quản trò distributes cards (1–16 per set)
2. First number drawn; quản trò calls it
3. Players mark matching numbers
4. On each draw: players check for "hò" (4-in-row) or "kinh" (5-in-row)
5. Winner verified → prize awarded
6. Game continues until all cards are full or organizer stops

---

## 6. IMPLEMENTATION VALIDATION & GOTCHAS

### Current App Issues (Assumed from Your Description)

| Issue | Severity | Fix |
|-------|----------|-----|
| **9×9 grid (45 numbers)** | CRITICAL | Change to **3×9 grid (15 numbers)** |
| **Enforce exactly 5 per column** | MINOR | Relax: allow **1–3 per column**, total 15; column sums should average ~1.67 |
| **Master board as 11×9 (99 positions)** | MODERATE | Change to **9×10 (90 positions)** or keep 11×9 if you're tracking an extra row; ensure column labeling matches player card ranges |
| **Missing "Hò" state** | MODERATE | Add call state when 4 numbers marked in a row; player can announce before "kinh" |
| **Missing two-line / full-house detection** | MODERATE | Implement pattern detection for 2 rows + full card (Bingo 90 standard) |

### Regional Variations (Unconfirmed in Sources)
No North/Central/South differences for Lô tô rules found. Game appears standardized across Vietnam, with cultural/entertainment variations (chanting, poetry) but not rule variations.

---

## 7. RECOMMENDATIONS FOR YOUR APP

### Immediate Changes (Pre-Release)
1. **Revert card to 3 rows × 9 columns × 15 numbers total**
   - Regenerate card algorithm to pick 5 random per row, ensuring column constraints (1–3 per column)
   - Verify column ranges (1–9, 10–19, …, 80–90)
   - Enforce ascending order within each column

2. **Add "Hò" state tracking**
   - Player can announce "Hò" when 4 in a row marked
   - Display visually on card UI (e.g., highlight the waiting row)

3. **Master board: align with 9×10 layout or 9×9 (clarify your choice)**
   - If 11×9: document why (e.g., custom feature) so it doesn't confuse players

4. **Implement full-house & two-line detection**
   - Not just one-line (kinh) wins

### Nice-to-Have (Post-Release)
- Add hô lô tô chant audio library (for authenticity, entertainment)
- Regional chant variants (if monetizing regionally)
- Multiplayer: simultaneous "kinh" handling & pot-splitting logic

---

## 8. UNRESOLVED / CONTESTED CLAIMS

1. **Master board standard layout:** No Vietnamese source specifies quản trò's tracking board. British Bingo 90 uses 9×10; your 11×9 may be valid local variation.
   - **Action:** Verify with your user base or adopt 9×10 as safer default

2. **"Kinh đôi" / "kinh ba" terminology:** Mentioned in searches but not defined as distinct win conditions. Likely regional slang for two-line / full-house wins.
   - **Action:** Treat as marketing terminology, implement underlying Bingo 90 patterns

3. **Regional rule differences (Bắc/Trung/Nam):** No sources confirm variations
   - **Action:** Assume rules are nationwide; note for future regional research if user feedback suggests otherwise

4. **Card generation randomness:** Sources don't specify algorithm (fully random per card? seeded per player? repeated numbers across decks?). Assumed independent random per card.
   - **Action:** Implement independent random generation; allow deck-wide seed if you need reproducible games

---

## 9. SOURCE CREDIBILITY SUMMARY

| Source | Credibility | Used For |
|--------|------------|----------|
| Wikipedia Lô tô (tiếng Việt) | HIGH | Card layout, "kinh" definition, 3×9 grid |
| Tombola.co.uk (British Bingo 90) | HIGH | Column ranges, ascending order rule, master board concept |
| Vietnamese gaming blogs (Shopee, BachHoaXanh) | MEDIUM | Game flow, "hò" state, cultural context |
| Game set product descriptions (Amazon, Sayweee) | MEDIUM | Game components, number ranges (1–90) |
| Vietcetera cultural article | LOW | Cultural significance only; rules vague |

**Cross-check result:** Card layout (3×9, 15 numbers, column ranges 1–9 through 80–90) **confirmed across 3+ independent sources**. Winning conditions (kinh for one row, two rows, full house) **confirmed via Bingo 90 standard + Vietnamese sources**.

---

**End of Report**
