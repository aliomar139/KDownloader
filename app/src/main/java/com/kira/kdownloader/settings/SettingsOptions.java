package com.kira.kdownloader.settings;

public final class SettingsOptions {
    private SettingsOptions() {}

    public static <T extends Enum<T> & SettingOption> T optionFromKey(T[] values, String key, T defaultValue) {
        for (T value : values) if (value.getKey().equals(key)) return value;
        return defaultValue;
    }
}
