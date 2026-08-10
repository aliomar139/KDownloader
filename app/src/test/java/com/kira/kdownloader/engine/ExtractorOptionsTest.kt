package com.kira.kdownloader.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class ExtractorOptionsTest {
    @Test
    fun `recognizes regular and shortened TikTok hosts`() {
        assertEquals(true, ExtractorOptions.isTikTokUrl("https://www.tiktok.com/@user/video/1"))
        assertEquals(true, ExtractorOptions.isTikTokUrl("https://vt.tiktok.com/example/"))
    }

    @Test
    fun `does not trust TikTok lookalike domains`() {
        assertEquals(
            false,
            ExtractorOptions.isTikTokUrl("https://tiktok.com.example.test/video/1"),
        )
    }

    @Test
    fun `recognizes YouTube hosts without trusting lookalikes`() {
        assertEquals(true, ExtractorOptions.isYouTubeUrl("https://www.youtube.com/watch?v=1"))
        assertEquals(true, ExtractorOptions.isYouTubeUrl("https://youtu.be/1"))
        assertEquals(
            false,
            ExtractorOptions.isYouTubeUrl("https://youtube.com.example.test/watch?v=1"),
        )
    }

    @Test
    fun `builds mobile API app info from persisted install IDs`() {
        assertEquals(
            "tiktok:app_info=7300000000000000001,7300000000000000002",
            ExtractorOptions.tikTokAppInfo(
                listOf("7300000000000000001", "7300000000000000002"),
            ),
        )
    }
}
