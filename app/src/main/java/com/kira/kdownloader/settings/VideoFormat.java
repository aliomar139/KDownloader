package com.kira.kdownloader.settings;

public enum VideoFormat implements SettingOption {
    MP4("mp4", "MP4"), WEBM("webm", "WebM"), BEST_COMPATIBLE("best_compatible", "Best compatible format");
    private final String key; private final String label;
    VideoFormat(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
