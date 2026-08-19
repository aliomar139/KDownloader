package com.kira.kdownloader.settings;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FilenameTemplateTest {
    private final FilenameTemplate.Values values = new FilenameTemplate.Values(
            "Song Title", "Artist", "2026-07-20", "1080p", "mp4", "abc123");

    @Test public void substitutesAllVariables() {
        assertEquals("Artist - Song Title [1080p]", FilenameTemplate.render("{channel} - {title} [{quality}]", values, 250));
    }

    @Test public void removesCharactersAndroidRejects() {
        String result = FilenameTemplate.render("{title}", values.withTitle("a/b:c*?\"<>|d"), 250);
        for (char c : "\\/:*?\"<>|".toCharArray()) assertTrue(result.indexOf(c) < 0);
    }

    @Test public void validateRejectsUnknownVariable() {
        assertTrue(FilenameTemplate.validate("{title}-{bogus}") instanceof FilenameTemplate.Validation.Invalid);
    }

    @Test public void validateRejectsUnbalancedBraces() {
        assertTrue(FilenameTemplate.validate("{title") instanceof FilenameTemplate.Validation.Invalid);
        assertTrue(FilenameTemplate.validate("title}") instanceof FilenameTemplate.Validation.Invalid);
    }

    @Test public void validateAcceptsAWellFormedTemplate() {
        assertEquals(FilenameTemplate.Validation.Valid, FilenameTemplate.validate("{title} {id}"));
    }

    @Test public void renderFallsBackToTitleForInvalidTemplate() {
        assertEquals("Song Title", FilenameTemplate.render("{unknown}", values, 250));
    }

    @Test public void clampPreservesATrailingExtension() {
        StringBuilder name = new StringBuilder();
        for (int i = 0; i < 300; i++) name.append('x');
        String result = FilenameTemplate.render(name + ".mp4", values, 20);
        assertTrue(result.endsWith(".mp4"));
        assertTrue(result.length() <= 20);
    }

    @Test public void renderNeverReturnsBlank() {
        assertTrue(!FilenameTemplate.render("{title}", values.withTitle("***"), 120).trim().isEmpty());
    }

    @Test public void sanitizeCollapsesRepeatedUnderscoresAndTrims() {
        assertEquals("a_b", FilenameTemplate.sanitize("__a///b__"));
    }

    @Test public void exampleIsNotBlank() {
        assertTrue(!FilenameTemplate.example("{title}-{channel}", 120).trim().isEmpty());
    }
}
