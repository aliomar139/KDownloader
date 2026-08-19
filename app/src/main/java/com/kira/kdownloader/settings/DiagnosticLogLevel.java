package com.kira.kdownloader.settings;

public enum DiagnosticLogLevel implements SettingOption {
    OFF("off", "Off"), ERRORS("errors", "Errors only"), DETAILED("detailed", "Detailed");
    private final String key; private final String label;
    DiagnosticLogLevel(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
