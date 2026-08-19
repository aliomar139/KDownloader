package com.kira.kdownloader.settings;

public enum ProxyType implements SettingOption {
    DISABLED("disabled", "Disabled"), HTTP("http", "HTTP"), SOCKS("socks", "SOCKS");
    private final String key; private final String label;
    ProxyType(String key, String label) { this.key = key; this.label = label; }
    public String getKey() { return key; } public String getLabel() { return label; }
}
