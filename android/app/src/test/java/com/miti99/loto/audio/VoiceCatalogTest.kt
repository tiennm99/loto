package com.miti99.loto.audio

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceCatalogTest {

    @Test
    fun `parses the real bundled manifest`() {
        // The same file Gradle mounts into assets/ — read straight from the
        // web app's static dir (test working dir is the app module).
        val manifest = File(AUDIO_ASSETS_DIR, VoiceCatalog.MANIFEST_ASSET_PATH)
        assertTrue("expected ${manifest.absolutePath} to exist", manifest.exists())
        val voices = VoiceCatalog.parse(manifest.readText())
        assertEquals(listOf("hoai-my", "nam-minh"), voices.map { it.id })
        assertEquals("vi-VN-HoaiMyNeural", voices[0].edgeName)
        assertEquals("female", voices[0].gender)
        assertEquals("Nam Minh (nam)", voices[1].label)
        // SettingsRepository.defaults uses the fallback default before the
        // manifest has loaded; it must name the same voice.
        assertEquals(
            VoiceCatalog.defaultVoiceId(VoiceCatalog.FALLBACK_VOICES),
            VoiceCatalog.defaultVoiceId(voices),
        )
    }

    @Test
    fun `falls back to the known voices on corrupt JSON`() {
        assertEquals(VoiceCatalog.FALLBACK_VOICES, VoiceCatalog.parse("{not json"))
    }

    @Test
    fun `falls back when the voices key is missing`() {
        assertEquals(VoiceCatalog.FALLBACK_VOICES, VoiceCatalog.parse("""{"other": 1}"""))
    }

    @Test
    fun `falls back when the voices array is empty`() {
        assertEquals(VoiceCatalog.FALLBACK_VOICES, VoiceCatalog.parse("""{"voices": []}"""))
    }

    @Test
    fun `falls back when an entry misses a field`() {
        assertEquals(
            VoiceCatalog.FALLBACK_VOICES,
            VoiceCatalog.parse("""{"voices": [{"id": "x"}]}"""),
        )
    }

    @Test
    fun `default voice is the first manifest entry`() {
        val voices = listOf(
            Voice("nam-minh", "vi-VN-NamMinhNeural", "Nam Minh (nam)", "male"),
            Voice("hoai-my", "vi-VN-HoaiMyNeural", "Hoai My (nữ)", "female"),
        )
        assertEquals("nam-minh", VoiceCatalog.defaultVoiceId(voices))
        assertEquals("hoai-my", VoiceCatalog.defaultVoiceId(emptyList()))
    }

    @Test
    fun `clipPath builds the asset-relative mp3 path`() {
        assertEquals("hoai-my/42.mp3", VoiceCatalog.clipPath("hoai-my", "42"))
        assertEquals("nam-minh/cho.mp3", VoiceCatalog.clipPath("nam-minh", VoiceCatalog.CLIP_CHO))
        assertEquals("hoai-my/kinh.mp3", VoiceCatalog.clipPath("hoai-my", VoiceCatalog.CLIP_KINH))
    }

    @Test
    fun `every clip path resolves inside the mounted assets dir`() {
        // Pins the build.gradle.kts mount (web/static/audio as the assets
        // root) to the paths the player requests at runtime.
        val voices = VoiceCatalog.parse(File(AUDIO_ASSETS_DIR, VoiceCatalog.MANIFEST_ASSET_PATH).readText())
        val clips = (1..90).map { it.toString() } + VoiceCatalog.CLIP_CHO + VoiceCatalog.CLIP_KINH
        val missing = voices.flatMap { voice ->
            clips.map { VoiceCatalog.clipPath(voice.id, it) }
                .filterNot { File(AUDIO_ASSETS_DIR, it).isFile }
        }
        assertEquals(emptyList<String>(), missing)
    }

    private companion object {
        /** Mirrors `assets.srcDir` in `app/build.gradle.kts` (test working dir is the app module). */
        const val AUDIO_ASSETS_DIR = "../../web/static/audio"
    }
}
