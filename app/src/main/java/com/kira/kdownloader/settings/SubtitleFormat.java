package com.kira.kdownloader.settings;

public enum SubtitleFormat implements SettingOption {
    SRT("srt", "SRT"), VTT("vtt", "VTT"), BEST("best", "Best available");
    private final String key; private final String label;
    SubtitleFormat(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
