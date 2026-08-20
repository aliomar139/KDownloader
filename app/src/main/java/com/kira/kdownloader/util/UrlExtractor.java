package com.kira.kdownloader.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UrlExtractor {
    private static final Pattern URL_PATTERN = Pattern.compile("https?://\\S+", Pattern.CASE_INSENSITIVE);

    private UrlExtractor() {}

    public static String fromText(String text) {
        Matcher matcher = URL_PATTERN.matcher(text);
        if (!matcher.find()) return text.trim();
        String value = matcher.group();
        int end = value.length();
        while (end > 0 && ".,;:)]}".indexOf(value.charAt(end - 1)) >= 0) end--;
        return value.substring(0, end);
    }
}
