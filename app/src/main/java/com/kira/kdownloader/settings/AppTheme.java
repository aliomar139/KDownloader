package com.kira.kdownloader.settings;

public enum AppTheme implements SettingOption {
    SYSTEM("system", "Follow system"), LIGHT("light", "Light"), DARK("dark", "Dark");
    private final String key; private final String label;
    AppTheme(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
