package com.miti99.loto.game

import kotlin.random.Random

/**
 * The master's draw deck, ported from `web/src/lib/master-store.svelte.js`.
 * Forward-only: numbers move from [remaining] to [called] and never back;
 * a new round replaces both lists wholesale.
 *
 * Pure state holder — persistence and reactivity live in the layers above.
 */
class DrawDeck(private val random: Random = Random.Default) {

    var called: List<Int> = emptyList()
        private set

    var remaining: List<Int> = emptyList()
        private set

    /** Start a fresh round: empty called, full shuffled remaining. */
    fun startNewGame() {
        called = emptyList()
        remaining = shuffled1to90()
    }

    /**
     * Draw the next number from [remaining] into [called].
     * @return the drawn number, or null if exhausted.
     */
    fun drawNext(): Int? {
        if (remaining.isEmpty()) return null
        val next = remaining.first()
        called = called + next
        remaining = remaining.drop(1)
        return next
    }

    /**
     * Restore a previously persisted round. Callers own payload validation
     * (range/shape checks live at the persistence layer, mirroring the web's
     * `loadMaster`).
     */
    fun restore(called: List<Int>, remaining: List<Int>) {
        this.called = called
        this.remaining = remaining
    }

    private fun shuffled1to90(): List<Int> {
        val all = (1..90).toMutableList()
        // Fisher-Yates, matching the web implementation.
        for (i in all.indices.reversed()) {
            if (i == 0) break
            val j = random.nextInt(i + 1)
            val tmp = all[i]
            all[i] = all[j]
            all[j] = tmp
        }
        return all
    }
}
