package com.kira.kdownloader.util;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RecentUrls {
    private static final String PREFS = "recent_urls";
    private static final String KEY = "urls";
    private static final int MAX = 3;

    private RecentUrls() {}

    public static List<String> all(Context context) {
        String stored = prefs(context).getString(KEY, "");
        if (stored == null || stored.isEmpty()) return Collections.emptyList();
        List<String> result = new ArrayList<>();
        for (String value : stored.split("\\n")) {
            if (!value.trim().isEmpty()) result.add(value);
            if (result.size() == MAX) break;
        }
        return result;
    }

    public static void add(Context context, String url) {
        String trimmed = url.trim();
        if (trimmed.isEmpty()) return;
        List<String> updated = new ArrayList<>();
        updated.add(trimmed);
        for (String value : all(context)) {
            if (!trimmed.equals(value) && updated.size() < MAX) updated.add(value);
        }
        prefs(context).edit().putString(KEY, join(updated)).apply();
    }

    public static void remove(Context context, String url) {
        List<String> updated = new ArrayList<>();
        for (String value : all(context)) if (!url.equals(value)) updated.add(value);
        prefs(context).edit().putString(KEY, join(updated)).apply();
    }

    private static String join(List<String> values) {
        StringBuilder result = new StringBuilder();
        for (String value : values) {
            if (result.length() > 0) result.append('\n');
            result.append(value);
        }
        return result.toString();
    }

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
