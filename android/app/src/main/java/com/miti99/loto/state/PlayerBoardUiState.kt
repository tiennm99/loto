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
)
