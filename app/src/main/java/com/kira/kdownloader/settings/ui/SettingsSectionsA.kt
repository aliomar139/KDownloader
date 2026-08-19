package com.kira.kdownloader.settings.ui

import androidx.compose.ui.res.stringResource
import com.kira.kdownloader.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.kira.kdownloader.settings.AppSettings
import com.kira.kdownloader.settings.AudioFormat
import com.kira.kdownloader.settings.AudioQuality
import com.kira.kdownloader.settings.DownloadType
import com.kira.kdownloader.settings.DuplicateDetection
import com.kira.kdownloader.settings.FilenameConflict
import com.kira.kdownloader.settings.FilenameTemplate
import com.kira.kdownloader.settings.FrameRatePreference
import com.kira.kdownloader.settings.NetworkType
import com.kira.kdownloader.settings.PostDownloadAction
import com.kira.kdownloader.settings.ProxyType
import com.kira.kdownloader.settings.ProxyValidator
import com.kira.kdownloader.settings.QueuePosition
import com.kira.kdownloader.settings.SubfolderOrganization
import com.kira.kdownloader.settings.SubtitleFormat
import com.kira.kdownloader.settings.SubtitleTypePreference
import com.kira.kdownloader.settings.VideoFormat
import com.kira.kdownloader.settings.VideoQuality
import com.kira.kdownloader.settings.platform.FolderAccessManager
import com.kira.kdownloader.settings.platform.LanguageManager
import com.kira.kdownloader.settings.ui.components.ClickablePreference
import com.kira.kdownloader.settings.ui.components.ConfirmDialog
import com.kira.kdownloader.settings.ui.components.IntSliderPreference
import com.kira.kdownloader.settings.ui.components.LabeledChoicePreference
import com.kira.kdownloader.settings.ui.components.PreferenceGroupTitle
import com.kira.kdownloader.settings.ui.components.PreferenceNote
import com.kira.kdownloader.settings.ui.components.SingleChoicePreference
import com.kira.kdownloader.settings.ui.components.SwitchPreference
import com.kira.kdownloader.settings.ui.components.TextEntryPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

// ---------------------------------------------------------------------------
// Section 2 — Download preferences
// ---------------------------------------------------------------------------

@Composable
fun DownloadSectionContent(settings: AppSettings, vm: SettingsViewModel) {
    val d = settings.download

    PreferenceGroupTitle(stringResource(R.string.type_and_format))
    SingleChoicePreference(stringResource(R.string.download_type), DownloadType.values().toList(), d.downloadType) {
        vm.setDownload(d.withDownloadType(it))
    }
    SingleChoicePreference(stringResource(R.string.video_format), VideoFormat.values().toList(), d.videoFormat,
        enabled = d.downloadType != DownloadType.AUDIO_ONLY) {
        vm.setDownload(d.withVideoFormat(it))
    }
    SingleChoicePreference(stringResource(R.string.audio_format), AudioFormat.values().toList(), d.audioFormat) {
        vm.setDownload(d.withAudioFormat(it))
    }

    PreferenceGroupTitle(stringResource(R.string.quality))
    SingleChoicePreference(stringResource(R.string.default_video_quality), VideoQuality.values().toList(), d.videoQuality,
        enabled = d.downloadType != DownloadType.AUDIO_ONLY) {
        vm.setDownload(d.withVideoQuality(it))
    }
    SingleChoicePreference(stringResource(R.string.default_audio_quality), AudioQuality.values().toList(), d.audioQuality) {
        vm.setDownload(d.withAudioQuality(it))
    }
    SingleChoicePreference(stringResource(R.string.frame_rate_preference), FrameRatePreference.values().toList(), d.frameRate,
        enabled = d.downloadType != DownloadType.AUDIO_ONLY) {
        vm.setDownload(d.withFrameRate(it))
    }
    SwitchPreference(stringResource(R.string.prefer_hdr_when_available), d.preferHdr,
        enabled = d.downloadType != DownloadType.AUDIO_ONLY) {
        vm.setDownload(d.withPreferHdr(it))
    }
    SwitchPreference(stringResource(R.string.prefer_android_compatible_codecs), d.preferAndroidCompatibleCodecs,
        subtitle = stringResource(R.string.choose_codecs_that_play_on_most_android_media_players)) {
        vm.setDownload(d.withPreferAndroidCompatibleCodecs(it))
    }
    SwitchPreference(stringResource(R.string.automatically_fall_back), d.autoFallbackQuality,
        subtitle = stringResource(R.string.use_the_closest_available_quality_or_format_when_the_requested_one_is)) {
        vm.setDownload(d.withAutoFallbackQuality(it))
    }
    SwitchPreference(stringResource(R.string.ask_before_each_download), d.askQualityBeforeEachDownload,
        subtitle = stringResource(R.string.show_a_quality_selection_dialog_every_time)) {
        vm.setDownload(d.withAskQualityBeforeEachDownload(it))
    }
    PreferenceNote(stringResource(R.string.when_a_format_or_quality_is_unavailable_the_app_explains_the_fallback))

    PreferenceGroupTitle(stringResource(R.string.metadata))
    SwitchPreference(stringResource(R.string.download_thumbnail_when_available), d.downloadThumbnail) {
        vm.setDownload(d.withDownloadThumbnail(it))
    }
    SwitchPreference(stringResource(R.string.embed_thumbnail_in_audio_files), d.embedThumbnail,
        enabled = d.downloadThumbnail) {
        vm.setDownload(d.withEmbedThumbnail(it))
    }
    SwitchPreference(stringResource(R.string.embed_title_artist_album_and_more), d.embedMetadata) {
        vm.setDownload(d.withEmbedMetadata(it))
    }
    SwitchPreference(stringResource(R.string.preserve_source_upload_date), d.preserveUploadDate,
        subtitle = stringResource(R.string.when_supported_by_the_source)) {
        vm.setDownload(d.withPreserveUploadDate(it))
    }
}

// ---------------------------------------------------------------------------
// Section 3 — Storage & file management
// ---------------------------------------------------------------------------

@Composable
fun StorageSectionContent(settings: AppSettings, vm: SettingsViewModel) {
    val s = settings.storage
    val scope = rememberCoroutineScope()
    var pendingSlot by remember { mutableStateOf(FolderSlot.DOWNLOAD) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.onFolderSelected(pendingSlot, uri)
    }
    fun pick(slot: FolderSlot) { pendingSlot = slot; picker.launch(null) }

    PreferenceGroupTitle(stringResource(R.string.folders))
    FolderRow("Default download folder", s.downloadFolderUri, vm.folders, { pick(FolderSlot.DOWNLOAD) }) {
        vm.clearFolder(FolderSlot.DOWNLOAD)
    }
    FolderRow("Video folder (optional)", s.videoFolderUri, vm.folders, { pick(FolderSlot.VIDEO) }) {
        vm.clearFolder(FolderSlot.VIDEO)
    }
    FolderRow("Audio folder (optional)", s.audioFolderUri, vm.folders, { pick(FolderSlot.AUDIO) }) {
        vm.clearFolder(FolderSlot.AUDIO)
    }
    FolderRow("Temporary files folder (optional)", s.tempFolderUri, vm.folders, { pick(FolderSlot.TEMP) }) {
        vm.clearFolder(FolderSlot.TEMP)
    }
    val available = FolderAccessManager.formatBytes(vm.folders.availableBytes())
    PreferenceNote("Available storage: $available. Folders are chosen with the Android system picker and access is remembered across restarts.")
    SwitchPreference(stringResource(R.string.warn_before_download_if_space_is_low), s.warnOnLowSpace) {
        vm.setStorage(s.withWarnOnLowSpace(it))
    }

    PreferenceGroupTitle(stringResource(R.string.file_names))
    SingleChoicePreference(stringResource(R.string.on_filename_conflict), FilenameConflict.values().toList(), s.filenameConflict) {
        vm.setStorage(s.withFilenameConflict(it))
    }
    TextEntryPreference(
        title = stringResource(R.string.filename_template),
        value = s.filenameTemplate,
        onValueChange = { vm.setStorage(s.withFilenameTemplate(it)) },
        summary = s.filenameTemplate,
        validate = { (FilenameTemplate.validate(it) as? FilenameTemplate.Validation.Invalid)?.reason },
    )
    PreferenceNote("Variables: ${FilenameTemplate.VARIABLES.joinToString(" ") { "{$it}" }}")
    PreferenceNote("Example: ${FilenameTemplate.example(s.filenameTemplate, s.maxFilenameLength)}.mp4")
    IntSliderPreference(
        title = stringResource(R.string.maximum_filename_length),
        value = s.maxFilenameLength,
        valueRange = FilenameTemplate.MIN_LENGTH..FilenameTemplate.MAX_LENGTH,
        onValueChange = { vm.setStorage(s.withMaxFilenameLength(it)) },
        valueLabel = { "$it chars" },
    )
    PreferenceNote(stringResource(R.string.characters_android_does_not_allow_in_filenames_are_removed_or_replaced))
    SingleChoicePreference(stringResource(R.string.organize_into_subfolders), SubfolderOrganization.values().toList(), s.subfolderOrganization) {
        vm.setStorage(s.withSubfolderOrganization(it))
    }

    PreferenceGroupTitle(stringResource(R.string.temporary_files))
    val recoverable by produceState(initialValue = -1L, s) {
        value = vm.recoverableTempBytes()
    }
    var confirmClear by remember { mutableStateOf(false) }
    ClickablePreference(
        title = stringResource(R.string.clear_temporary_files),
        subtitle = if (recoverable < 0) "Calculating…" else "Recoverable: ${FolderAccessManager.formatBytes(recoverable)}",
        onClick = { confirmClear = true },
    )
    PreferenceNote(stringResource(R.string.clearing_temporary_files_never_deletes_your_completed_downloads))
    if (confirmClear) {
        ConfirmDialog(
            title = stringResource(R.string.clear_temporary_files_9fffa1),
            message = stringResource(R.string.this_removes_intermediate_and_cache_files_only_completed_downloads_are),
            confirmLabel = stringResource(R.string.clear),
            onConfirm = { scope.launch { vm.clearTempFiles() } },
            onDismiss = { confirmClear = false },
        )
    }
}

@Composable
private fun FolderRow(
    label: String,
    uri: String,
    folders: FolderAccessManager,
    onPick: () -> Unit,
    onClear: () -> Unit,
) {
    val hasAccess = uri.isNotBlank() && folders.hasAccess(uri)
    val summary = when {
        uri.isBlank() -> "Not set — tap to choose"
        !hasAccess -> "${folders.displayName(uri)} — access lost, tap to reselect"
        else -> folders.displayName(uri)
    }
    ClickablePreference(title = label, subtitle = summary, onClick = onPick, destructive = uri.isNotBlank() && !hasAccess)
    if (uri.isNotBlank()) {
        ClickablePreference(title = "Clear \"$label\"", onClick = onClear)
    }
}

// ---------------------------------------------------------------------------
// Section 4 — Download behavior
// ---------------------------------------------------------------------------

@Composable
fun BehaviorSectionContent(settings: AppSettings, vm: SettingsViewModel) {
    val b = settings.behavior

    PreferenceGroupTitle(stringResource(R.string.queue))
    SwitchPreference(stringResource(R.string.ask_for_confirmation_before_downloading), b.confirmBeforeDownload,
        subtitle = if (b.confirmBeforeDownload) "You'll confirm each download" else "Downloads start immediately") {
        vm.setBehavior(b.withConfirmBeforeDownload(it))
    }
    IntSliderPreference(stringResource(R.string.maximum_simultaneous_downloads), b.maxSimultaneousDownloads, 1..5,
        onValueChange = { vm.setBehavior(b.withMaxSimultaneousDownloads(it)) }, valueLabel = { "$it" })
    IntSliderPreference(stringResource(R.string.maximum_retries_for_failed_downloads), b.maxRetryCount, 0..10,
        onValueChange = { vm.setBehavior(b.withMaxRetryCount(it)) }, valueLabel = { "$it" })
    SingleChoicePreference(stringResource(R.string.add_new_downloads_to), QueuePosition.values().toList(), b.newDownloadPosition) {
        vm.setBehavior(b.withNewDownloadPosition(it))
    }
    PreferenceNote(stringResource(R.string.the_parallel_limit_applies_to_newly_started_downloads_downloads_alread))

    PreferenceGroupTitle(stringResource(R.string.resume_and_duplicates))
    SwitchPreference(stringResource(R.string.automatically_resume_interrupted_downloads), b.autoResumeInterrupted) {
        vm.setBehavior(b.withAutoResumeInterrupted(it))
    }
    SwitchPreference(stringResource(R.string.resume_queued_downloads_after_restart), b.resumeQueueAfterRestart) {
        vm.setBehavior(b.withResumeQueueAfterRestart(it))
    }
    SwitchPreference(stringResource(R.string.prevent_duplicate_downloads), b.preventDuplicates) {
        vm.setBehavior(b.withPreventDuplicates(it))
    }
    SingleChoicePreference(stringResource(R.string.duplicate_detection_method), DuplicateDetection.values().toList(), b.duplicateDetection,
        enabled = b.preventDuplicates) {
        vm.setBehavior(b.withDuplicateDetection(it))
    }

    PreferenceGroupTitle(stringResource(R.string.power_and_thermal))
    SwitchPreference(stringResource(R.string.keep_the_screen_awake_during_downloads), b.keepScreenAwake) {
        vm.setBehavior(b.withKeepScreenAwake(it))
    }
    SwitchPreference(stringResource(R.string.pause_when_battery_saver_is_on), b.pauseOnBatterySaver) {
        vm.setBehavior(b.withPauseOnBatterySaver(it))
    }
    SwitchPreference(stringResource(R.string.pause_when_the_device_is_too_hot), b.pauseOnOverheat,
        subtitle = stringResource(R.string.when_thermal_status_is_available)) {
        vm.setBehavior(b.withPauseOnOverheat(it))
    }
    SwitchPreference(stringResource(R.string.automatically_retry_when_connectivity_returns), b.autoRetryOnReconnect) {
        vm.setBehavior(b.withAutoRetryOnReconnect(it))
    }

    PreferenceGroupTitle(stringResource(R.string.limits_and_scheduling))
    SwitchPreference(stringResource(R.string.limit_download_speed), b.speedLimitEnabled) {
        vm.setBehavior(b.withSpeedLimitEnabled(it))
    }
    TextEntryPreference(
        title = stringResource(R.string.speed_limit_kb_s),
        value = b.speedLimitKbps.toString(),
        onValueChange = { it.toIntOrNull()?.let { v -> vm.setBehavior(b.withSpeedLimitKbps(v)) } },
        enabled = b.speedLimitEnabled,
        keyboardNumeric = true,
        validate = { if ((it.toIntOrNull() ?: 0) < 1) "Enter a value of 1 or more" else null },
    )
    SwitchPreference(stringResource(R.string.only_download_within_a_time_window), b.scheduleEnabled) {
        vm.setBehavior(b.withScheduleEnabled(it))
    }
    TextEntryPreference(
        title = stringResource(R.string.window_start_hh_mm),
        value = formatMinutes(b.scheduleStartMinutes),
        onValueChange = { parseMinutes(it)?.let { m -> vm.setBehavior(b.withScheduleStartMinutes(m)) } },
        enabled = b.scheduleEnabled,
        validate = { if (parseMinutes(it) == null) "Use 24-hour HH:MM" else null },
    )
    TextEntryPreference(
        title = stringResource(R.string.window_end_hh_mm),
        value = formatMinutes(b.scheduleEndMinutes),
        onValueChange = { parseMinutes(it)?.let { m -> vm.setBehavior(b.withScheduleEndMinutes(m)) } },
        enabled = b.scheduleEnabled,
        validate = { if (parseMinutes(it) == null) "Use 24-hour HH:MM" else null },
    )

    PreferenceGroupTitle(stringResource(R.string.after_completion))
    SingleChoicePreference(stringResource(R.string.when_a_download_finishes), PostDownloadAction.values().toList(), b.postDownloadAction) {
        vm.setBehavior(b.withPostDownloadAction(it))
    }
}

private fun formatMinutes(minutes: Int): String {
    val m = minutes.coerceIn(0, 1439)
    return "%02d:%02d".format(m / 60, m % 60)
}

private fun parseMinutes(text: String): Int? {
    val parts = text.trim().split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val min = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || min !in 0..59) return null
    return h * 60 + min
}

// ---------------------------------------------------------------------------
// Section 5 — Network
// ---------------------------------------------------------------------------

@Composable
fun NetworkSectionContent(settings: AppSettings, vm: SettingsViewModel) {
    val n = settings.network

    PreferenceGroupTitle(stringResource(R.string.connectivity))
    SingleChoicePreference(stringResource(R.string.allowed_networks), NetworkType.values().toList(), n.allowedNetworks) {
        vm.setNetwork(n.withAllowedNetworks(it))
    }
    SwitchPreference(stringResource(R.string.allow_downloads_while_roaming), n.allowRoaming) {
        vm.setNetwork(n.withAllowRoaming(it))
    }
    SwitchPreference(stringResource(R.string.confirm_before_using_mobile_data), n.confirmMobileData) {
        vm.setNetwork(n.withConfirmMobileData(it))
    }
    TextEntryPreference(
        title = stringResource(R.string.mobile_data_warning_threshold_mb),
        value = n.mobileDataWarningMb.toString(),
        onValueChange = { it.toIntOrNull()?.let { v -> vm.setNetwork(n.withMobileDataWarningMb(v)) } },
        keyboardNumeric = true,
        validate = { if ((it.toIntOrNull() ?: 0) < 1) "Enter a value of 1 or more" else null },
    )
    SwitchPreference(stringResource(R.string.treat_metered_wi_fi_as_mobile_data), n.treatMeteredWifiAsMobile) {
        vm.setNetwork(n.withTreatMeteredWifiAsMobile(it))
    }
    SwitchPreference(stringResource(R.string.pause_when_the_network_changes), n.pauseOnNetworkChange) {
        vm.setNetwork(n.withPauseOnNetworkChange(it))
    }
    SwitchPreference(stringResource(R.string.retry_automatically_after_connection_loss), n.retryAfterConnectionLoss) {
        vm.setNetwork(n.withRetryAfterConnectionLoss(it))
    }
    PreferenceNote(stringResource(R.string.if_a_network_setting_blocks_a_download_the_app_explains_which_one_and))

    PreferenceGroupTitle(stringResource(R.string.proxy))
    val proxyEnabled = n.proxyType != ProxyType.DISABLED
    SingleChoicePreference(stringResource(R.string.proxy), ProxyType.values().toList(), n.proxyType) {
        vm.setNetwork(n.withProxyType(it))
    }
    TextEntryPreference(
        title = stringResource(R.string.host),
        value = n.proxyHost,
        onValueChange = { vm.setNetwork(n.withProxyHost(it.trim())) },
        enabled = proxyEnabled,
        validate = { if (it.isNotBlank() && !ProxyValidator.isValidHost(it)) "Enter a valid host or IP" else null },
    )
    TextEntryPreference(
        title = stringResource(R.string.port),
        value = if (n.proxyPort == 0) "" else n.proxyPort.toString(),
        onValueChange = { vm.setNetwork(n.withProxyPort(it.toIntOrNull() ?: 0)) },
        enabled = proxyEnabled,
        keyboardNumeric = true,
        placeholder = "1-65535",
        validate = { val p = it.toIntOrNull(); if (p == null || p !in 1..65535) "Port must be 1-65535" else null },
    )
    TextEntryPreference(
        title = stringResource(R.string.username_optional),
        value = n.proxyUsername,
        onValueChange = { vm.setNetwork(n.withProxyUsername(it)) },
        enabled = proxyEnabled,
    )
    TextEntryPreference(
        title = stringResource(R.string.password_optional),
        value = "",
        onValueChange = { vm.setProxyPassword(it) },
        summary = if (n.proxyPasswordSet) "•••••• (stored securely)" else "Not set",
        enabled = proxyEnabled,
        isPassword = true,
    )
    PreferenceNote(stringResource(R.string.proxy_passwords_are_stored_using_android_keystore_backed_encryption_an))

    // Test connection
    val scope = rememberCoroutineScope()
    var testResult by remember { mutableStateOf<String?>(null) }
    ClickablePreference(
        title = stringResource(R.string.test_connection),
        subtitle = testResult,
        enabled = proxyEnabled && ProxyValidator.isValidHost(n.proxyHost) && ProxyValidator.isValidPort(n.proxyPort),
        onClick = {
            testResult = "Testing…"
            scope.launch {
                testResult = testProxy(n.proxyHost, n.proxyPort)
            }
        },
    )
}

private suspend fun testProxy(host: String, port: Int): String = withContext(Dispatchers.IO) {
    runCatching {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(host, port), 5000)
        }
        "Connection succeeded"
    }.getOrElse { "Connection failed: ${it.message ?: "unreachable"}" }
}

// ---------------------------------------------------------------------------
// Section 6 — Subtitles & captions
// ---------------------------------------------------------------------------

@Composable
fun SubtitlesSectionContent(settings: AppSettings, vm: SettingsViewModel) {
    val sub = settings.subtitles
    val enabled = sub.downloadSubtitles
    val languages = LanguageManager.SUPPORTED.drop(1).map { it.tag to it.display }
    val fallbackLanguages = listOf("" to "None") + languages

    SwitchPreference(stringResource(R.string.download_subtitles_when_available), sub.downloadSubtitles) {
        vm.setSubtitles(sub.withDownloadSubtitles(it))
    }
    LabeledChoicePreference(stringResource(R.string.preferred_language), languages, sub.preferredLanguage,
        enabled = enabled) { vm.setSubtitles(sub.withPreferredLanguage(it)) }
    LabeledChoicePreference(stringResource(R.string.fallback_language), fallbackLanguages, sub.fallbackLanguage,
        enabled = enabled) { vm.setSubtitles(sub.withFallbackLanguage(it)) }
    SingleChoicePreference(stringResource(R.string.subtitle_type), SubtitleTypePreference.values().toList(), sub.subtitleType,
        enabled = enabled) { vm.setSubtitles(sub.withSubtitleType(it)) }
    SingleChoicePreference(stringResource(R.string.subtitle_format), SubtitleFormat.values().toList(), sub.format,
        enabled = enabled) { vm.setSubtitles(sub.withFormat(it)) }
    SwitchPreference(stringResource(R.string.embed_subtitles_in_supported_video), sub.embedInVideo,
        enabled = enabled) { vm.setSubtitles(sub.withEmbedInVideo(it)) }
    SwitchPreference(stringResource(R.string.also_save_subtitles_as_separate_files), sub.saveAsSeparateFiles,
        enabled = enabled) { vm.setSubtitles(sub.withSaveAsSeparateFiles(it)) }
    SwitchPreference(stringResource(R.string.include_all_available_languages), sub.includeAllLanguages,
        enabled = enabled) { vm.setSubtitles(sub.withIncludeAllLanguages(it)) }
    SwitchPreference(stringResource(R.string.add_language_codes_to_filenames), sub.addLanguageCodeToFilename,
        enabled = enabled) { vm.setSubtitles(sub.withAddLanguageCodeToFilename(it)) }
}
