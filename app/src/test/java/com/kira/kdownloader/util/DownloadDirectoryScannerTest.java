package com.kira.kdownloader.util;

import com.kira.kdownloader.data.DownloadEntity;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DownloadDirectoryScannerTest {
    @Test public void mapsDownloadedVideoIntoACompletedHistoryRow() {
        DownloadEntity entity = DownloadDirectoryScanner.entityFrom(
                "My reel.mp4", "video/mp4", "content://media/external/downloads/10", 123_000L);
        assertNotNull(entity);
        assertEquals("My reel", entity.getTitle());
        assertEquals("VIDEO", entity.getKind());
        assertEquals("MP4", entity.getFormatLabel());
        assertEquals(123_000L, entity.getCreatedAt());
    }

    @Test public void recognizesAudioFromItsExtensionWhenMimeTypeIsAbsent() {
        DownloadEntity entity = DownloadDirectoryScanner.entityFrom(
                "Track.mp3", null, "content://media/external/downloads/11", 456_000L);
        assertNotNull(entity);
        assertEquals("AUDIO", entity.getKind());
    }

    @Test public void ignoresNonMediaFiles() {
        assertNull(DownloadDirectoryScanner.entityFrom(
                "notes.txt", "text/plain", "content://media/external/downloads/12", 789_000L));
    }

    @Test public void acceptsCurrentAndPluralDownloadDirectoryNamesRegardlessOfCase() {
        assertEquals(true, DownloadDirectoryScanner.isSupportedRelativePath("Download/KDownloader/"));
        assertEquals(true, DownloadDirectoryScanner.isSupportedRelativePath("download/KDOWNLOADS/nested/"));
    }

    @Test public void rejectsLookalikeAndUnrelatedDownloadDirectories() {
        assertEquals(false, DownloadDirectoryScanner.isSupportedRelativePath("Download/KDownloads-old/"));
        assertEquals(false, DownloadDirectoryScanner.isSupportedRelativePath("Movies/KDownloads/"));
    }

    @Test public void recognizesLegacyAbsolutePathsForBothDirectoryNames() {
        assertEquals(true, DownloadDirectoryScanner.isSupportedLegacyPath("/storage/emulated/0/Download/KDownloader/video.mp4"));
        assertEquals(true, DownloadDirectoryScanner.isSupportedLegacyPath("/storage/emulated/0/Download/KDownloads/audio.mp3"));
    }
}
