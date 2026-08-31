package com.miti99.loto.audio

/**
 * Recreatable holder for an app-scoped [VoicePlayerApi]. A plain `by lazy`
 * singleton is one-shot: once [release] is called the delegate is terminal
 * (per [VoicePlayerApi.release]'s own contract), but `finish()` does not
 * guarantee the process dies — Android can relaunch the same cached process,
 * reusing the same `Application` instance, and a `by lazy` field would then
 * hand every ViewModel an already-released, permanently-dead player (H1).
 *
 * [value] rebuilds via [factory] whenever the current instance has been
 * released, so a release-then-relaunch-in-process gets a working player
 * again instead of a dead one, while an explicit exit still actually frees
 * the previous instance's resources (the original intent behind releasing
 * at all, rather than merely cancelling).
 */
class VoicePlayerHolder(private val factory: () -> VoicePlayerApi) {

    private var instance: VoicePlayerApi? = null

    /** Builds a fresh player on first access and after every [release]. */
    val value: VoicePlayerApi
        get() = instance ?: factory().also { instance = it }

    /** Release the current player, if any, and drop the reference. */
    fun release() {
        instance?.release()
        instance = null
    }
}
