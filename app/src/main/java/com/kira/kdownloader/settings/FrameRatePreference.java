package com.kira.kdownloader.settings;

public enum FrameRatePreference implements SettingOption {
    BEST("best", "Best available"), PREFER_60("60", "Prefer 60 FPS"), PREFER_30("30", "Prefer 30 FPS");
    private final String key; private final String label;
    FrameRatePreference(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
