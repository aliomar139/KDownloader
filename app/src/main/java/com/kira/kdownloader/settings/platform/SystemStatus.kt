package com.kira.kdownloader.settings.platform

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

/**
 * Reads Android permission/integration status and builds intents to the right system screens
 * (Section 11). Nothing here requests a permission — it only reports state and hands the user to
 * the correct settings page, honouring "don't request until the related feature is used".
 */
class SystemStatus(context: Context) {
    private val appContext = context.applicationContext

    data class Snapshot(
        val notificationsEnabled: Boolean,
        val ignoringBatteryOptimizations: Boolean,
        val backgroundRestricted: Boolean,
        val hasMediaAccess: Boolean,
    )

    fun snapshot(): Snapshot = Snapshot(
        notificationsEnabled = notificationsEnabled(),
        ignoringBatteryOptimizations = ignoringBatteryOptimizations(),
        backgroundRestricted = backgroundRestricted(),
        hasMediaAccess = hasMediaAccess(),
    )

    fun notificationsEnabled(): Boolean =
        NotificationManagerCompat.from(appContext).areNotificationsEnabled()

    fun ignoringBatteryOptimizations(): Boolean {
        val pm = appContext.getSystemService(Context.POWER_SERVICE) as? PowerManager ?: return true
        return pm.isIgnoringBatteryOptimizations(appContext.packageName)
    }

    /** True when the OS has restricted this app's background execution (API 28+). */
    fun backgroundRestricted(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return false
        val am = appContext.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager ?: return false
        return runCatching { am.isBackgroundRestricted }.getOrDefault(false)
    }

    /**
     * Whether media read access is granted, when it is actually needed. On API 33+ we only need
     * READ_MEDIA_* to scan existing downloads; the SAF picker covers writing, so this is optional.
     */
    fun hasMediaAccess(): Boolean {
        val perms = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                listOf("android.permission.READ_MEDIA_VIDEO", "android.permission.READ_MEDIA_AUDIO")
            Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 ->
                listOf("android.permission.READ_EXTERNAL_STORAGE")
            else -> emptyList()
        }
        if (perms.isEmpty()) return true
        return perms.all {
            appContext.checkSelfPermission(it) == android.content.pm.PackageManager.PERMISSION_GRANTED
        }
    }

    /** Whether uninterrupted background downloads are likely to work reliably. */
    fun canDownloadReliablyInBackground(): Boolean =
        ignoringBatteryOptimizations() && !backgroundRestricted()

    // ---- Intents to system screens ------------------------------------------

    fun notificationSettingsIntent(): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, appContext.packageName)

    fun batteryOptimizationSettingsIntent(): Intent =
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)

    fun appDetailsSettingsIntent(): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            .setData(Uri.fromParts("package", appContext.packageName, null))

    fun appLocaleSettingsIntent(): Intent? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent(Settings.ACTION_APP_LOCALE_SETTINGS)
                .setData(Uri.fromParts("package", appContext.packageName, null))
        } else {
            null
        }
}
