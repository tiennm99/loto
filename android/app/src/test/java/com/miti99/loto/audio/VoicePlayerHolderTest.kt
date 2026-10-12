package com.miti99.loto.audio

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * H1: MainActivity.onDestroy() releases the app-scoped voice player on
 * `isFinishing`, but `finish()` does not guarantee the process dies —
 * Android can relaunch into the same cached process. These cover the
 * holder's recreate-after-release contract in isolation, since
 * `LotoApplication`/`ExoVoicePlayer` need a real Android `Context` and
 * cannot be constructed in a plain JVM unit test. The voice-sync glue
 * `LotoApplication` wires up ([VoicePlayerHolder.followVoice] over the
 * settings voice) is covered here the same way, with a fake player.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class VoicePlayerHolderTest {

    @Test
    fun `value builds lazily and reuses the same instance across accesses`() {
        var buildCount = 0
        val holder = VoicePlayerHolder { buildCount++; FakeVoicePlayer() }

        assertEquals(0, buildCount)
        val first = holder.value
        assertEquals(1, buildCount)
        assertSame(first, holder.value)
        assertEquals(1, buildCount)
    }

    @Test
    fun `release stops the current instance and clears the reference`() {
        val holder = VoicePlayerHolder { FakeVoicePlayer() }
        val first = holder.value as FakeVoicePlayer
        assertFalse(first.released)

        holder.release()
        assertTrue(first.released)
    }

    @Test
    fun `release with no instance built is a no-op`() {
        val holder = VoicePlayerHolder { FakeVoicePlayer() }
        holder.release() // must not throw despite value never having been read
    }

    @Test
    fun `a relaunch in the same process rebuilds a working player instead of the released one`() {
        val holder = VoicePlayerHolder { FakeVoicePlayer() }
        val first = holder.value as FakeVoicePlayer
        holder.release()

        // Simulates finish() + Android reusing the cached process/Application
        // instance: the next access must not hand back the terminal player.
        val second = holder.value as FakeVoicePlayer
        assertNotSame(first, second)
        assertFalse(second.released)
    }

    @Test
    fun `followVoice applies every configured voice to the live player`() = runTest {
        val holder = VoicePlayerHolder { FakeVoicePlayer() }
        val player = holder.value
        val voice = MutableStateFlow("hoai-my")
        backgroundScope.launch { holder.followVoice(voice) }
        runCurrent()
        assertEquals("hoai-my", player.voiceId)

        voice.value = "nam-minh"
        runCurrent()
        assertEquals("nam-minh", player.voiceId)
    }

    @Test
    fun `a player rebuilt after release starts on the configured voice, not the factory default`() =
        runTest {
            val holder = VoicePlayerHolder { FakeVoicePlayer() } // fake defaults to hoai-my
            holder.value
            val voice = MutableStateFlow("nam-minh")
            backgroundScope.launch { holder.followVoice(voice) }
            runCurrent()

            holder.release()
            // The settings flow does not re-emit an unchanged voice, so the
            // rebuilt player must pick it up from the holder itself.
            assertEquals("nam-minh", holder.value.voiceId)
        }

    @Test
    fun `followVoice does not build a player while none is held`() = runTest {
        var buildCount = 0
        val holder = VoicePlayerHolder { buildCount++; FakeVoicePlayer() }
        holder.value
        holder.release()
        val voice = MutableStateFlow("hoai-my")
        backgroundScope.launch { holder.followVoice(voice) }
        runCurrent()
        voice.value = "nam-minh"
        runCurrent()
        assertEquals(1, buildCount)

        assertEquals("nam-minh", holder.value.voiceId)
        assertEquals(2, buildCount)
    }
}
