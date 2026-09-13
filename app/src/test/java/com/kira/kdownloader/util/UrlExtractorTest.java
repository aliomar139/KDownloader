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

    @Test public void stripsTrackingParametersFromTikTokAndSocialUrls() {
        assertEquals("https://www.tiktok.com/@user/video/1234567890",
                UrlExtractor.fromText("Check this out: https://www.tiktok.com/@user/video/1234567890?is_from_webapp=1&sender_device=pc&share_app_id=1233"));
        assertEquals("https://www.youtube.com/watch?v=abc123",
                UrlExtractor.fromText("https://www.youtube.com/watch?v=abc123&si=xyz123&feature=shared"));
    }
}
