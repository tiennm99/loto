package com.miti99.loto.state

data class BingoEvent(val row1Based: Int, val tier: Int)

data class WaitingToast(val message: String, val seenAt: Long)

/**
 * Snapshot of the player card surface. `grid == null` means cold-start (show
 * the faded preview). `rowComplete` is precomputed in the VM so the UI doesn't
 * call isRowComplete inside Compose composition.
 */
data class PlayerBoardUiState(
    val grid: List<List<Int>>? = null,
    val crossed: List<List<Boolean>> = emptyList(),
    val rowComplete: List<Boolean> = emptyList(),
    val bingoEvent: BingoEvent? = null,
    val waitingToast: WaitingToast? = null,
    val celebratedRows: Set<Int> = emptySet(),
    val notifiedWaitingRows: Set<Int> = emptySet(),
    val lastConsumedEventId: Long = 0L,
    /**
     * Per-section flag (size 3): rows 0-2, 3-5, 6-8. True iff any row in
     * that section is one cell from bingo and not yet complete. Drives
     * the persistent amber ring on the section label band.
     */
    val sectionHasWaiting: List<Boolean> = listOf(false, false, false),
)
