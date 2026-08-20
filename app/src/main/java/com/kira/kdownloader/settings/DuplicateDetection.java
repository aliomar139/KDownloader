package com.kira.kdownloader.settings;

public enum DuplicateDetection implements SettingOption {
    SOURCE_URL("url", "Source URL"), MEDIA_ID("id", "Media ID"), FILENAME("filename", "Existing filename");
    private final String key; private final String label;
    DuplicateDetection(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
