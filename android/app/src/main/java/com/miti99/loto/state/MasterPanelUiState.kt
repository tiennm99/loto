package com.miti99.loto.state

/** Master panel snapshot. `deck == null` until first newGame(). */
data class MasterPanelUiState(
    val deck: DeckState? = null,
    val lastCalled: Int? = null,
    val autoRunning: Boolean = false,
    val callOrder: Map<Int, Int> = emptyMap(),
)
