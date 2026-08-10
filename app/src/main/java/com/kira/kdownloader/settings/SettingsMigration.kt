package com.kira.kdownloader.settings

import com.kira.kdownloader.settings.store.KeyValueStore

/**
 * Versioned, forward-only migration of the settings store (Section 14, Section 15).
 *
 * On every app start [migrateIfNeeded] compares the stored schema version against
 * [SettingsKeys.CURRENT_VERSION] and applies each pending step in order. Steps are individual,
 * idempotent transforms so a migration interrupted by process death can safely re-run.
 *
 * Unknown keys written by a *newer* app version are deliberately left untouched — they are ignored
 * on read (values are resolved through safe defaults) and preserved on write, so downgrading and
 * re-upgrading does not lose data.
 */
object SettingsMigration {

    /** Ordered migration steps. Index i migrates from version i to i+1. */
    private val steps: List<(KeyValueStore) -> Unit> = listOf(
        // v0 -> v1: baseline. Nothing to transform; the initial schema is version 1.
        { },
        // v1 -> v2: dynamic color now defaults off (the red brand theme). Drop any previously
        // stored value so existing installs fall back to the new default; users can turn it back
        // on from Settings, which writes the key again.
        { store -> store.edit { remove(SettingsKeys.AP_DYNAMIC_COLOR) } },
    )

    fun migrateIfNeeded(store: KeyValueStore) {
        val stored = store.getInt(SettingsKeys.VERSION, 0)

        // A store written by a newer build than we understand: don't attempt to downgrade it.
        if (stored > SettingsKeys.CURRENT_VERSION) return

        if (stored == SettingsKeys.CURRENT_VERSION) return

        var version = stored
        while (version < SettingsKeys.CURRENT_VERSION) {
            steps.getOrNull(version)?.invoke(store)
            version++
        }

        store.edit { putInt(SettingsKeys.VERSION, SettingsKeys.CURRENT_VERSION) }
    }
}
