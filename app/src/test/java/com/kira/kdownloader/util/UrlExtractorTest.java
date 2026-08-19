package com.kira.kdownloader.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UrlExtractorTest {
    @Test public void keepsAPlainUrl() {
        assertEquals("https://www.youtube.com/watch?v=abc123", UrlExtractor.fromText("https://www.youtube.com/watch?v=abc123"));
    }

    @Test public void extractsATikTokUrlFromSharedText() {
        assertEquals("https://vt.tiktok.com/ZSExample/", UrlExtractor.fromText("Watch this video https://vt.tiktok.com/ZSExample/ shared via TikTok"));
    }

    @Test public void removesSentencePunctuationAfterAUrl() {
        assertEquals("https://example.com/video", UrlExtractor.fromText("Try https://example.com/video)."));
    }
}
