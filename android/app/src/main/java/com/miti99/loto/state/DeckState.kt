package com.miti99.loto.state

import kotlinx.serialization.Serializable

/** Master deck — shuffled remaining + ordered called list. */
@Serializable
data class DeckState(
    val called: List<Int>,
    val remaining: List<Int>,
) {
    companion object {
        fun fresh(random: kotlin.random.Random = kotlin.random.Random.Default): DeckState =
            DeckState(called = emptyList(), remaining = (1..90).shuffled(random))
    }
}
