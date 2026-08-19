package com.kira.kdownloader.settings;

public enum PostDownloadAction implements SettingOption {
    NOTHING("nothing", "Do nothing"), OPEN_FILE("open_file", "Open the file"),
    OPEN_DOWNLOADS("open_downloads", "Open the downloads screen"), SHARE("share", "Share the file");
    private final String key; private final String label;
    PostDownloadAction(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
