package com.kira.kdownloader.settings;

import com.kira.kdownloader.settings.store.KeyValueStore;

public final class SettingsMigration {
    private SettingsMigration() {}

    public static void migrateIfNeeded(KeyValueStore store) {
        int stored = store.getInt(SettingsKeys.VERSION, 0);
        if (stored >= SettingsKeys.CURRENT_VERSION) return;

        int version = stored;
        while (version < SettingsKeys.CURRENT_VERSION) {
            if (version == 1) {
                store.edit(editor -> editor.remove(SettingsKeys.AP_DYNAMIC_COLOR));
            }
            version++;
        }
        store.edit(editor -> editor.putInt(SettingsKeys.VERSION, SettingsKeys.CURRENT_VERSION));
    }
}
