package com.kira.kdownloader.settings.store

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/**
 * Secure storage for sensitive credentials (currently the proxy password, Section 5).
 *
 * Values are encrypted at rest with a key held in the Android Keystore. Callers only ever set,
 * read, or clear a secret — the plaintext never touches the regular settings store and is therefore
 * never included in exports (Section 12).
 */
interface SecureStore {
    fun put(key: String, value: String)
    fun get(key: String): String?
    fun contains(key: String): Boolean
    fun remove(key: String)
}

/** In-memory secure store for tests. Not persisted, not encrypted. */
class InMemorySecureStore : SecureStore {
    private val map = HashMap<String, String>()
    override fun put(key: String, value: String) { map[key] = value }
    override fun get(key: String): String? = map[key]
    override fun contains(key: String): Boolean = map.containsKey(key)
    override fun remove(key: String) { map.remove(key) }
}

/**
 * Keystore-backed [SecureStore] using [EncryptedSharedPreferences].
 *
 * Initialization is defensive: if the encrypted file becomes unreadable (e.g. after a keystore
 * reset following a device restore), it is recreated once rather than crashing the app.
 */
class KeystoreSecureStore(context: Context) : SecureStore {
    private val appContext = context.applicationContext
    private val prefs by lazy { openOrRecreate() }

    private fun openOrRecreate() = try {
        create()
    } catch (error: Throwable) {
        Log.w(TAG, "Secure store unreadable; recreating", error)
        appContext.deleteSharedPreferences(FILE_NAME)
        create()
    }

    private fun create() = EncryptedSharedPreferences.create(
        appContext,
        FILE_NAME,
        MasterKey.Builder(appContext)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override fun put(key: String, value: String) {
        runCatching { prefs.edit().putString(key, value).apply() }
            .onFailure { Log.w(TAG, "Could not store secret", it) }
    }

    override fun get(key: String): String? =
        runCatching { prefs.getString(key, null) }.getOrNull()

    override fun contains(key: String): Boolean =
        runCatching { prefs.contains(key) }.getOrDefault(false)

    override fun remove(key: String) {
        runCatching { prefs.edit().remove(key).apply() }
    }

    companion object {
        private const val TAG = "KeystoreSecureStore"
        private const val FILE_NAME = "secure_settings"
    }
}
