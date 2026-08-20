package com.kira.kdownloader.settings;

public enum SubtitleTypePreference implements SettingOption {
    MANUAL_ONLY("manual", "Manual captions only"), ALLOW_GENERATED("generated", "Allow automatically generated"),
    PREFER_MANUAL("prefer_manual", "Prefer manual, fall back to generated");
    private final String key; private final String label;
    SubtitleTypePreference(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
