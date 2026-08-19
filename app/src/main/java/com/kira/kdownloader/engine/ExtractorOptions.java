package com.kira.kdownloader.engine;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public final class ExtractorOptions {
    private static final Pattern INSTALL_ID_PATTERN = Pattern.compile("\\d{19}");

    private ExtractorOptions() {}

    public static boolean isTikTokUrl(String sourceUrl) {
        return hostMatches(sourceUrl, "tiktok.com");
    }

    public static boolean isYouTubeUrl(String sourceUrl) {
        return hostMatches(sourceUrl, "youtube.com") || hostMatches(sourceUrl, "youtu.be");
    }

    public static String tikTokAppInfo(List<String> installIds) {
        if (installIds.isEmpty()) throw new IllegalArgumentException("At least one TikTok install ID is required");
        StringBuilder value = new StringBuilder("tiktok:app_info=");
        for (String id : installIds) {
            if (!INSTALL_ID_PATTERN.matcher(id).matches()) {
                throw new IllegalArgumentException("TikTok install IDs must contain exactly 19 digits");
            }
            if (value.charAt(value.length() - 1) != '=') value.append(',');
            value.append(id);
        }
        return value.toString();
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
