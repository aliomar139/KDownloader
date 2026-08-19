package com.kira.kdownloader.settings;

import com.kira.kdownloader.settings.store.InMemoryKeyValueStore;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SettingsMigrationTest {
    @Test public void freshStoreIsStampedWithTheCurrentVersion() {
        InMemoryKeyValueStore store = new InMemoryKeyValueStore();
        SettingsMigration.migrateIfNeeded(store);
        assertEquals(SettingsKeys.CURRENT_VERSION, store.getInt(SettingsKeys.VERSION, -1));
    }

    @Test public void alreadyCurrentStoreIsLeftUnchanged() {
        Map<String, String> initial = new HashMap<>();
        initial.put(SettingsKeys.VERSION, Integer.toString(SettingsKeys.CURRENT_VERSION));
        InMemoryKeyValueStore store = new InMemoryKeyValueStore(initial);
        SettingsMigration.migrateIfNeeded(store);
        assertEquals(SettingsKeys.CURRENT_VERSION, store.getInt(SettingsKeys.VERSION, -1));
    }

    @Test public void aNewerStoreIsNotDowngraded() {
        int newer = SettingsKeys.CURRENT_VERSION + 5;
        Map<String, String> initial = new HashMap<>();
        initial.put(SettingsKeys.VERSION, Integer.toString(newer));
        InMemoryKeyValueStore store = new InMemoryKeyValueStore(initial);
        SettingsMigration.migrateIfNeeded(store);
        assertEquals(newer, store.getInt(SettingsKeys.VERSION, -1));
    }
}
