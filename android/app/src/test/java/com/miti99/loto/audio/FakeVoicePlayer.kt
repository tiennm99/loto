package com.miti99.loto.audio

/**
 * Recording fake for [VoicePlayerApi]. [utterances] logs every speak call;
 * [active] models the single in-flight utterance ([speak] replaces it —
 * the cancellation contract — and [cancel] clears it).
 */
class FakeVoicePlayer : VoicePlayerApi {

    override var voiceId: String = "hoai-my"

    val utterances = mutableListOf<List<String>>()

    var active: List<String>? = null
        private set

    var released = false
        private set

    var cancelCount = 0
        private set

    override fun speak(clipNames: List<String>) {
        utterances.add(clipNames)
        active = clipNames
    }

    override fun cancel() {
        cancelCount++
        active = null
    }

    override fun release() {
        released = true
    }
}
