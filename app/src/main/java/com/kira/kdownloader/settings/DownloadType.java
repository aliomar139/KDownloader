package com.kira.kdownloader.settings;

public enum DownloadType implements SettingOption {
    VIDEO("video", "Video"), AUDIO_ONLY("audio", "Audio only"), ASK("ask", "Ask every time");
    private final String key; private final String label;
    DownloadType(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
