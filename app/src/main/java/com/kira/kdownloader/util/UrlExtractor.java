package com.kira.kdownloader.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlExtractor {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);

    private UrlExtractor() {}

    public static String fromText(String text) {
        if (text == null) return "";
        Matcher matcher = URL_PATTERN.matcher(text);
        if (!matcher.find()) return cleanUrl(text.trim());
        String value = matcher.group();
        int end = value.length();
        while (end > 0 && ".,;:)]}\"'".indexOf(value.charAt(end - 1)) >= 0) end--;
        return cleanUrl(value.substring(0, end));
    }

    public static String cleanUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isEmpty()) return "";
        String url = rawUrl.trim();
        int queryIndex = url.indexOf('?');
        if (queryIndex < 0) return url;

        String base = url.substring(0, queryIndex);
        String query = url.substring(queryIndex + 1);

        if (base.contains("tiktok.com") || base.contains("douyin.com")) {
            return base;
        }

        String[] params = query.split("&");
        StringBuilder cleanedQuery = new StringBuilder();
        for (String param : params) {
            String lower = param.toLowerCase(java.util.Locale.ROOT);
            if (lower.startsWith("utm_") || lower.startsWith("si=") || lower.startsWith("feature=")
                    || lower.startsWith("fbclid=") || lower.startsWith("igshid=")
                    || lower.startsWith("_r=") || lower.startsWith("share_app_id=")) {
                continue;
            }
            if (cleanedQuery.length() > 0) cleanedQuery.append('&');
            cleanedQuery.append(param);
        }
        return cleanedQuery.length() > 0 ? base + "?" + cleanedQuery : base;
    }
}
