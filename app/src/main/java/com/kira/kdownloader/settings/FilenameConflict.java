package com.kira.kdownloader.settings;

public enum FilenameConflict implements SettingOption {
    ADD_NUMBER("add_number", "Automatically add a number"), REPLACE("replace", "Replace after confirmation"),
    SKIP("skip", "Skip the download"), ASK("ask", "Ask every time");
    private final String key; private final String label;
    FilenameConflict(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
