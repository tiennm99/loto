package com.miti99.loto.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Twins of `web/src/lib/voice.test.js` against the recording fake — the
 * cancellation and chờ-sequencing contracts the production player must obey.
 */
class VoicePlayerTest {

    @Test
    fun `cancel clears the active clip`() {
        val player = FakeVoicePlayer()
        player.playNumber(7)
        assertEquals(listOf("7"), player.active)
        player.cancel()
        assertNull(player.active)
    }

    @Test
    fun `a second playNumber cancels the first`() {
        val player = FakeVoicePlayer()
        player.playNumber(3)
        player.playNumber(5)
        assertEquals(listOf(listOf("3"), listOf("5")), player.utterances)
        // Only the second utterance is in flight.
        assertEquals(listOf("5"), player.active)
    }

    @Test
    fun `playWaiting plays only cho when voiceWaitingNumber is off`() {
        val player = FakeVoicePlayer()
        player.playWaiting(42, voiceWaitingNumber = false, modeIsBoth = false)
        assertEquals(listOf(listOf("cho")), player.utterances)
    }

    @Test
    fun `playWaiting chains cho then number when voiceWaitingNumber is on`() {
        val player = FakeVoicePlayer()
        player.playWaiting(42, voiceWaitingNumber = true, modeIsBoth = false)
        assertEquals(listOf(listOf("cho", "42")), player.utterances)
    }

    @Test
    fun `playWaiting suppresses trailing number in both mode even when flag is on`() {
        val player = FakeVoicePlayer()
        player.playWaiting(42, voiceWaitingNumber = true, modeIsBoth = true)
        assertEquals(listOf(listOf("cho")), player.utterances)
    }

    @Test
    fun `playBingo speaks the kinh clip`() {
        val player = FakeVoicePlayer()
        player.playBingo()
        assertEquals(listOf(listOf("kinh")), player.utterances)
    }
}
