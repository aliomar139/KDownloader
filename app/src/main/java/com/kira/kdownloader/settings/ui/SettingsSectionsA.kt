package com.kira.kdownloader.settings.ui

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

    PreferenceGroupTitle("Type & format")
    SingleChoicePreference("Download type", DownloadType.entries.toList(), d.downloadType) {
        vm.setDownload(d.copy(downloadType = it))
    }
    SingleChoicePreference("Video format", VideoFormat.entries.toList(), d.videoFormat,
        enabled = d.downloadType != DownloadType.AUDIO_ONLY) {
        vm.setDownload(d.copy(videoFormat = it))
    }
    SingleChoicePreference("Audio format", AudioFormat.entries.toList(), d.audioFormat) {
        vm.setDownload(d.copy(audioFormat = it))
    }

    PreferenceGroupTitle("Quality")
    SingleChoicePreference("Default video quality", VideoQuality.entries.toList(), d.videoQuality,
        enabled = d.downloadType != DownloadType.AUDIO_ONLY) {
        vm.setDownload(d.copy(videoQuality = it))
    }
    SingleChoicePreference("Default audio quality", AudioQuality.entries.toList(), d.audioQuality) {
        vm.setDownload(d.copy(audioQuality = it))
    }
    SingleChoicePreference("Frame-rate preference", FrameRatePreference.entries.toList(), d.frameRate,
        enabled = d.downloadType != DownloadType.AUDIO_ONLY) {
        vm.setDownload(d.copy(frameRate = it))
    }
    SwitchPreference("Prefer HDR when available", d.preferHdr,
        enabled = d.downloadType != DownloadType.AUDIO_ONLY) {
        vm.setDownload(d.copy(preferHdr = it))
    }
    SwitchPreference("Prefer Android-compatible codecs", d.preferAndroidCompatibleCodecs,
        subtitle = "Choose codecs that play on most Android media players") {
        vm.setDownload(d.copy(preferAndroidCompatibleCodecs = it))
    }
    SwitchPreference("Automatically fall back", d.autoFallbackQuality,
        subtitle = "Use the closest available quality or format when the requested one is missing") {
        vm.setDownload(d.copy(autoFallbackQuality = it))
    }
    SwitchPreference("Ask before each download", d.askQualityBeforeEachDownload,
        subtitle = "Show a quality-selection dialog every time") {
        vm.setDownload(d.copy(askQualityBeforeEachDownload = it))
    }
    PreferenceNote("When a format or quality is unavailable, the app explains the fallback it used instead of failing silently.")

    PreferenceGroupTitle("Metadata")
    SwitchPreference("Download thumbnail when available", d.downloadThumbnail) {
        vm.setDownload(d.copy(downloadThumbnail = it))
    }
    SwitchPreference("Embed thumbnail in audio files", d.embedThumbnail,
        enabled = d.downloadThumbnail) {
        vm.setDownload(d.copy(embedThumbnail = it))
    }
    SwitchPreference("Embed title, artist, album & more", d.embedMetadata) {
        vm.setDownload(d.copy(embedMetadata = it))
    }
    SwitchPreference("Preserve source upload date", d.preserveUploadDate,
        subtitle = "When supported by the source") {
        vm.setDownload(d.copy(preserveUploadDate = it))
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

    PreferenceGroupTitle("Folders")
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
    SwitchPreference("Warn before download if space is low", s.warnOnLowSpace) {
        vm.setStorage(s.copy(warnOnLowSpace = it))
    }

    PreferenceGroupTitle("File names")
    SingleChoicePreference("On filename conflict", FilenameConflict.entries.toList(), s.filenameConflict) {
        vm.setStorage(s.copy(filenameConflict = it))
    }
    TextEntryPreference(
        title = "Filename template",
        value = s.filenameTemplate,
        onValueChange = { vm.setStorage(s.copy(filenameTemplate = it)) },
        summary = s.filenameTemplate,
        validate = { (FilenameTemplate.validate(it) as? FilenameTemplate.Validation.Invalid)?.reason },
    )
    PreferenceNote("Variables: ${FilenameTemplate.VARIABLES.joinToString(" ") { "{$it}" }}")
    PreferenceNote("Example: ${FilenameTemplate.example(s.filenameTemplate, s.maxFilenameLength)}.mp4")
    IntSliderPreference(
        title = "Maximum filename length",
        value = s.maxFilenameLength,
        valueRange = FilenameTemplate.MIN_LENGTH..FilenameTemplate.MAX_LENGTH,
        onValueChange = { vm.setStorage(s.copy(maxFilenameLength = it)) },
        valueLabel = { "$it chars" },
    )
    PreferenceNote("Characters Android does not allow in filenames are removed or replaced automatically.")
    SingleChoicePreference("Organize into subfolders", SubfolderOrganization.entries.toList(), s.subfolderOrganization) {
        vm.setStorage(s.copy(subfolderOrganization = it))
    }

    PreferenceGroupTitle("Temporary files")
    val recoverable by produceState(initialValue = -1L, s) {
        value = vm.recoverableTempBytes()
    }
    var confirmClear by remember { mutableStateOf(false) }
    ClickablePreference(
        title = "Clear temporary files",
        subtitle = if (recoverable < 0) "Calculating…" else "Recoverable: ${FolderAccessManager.formatBytes(recoverable)}",
        onClick = { confirmClear = true },
    )
    PreferenceNote("Clearing temporary files never deletes your completed downloads.")
    if (confirmClear) {
        ConfirmDialog(
            title = "Clear temporary files?",
            message = "This removes intermediate and cache files only. Completed downloads are kept.",
            confirmLabel = "Clear",
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

    PreferenceGroupTitle("Queue")
    SwitchPreference("Ask for confirmation before downloading", b.confirmBeforeDownload,
        subtitle = if (b.confirmBeforeDownload) "You'll confirm each download" else "Downloads start immediately") {
        vm.setBehavior(b.copy(confirmBeforeDownload = it))
    }
    IntSliderPreference("Maximum simultaneous downloads", b.maxSimultaneousDownloads, 1..5,
        onValueChange = { vm.setBehavior(b.copy(maxSimultaneousDownloads = it)) }, valueLabel = { "$it" })
    IntSliderPreference("Maximum retries for failed downloads", b.maxRetryCount, 0..10,
        onValueChange = { vm.setBehavior(b.copy(maxRetryCount = it)) }, valueLabel = { "$it" })
    SingleChoicePreference("Add new downloads to", QueuePosition.entries.toList(), b.newDownloadPosition) {
        vm.setBehavior(b.copy(newDownloadPosition = it))
    }
    PreferenceNote("The parallel limit applies to newly started downloads; downloads already running are not interrupted.")

    PreferenceGroupTitle("Resume & duplicates")
    SwitchPreference("Automatically resume interrupted downloads", b.autoResumeInterrupted) {
        vm.setBehavior(b.copy(autoResumeInterrupted = it))
    }
    SwitchPreference("Resume queued downloads after restart", b.resumeQueueAfterRestart) {
        vm.setBehavior(b.copy(resumeQueueAfterRestart = it))
    }
    SwitchPreference("Prevent duplicate downloads", b.preventDuplicates) {
        vm.setBehavior(b.copy(preventDuplicates = it))
    }
    SingleChoicePreference("Duplicate detection method", DuplicateDetection.entries.toList(), b.duplicateDetection,
        enabled = b.preventDuplicates) {
        vm.setBehavior(b.copy(duplicateDetection = it))
    }

    PreferenceGroupTitle("Power & thermal")
    SwitchPreference("Keep the screen awake during downloads", b.keepScreenAwake) {
        vm.setBehavior(b.copy(keepScreenAwake = it))
    }
    SwitchPreference("Pause when battery saver is on", b.pauseOnBatterySaver) {
        vm.setBehavior(b.copy(pauseOnBatterySaver = it))
    }
    SwitchPreference("Pause when the device is too hot", b.pauseOnOverheat,
        subtitle = "When thermal status is available") {
        vm.setBehavior(b.copy(pauseOnOverheat = it))
    }
    SwitchPreference("Automatically retry when connectivity returns", b.autoRetryOnReconnect) {
        vm.setBehavior(b.copy(autoRetryOnReconnect = it))
    }

    PreferenceGroupTitle("Limits & scheduling")
    SwitchPreference("Limit download speed", b.speedLimitEnabled) {
        vm.setBehavior(b.copy(speedLimitEnabled = it))
    }
    TextEntryPreference(
        title = "Speed limit (KB/s)",
        value = b.speedLimitKbps.toString(),
        onValueChange = { it.toIntOrNull()?.let { v -> vm.setBehavior(b.copy(speedLimitKbps = v)) } },
        enabled = b.speedLimitEnabled,
        keyboardNumeric = true,
        validate = { if ((it.toIntOrNull() ?: 0) < 1) "Enter a value of 1 or more" else null },
    )
    SwitchPreference("Only download within a time window", b.scheduleEnabled) {
        vm.setBehavior(b.copy(scheduleEnabled = it))
    }
    TextEntryPreference(
        title = "Window start (HH:MM)",
        value = formatMinutes(b.scheduleStartMinutes),
        onValueChange = { parseMinutes(it)?.let { m -> vm.setBehavior(b.copy(scheduleStartMinutes = m)) } },
        enabled = b.scheduleEnabled,
        validate = { if (parseMinutes(it) == null) "Use 24-hour HH:MM" else null },
    )
    TextEntryPreference(
        title = "Window end (HH:MM)",
        value = formatMinutes(b.scheduleEndMinutes),
        onValueChange = { parseMinutes(it)?.let { m -> vm.setBehavior(b.copy(scheduleEndMinutes = m)) } },
        enabled = b.scheduleEnabled,
        validate = { if (parseMinutes(it) == null) "Use 24-hour HH:MM" else null },
    )

    PreferenceGroupTitle("After completion")
    SingleChoicePreference("When a download finishes", PostDownloadAction.entries.toList(), b.postDownloadAction) {
        vm.setBehavior(b.copy(postDownloadAction = it))
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

    PreferenceGroupTitle("Connectivity")
    SingleChoicePreference("Allowed networks", NetworkType.entries.toList(), n.allowedNetworks) {
        vm.setNetwork(n.copy(allowedNetworks = it))
    }
    SwitchPreference("Allow downloads while roaming", n.allowRoaming) {
        vm.setNetwork(n.copy(allowRoaming = it))
    }
    SwitchPreference("Confirm before using mobile data", n.confirmMobileData) {
        vm.setNetwork(n.copy(confirmMobileData = it))
    }
    TextEntryPreference(
        title = "Mobile-data warning threshold (MB)",
        value = n.mobileDataWarningMb.toString(),
        onValueChange = { it.toIntOrNull()?.let { v -> vm.setNetwork(n.copy(mobileDataWarningMb = v)) } },
        keyboardNumeric = true,
        validate = { if ((it.toIntOrNull() ?: 0) < 1) "Enter a value of 1 or more" else null },
    )
    SwitchPreference("Treat metered Wi-Fi as mobile data", n.treatMeteredWifiAsMobile) {
        vm.setNetwork(n.copy(treatMeteredWifiAsMobile = it))
    }
    SwitchPreference("Pause when the network changes", n.pauseOnNetworkChange) {
        vm.setNetwork(n.copy(pauseOnNetworkChange = it))
    }
    SwitchPreference("Retry automatically after connection loss", n.retryAfterConnectionLoss) {
        vm.setNetwork(n.copy(retryAfterConnectionLoss = it))
    }
    PreferenceNote("If a network setting blocks a download, the app explains which one and offers to open the relevant settings.")

    PreferenceGroupTitle("Proxy")
    val proxyEnabled = n.proxyType != ProxyType.DISABLED
    SingleChoicePreference("Proxy", ProxyType.entries.toList(), n.proxyType) {
        vm.setNetwork(n.copy(proxyType = it))
    }
    TextEntryPreference(
        title = "Host",
        value = n.proxyHost,
        onValueChange = { vm.setNetwork(n.copy(proxyHost = it.trim())) },
        enabled = proxyEnabled,
        validate = { if (it.isNotBlank() && !ProxyValidator.isValidHost(it)) "Enter a valid host or IP" else null },
    )
    TextEntryPreference(
        title = "Port",
        value = if (n.proxyPort == 0) "" else n.proxyPort.toString(),
        onValueChange = { vm.setNetwork(n.copy(proxyPort = it.toIntOrNull() ?: 0)) },
        enabled = proxyEnabled,
        keyboardNumeric = true,
        placeholder = "1-65535",
        validate = { val p = it.toIntOrNull(); if (p == null || p !in 1..65535) "Port must be 1-65535" else null },
    )
    TextEntryPreference(
        title = "Username (optional)",
        value = n.proxyUsername,
        onValueChange = { vm.setNetwork(n.copy(proxyUsername = it)) },
        enabled = proxyEnabled,
    )
    TextEntryPreference(
        title = "Password (optional)",
        value = "",
        onValueChange = { vm.setProxyPassword(it) },
        summary = if (n.proxyPasswordSet) "•••••• (stored securely)" else "Not set",
        enabled = proxyEnabled,
        isPassword = true,
    )
    PreferenceNote("Proxy passwords are stored using Android Keystore-backed encryption and are never shown or exported.")

    // Test connection
    val scope = rememberCoroutineScope()
    var testResult by remember { mutableStateOf<String?>(null) }
    ClickablePreference(
        title = "Test connection",
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

    SwitchPreference("Download subtitles when available", sub.downloadSubtitles) {
        vm.setSubtitles(sub.copy(downloadSubtitles = it))
    }
    LabeledChoicePreference("Preferred language", languages, sub.preferredLanguage,
        enabled = enabled) { vm.setSubtitles(sub.copy(preferredLanguage = it)) }
    LabeledChoicePreference("Fallback language", fallbackLanguages, sub.fallbackLanguage,
        enabled = enabled) { vm.setSubtitles(sub.copy(fallbackLanguage = it)) }
    SingleChoicePreference("Subtitle type", SubtitleTypePreference.entries.toList(), sub.subtitleType,
        enabled = enabled) { vm.setSubtitles(sub.copy(subtitleType = it)) }
    SingleChoicePreference("Subtitle format", SubtitleFormat.entries.toList(), sub.format,
        enabled = enabled) { vm.setSubtitles(sub.copy(format = it)) }
    SwitchPreference("Embed subtitles in supported video", sub.embedInVideo,
        enabled = enabled) { vm.setSubtitles(sub.copy(embedInVideo = it)) }
    SwitchPreference("Also save subtitles as separate files", sub.saveAsSeparateFiles,
        enabled = enabled) { vm.setSubtitles(sub.copy(saveAsSeparateFiles = it)) }
    SwitchPreference("Include all available languages", sub.includeAllLanguages,
        enabled = enabled) { vm.setSubtitles(sub.copy(includeAllLanguages = it)) }
    SwitchPreference("Add language codes to filenames", sub.addLanguageCodeToFilename,
        enabled = enabled) { vm.setSubtitles(sub.copy(addLanguageCodeToFilename = it)) }
}
