/**
 * Vietnamese number-words for lô tô calls. Pure, no DOM, no I/O.
 * @module lib/vietnamese-number
 */

const ONES = [
  "không",
  "một",
  "hai",
  "ba",
  "bốn",
  "năm",
  "sáu",
  "bảy",
  "tám",
  "chín",
];

/**
 * Convert an integer 0..90 to its spoken Vietnamese form, honoring the
 * tonal exceptions that show up in lô tô (15 = "mười lăm", 21 = "hai mươi
 * mốt", 25 = "hai mươi lăm", etc.). Out-of-range values fall back to a
 * plain `String(n)` so the caller never has to guard against undefined.
 * @param {number} n
 * @returns {string}
 */
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
