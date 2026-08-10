package com.kira.kdownloader.settings.store

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.ConcurrentHashMap

/**
 * A minimal key/value persistence abstraction.
 *
 * The whole settings stack depends on this interface rather than on [android.content.SharedPreferences]
 * directly. That keeps the repository, migrations, and reset logic testable on the JVM via
 * [InMemoryKeyValueStore], with the Android-backed implementation
 * ([SharedPreferencesKeyValueStore]) used at runtime.
 *
 * All getters take a default so a missing or type-mismatched value can never throw (Section 14 —
 * "Never crash because a stored preference is missing, malformed, or outdated").
 */
interface KeyValueStore {
    fun getString(key: String, default: String): String
    fun getInt(key: String, default: Int): Int
    fun getBoolean(key: String, default: Boolean): Boolean
    fun contains(key: String): Boolean
    fun keys(): Set<String>

    /** Emits the key that changed (or null when everything changed, e.g. after a bulk clear). */
    val changes: Flow<String?>

    /** Applies a batch of edits atomically. */
    fun edit(block: Editor.() -> Unit)

    interface Editor {
        fun putString(key: String, value: String): Editor
        fun putInt(key: String, value: Int): Editor
        fun putBoolean(key: String, value: Boolean): Editor
        fun remove(key: String): Editor
        fun clear(): Editor
    }
}

/**
 * Thread-safe in-memory store used in unit tests. Values are stored as strings and coerced on read,
 * exactly mirroring how the SharedPreferences implementation tolerates malformed data.
 */
class InMemoryKeyValueStore(initial: Map<String, String> = emptyMap()) : KeyValueStore {
    private val map = ConcurrentHashMap<String, String>().apply { putAll(initial) }
    private val flow = MutableSharedFlow<String?>(extraBufferCapacity = 64)

    override val changes: Flow<String?> = flow

    override fun getString(key: String, default: String): String = map[key] ?: default

    override fun getInt(key: String, default: Int): Int = map[key]?.toIntOrNull() ?: default

    override fun getBoolean(key: String, default: Boolean): Boolean =
        map[key]?.toBooleanStrictOrNull() ?: default

    override fun contains(key: String): Boolean = map.containsKey(key)

    override fun keys(): Set<String> = map.keys.toSet()

    override fun edit(block: KeyValueStore.Editor.() -> Unit) {
        val touched = mutableListOf<String?>()
        val editor = object : KeyValueStore.Editor {
            override fun putString(key: String, value: String) = apply { map[key] = value; touched += key }
            override fun putInt(key: String, value: Int) = apply { map[key] = value.toString(); touched += key }
            override fun putBoolean(key: String, value: Boolean) = apply { map[key] = value.toString(); touched += key }
            override fun remove(key: String) = apply { map.remove(key); touched += key }
            override fun clear() = apply { map.clear(); touched += null }
        }
        editor.block()
        touched.forEach(flow::tryEmit)
    }

    fun snapshot(): Map<String, String> = map.toMap()
}
