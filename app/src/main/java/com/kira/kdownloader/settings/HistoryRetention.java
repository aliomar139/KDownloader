package com.kira.kdownloader.settings;

public enum HistoryRetention implements SettingOption {
    FOREVER("forever", "Forever"), DAYS_30("30d", "30 days"), DAYS_7("7d", "7 days"), UNTIL_CLOSE("session", "Until app closes");
    private final String key; private final String label;
    HistoryRetention(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
