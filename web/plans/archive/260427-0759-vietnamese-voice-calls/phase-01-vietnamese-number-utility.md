# Phase 1 — Vietnamese number words utility

## Context
- [plan.md](plan.md). No dependencies; self-contained pure module.

## Overview
- Priority: P0 (everything else depends on it)
- Status: TODO
- Effort: ~15 min

## Goal

Pure function `numberToVietnamese(n)` mapping `1..90` to spoken Vietnamese,
honoring the tonal exceptions that show up in lô tô numbers.

## Tonal rules (the parts that matter)

| Position | Rule | Example |
|---|---|---|
| Standalone unit | "không" / "một" / ... / "chín" | `5 = năm` |
| 10 alone | "mười" | `10 = mười` |
| 11–19, units 1–9 | "mười X" | `12 = mười hai` |
| 11–19, unit = 5 | **"mười lăm"** (not "năm") | `15 = mười lăm` |
| 20–90 tens place | "X mươi" (not "mười") | `30 = ba mươi` |
| 21+ unit = 1 | **"…mốt"** (not "một") | `21 = hai mươi mốt` |
| 21+ unit = 5 | **"…lăm"** (not "năm") | `25 = hai mươi lăm` |
| Tens 0 | drop unit | `40 = bốn mươi` |

(`tư` for 4 in unit position is regional — we stay with `bốn` for
consistency.)

## Files

| File | Change |
|---|---|
| `src/lib/vietnamese-number.js` | NEW — exports `numberToVietnamese(n)` |
| `src/lib/vietnamese-number.test.js` | NEW — covers edge cases below |

## Implementation sketch

```js
const ONES = [
  "không", "một", "hai", "ba", "bốn",
  "năm", "sáu", "bảy", "tám", "chín",
];

/** @param {number} n integer 0..90 */
export function numberToVietnamese(n) {
  if (!Number.isInteger(n) || n < 0 || n > 90) return String(n);
  if (n < 10) return ONES[n];
  if (n === 10) return "mười";
  if (n < 20) {
    const u = n - 10;
    return u === 5 ? "mười lăm" : `mười ${ONES[u]}`;
  }
  const t = Math.floor(n / 10);
  const u = n % 10;
  const tens = `${ONES[t]} mươi`;
  if (u === 0) return tens;
  if (u === 1) return `${tens} mốt`;
  if (u === 5) return `${tens} lăm`;
  return `${tens} ${ONES[u]}`;
}
```

## Tests (must cover)

| n | Expected |
|---|---|
| 1 | "một" |
| 5 | "năm" |
| 10 | "mười" |
| 11 | "mười một" |
| 15 | "mười lăm" |
| 19 | "mười chín" |
| 20 | "hai mươi" |
| 21 | "hai mươi mốt" |
| 25 | "hai mươi lăm" |
| 45 | "bốn mươi lăm" |
| 81 | "tám mươi mốt" |
| 90 | "chín mươi" |

Plus: out-of-range fall-through (`91 → "91"`, `0.5 → "0.5"`, `-1 → "-1"`).

## Success criteria

- All ~12 table cases pass.
- `numberToVietnamese` is pure (no side effects, idempotent, no DOM).
- Test file slots into existing Vitest setup with no config changes.

## Next
- Phase 2 imports it from the voice module.
