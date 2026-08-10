package com.kira.kdownloader.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsCodecTest {

    @Test
    fun `round trips a flat string map`() {
        val map = linkedMapOf(
            "download.type" to "audio",
            "storage.filename_template" to "{title} - {channel}",
            "behavior.max_parallel" to "3",
        )
        val decoded = SettingsCodec.decode(SettingsCodec.encode(map))
        assertEquals(map, decoded)
    }

    @Test
    fun `excludes sensitive keys`() {
        val map = mapOf(
            SettingsKeys.NW_PROXY_USER to "secret_user",
            SettingsKeys.NW_PROXY_HOST to "proxy.example.com",
        )
        val json = SettingsCodec.encode(map)
        assertFalse(json.contains("secret_user"))
        assertTrue(json.contains("proxy.example.com"))
    }

    @Test
    fun `escapes special characters`() {
        val map = mapOf("k" to "line1\nline2 \"quoted\" \\slash")
        val decoded = SettingsCodec.decode(SettingsCodec.encode(map))
        assertEquals(map, decoded)
    }

    @Test
    fun `decode coerces scalar json values to strings`() {
        val decoded = SettingsCodec.decode("""{"a": 42, "b": true, "c": null}""")
        assertEquals("42", decoded?.get("a"))
        assertEquals("true", decoded?.get("b"))
        assertEquals("", decoded?.get("c"))
    }

    @Test
    fun `decode returns null for non-object input`() {
        assertNull(SettingsCodec.decode("not json"))
        assertNull(SettingsCodec.decode("[1,2,3]"))
    }
}
