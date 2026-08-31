package com.miti99.loto.audio

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
 * cannot be constructed in a plain JVM unit test.
 */
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
}
