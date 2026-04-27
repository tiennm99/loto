package com.miti99.loto.game

/**
 * Spoken Vietnamese for lô tô calls (0..90). Honors tonal exceptions:
 *   - 5 → "năm", but `*5` ≥ 20 → "lăm" (e.g. 25 = "hai mươi lăm")
 *   - 1 → "một", but `*1` ≥ 20 → "mốt" (e.g. 21 = "hai mươi mốt")
 *   - 15 → "mười lăm" (special)
 *
 * Out-of-range values fall back to `n.toString()` — matches the JS source's
 * defensive posture (`String(n)` for non-integer / out-of-range), so callers
 * never have to guard.
 */
object VietnameseNumber {

    private val ONES = listOf(
        "không", "một", "hai", "ba", "bốn",
        "năm", "sáu", "bảy", "tám", "chín",
    )

    fun numberToVietnamese(n: Int): String {
        if (n < 0 || n > 90) return n.toString()
        if (n < 10) return ONES[n]
        if (n == 10) return "mười"
        if (n < 20) {
            val u = n - 10
            return if (u == 5) "mười lăm" else "mười ${ONES[u]}"
        }
        val t = n / 10
        val u = n % 10
        val tens = "${ONES[t]} mươi"
        return when (u) {
            0 -> tens
            1 -> "$tens mốt"
            5 -> "$tens lăm"
            else -> "$tens ${ONES[u]}"
        }
    }
}
