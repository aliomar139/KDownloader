package com.kira.kdownloader.settings.store

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * [KeyValueStore] backed by a named [SharedPreferences] file.
 *
 * SharedPreferences already survives app restarts and app updates (Section 1) and is included in
 * auto-backup. Writes use [SharedPreferences.Editor.apply], which is atomic and durable — a process
 * killed mid-write either sees the old value or the new one, never a corrupt one (Section 14).
 *
 * Reads are defensive: a value stored under an unexpected type returns the caller's default rather
 * than throwing [ClassCastException].
 */
class SharedPreferencesKeyValueStore(
    context: Context,
    name: String = PREFERENCES_NAME,
) : KeyValueStore {
    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun getString(key: String, default: String): String =
        runCatching { prefs.getString(key, default) ?: default }.getOrDefault(default)

    override fun getInt(key: String, default: Int): Int = runCatching {
        when (val value = prefs.all[key]) {
            is Int -> value
            is Long -> value.toInt()
            is String -> value.toIntOrNull() ?: default
            else -> default
        }
    }.getOrDefault(default)

    override fun getBoolean(key: String, default: Boolean): Boolean = runCatching {
        when (val value = prefs.all[key]) {
            is Boolean -> value
            is String -> value.toBooleanStrictOrNull() ?: default
            else -> default
        }
    }.getOrDefault(default)

    override fun contains(key: String): Boolean = prefs.contains(key)

    override fun keys(): Set<String> = prefs.all.keys.toSet()

    override val changes: Flow<String?> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> trySend(key) }
        prefs.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        val editor = prefs.edit()
        object : KeyValueStore.Editor {
            override fun putString(key: String, value: String) = apply { editor.putString(key, value) }
            override fun putInt(key: String, value: Int) = apply { editor.putInt(key, value) }
            override fun putBoolean(key: String, value: Boolean) = apply { editor.putBoolean(key, value) }
            override fun remove(key: String) = apply { editor.remove(key) }
            override fun clear() = apply { editor.clear() }
        }.block()
        editor.apply()
    }

    companion object {
        const val PREFERENCES_NAME = "app_settings"
    }
}
