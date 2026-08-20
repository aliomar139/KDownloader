package com.kira.kdownloader.settings.store;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public interface KeyValueStore {
    String getString(String key, String defaultValue);
    int getInt(String key, int defaultValue);
    boolean getBoolean(String key, boolean defaultValue);
    boolean contains(String key);
    Set<String> keys();
    LiveData<String> getChanges();
    void edit(EditorAction action);

    interface EditorAction { void apply(Editor editor); }

    interface Editor {
        Editor putString(String key, String value);
        Editor putInt(String key, int value);
        Editor putBoolean(String key, boolean value);
        Editor remove(String key);
        Editor clear();
    }
}

final class StoreEditor implements KeyValueStore.Editor {
    interface Operation { void apply(String key, String value); }
    private final Operation operation;

    StoreEditor(Operation operation) { this.operation = operation; }
    @Override public KeyValueStore.Editor putString(String key, String value) { operation.apply(key, value); return this; }
    @Override public KeyValueStore.Editor putInt(String key, int value) { operation.apply(key, Integer.toString(value)); return this; }
    @Override public KeyValueStore.Editor putBoolean(String key, boolean value) { operation.apply(key, Boolean.toString(value)); return this; }
    @Override public KeyValueStore.Editor remove(String key) { operation.apply(key, null); return this; }
    @Override public KeyValueStore.Editor clear() { operation.apply(null, null); return this; }
}
