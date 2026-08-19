package com.kira.kdownloader.settings.ui

import androidx.compose.ui.res.stringResource
import com.kira.kdownloader.R
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
        .onFailure { Toast.makeText(context, context.getString(R.string.no_app_can_handle_this_action), Toast.LENGTH_SHORT).show() }
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
        PreferenceNote(stringResource(R.string.notifications_are_turned_off_for_this_app_so_progress_and_completion_a))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ClickablePreference(stringResource(R.string.allow_notifications), onClick = {
                permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
            })
        }
        ClickablePreference(stringResource(R.string.open_android_notification_settings), onClick = {
            launchIntent(context, vm.system.notificationSettingsIntent())
        })
    }

    PreferenceGroupTitle(stringResource(R.string.alerts))
    SwitchPreference(stringResource(R.string.show_download_progress_notifications), nt.showProgress) {
        vm.setNotifications(nt.copy(showProgress = it))
    }
    SwitchPreference(stringResource(R.string.notify_when_a_download_completes), nt.notifyOnEachComplete) {
        vm.setNotifications(nt.copy(notifyOnEachComplete = it))
    }
    SwitchPreference(stringResource(R.string.notify_when_all_downloads_complete), nt.notifyOnAllComplete) {
        vm.setNotifications(nt.copy(notifyOnAllComplete = it))
    }
    SwitchPreference(stringResource(R.string.notify_when_a_download_fails), nt.notifyOnFailure) {
        vm.setNotifications(nt.copy(notifyOnFailure = it))
    }

    PreferenceGroupTitle(stringResource(R.string.style))
    SwitchPreference(stringResource(R.string.notification_sound), nt.sound) { vm.setNotifications(nt.copy(sound = it)) }
    SwitchPreference(stringResource(R.string.vibration), nt.vibration) { vm.setNotifications(nt.copy(vibration = it)) }
    SwitchPreference(stringResource(R.string.show_pause_resume_cancel_actions), nt.showActions) {
        vm.setNotifications(nt.copy(showActions = it))
    }
    SwitchPreference(stringResource(R.string.group_multiple_notifications), nt.groupNotifications) {
        vm.setNotifications(nt.copy(groupNotifications = it))
    }
    ClickablePreference(stringResource(R.string.open_android_notification_settings), onClick = {
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

    PreferenceGroupTitle(stringResource(R.string.theme))
    SingleChoicePreference(stringResource(R.string.app_theme), AppTheme.entries.toList(), a.theme) {
        vm.setAppearance(a.copy(theme = it))
    }
    SwitchPreference(stringResource(R.string.dynamic_color), a.dynamicColor,
        subtitle = if (dynamicSupported) "Use colors from your wallpaper" else "Requires Android 12 or newer",
        enabled = dynamicSupported) {
        vm.setAppearance(a.copy(dynamicColor = it))
    }
    SwitchPreference(stringResource(R.string.high_contrast_mode), a.highContrast) {
        vm.setAppearance(a.copy(highContrast = it))
    }

    PreferenceGroupTitle(stringResource(R.string.language))
    LabeledChoicePreference(
        title = stringResource(R.string.app_language),
        options = LanguageManager.SUPPORTED.map { it.tag to it.display },
        selectedValue = a.languageTag,
    ) { vm.setAppearance(a.copy(languageTag = it)) }
    PreferenceNote(stringResource(R.string.language_and_theme_changes_are_applied_right_away_and_keep_your_place))

    PreferenceGroupTitle(stringResource(R.string.download_list))
    SwitchPreference(stringResource(R.string.compact_download_list_mode), a.compactList) {
        vm.setAppearance(a.copy(compactList = it))
    }
    SwitchPreference(stringResource(R.string.show_file_size_estimates), a.showFileSize) {
        vm.setAppearance(a.copy(showFileSize = it))
    }
    SwitchPreference(stringResource(R.string.show_download_speed), a.showSpeed) {
        vm.setAppearance(a.copy(showSpeed = it))
    }
    SwitchPreference(stringResource(R.string.show_estimated_time_remaining), a.showEta) {
        vm.setAppearance(a.copy(showEta = it))
    }

    PreferenceGroupTitle(stringResource(R.string.motion))
    SwitchPreference(stringResource(R.string.reduce_animations), a.reduceAnimations) {
        vm.setAppearance(a.copy(reduceAnimations = it))
    }
    PreferenceNote(stringResource(R.string.text_size_follows_your_android_system_accessibility_settings))
}

// ---------------------------------------------------------------------------
// Section 9 — History & privacy
// ---------------------------------------------------------------------------

@Composable
fun HistoryPrivacySectionContent(settings: AppSettings, vm: SettingsViewModel) {
    val h = settings.history
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    PreferenceGroupTitle(stringResource(R.string.history))
    SwitchPreference(stringResource(R.string.keep_download_history), h.keepHistory) {
        vm.setHistory(h.copy(keepHistory = it))
    }
    SingleChoicePreference(stringResource(R.string.history_retention), HistoryRetention.entries.toList(), h.retention,
        enabled = h.keepHistory) { vm.setHistory(h.copy(retention = it)) }
    SwitchPreference(stringResource(R.string.save_recently_used_urls), h.saveRecentUrls) {
        vm.setHistory(h.copy(saveRecentUrls = it))
    }
    SwitchPreference(stringResource(R.string.save_search_history), h.saveSearchHistory) {
        vm.setHistory(h.copy(saveSearchHistory = it))
    }
    PreferenceNote(stringResource(R.string.clearing_history_never_deletes_your_downloaded_media))

    PreferenceGroupTitle(stringResource(R.string.clear))
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
        title = stringResource(R.string.clear_all_app_data),
        message = stringResource(R.string.resets_every_setting_clears_history_recent_urls_searches_and_caches_yo),
        confirmLabel = stringResource(R.string.clear_everything),
    ) { vm.clearAllAppData() }

    PreferenceGroupTitle(stringResource(R.string.backup))
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
    ClickablePreference(stringResource(R.string.export_settings_to_a_file), onClick = { exporter.launch("kdownloader-settings.json") })
    ClickablePreference(stringResource(R.string.import_settings_from_a_file), onClick = { importer.launch(arrayOf("application/json", "text/*")) })
    PreferenceNote(stringResource(R.string.exported_files_never_contain_passwords_tokens_or_other_credentials))
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

    PreferenceGroupTitle(stringResource(R.string.conversion))
    SwitchPreference(stringResource(R.string.enable_post_download_conversion), p.enableConversion) {
        vm.setProcessing(p.copy(enableConversion = it))
    }
    SwitchPreference(stringResource(R.string.delete_source_files_after_successful_conversion), p.deleteSourceAfterConversion,
        enabled = on) { vm.setProcessing(p.copy(deleteSourceAfterConversion = it)) }
    SwitchPreference(stringResource(R.string.preserve_source_files_when_conversion_fails), p.preserveSourceOnFailure,
        enabled = on) { vm.setProcessing(p.copy(preserveSourceOnFailure = it)) }
    SwitchPreference(stringResource(R.string.prefer_hardware_acceleration_when_supported), p.preferHardwareAcceleration,
        enabled = on) { vm.setProcessing(p.copy(preferHardwareAcceleration = it)) }
    SingleChoicePreference(stringResource(R.string.processing_priority), ProcessingPriority.entries.toList(), p.priority,
        enabled = on) { vm.setProcessing(p.copy(priority = it)) }
    SwitchPreference(stringResource(R.string.allow_processing_in_the_background), p.allowBackgroundProcessing,
        enabled = on) { vm.setProcessing(p.copy(allowBackgroundProcessing = it)) }
    TextEntryPreference(
        title = stringResource(R.string.maximum_temporary_storage_mb),
        value = p.maxTempStorageMb.toString(),
        onValueChange = { it.toIntOrNull()?.let { v -> vm.setProcessing(p.copy(maxTempStorageMb = v)) } },
        enabled = on,
        keyboardNumeric = true,
        validate = { if ((it.toIntOrNull() ?: 0) < 1) "Enter a value of 1 or more" else null },
    )
    PreferenceNote(stringResource(R.string.conversion_can_increase_processing_time_heat_battery_use_and_temporary))

    PreferenceGroupTitle(stringResource(R.string.diagnostics))
    SingleChoicePreference(stringResource(R.string.diagnostic_logging), DiagnosticLogLevel.entries.toList(), p.logLevel) {
        vm.setProcessing(p.copy(logLevel = it))
    }
    val exporter = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) scope.launch {
            withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(vm.buildDiagnostics().toByteArray()) } }
            }
            Toast.makeText(context, context.getString(R.string.diagnostics_exported), Toast.LENGTH_SHORT).show()
        }
    }
    ClickablePreference(stringResource(R.string.export_diagnostic_log), onClick = { exporter.launch("kdownloader-diagnostics.txt") })
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
        title = stringResource(R.string.notifications),
        ok = status.notificationsEnabled,
        onAction = { launchIntent(context, vm.system.notificationSettingsIntent()) },
    )
    StatusRow(
        title = stringResource(R.string.download_folder_access),
        ok = folderOk,
        okText = if (folderUri.isBlank()) "Not set" else "Granted",
        actionText = "Manage",
        onAction = { launchIntent(context, vm.system.appDetailsSettingsIntent()) },
    )
    StatusRow(
        title = stringResource(R.string.battery_optimization_exemption),
        ok = status.ignoringBatteryOptimizations,
        onAction = { launchIntent(context, vm.system.batteryOptimizationSettingsIntent()) },
    )
    StatusRow(
        title = stringResource(R.string.background_activity),
        ok = !status.backgroundRestricted,
        okText = if (status.backgroundRestricted) "Restricted" else "Allowed",
        onAction = { launchIntent(context, vm.system.appDetailsSettingsIntent()) },
    )
    StatusRow(
        title = stringResource(R.string.media_access_when_needed),
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
    PreferenceNote(stringResource(R.string.long_downloads_run_in_a_foreground_service_with_an_ongoing_notificatio))
    ClickablePreference(stringResource(R.string.re_check_status), onClick = { vm.refreshStatus() })
}

// ---------------------------------------------------------------------------
// Section 12 — About & support
// ---------------------------------------------------------------------------

@Composable
fun AboutSectionContent() {
    val context = LocalContext.current

    PreferenceGroupTitle(stringResource(R.string.about))
    ClickablePreference(stringResource(R.string.app), subtitle = "KDownloader ${BuildConfig.VERSION_NAME}", onClick = {})
    ClickablePreference(stringResource(R.string.build_number), subtitle = BuildConfig.VERSION_CODE.toString(), onClick = {})
    ClickablePreference(stringResource(R.string.download_engine), subtitle = "yt-dlp ${BuildConfig.BUNDLED_YTDLP_VERSION}", onClick = {})
    PreferenceNote(stringResource(R.string.updates_are_delivered_through_your_app_store_there_is_no_in_app_update))

    PreferenceGroupTitle(stringResource(R.string.legal))
    ClickablePreference(stringResource(R.string.open_source_licenses), onClick = { openUrl(context, "https://github.com/yt-dlp/yt-dlp") })
    ClickablePreference(stringResource(R.string.privacy_policy), onClick = { openUrl(context, "https://example.com/privacy") })
    ClickablePreference(stringResource(R.string.terms_of_service), onClick = { openUrl(context, "https://example.com/terms") })

    PreferenceGroupTitle(stringResource(R.string.support))
    ClickablePreference(stringResource(R.string.help_faq), onClick = { openUrl(context, "https://example.com/help") })
    ClickablePreference(stringResource(R.string.report_a_problem), onClick = {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:support@example.com"))
            .putExtra(Intent.EXTRA_SUBJECT, "KDownloader ${BuildConfig.VERSION_NAME} problem report")
        launchIntent(context, intent)
    })
    ClickablePreference(stringResource(R.string.source_code), onClick = { openUrl(context, "https://github.com/") })
    PreferenceNote(stringResource(R.string.diagnostic_exports_exclude_credentials_private_urls_folder_contents_an))
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
    PreferenceNote(stringResource(R.string.resetting_settings_never_deletes_downloaded_files_or_history))
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
