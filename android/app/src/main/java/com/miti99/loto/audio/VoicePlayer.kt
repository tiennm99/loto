package com.miti99.loto.audio

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * Application-scoped Vietnamese voice playback. Single ExoPlayer instance,
 * token-based cancellation matching `tiennm99/loto/src/lib/voice.js`.
 *
 * One Player handles both voices — assets are addressed by URI, no dual
 * instance needed. Voice change cancels in-flight clip; next play uses the
 * new voice's path.
 */
@OptIn(UnstableApi::class)
class VoicePlayer(
    private val appContext: Context,
    private val voiceFlow: StateFlow<String>,
    appScope: CoroutineScope,
) {

    private val handler = Handler(Looper.getMainLooper())
    private var activeToken: Any? = null

    private val player: ExoPlayer = ExoPlayer.Builder(appContext)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .build()

    init {
        player.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    activeToken = null
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                activeToken = null
            }
        })

        // Drop the first emission so initial flow value (current voice) doesn't
        // trigger a spurious cancel before any clip plays.
        appScope.launch {
            voiceFlow.distinctUntilChanged().drop(1).collect { clearCache() }
        }
    }

    /** Play the clip for `n`. Cancels any in-flight clip first. */
    fun playNumber(n: Int) = playSequence(listOf(n.toString()))

    /**
     * Play "Chờ" prefix then optionally the awaited number. Cancels in-flight.
     * Mirrors `voice.js:104-115`.
     */
    fun playWaiting(n: Int, includeNumber: Boolean) {
        val names = if (includeNumber) listOf("cho", n.toString()) else listOf("cho")
        playSequence(names)
    }

    /** Play the bingo win announcement. Cancels in-flight. */
    fun playBingo() = playSequence(listOf("kinh"))

    /** Stop in-flight playback. Idempotent. */
    fun cancel() {
        val token = Any()
        activeToken = null
        handler.post {
            // Use a fresh token only to satisfy the same-thread invariant; we
            // don't gate on this one — cancel always wins.
            @Suppress("UNUSED_VARIABLE")
            val _t = token
            player.stop()
            player.clearMediaItems()
        }
    }

    /**
     * Drop in-flight playback and force the next call to resolve the current
     * voice path fresh. Matches `voice.js:46-49 clearAudioCache` semantics.
     */
    fun clearCache() = cancel()

    /** Best-effort cleanup. Process death cleans this up regardless. */
    fun release() {
        handler.post {
            activeToken = null
            player.release()
        }
    }

    // ----- internals -----

    private fun playSequence(names: List<String>) {
        val token = Any()
        activeToken = token
        val items = names.map { name -> MediaItem.fromUri(assetUri(name)) }
        handler.post {
            if (activeToken !== token) return@post
            player.stop()
            player.clearMediaItems()
            player.setMediaItems(items)
            player.prepare()
            player.playWhenReady = true
        }
    }

    private fun assetUri(name: String): String =
        "asset:///audio/${voiceFlow.value}/$name.mp3"
}
