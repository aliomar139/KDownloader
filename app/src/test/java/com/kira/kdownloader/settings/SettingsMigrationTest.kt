package com.kira.kdownloader.settings

import com.kira.kdownloader.settings.store.InMemoryKeyValueStore
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsMigrationTest {

    @Test
    fun `fresh store is stamped with the current version`() {
        val store = InMemoryKeyValueStore()
        SettingsMigration.migrateIfNeeded(store)
        assertEquals(SettingsKeys.CURRENT_VERSION, store.getInt(SettingsKeys.VERSION, -1))
    }

    @Test
    fun `already-current store is left unchanged`() {
        val store = InMemoryKeyValueStore(
            mapOf(SettingsKeys.VERSION to SettingsKeys.CURRENT_VERSION.toString()),
        )
        SettingsMigration.migrateIfNeeded(store)
        assertEquals(SettingsKeys.CURRENT_VERSION, store.getInt(SettingsKeys.VERSION, -1))
    }

    @Test
    fun `a newer store is not downgraded`() {
        val newer = SettingsKeys.CURRENT_VERSION + 5
        val store = InMemoryKeyValueStore(mapOf(SettingsKeys.VERSION to newer.toString()))
        SettingsMigration.migrateIfNeeded(store)
        assertEquals(newer, store.getInt(SettingsKeys.VERSION, -1))
    }
}
