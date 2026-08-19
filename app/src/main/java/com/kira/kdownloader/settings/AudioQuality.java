package com.kira.kdownloader.settings;

public enum AudioQuality implements SettingOption {
    BEST("best", "Best available"), K320("320", "320 kbps"), K256("256", "256 kbps"),
    K192("192", "192 kbps"), K128("128", "128 kbps"), K96("96", "96 kbps");
    private final String key; private final String label;
    AudioQuality(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
