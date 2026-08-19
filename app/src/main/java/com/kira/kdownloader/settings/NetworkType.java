package com.kira.kdownloader.settings;

public enum NetworkType implements SettingOption {
    WIFI_ONLY("wifi", "Wi-Fi only"), WIFI_AND_MOBILE("wifi_mobile", "Wi-Fi and mobile data"), ANY("any", "Any network");
    private final String key; private final String label;
    NetworkType(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
