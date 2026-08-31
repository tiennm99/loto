package com.miti99.loto.audio

import android.content.Context
import org.json.JSONObject

/**
 * A bundled announcer voice, mirroring the entries of
 * `assets/audio/manifest.json` (the same file the web app imports).
 */
data class Voice(
    val id: String,
    val edgeName: String,
    val label: String,
    val gender: String,
)

/**
 * Catalog of the bundled voices. Clips live under
 * `assets/audio/{voiceId}/{1..90,cho,kinh}.mp3`; the manifest is produced by
 * `scripts/generate-audio.py` in the web app and shipped verbatim.
 */
object VoiceCatalog {

    const val MANIFEST_ASSET_PATH = "audio/manifest.json"

    /** Clip basename for the "Chờ" announcement. */
    const val CLIP_CHO = "cho"

    /** Clip basename for the "Kinh" (row win) announcement. */
    const val CLIP_KINH = "kinh"

    /**
     * Known-good voices matching the shipped assets, used when the manifest
     * is missing or malformed (mirror of the web app's defensive posture —
     * the first entry is the default voice).
     */
    val FALLBACK_VOICES = listOf(
        Voice("hoai-my", "vi-VN-HoaiMyNeural", "Hoai My (nữ)", "female"),
        Voice("nam-minh", "vi-VN-NamMinhNeural", "Nam Minh (nam)", "male"),
    )

    /** Parse manifest JSON; any shape problem falls back to [FALLBACK_VOICES]. */
    fun parse(json: String): List<Voice> {
        return try {
            val voicesJson = JSONObject(json).getJSONArray("voices")
            val voices = (0 until voicesJson.length()).map { i ->
                val v = voicesJson.getJSONObject(i)
                Voice(
                    id = v.getString("id"),
                    edgeName = v.getString("edgeName"),
                    label = v.getString("label"),
                    gender = v.getString("gender"),
                )
            }
            voices.ifEmpty { FALLBACK_VOICES }
        } catch (_: Exception) {
            FALLBACK_VOICES
        }
    }

    /** Read and parse the bundled manifest; falls back on any IO/parse error. */
    fun load(context: Context): List<Voice> {
        return try {
            val json = context.assets.open(MANIFEST_ASSET_PATH)
                .bufferedReader()
                .use { it.readText() }
            parse(json)
        } catch (_: Exception) {
            FALLBACK_VOICES
        }
    }

    /** First entry is the default — the generator writes them in edge-tts order. */
    fun defaultVoiceId(voices: List<Voice>): String =
        voices.firstOrNull()?.id ?: FALLBACK_VOICES.first().id

    /**
     * Asset path of one clip, relative to the assets root.
     * [clipName] ∈ "1".."90" | [CLIP_CHO] | [CLIP_KINH].
     */
    fun clipPath(voiceId: String, clipName: String): String =
        "audio/$voiceId/$clipName.mp3"
}
