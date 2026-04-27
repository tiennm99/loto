package com.miti99.loto.audio

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Parsed entry from `assets/audio/manifest.json`. */
@Serializable
data class VoiceEntry(
    val id: String,
    val edgeName: String,
    val label: String,
    val gender: String,
)

@Serializable
private data class ManifestRoot(val voices: List<VoiceEntry>)

object VoiceManifest {

    private val json = Json { ignoreUnknownKeys = true }

    /** Read and parse the bundled voice manifest. Throws on malformed input. */
    fun load(context: Context): List<VoiceEntry> {
        val text = context.assets.open("audio/manifest.json").bufferedReader().use { it.readText() }
        return json.decodeFromString<ManifestRoot>(text).voices
    }
}
