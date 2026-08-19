package com.kira.kdownloader.engine;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class ExtractorOptionsTest {
    @Test public void recognizesRegularAndShortenedTikTokHosts() {
        assertEquals(true, ExtractorOptions.isTikTokUrl("https://www.tiktok.com/@user/video/1"));
        assertEquals(true, ExtractorOptions.isTikTokUrl("https://vt.tiktok.com/example/"));
    }

    @Test public void doesNotTrustTikTokLookalikeDomains() {
        assertEquals(false, ExtractorOptions.isTikTokUrl("https://tiktok.com.example.test/video/1"));
    }

    @Test public void recognizesYouTubeHostsWithoutTrustingLookalikes() {
        assertEquals(true, ExtractorOptions.isYouTubeUrl("https://www.youtube.com/watch?v=1"));
        assertEquals(true, ExtractorOptions.isYouTubeUrl("https://youtu.be/1"));
        assertEquals(false, ExtractorOptions.isYouTubeUrl("https://youtube.com.example.test/watch?v=1"));
    }

    @Test public void buildsMobileApiAppInfoFromPersistedInstallIds() {
        assertEquals("tiktok:app_info=7300000000000000001,7300000000000000002",
                ExtractorOptions.tikTokAppInfo(Arrays.asList("7300000000000000001", "7300000000000000002")));
    }
}
