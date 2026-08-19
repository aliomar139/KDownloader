package com.kira.kdownloader.settings;

public enum SubfolderOrganization implements SettingOption {
    NONE("none", "Don't organize"), BY_CHANNEL("channel", "By channel"),
    BY_PLAYLIST("playlist", "By playlist"), BY_MEDIA_TYPE("media_type", "By media type");
    private final String key; private final String label;
    SubfolderOrganization(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
