package com.kira.kdownloader.settings.store;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryKeyValueStore implements KeyValueStore {
    private final ConcurrentHashMap<String, String> values = new ConcurrentHashMap<>();
    private final MutableLiveData<String> changes = new MutableLiveData<>();

    public InMemoryKeyValueStore() {}
    public InMemoryKeyValueStore(Map<String, String> initial) { values.putAll(initial); }

    @Override public String getString(String key, String defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : value;
    }

    @Override public int getInt(String key, int defaultValue) {
        try { return Integer.parseInt(getString(key, Integer.toString(defaultValue))); }
        catch (NumberFormatException ignored) { return defaultValue; }
    }

    @Override public boolean getBoolean(String key, boolean defaultValue) {
        String value = values.get(key);
        if ("true".equals(value)) return true;
        if ("false".equals(value)) return false;
        return defaultValue;
    }

    @Override public boolean contains(String key) { return values.containsKey(key); }
    @Override public Set<String> keys() { return new HashSet<>(values.keySet()); }
    @Override public LiveData<String> getChanges() { return changes; }

    @Override public void edit(EditorAction action) {
        action.apply(new StoreEditor((key, value) -> {
            if (key == null) values.clear();
            else if (value == null) values.remove(key);
            else values.put(key, value);
            if (changes.hasActiveObservers()) changes.postValue(key);
        }));
    }

    public Map<String, String> snapshot() {
        return Collections.unmodifiableMap(new HashMap<>(values));
    }
}
