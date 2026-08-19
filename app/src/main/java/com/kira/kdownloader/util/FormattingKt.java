package com.kira.kdownloader.util;

import java.util.Locale;

public final class FormattingKt {
    private FormattingKt() {}

    public static String formatBytes(Long bytes) {
        if (bytes == null || bytes <= 0) return "";
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        double value = bytes;
        int unit = 0;
        while (value >= 1024 && unit < units.length - 1) {
            value /= 1024;
            unit++;
        }
        return unit == 0 ? bytes + " B" : String.format(Locale.US, "%.1f %s", value, units[unit]);
    }

    public static String formatDuration(int totalSeconds) {
        if (totalSeconds <= 0) return "";
        int hours = totalSeconds / 3600;
        int minutes = (totalSeconds % 3600) / 60;
        int seconds = totalSeconds % 60;
        return hours > 0
                ? String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
                : String.format(Locale.US, "%d:%02d", minutes, seconds);
    }

    public static String formatEta(long seconds) {
        if (seconds < 0) return "";
        if (seconds < 60) return seconds + "s";
        if (seconds < 3600) return seconds / 60 + "m " + seconds % 60 + "s";
        return seconds / 3600 + "h " + (seconds % 3600) / 60 + "m";
    }
}
