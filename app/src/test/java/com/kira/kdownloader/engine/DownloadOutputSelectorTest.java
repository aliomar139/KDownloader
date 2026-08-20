package com.kira.kdownloader.engine;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DownloadOutputSelectorTest {
    @Rule public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test public void videoSelectionIgnoresAudioAndIntermediateDashStreams() throws IOException {
        mediaFile("reel.f123.mp4", 300L);
        mediaFile("reel.m4a", 400L);
        File completed = mediaFile("reel.mp4", 100L);
        assertEquals(completed, DownloadOutputSelector.select(Arrays.asList(temporaryFolder.getRoot().listFiles()), FormatSelector.Kind.VIDEO));
    }

    @Test public void videoSelectionFailsInsteadOfPublishingAnAudioOnlyResult() throws IOException {
        mediaFile("reel.m4a", 100L);
        mediaFile("reel.f123.mp4", 100L);
        assertNull(DownloadOutputSelector.select(Arrays.asList(temporaryFolder.getRoot().listFiles()), FormatSelector.Kind.VIDEO));
    }

    @Test public void audioSelectionPrefersTheConvertedMp3() throws IOException {
        mediaFile("track.m4a", 500L);
        File mp3 = mediaFile("track.mp3", 100L);
        assertEquals(mp3, DownloadOutputSelector.select(Arrays.asList(temporaryFolder.getRoot().listFiles()), FormatSelector.Kind.AUDIO));
    }

    private File mediaFile(String name, long modifiedAt) throws IOException {
        File file = temporaryFolder.newFile(name);
        Files.write(file.toPath(), "media".getBytes(StandardCharsets.UTF_8));
        file.setLastModified(modifiedAt);
        return file;
    }
}
