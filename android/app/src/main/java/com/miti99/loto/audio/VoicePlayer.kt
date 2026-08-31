package com.miti99.loto.audio

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/**
 * Playback abstraction the ViewModels depend on; unit tests substitute a
 * recording fake, the production implementation is [ExoVoicePlayer].
 *
 * Cancellation contract (ported from `web/src/lib/voice.js`): every [speak]
 * call replaces any in-flight utterance — clips never overlap or queue
 * across calls. Switching the voice takes effect on the next utterance.
 */
interface VoicePlayerApi {
    /** The active voice id (folder slug under `assets/audio/`). */
    var voiceId: String

    /** Play [clipNames] as one gapless sequence, canceling any in-flight one. */
    fun speak(clipNames: List<String>)

    /** Stop the in-flight utterance, if any. */
    fun cancel()

    /** Release playback resources; the instance is unusable afterwards. */
    fun release()
}

/** Announce a drawn number. */
fun VoicePlayerApi.playNumber(n: Int) = speak(listOf(n.toString()))

/**
 * Announce "Chờ", optionally chained with the awaited number.
 * Gating ported from the web's `playWaiting`: the trailing number is
 * suppressed in both mode — the master is already calling numbers aloud, so
 * "Chờ 42" right after a "33" call confuses listeners who can't tell which
 * number is the active draw.
 */
fun VoicePlayerApi.playWaiting(n: Int, voiceWaitingNumber: Boolean, modeIsBoth: Boolean) =
    speak(waitingClips(n, voiceWaitingNumber, modeIsBoth))

/** Announce "Kinh" (row win). */
fun VoicePlayerApi.playBingo() = speak(listOf(VoiceCatalog.CLIP_KINH))

/** Pure clip-sequence rule for the chờ announcement (see [playWaiting]). */
fun waitingClips(n: Int, voiceWaitingNumber: Boolean, modeIsBoth: Boolean): List<String> =
    if (voiceWaitingNumber && !modeIsBoth) {
        listOf(VoiceCatalog.CLIP_CHO, n.toString())
    } else {
        listOf(VoiceCatalog.CLIP_CHO)
    }

/**
 * Media3 ExoPlayer implementation over the bundled `asset:///audio/...`
 * clips. Main-thread only (ExoPlayer requirement) — constructed and used
 * from the UI thread.
 *
 * Audio-focus decision: the clips are ~1s speech cues layered over whatever
 * the table is already listening to, so the player requests NO audio focus
 * (music keeps playing, calls behave normally) but still pauses when audio
 * would become noisy (headphones unplugged).
 */
class ExoVoicePlayer(context: Context) : VoicePlayerApi {

    override var voiceId: String = VoiceCatalog.defaultVoiceId(VoiceCatalog.FALLBACK_VOICES)

    private val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                .build(),
            /* handleAudioFocus = */ false,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()

    override fun speak(clipNames: List<String>) {
        if (clipNames.isEmpty()) return
        // Replacing the playlist stops the in-flight utterance — the native
        // equivalent of voice.js's token cancellation.
        player.setMediaItems(
            clipNames.map { clip ->
                MediaItem.fromUri("asset:///${VoiceCatalog.clipPath(voiceId, clip)}")
            },
        )
        player.prepare()
        player.play()
    }

    override fun cancel() {
        player.stop()
        player.clearMediaItems()
    }

    override fun release() {
        player.release()
    }
}
