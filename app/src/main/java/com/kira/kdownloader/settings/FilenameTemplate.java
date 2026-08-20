package com.kira.kdownloader.settings;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FilenameTemplate {
    public static final List<String> VARIABLES = Collections.unmodifiableList(
            Arrays.asList("title", "channel", "date", "quality", "format", "id"));
    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 250;
    public static final String FALLBACK = "download";

    private static final Values SAMPLE = new Values(
            "My Great Video", "Creator", "2026-07-20", "1080p", "mp4", "dQw4w9WgXcQ");

    private FilenameTemplate() {}

    public static final class Values {
        private final String title;
        private final String channel;
        private final String date;
        private final String quality;
        private final String format;
        private final String id;

        public Values(String title, String channel, String date, String quality, String format, String id) {
            this.title = title;
            this.channel = channel;
            this.date = date;
            this.quality = quality;
            this.format = format;
            this.id = id;
        }

        public String getTitle() { return title; }
        public String getChannel() { return channel; }
        public String getDate() { return date; }
        public String getQuality() { return quality; }
        public String getFormat() { return format; }
        public String getId() { return id; }

        public Values withTitle(String value) { return new Values(value, channel, date, quality, format, id); }

        Map<String, String> asMap() {
            Map<String, String> values = new LinkedHashMap<>();
            values.put("title", title);
            values.put("channel", channel);
            values.put("date", date);
            values.put("quality", quality);
            values.put("format", format);
            values.put("id", id);
            return values;
        }
    }

    public interface Validation {
        Valid Valid = new Valid();

        final class Valid implements Validation {
            private Valid() {}
        }

        final class Invalid implements Validation {
            private final String reason;

            public Invalid(String reason) { this.reason = reason; }
            public String getReason() { return reason; }
        }
    }

    private static final Validation VALID = Validation.Valid;

    public static Validation validate(String template) {
        if (template.trim().isEmpty()) return new Validation.Invalid("Template cannot be empty");
        int depth = 0;
        for (int i = 0; i < template.length(); i++) {
            char c = template.charAt(i);
            if (c == '{' && ++depth > 1) return new Validation.Invalid("Nested braces are not allowed");
            if (c == '}' && --depth < 0) return new Validation.Invalid("Unmatched '}'");
        }
        if (depth != 0) return new Validation.Invalid("Unmatched '{'");

        int index = 0;
        while (index < template.length()) {
            if (template.charAt(index) != '{') { index++; continue; }
            int end = template.indexOf('}', index + 1);
            String name = template.substring(index + 1, end);
            if (!VARIABLES.contains(name)) return new Validation.Invalid("Unknown variable {" + name + "}");
            index = end + 1;
        }
        return VALID;
    }

    public static String render(String template, Values values, int maxLength) {
        String effective = validate(template) instanceof Validation.Valid ? template : "{title}";
        return clamp(sanitize(substitute(effective, values.asMap())), maxLength);
    }

    public static String example(String template, int maxLength) { return render(template, SAMPLE, maxLength); }

    private static String substitute(String template, Map<String, String> values) {
        StringBuilder result = new StringBuilder(template.length());
        int index = 0;
        while (index < template.length()) {
            char c = template.charAt(index);
            if (c == '{') {
                int end = template.indexOf('}', index + 1);
                if (end != -1) {
                    String replacement = values.get(template.substring(index + 1, end));
                    if (replacement != null) {
                        result.append(replacement);
                        index = end + 1;
                        continue;
                    }
                }
            }
            result.append(c);
            index++;
        }
        return result.toString();
    }

    public static String sanitize(String raw) {
        StringBuilder result = new StringBuilder(raw.length());
        boolean lastUnderscore = false;
        for (int i = 0; i < raw.length(); i++) {
            char c = raw.charAt(i);
            if (isIllegal(c) || c == '_') {
                if (!lastUnderscore) result.append('_');
                lastUnderscore = true;
            } else {
                result.append(c);
                lastUnderscore = false;
            }
        }
        return trimSeparators(result.toString());
    }

    private static boolean isIllegal(char c) {
        return c < 0x20 || "\\/:*?\"<>|".indexOf(c) >= 0;
    }

    private static String clamp(String name, int maxLength) {
        int bound = Math.max(MIN_LENGTH, Math.min(MAX_LENGTH, maxLength));
        String cleaned = name.trim().isEmpty() ? FALLBACK : name;
        if (cleaned.length() <= bound) return cleaned;
        int dot = cleaned.lastIndexOf('.');
        if (dot > 0 && dot < cleaned.length()) {
            String extension = cleaned.substring(dot);
            if (extension.length() >= 2 && extension.length() <= 6) {
                int room = Math.max(1, bound - extension.length());
                String result = trimSeparators(cleaned.substring(0, Math.min(room, dot))) + extension;
                return result.trim().isEmpty() ? FALLBACK : result;
            }
        }
        String result = trimSeparators(cleaned.substring(0, bound));
        return result.trim().isEmpty() ? FALLBACK : result;
    }

    private static String trimSeparators(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && isSeparator(value.charAt(start))) start++;
        while (end > start && isSeparator(value.charAt(end - 1))) end--;
        return value.substring(start, end);
    }

    private static boolean isSeparator(char c) { return c == ' ' || c == '.' || c == '_'; }
}
