package com.kira.kdownloader.settings;

public enum VideoQuality implements SettingOption {
    BEST("best", "Best available"), P2160("2160", "2160p"), P1440("1440", "1440p"),
    P1080("1080", "1080p"), P720("720", "720p"), P480("480", "480p"),
    P360("360", "360p"), ASK("ask", "Ask every time");
    private final String key; private final String label;
    VideoQuality(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
