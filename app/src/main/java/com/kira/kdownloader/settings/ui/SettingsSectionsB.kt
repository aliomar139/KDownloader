package com.kira.kdownloader.settings.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.kira.kdownloader.BuildConfig
import com.kira.kdownloader.settings.AppSettings
import com.kira.kdownloader.settings.AppTheme
import com.kira.kdownloader.settings.DiagnosticLogLevel
import com.kira.kdownloader.settings.HistoryRetention
import com.kira.kdownloader.settings.ProcessingPriority
import com.kira.kdownloader.settings.SettingsCategory
import com.kira.kdownloader.settings.SettingsRepository
import com.kira.kdownloader.settings.platform.LanguageManager
import com.kira.kdownloader.settings.platform.SystemStatus
import com.kira.kdownloader.settings.ui.components.ClickablePreference
import com.kira.kdownloader.settings.ui.components.ConfirmDialog
import com.kira.kdownloader.settings.ui.components.LabeledChoicePreference
import com.kira.kdownloader.settings.ui.components.PreferenceGroupTitle
import com.kira.kdownloader.settings.ui.components.PreferenceNote
import com.kira.kdownloader.settings.ui.components.SingleChoicePreference
import com.kira.kdownloader.settings.ui.components.SwitchPreference
import com.kira.kdownloader.settings.ui.components.TextEntryPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun launchIntent(context: Context, intent: Intent) {
    val toLaunch = if (context !is Activity) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) else intent
    runCatching { context.startActivity(toLaunch) }
        .onFailure { Toast.makeText(context, "No app can handle this action", Toast.LENGTH_SHORT).show() }
}

// ---------------------------------------------------------------------------
// Section 7 — Notifications
// ---------------------------------------------------------------------------

@Composable
fun NotificationsSectionContent(settings: AppSettings, vm: SettingsViewModel) {
    val nt = settings.notifications
    val context = LocalContext.current
    val enabled = vm.systemStatus.value.notificationsEnabled

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { vm.refreshStatus() }

    if (!enabled) {
        PreferenceNote("Notifications are turned off for this app, so progress and completion alerts won't appear.")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ClickablePreference("Allow notifications", onClick = {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            })
        }
        ClickablePreference("Open Android notification settings", onClick = {
            launchIntent(context, vm.system.notificationSettingsIntent())
        })
    }

    PreferenceGroupTitle("Alerts")
    SwitchPreference("Show download progress notifications", nt.showProgress) {
        vm.setNotifications(nt.copy(showProgress = it))
    }
    SwitchPreference("Notify when a download completes", nt.notifyOnEachComplete) {
        vm.setNotifications(nt.copy(notifyOnEachComplete = it))
    }
    SwitchPreference("Notify when all downloads complete", nt.notifyOnAllComplete) {
        vm.setNotifications(nt.copy(notifyOnAllComplete = it))
    }
    SwitchPreference("Notify when a download fails", nt.notifyOnFailure) {
        vm.setNotifications(nt.copy(notifyOnFailure = it))
    }

    PreferenceGroupTitle("Style")
    SwitchPreference("Notification sound", nt.sound) { vm.setNotifications(nt.copy(sound = it)) }
    SwitchPreference("Vibration", nt.vibration) { vm.setNotifications(nt.copy(vibration = it)) }
    SwitchPreference("Show Pause / Resume / Cancel actions", nt.showActions) {
        vm.setNotifications(nt.copy(showActions = it))
    }
    SwitchPreference("Group multiple notifications", nt.groupNotifications) {
        vm.setNotifications(nt.copy(groupNotifications = it))
    }
    ClickablePreference("Open Android notification settings", onClick = {
        launchIntent(context, vm.system.notificationSettingsIntent())
    })
}

// ---------------------------------------------------------------------------
// Section 8 — Appearance & accessibility
// ---------------------------------------------------------------------------

@Composable
fun AppearanceSectionContent(settings: AppSettings, vm: SettingsViewModel) {
    val a = settings.appearance
    val dynamicSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    PreferenceGroupTitle("Theme")
    SingleChoicePreference("App theme", AppTheme.entries.toList(), a.theme) {
        vm.setAppearance(a.copy(theme = it))
    }
    SwitchPreference("Dynamic color", a.dynamicColor,
        subtitle = if (dynamicSupported) "Use colors from your wallpaper" else "Requires Android 12 or newer",
        enabled = dynamicSupported) {
        vm.setAppearance(a.copy(dynamicColor = it))
    }
    SwitchPreference("High-contrast mode", a.highContrast) {
        vm.setAppearance(a.copy(highContrast = it))
    }

    PreferenceGroupTitle("Language")
    LabeledChoicePreference(
        title = "App language",
        options = LanguageManager.SUPPORTED.map { it.tag to it.display },
        selectedValue = a.languageTag,
    ) { vm.setAppearance(a.copy(languageTag = it)) }
    PreferenceNote("Language and theme changes are applied right away and keep your place in the app.")

    PreferenceGroupTitle("Download list")
    SwitchPreference("Compact download-list mode", a.compactList) {
        vm.setAppearance(a.copy(compactList = it))
    }
    SwitchPreference("Show file size estimates", a.showFileSize) {
        vm.setAppearance(a.copy(showFileSize = it))
    }
    SwitchPreference("Show download speed", a.showSpeed) {
        vm.setAppearance(a.copy(showSpeed = it))
    }
    SwitchPreference("Show estimated time remaining", a.showEta) {
        vm.setAppearance(a.copy(showEta = it))
    }

    PreferenceGroupTitle("Motion")
    SwitchPreference("Reduce animations", a.reduceAnimations) {
        vm.setAppearance(a.copy(reduceAnimations = it))
    }
    PreferenceNote("Text size follows your Android system accessibility settings.")
}

// ---------------------------------------------------------------------------
// Section 9 — History & privacy
// ---------------------------------------------------------------------------

@Composable
fun HistoryPrivacySectionContent(settings: AppSettings, vm: SettingsViewModel) {
    val h = settings.history
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    PreferenceGroupTitle("History")
    SwitchPreference("Keep download history", h.keepHistory) {
        vm.setHistory(h.copy(keepHistory = it))
    }
    SingleChoicePreference("History retention", HistoryRetention.entries.toList(), h.retention,
        enabled = h.keepHistory) { vm.setHistory(h.copy(retention = it)) }
    SwitchPreference("Save recently used URLs", h.saveRecentUrls) {
        vm.setHistory(h.copy(saveRecentUrls = it))
    }
    SwitchPreference("Save search history", h.saveSearchHistory) {
        vm.setHistory(h.copy(saveSearchHistory = it))
    }
    PreferenceNote("Clearing history never deletes your downloaded media.")

    PreferenceGroupTitle("Clear")
    ConfirmingAction("Clear download history", "Remove all history entries. Downloaded files are kept.", "Clear") {
        vm.clearHistory()
    }
    ConfirmingAction("Clear recent URLs", "Remove the list of recently used URLs.", "Clear") {
        vm.clearRecentUrls()
    }
    ConfirmingAction("Clear search history", "Remove your saved searches.", "Clear") {
        vm.clearSearchHistory()
    }
    ConfirmingAction(
        title = "Clear all app data",
        message = "Resets every setting, clears history, recent URLs, searches, and caches. Your downloaded media files are NOT deleted.",
        confirmLabel = "Clear everything",
    ) { vm.clearAllAppData() }

    PreferenceGroupTitle("Backup")
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { it.write(vm.exportJson().toByteArray()) }
                }.isSuccess
            }
            Toast.makeText(context, if (ok) "Settings exported" else "Export failed", Toast.LENGTH_SHORT).show()
        }
    }
    val importer = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) scope.launch {
            val json = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } }.getOrNull()
            }
            val message = when (val result = json?.let { vm.importJson(it) }) {
                is SettingsRepository.ImportResult.Success -> "Imported ${result.applied} settings"
                is SettingsRepository.ImportResult.Failure -> result.reason
                null -> "Could not read the file"
            }
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    ClickablePreference("Export settings to a file", onClick = { exporter.launch("kdownloader-settings.json") })
    ClickablePreference("Import settings from a file", onClick = { importer.launch(arrayOf("application/json", "text/*")) })
    PreferenceNote("Exported files never contain passwords, tokens, or other credentials.")
}

// ---------------------------------------------------------------------------
// Section 10 — Advanced processing
// ---------------------------------------------------------------------------

@Composable
fun AdvancedSectionContent(settings: AppSettings, vm: SettingsViewModel) {
    val p = settings.processing
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val on = p.enableConversion

    PreferenceGroupTitle("Conversion")
    SwitchPreference("Enable post-download conversion", p.enableConversion) {
        vm.setProcessing(p.copy(enableConversion = it))
    }
    SwitchPreference("Delete source files after successful conversion", p.deleteSourceAfterConversion,
        enabled = on) { vm.setProcessing(p.copy(deleteSourceAfterConversion = it)) }
    SwitchPreference("Preserve source files when conversion fails", p.preserveSourceOnFailure,
        enabled = on) { vm.setProcessing(p.copy(preserveSourceOnFailure = it)) }
    SwitchPreference("Prefer hardware acceleration when supported", p.preferHardwareAcceleration,
        enabled = on) { vm.setProcessing(p.copy(preferHardwareAcceleration = it)) }
    SingleChoicePreference("Processing priority", ProcessingPriority.entries.toList(), p.priority,
        enabled = on) { vm.setProcessing(p.copy(priority = it)) }
    SwitchPreference("Allow processing in the background", p.allowBackgroundProcessing,
        enabled = on) { vm.setProcessing(p.copy(allowBackgroundProcessing = it)) }
    TextEntryPreference(
        title = "Maximum temporary-storage (MB)",
        value = p.maxTempStorageMb.toString(),
        onValueChange = { it.toIntOrNull()?.let { v -> vm.setProcessing(p.copy(maxTempStorageMb = v)) } },
        enabled = on,
        keyboardNumeric = true,
        validate = { if ((it.toIntOrNull() ?: 0) < 1) "Enter a value of 1 or more" else null },
    )
    PreferenceNote("Conversion can increase processing time, heat, battery use, and temporary storage.")

    PreferenceGroupTitle("Diagnostics")
    SingleChoicePreference("Diagnostic logging", DiagnosticLogLevel.entries.toList(), p.logLevel) {
        vm.setProcessing(p.copy(logLevel = it))
    }
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(vm.buildDiagnostics().toByteArray()) } }
            }
            Toast.makeText(context, "Diagnostics exported", Toast.LENGTH_SHORT).show()
        }
    }
    ClickablePreference("Export diagnostic log", onClick = { exporter.launch("kdownloader-diagnostics.txt") })
    ConfirmingAction("Clear diagnostic log", "Delete the stored diagnostic log.", "Clear") {
        vm.clearDiagnosticLog()
    }
}

// ---------------------------------------------------------------------------
// Section 11 — Permissions & status
// ---------------------------------------------------------------------------

@Composable
fun PermissionsSectionContent(status: SystemStatus.Snapshot, vm: SettingsViewModel) {
    val context = LocalContext.current
    val folderUri = vm.settings.value.storage.downloadFolderUri
    val folderOk = folderUri.isNotBlank() && vm.folders.hasAccess(folderUri)

    StatusRow(
        title = "Notifications",
        ok = status.notificationsEnabled,
        onAction = { launchIntent(context, vm.system.notificationSettingsIntent()) },
    )
    StatusRow(
        title = "Download-folder access",
        ok = folderOk,
        okText = if (folderUri.isBlank()) "Not set" else "Granted",
        actionText = "Manage",
        onAction = { launchIntent(context, vm.system.appDetailsSettingsIntent()) },
    )
    StatusRow(
        title = "Battery optimization exemption",
        ok = status.ignoringBatteryOptimizations,
        onAction = { launchIntent(context, vm.system.batteryOptimizationSettingsIntent()) },
    )
    StatusRow(
        title = "Background activity",
        ok = !status.backgroundRestricted,
        okText = if (status.backgroundRestricted) "Restricted" else "Allowed",
        onAction = { launchIntent(context, vm.system.appDetailsSettingsIntent()) },
    )
    StatusRow(
        title = "Media access (when needed)",
        ok = status.hasMediaAccess,
        onAction = { launchIntent(context, vm.system.appDetailsSettingsIntent()) },
    )
    PreferenceNote(
        if (vm.system.canDownloadReliablyInBackground()) {
            "This app can continue downloads reliably in the background."
        } else {
            "Background downloads may be interrupted. Exempt the app from battery optimization for reliable long downloads."
        },
    )
    PreferenceNote("Long downloads run in a foreground service with an ongoing notification, as required by Android.")
    ClickablePreference("Re-check status", onClick = { vm.refreshStatus() })
}

// ---------------------------------------------------------------------------
// Section 12 — About & support
// ---------------------------------------------------------------------------

@Composable
fun AboutSectionContent() {
    val context = LocalContext.current

    PreferenceGroupTitle("About")
    ClickablePreference("App", subtitle = "KDownloader ${BuildConfig.VERSION_NAME}", onClick = {})
    ClickablePreference("Build number", subtitle = BuildConfig.VERSION_CODE.toString(), onClick = {})
    ClickablePreference("Download engine", subtitle = "yt-dlp ${BuildConfig.BUNDLED_YTDLP_VERSION}", onClick = {})
    PreferenceNote("Updates are delivered through your app store; there is no in-app updater.")

    PreferenceGroupTitle("Legal")
    ClickablePreference("Open-source licenses", onClick = { openUrl(context, "https://github.com/yt-dlp/yt-dlp") })
    ClickablePreference("Privacy policy", onClick = { openUrl(context, "https://example.com/privacy") })
    ClickablePreference("Terms of service", onClick = { openUrl(context, "https://example.com/terms") })

    PreferenceGroupTitle("Support")
    ClickablePreference("Help / FAQ", onClick = { openUrl(context, "https://example.com/help") })
    ClickablePreference("Report a problem", onClick = {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@example.com"))
            .putExtra(Intent.EXTRA_SUBJECT, "KDownloader ${BuildConfig.VERSION_NAME} problem report")
        launchIntent(context, intent)
    })
    ClickablePreference("Source code", onClick = { openUrl(context, "https://github.com/") })
    PreferenceNote("Diagnostic exports exclude credentials, private URLs, folder contents, and personal information.")
}

private fun openUrl(context: Context, url: String) =
    launchIntent(context, Intent(Intent.ACTION_VIEW, Uri.parse(url)))

// ---------------------------------------------------------------------------
// Section 13 — Reset
// ---------------------------------------------------------------------------

@Composable
fun ResetSectionContent(vm: SettingsViewModel) {
    ConfirmingAction("Reset download preferences", "Restores download type, format, quality, and metadata options to their defaults.", "Reset", destructive = false) {
        vm.resetCategory(SettingsCategory.DOWNLOAD)
    }
    ConfirmingAction("Reset network preferences", "Restores network and proxy settings to their defaults and clears the saved proxy password.", "Reset", destructive = false) {
        vm.resetCategory(SettingsCategory.NETWORK)
    }
    ConfirmingAction("Reset appearance preferences", "Restores theme, language, list, and motion options to their defaults.", "Reset", destructive = false) {
        vm.resetCategory(SettingsCategory.APPEARANCE)
    }
    ConfirmingAction("Reset all settings", "Restores every setting to its default. Downloaded files and history are NOT deleted.", "Reset all") {
        vm.resetAll()
    }
    PreferenceNote("Resetting settings never deletes downloaded files or history.")
}

// ---------------------------------------------------------------------------
// Shared helpers
// ---------------------------------------------------------------------------

@Composable
private fun ConfirmingAction(
    title: String,
    message: String,
    confirmLabel: String,
    destructive: Boolean = true,
    onConfirm: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    ClickablePreference(title = title, onClick = { open = true }, destructive = destructive)
    if (open) {
        ConfirmDialog(
            title = title,
            message = message,
            confirmLabel = confirmLabel,
            destructive = destructive,
            onConfirm = onConfirm,
            onDismiss = { open = false },
        )
    }
}

@Composable
private fun StatusRow(
    title: String,
    ok: Boolean,
    okText: String = "Granted",
    notOkText: String = "Not granted",
    actionText: String = "Open settings",
    onAction: () -> Unit,
) {
    ClickablePreference(
        title = title,
        subtitle = if (ok) okText else "$notOkText — tap to $actionText",
        onClick = onAction,
        destructive = !ok,
    )
}
