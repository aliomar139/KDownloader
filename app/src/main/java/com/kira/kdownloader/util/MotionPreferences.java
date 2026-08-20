package com.kira.kdownloader.util;

import android.content.Context;
import android.provider.Settings;

import com.kira.kdownloader.settings.SettingsRepository;
import com.kira.kdownloader.settings.store.SharedPreferencesKeyValueStore;

public final class MotionPreferences {
    private MotionPreferences() { }

    public static boolean reduce(Context context) {
        try {
            if (Settings.Global.getFloat(context.getContentResolver(),
                    Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f) return true;
        } catch (Throwable ignored) { }
        try {
            return new SettingsRepository(new SharedPreferencesKeyValueStore(context)).read()
                    .getAppearance().getReduceAnimations();
        } catch (Throwable ignored) {
            return false;
        }
    }
}
