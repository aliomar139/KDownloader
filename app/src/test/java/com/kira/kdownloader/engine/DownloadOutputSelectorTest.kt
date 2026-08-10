package com.kira.kdownloader.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class DownloadOutputSelectorTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun `video selection ignores audio and intermediate DASH streams`() {
        mediaFile("reel.f123.mp4", modifiedAt = 300L)
        mediaFile("reel.m4a", modifiedAt = 400L)
        val completedVideo = mediaFile("reel.mp4", modifiedAt = 100L)

        assertEquals(
            completedVideo,
            DownloadOutputSelector.select(
                temporaryFolder.root.listFiles().orEmpty().toList(),
                FormatSelector.Kind.VIDEO,
            ),
        )
    }

    @Test
    fun `video selection fails instead of publishing an audio-only result`() {
        mediaFile("reel.m4a")
        mediaFile("reel.f123.mp4")

        assertNull(
            DownloadOutputSelector.select(
                temporaryFolder.root.listFiles().orEmpty().toList(),
                FormatSelector.Kind.VIDEO,
            ),
        )
    }

    @Test
    fun `audio selection prefers the converted mp3`() {
        mediaFile("track.m4a", modifiedAt = 500L)
        val mp3 = mediaFile("track.mp3", modifiedAt = 100L)

        assertEquals(
            mp3,
            DownloadOutputSelector.select(
                temporaryFolder.root.listFiles().orEmpty().toList(),
                FormatSelector.Kind.AUDIO,
            ),
        )
    }

    private fun mediaFile(name: String, modifiedAt: Long = 100L): File =
        temporaryFolder.newFile(name).apply {
            writeText("media")
            setLastModified(modifiedAt)
        }
}
