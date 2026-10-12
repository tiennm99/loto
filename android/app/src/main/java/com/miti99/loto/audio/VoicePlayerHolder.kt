package com.miti99.loto.audio

import kotlinx.coroutines.flow.Flow

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
 *
 * [followVoice] owns the configured voice id rather than writing it straight
 * into one player instance, so a rebuilt player starts on the user's voice
 * instead of the factory default (the settings flow does not re-emit an
 * unchanged value just because the player behind it was replaced).
 */
class VoicePlayerHolder(private val factory: () -> VoicePlayerApi) {

    private var instance: VoicePlayerApi? = null

    /** Latest id from [followVoice]; null = keep the factory's default. */
    private var voiceId: String? = null

    /** Builds a fresh player on first access and after every [release]. */
    val value: VoicePlayerApi
        get() = instance ?: factory().also { player ->
            voiceId?.let { player.voiceId = it }
            instance = player
        }

    /**
     * Keeps the current player, and every one rebuilt later, on the latest
     * id from [voiceIds]. Does not build a player itself: a released holder
     * just remembers the id for the next [value] access. Suspends for as
     * long as [voiceIds] does.
     */
    suspend fun followVoice(voiceIds: Flow<String>) {
        voiceIds.collect { id ->
            voiceId = id
            instance?.voiceId = id
        }
    }

    /** Release the current player, if any, and drop the reference. */
    fun release() {
        instance?.release()
        instance = null
    }
}
