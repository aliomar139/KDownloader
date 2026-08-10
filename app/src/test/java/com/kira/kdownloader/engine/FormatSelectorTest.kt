package com.kira.kdownloader.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatSelectorTest {
    private fun video(id: String, height: Int) = FormatInput(
        formatId = id,
        ext = "mp4",
        height = height,
        vcodec = "avc1",
        acodec = "mp4a",
    )

    @Test
    fun `returns video choices high-to-low plus one audio choice`() {
        val choices = FormatSelector.choices(
            listOf(video("a", 360), video("b", 1080), video("c", 720)),
        )

        assertEquals(
            listOf("1080p", "720p", "360p", "Audio (mp3)"),
            choices.map { it.label },
        )
        assertEquals(FormatSelector.Kind.AUDIO, choices.last().kind)
    }

    @Test
    fun `dedupes repeated heights`() {
        val choices = FormatSelector.choices(listOf(video("a", 720), video("b", 720)))

        assertEquals(listOf("720p", "Audio (mp3)"), choices.map { it.label })
    }

    @Test
    fun `audio-only source yields just the audio choice`() {
        val choices = FormatSelector.choices(
            listOf(FormatInput("a", "m4a", null, "none", "mp4a")),
        )

        assertEquals(1, choices.size)
        assertEquals("Audio (mp3)", choices.first().label)
        assertEquals(FormatSelector.Kind.AUDIO, choices.first().kind)
    }

    @Test
    fun `video choice uses height-capped selector`() {
        val choice = FormatSelector.choices(listOf(video("a", 720))).first()

        assertEquals(
            "bestvideo[height<=720]+bestaudio/best[height<=720]",
            choice.formatSelector,
        )
    }

    @Test
    fun `ignores heights outside the curated set`() {
        val choices = FormatSelector.choices(listOf(video("a", 1440), video("b", 240)))

        assertEquals(listOf("Audio (mp3)"), choices.map { it.label })
    }

    @Test
    fun `instagram exposes one best video quality plus audio`() {
        val choices = FormatSelector.choices(
            formats = listOf(video("low", 640), video("best", 960)),
            sourceUrl = "https://www.instagram.com/reel/example/",
        )

        assertEquals(listOf("960p", "Audio (mp3)"), choices.map { it.label })
        assertEquals(FormatSelector.FALLBACK_VIDEO_SELECTOR, choices.first().formatSelector)
    }

    @Test
    fun `tiktok uses a separate audio stream when the best video has no audio`() {
        val choices = FormatSelector.choices(
            formats = listOf(
                video("combined", 720),
                video("video-only", 1080).copy(acodec = "none"),
            ),
            sourceUrl = "https://vt.tiktok.com/example/",
        )

        assertEquals(listOf("1080p", "Audio (mp3)"), choices.map { it.label })
        assertEquals("video-only+bestaudio/best", choices.first().formatSelector)
    }

    @Test
    fun `tiktok reuses the best combined direct format instead of extracting twice`() {
        val choices = FormatSelector.choices(
            formats = listOf(
                video("combined", 720).copy(
                    url = "https://video.example/tiktok.mp4",
                    httpHeaders = mapOf("Referer" to "https://www.tiktok.com/"),
                ),
                video("video-only", 1080).copy(
                    acodec = "none",
                    url = "https://video.example/tiktok-video-only.mp4",
                ),
            ),
            sourceUrl = "https://www.tiktok.com/@example/video/1",
        )

        val videoChoice = choices.first()
        assertEquals("720p", videoChoice.label)
        assertEquals("https://video.example/tiktok.mp4", videoChoice.directUrl)
        assertEquals("https://www.tiktok.com/", videoChoice.httpHeaders["Referer"])
    }

    @Test
    fun `facebook reel exposes one best video quality plus audio`() {
        val choices = FormatSelector.choices(
            formats = listOf(video("low", 540), video("best", 1350)),
            sourceUrl = "https://www.facebook.com/reel/123456789/",
        )

        assertEquals(listOf("1350p", "Audio (mp3)"), choices.map { it.label })
        assertEquals(FormatSelector.FALLBACK_VIDEO_SELECTOR, choices.first().formatSelector)
    }

    @Test
    fun `facebook post merges audio when its best video is video only`() {
        val choices = FormatSelector.choices(
            formats = listOf(
                video("combined", 640),
                video("video-only", 1280).copy(acodec = "none"),
            ),
            sourceUrl = "https://m.facebook.com/example/posts/123456789/",
        )

        assertEquals(listOf("1280p", "Audio (mp3)"), choices.map { it.label })
        assertEquals(FormatSelector.FALLBACK_VIDEO_SELECTOR, choices.first().formatSelector)
    }

    @Test
    fun `fb watch links use the facebook single quality behavior`() {
        val choices = FormatSelector.choices(
            formats = listOf(video("watch-best", 960)),
            sourceUrl = "https://fb.watch/example/",
        )

        assertEquals(FormatSelector.FALLBACK_VIDEO_SELECTOR, choices.first().formatSelector)
        assertEquals(FormatSelector.Kind.VIDEO, choices.first().kind)
    }

    @Test
    fun `single quality detection does not trust lookalike domains`() {
        assertEquals(
            false,
            FormatSelector.usesSingleVideoQuality("https://instagram.com.example.test/reel/1"),
        )
        assertEquals(
            false,
            FormatSelector.usesSingleVideoQuality("https://facebook.com.example.test/reel/1"),
        )
    }
}
