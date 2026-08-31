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
        val manifest = File("../../web/static/audio/manifest.json")
        assertTrue("expected ${manifest.absolutePath} to exist", manifest.exists())
        val voices = VoiceCatalog.parse(manifest.readText())
        assertEquals(listOf("hoai-my", "nam-minh"), voices.map { it.id })
        assertEquals("vi-VN-HoaiMyNeural", voices[0].edgeName)
        assertEquals("female", voices[0].gender)
        assertEquals("Nam Minh (nam)", voices[1].label)
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
        assertEquals("audio/hoai-my/42.mp3", VoiceCatalog.clipPath("hoai-my", "42"))
        assertEquals("audio/nam-minh/cho.mp3", VoiceCatalog.clipPath("nam-minh", VoiceCatalog.CLIP_CHO))
        assertEquals("audio/hoai-my/kinh.mp3", VoiceCatalog.clipPath("hoai-my", VoiceCatalog.CLIP_KINH))
    }
}
