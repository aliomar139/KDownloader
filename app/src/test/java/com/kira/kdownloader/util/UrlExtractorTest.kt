package com.kira.kdownloader.util

import org.junit.Assert.assertEquals
import org.junit.Test

class UrlExtractorTest {
    @Test
    fun `keeps a plain URL`() {
        assertEquals(
            "https://www.youtube.com/watch?v=abc123",
            UrlExtractor.fromText("https://www.youtube.com/watch?v=abc123"),
        )
    }

    @Test
    fun `extracts a TikTok URL from shared text`() {
        assertEquals(
            "https://vt.tiktok.com/ZSExample/",
            UrlExtractor.fromText("Watch this video https://vt.tiktok.com/ZSExample/ shared via TikTok"),
        )
    }

    @Test
    fun `removes sentence punctuation after a URL`() {
        assertEquals(
            "https://example.com/video",
            UrlExtractor.fromText("Try https://example.com/video)."),
        )
    }
}
