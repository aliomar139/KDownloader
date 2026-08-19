package com.kira.kdownloader.engine;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class YtDlpVersionPolicy {
    private static final Pattern DATE_PATTERN = Pattern.compile("(\\d{4})\\.(\\d{1,2})\\.(\\d{1,2})");

    private YtDlpVersionPolicy() {}

    static boolean shouldInstallBundled(String installedVersion, String bundledVersion) {
        if (bundledVersion.equals(installedVersion)) return false;
        Integer installedDate = installedVersion == null ? null : dateKey(installedVersion);
        if (installedDate == null) return true;
        Integer bundledDate = dateKey(bundledVersion);
        return bundledDate == null ? !installedVersion.equals(bundledVersion) : installedDate < bundledDate;
    }

    private static Integer dateKey(String version) {
        Matcher match = DATE_PATTERN.matcher(version);
        if (!match.find()) return null;
        try {
            return Integer.parseInt(match.group(1)) * 10_000
                    + Integer.parseInt(match.group(2)) * 100
                    + Integer.parseInt(match.group(3));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
