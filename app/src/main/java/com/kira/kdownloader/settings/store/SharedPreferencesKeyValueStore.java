package com.kira.kdownloader.settings.store;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class SharedPreferencesKeyValueStore implements KeyValueStore {
    public static final String PREFERENCES_NAME = "app_settings";
    private final SharedPreferences preferences;
    private final LiveData<String> changes;

    public SharedPreferencesKeyValueStore(Context context) { this(context, PREFERENCES_NAME); }

    public SharedPreferencesKeyValueStore(Context context, String name) {
        preferences = context.getApplicationContext().getSharedPreferences(name, Context.MODE_PRIVATE);
        changes = new PreferenceChanges(preferences);
    }

    @Override public String getString(String key, String defaultValue) {
        try {
            String value = preferences.getString(key, defaultValue);
            return value == null ? defaultValue : value;
        } catch (ClassCastException ignored) {
            return defaultValue;
        }
    }

    @Override public int getInt(String key, int defaultValue) {
        try {
            Object value = preferences.getAll().get(key);
            if (value instanceof Integer) return (Integer) value;
            if (value instanceof Long) return ((Long) value).intValue();
            if (value instanceof String) return Integer.parseInt((String) value);
        } catch (RuntimeException ignored) {}
        return defaultValue;
    }

    @Override public boolean getBoolean(String key, boolean defaultValue) {
        try {
            Object value = preferences.getAll().get(key);
            if (value instanceof Boolean) return (Boolean) value;
            if ("true".equals(value)) return true;
            if ("false".equals(value)) return false;
        } catch (RuntimeException ignored) {}
        return defaultValue;
    }

    @Override public boolean contains(String key) { return preferences.contains(key); }
    @Override public Set<String> keys() { return new HashSet<>(preferences.getAll().keySet()); }
    @Override public LiveData<String> getChanges() { return changes; }

    @Override public void edit(EditorAction action) {
        SharedPreferences.Editor editor = preferences.edit();
        action.apply(new KeyValueStore.Editor() {
            @Override public KeyValueStore.Editor putString(String key, String value) { editor.putString(key, value); return this; }
            @Override public KeyValueStore.Editor putInt(String key, int value) { editor.putInt(key, value); return this; }
            @Override public KeyValueStore.Editor putBoolean(String key, boolean value) { editor.putBoolean(key, value); return this; }
            @Override public KeyValueStore.Editor remove(String key) { editor.remove(key); return this; }
            @Override public KeyValueStore.Editor clear() { editor.clear(); return this; }
        });
        editor.apply();
    }

    private static final class PreferenceChanges extends LiveData<String>
            implements SharedPreferences.OnSharedPreferenceChangeListener {
        private final SharedPreferences preferences;

        PreferenceChanges(SharedPreferences preferences) { this.preferences = preferences; }
        @Override protected void onActive() { preferences.registerOnSharedPreferenceChangeListener(this); }
        @Override protected void onInactive() { preferences.unregisterOnSharedPreferenceChangeListener(this); }
        @Override public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) { setValue(key); }
    }
}
