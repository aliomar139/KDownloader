package com.kira.kdownloader.settings.store;

import java.util.HashMap;
import java.util.Map;

public final class InMemorySecureStore implements SecureStore {
    private final Map<String, String> values = new HashMap<>();
    @Override public void put(String key, String value) { values.put(key, value); }
    @Override public String get(String key) { return values.get(key); }
    @Override public boolean contains(String key) { return values.containsKey(key); }
    @Override public void remove(String key) { values.remove(key); }
}
