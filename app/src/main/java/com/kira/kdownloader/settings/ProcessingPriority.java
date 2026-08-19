package com.kira.kdownloader.settings;

public enum ProcessingPriority implements SettingOption {
    FASTER("faster", "Faster processing"), BALANCED("balanced", "Balanced"), LOWER_BATTERY("battery", "Lower battery usage");
    private final String key; private final String label;
    ProcessingPriority(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
