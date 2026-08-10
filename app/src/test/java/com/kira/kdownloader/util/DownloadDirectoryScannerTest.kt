package com.kira.kdownloader.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DownloadDirectoryScannerTest {
    @Test
    fun `maps downloaded video into a completed history row`() {
        val entity = DownloadDirectoryScanner.entityFrom(
            displayName = "My reel.mp4",
            mimeType = "video/mp4",
            fileUri = "content://media/external/downloads/10",
            createdAt = 123_000L,
        )

        requireNotNull(entity)
        assertEquals("My reel", entity.title)
        assertEquals("VIDEO", entity.kind)
        assertEquals("MP4", entity.formatLabel)
        assertEquals(123_000L, entity.createdAt)
    }

    @Test
    fun `recognizes audio from its extension when mime type is absent`() {
        val entity = DownloadDirectoryScanner.entityFrom(
            displayName = "Track.mp3",
            mimeType = null,
            fileUri = "content://media/external/downloads/11",
            createdAt = 456_000L,
        )

        requireNotNull(entity)
        assertEquals("AUDIO", entity.kind)
    }

    @Test
    fun `ignores non-media files`() {
        assertNull(
            DownloadDirectoryScanner.entityFrom(
                displayName = "notes.txt",
                mimeType = "text/plain",
                fileUri = "content://media/external/downloads/12",
                createdAt = 789_000L,
            ),
        )
    }

    @Test
    fun `accepts current and plural download directory names regardless of case`() {
        assertEquals(
            true,
            DownloadDirectoryScanner.isSupportedRelativePath("Download/KDownloader/"),
        )
        assertEquals(
            true,
            DownloadDirectoryScanner.isSupportedRelativePath("download/KDOWNLOADS/nested/"),
        )
    }

    @Test
    fun `rejects lookalike and unrelated download directories`() {
        assertEquals(
            false,
            DownloadDirectoryScanner.isSupportedRelativePath("Download/KDownloads-old/"),
        )
        assertEquals(
            false,
            DownloadDirectoryScanner.isSupportedRelativePath("Movies/KDownloads/"),
        )
    }

    @Test
    fun `recognizes legacy absolute paths for both directory names`() {
        assertEquals(
            true,
            DownloadDirectoryScanner.isSupportedLegacyPath(
                "/storage/emulated/0/Download/KDownloader/video.mp4",
            ),
        )
        assertEquals(
            true,
            DownloadDirectoryScanner.isSupportedLegacyPath(
                "/storage/emulated/0/Download/KDownloads/audio.mp3",
            ),
        )
    }
}
