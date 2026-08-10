package com.kira.kdownloader.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FilenameTemplateTest {

    private val values = FilenameTemplate.Values(
        title = "Song Title",
        channel = "Artist",
        date = "2026-07-20",
        quality = "1080p",
        format = "mp4",
        id = "abc123",
    )

    @Test
    fun `substitutes all variables`() {
        val result = FilenameTemplate.render("{channel} - {title} [{quality}]", values, 250)
        assertEquals("Artist - Song Title [1080p]", result)
    }

    @Test
    fun `removes characters android rejects`() {
        val result = FilenameTemplate.render("{title}", values.copy(title = "a/b:c*?\"<>|d"), 250)
        assertTrue(result.none { it in "\\/:*?\"<>|" })
    }

    @Test
    fun `validate rejects unknown variable`() {
        val validation = FilenameTemplate.validate("{title}-{bogus}")
        assertTrue(validation is FilenameTemplate.Validation.Invalid)
    }

    @Test
    fun `validate rejects unbalanced braces`() {
        assertTrue(FilenameTemplate.validate("{title") is FilenameTemplate.Validation.Invalid)
        assertTrue(FilenameTemplate.validate("title}") is FilenameTemplate.Validation.Invalid)
    }

    @Test
    fun `validate accepts a well formed template`() {
        assertEquals(FilenameTemplate.Validation.Valid, FilenameTemplate.validate("{title} {id}"))
    }

    @Test
    fun `render falls back to title for invalid template`() {
        val result = FilenameTemplate.render("{unknown}", values, 250)
        assertEquals("Song Title", result)
    }

    @Test
    fun `clamp preserves a trailing extension`() {
        val long = "x".repeat(300) + ".mp4"
        val result = FilenameTemplate.render(long, values, 20)
        assertTrue(result.endsWith(".mp4"))
        assertTrue(result.length <= 20)
    }

    @Test
    fun `render never returns blank`() {
        val result = FilenameTemplate.render("{title}", values.copy(title = "***"), 120)
        assertTrue(result.isNotBlank())
    }

    @Test
    fun `sanitize collapses repeated underscores and trims`() {
        assertEquals("a_b", FilenameTemplate.sanitize("__a///b__"))
    }

    @Test
    fun `example is not blank`() {
        assertTrue(FilenameTemplate.example("{title}-{channel}", 120).isNotBlank())
    }
}
