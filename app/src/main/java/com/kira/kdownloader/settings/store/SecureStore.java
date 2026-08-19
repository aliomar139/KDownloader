package com.kira.kdownloader.settings.store;

public interface SecureStore {
    void put(String key, String value);
    String get(String key);
    boolean contains(String key);
    void remove(String key);
}
