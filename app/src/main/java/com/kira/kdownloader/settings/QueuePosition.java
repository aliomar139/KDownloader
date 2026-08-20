package com.kira.kdownloader.settings;

public enum QueuePosition implements SettingOption {
    TOP("top", "Top of queue"), BOTTOM("bottom", "Bottom of queue");
    private final String key; private final String label;
    QueuePosition(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
