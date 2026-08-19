package com.kira.kdownloader.settings;

public enum AudioFormat implements SettingOption {
    MP3("mp3", "MP3"), M4A("m4a", "M4A"), OPUS("opus", "Opus"), ORIGINAL("original", "Original audio");
    private final String key; private final String label;
    AudioFormat(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
