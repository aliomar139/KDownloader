package com.kira.kdownloader.settings.store;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import java.io.IOException;
import java.security.GeneralSecurityException;

public final class KeystoreSecureStore implements SecureStore {
    private static final String TAG = "KeystoreSecureStore";
    private static final String FILE_NAME = "secure_settings";
    private final Context context;
    private volatile SharedPreferences preferences;

    public KeystoreSecureStore(Context context) { this.context = context.getApplicationContext(); }

    private SharedPreferences preferences() {
        SharedPreferences current = preferences;
        if (current != null) return current;
        synchronized (this) {
            if (preferences == null) preferences = openOrRecreate();
            return preferences;
        }
    }

    private SharedPreferences openOrRecreate() {
        try {
            return create();
        } catch (Throwable error) {
            Log.w(TAG, "Secure store unreadable; recreating", error);
            context.deleteSharedPreferences(FILE_NAME);
            try {
                return create();
            } catch (GeneralSecurityException | IOException secondError) {
                throw new IllegalStateException(secondError);
            }
        }
    }

    private SharedPreferences create() throws GeneralSecurityException, IOException {
        MasterKey key = new MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build();
        return EncryptedSharedPreferences.create(
                context, FILE_NAME, key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
    }

    @Override public void put(String key, String value) {
        try { preferences().edit().putString(key, value).apply(); }
        catch (Throwable error) { Log.w(TAG, "Could not store secret", error); }
    }

    @Override public String get(String key) {
        try { return preferences().getString(key, null); }
        catch (Throwable ignored) { return null; }
    }

    @Override public boolean contains(String key) {
        try { return preferences().contains(key); }
        catch (Throwable ignored) { return false; }
    }

    @Override public void remove(String key) {
        try { preferences().edit().remove(key).apply(); }
        catch (Throwable ignored) {}
    }
}
