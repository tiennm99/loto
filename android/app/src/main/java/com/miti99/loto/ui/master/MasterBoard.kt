package com.miti99.loto.ui.master

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/**
 * 11×9 ones-digit-aligned master board.
 *
 * @param called     ordered list of called numbers (earliest first)
 * @param lastCalled the most-recently drawn number, for red-ring highlight
 */
@Composable
fun MasterBoard(
    called: List<Int>,
    lastCalled: Int?,
    modifier: Modifier = Modifier,
) {
    // Map number → 1-based call order. Recomputed only when `called` changes.
    val callOrderMap: Map<Int, Int> = remember(called) {
        called.withIndex().associate { (i, n) -> n to (i + 1) }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = "Bảng theo dõi số đã xổ" },
    ) {
        for (row in MASTER_BOARD) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (num in row) {
                    MasterCell(
                        num = num,
                        callOrder = callOrderMap[num],
                        isLast = num > 0 && num == lastCalled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
