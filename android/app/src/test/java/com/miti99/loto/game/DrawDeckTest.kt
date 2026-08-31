package com.miti99.loto.game

import kotlin.random.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Ported one-to-one from `web/src/lib/master-store.test.js` (pure deck
 * semantics; the storage-validation cases have their twins at the
 * persistence layer). Test names mirror the web `it` strings.
 */
class DrawDeckTest {

    @Test
    fun `starts empty`() {
        val deck = DrawDeck()
        assertEquals(emptyList<Int>(), deck.called)
        assertEquals(emptyList<Int>(), deck.remaining)
    }

    @Test
    fun `startNewGame fills remaining with 90 unique 1 to 90`() {
        val deck = DrawDeck()
        deck.startNewGame()
        assertEquals(emptyList<Int>(), deck.called)
        assertEquals(90, deck.remaining.size)
        val set = deck.remaining.toSet()
        assertEquals(90, set.size)
        for (n in 1..90) assertTrue(set.contains(n))
    }

    @Test
    fun `drawNext appends called and shifts remaining, returns drawn`() {
        val deck = DrawDeck()
        deck.startNewGame()
        val first = deck.remaining.first()
        val drawn = deck.drawNext()
        assertEquals(first, drawn)
        assertEquals(listOf(first), deck.called)
        assertEquals(89, deck.remaining.size)
        assertFalse(deck.remaining.contains(first))
    }

    @Test
    fun `drawNext returns null when exhausted`() {
        val deck = DrawDeck()
        deck.startNewGame()
        repeat(90) { deck.drawNext() }
        assertNull(deck.drawNext())
        assertEquals(90, deck.called.size)
        assertEquals(emptyList<Int>(), deck.remaining)
    }

    @Test
    fun `restore round-trip preserves state`() {
        val deck = DrawDeck()
        deck.startNewGame()
        deck.drawNext()
        deck.drawNext()
        val calledBefore = deck.called
        val remainingBefore = deck.remaining

        val restored = DrawDeck()
        restored.restore(calledBefore, remainingBefore)
        assertEquals(calledBefore, restored.called)
        assertEquals(remainingBefore, restored.remaining)
    }

    @Test
    fun `seeded deck draws deterministically and in monotonic call order`() {
        val a = DrawDeck(Random(7))
        val b = DrawDeck(Random(7))
        a.startNewGame()
        b.startNewGame()
        val drawsA = (1..90).mapNotNull { a.drawNext() }
        val drawsB = (1..90).mapNotNull { b.drawNext() }
        assertEquals(drawsA, drawsB)
        assertEquals(drawsA, a.called)
    }
}
