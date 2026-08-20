package com.kira.kdownloader.engine;

import java.net.URI;
import java.util.Locale;

public final class ExtractorOptions {
    private ExtractorOptions() {}

    public static boolean isTikTokUrl(String sourceUrl) {
        return hostMatches(sourceUrl, "tiktok.com");
    }

    public static boolean isYouTubeUrl(String sourceUrl) {
        return hostMatches(sourceUrl, "youtube.com") || hostMatches(sourceUrl, "youtu.be");
    }

    public static boolean isInstagramUrl(String sourceUrl) {
        return hostMatches(sourceUrl, "instagram.com") || hostMatches(sourceUrl, "instagr.am");
    }

    public static boolean isFacebookUrl(String sourceUrl) {
        return hostMatches(sourceUrl, "facebook.com")
                || hostMatches(sourceUrl, "fb.com")
                || hostMatches(sourceUrl, "fb.watch");
    }

    private static boolean hostMatches(String sourceUrl, String expectedHost) {
        try {
            String host = new URI(sourceUrl.trim()).getHost();
            if (host == null) return false;
            host = host.toLowerCase(Locale.ROOT);
            return host.equals(expectedHost) || host.endsWith("." + expectedHost);
        } catch (Exception ignored) {
            return false;
        }
    }
}
