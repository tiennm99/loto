package com.miti99.loto.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

/**
 * Port of `tiennm99/loto/src/lib/vietnamese-number.test.js`. Out-of-range
 * fallback to `n.toString()` matches the JS source.
 */
class VietnameseNumberTest {

    @ParameterizedTest(name = "{0} → {1}")
    @CsvSource(
        // units 0..9
        "0,'không'", "1,'một'", "2,'hai'", "3,'ba'", "4,'bốn'",
        "5,'năm'", "6,'sáu'", "7,'bảy'", "8,'tám'", "9,'chín'",
        // teens 10..19 with mười lăm exception
        "10,'mười'", "11,'mười một'", "12,'mười hai'", "13,'mười ba'", "14,'mười bốn'",
        "15,'mười lăm'", "16,'mười sáu'", "17,'mười bảy'", "18,'mười tám'", "19,'mười chín'",
        // 20..90 with mốt and lăm exceptions
        "20,'hai mươi'", "21,'hai mươi mốt'", "22,'hai mươi hai'",
        "25,'hai mươi lăm'", "29,'hai mươi chín'",
        "30,'ba mươi'", "31,'ba mươi mốt'",
        "40,'bốn mươi'", "45,'bốn mươi lăm'",
        "55,'năm mươi lăm'", "61,'sáu mươi mốt'",
        "70,'bảy mươi'", "81,'tám mươi mốt'", "85,'tám mươi lăm'", "90,'chín mươi'",
        // out-of-range fallback
        "-1,'-1'", "91,'91'", "100,'100'",
    )
    fun `numberToVietnamese matches table`(n: Int, expected: String) {
        assertEquals(expected, VietnameseNumber.numberToVietnamese(n))
    }
}
