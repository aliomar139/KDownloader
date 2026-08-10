package com.kira.kdownloader.settings

/**
 * Immutable settings model for the whole app.
 *
 * Every option that has a fixed set of choices is modelled as an enum implementing
 * [SettingOption]. Each option carries a *stable* [key] that is what gets written to storage — the
 * enum ordinal is never persisted, so reordering or inserting enum constants can never corrupt a
 * stored preference. Display [label]s live next to the keys so the UI and storage never drift.
 *
 * The grouped `*Settings` data classes are plain immutable snapshots. They are produced by
 * [SettingsRepository] when reading and consumed when writing, which keeps the UI free of any
 * storage-key strings.
 */
interface SettingOption {
    /** Stable identifier persisted to storage. Must never change once shipped. */
    val key: String

    /** Human readable label shown in the UI. */
    val label: String
}

/** Resolves a stored [key] back to an enum value, falling back safely to [default]. */
fun <T> optionFromKey(values: Array<T>, key: String?, default: T): T where T : Enum<T>, T : SettingOption =
    values.firstOrNull { it.key == key } ?: default

// ---------------------------------------------------------------------------
// Section 2 — Download preferences
// ---------------------------------------------------------------------------

enum class DownloadType(override val key: String, override val label: String) : SettingOption {
    VIDEO("video", "Video"),
    AUDIO_ONLY("audio", "Audio only"),
    ASK("ask", "Ask every time"),
}

enum class VideoFormat(override val key: String, override val label: String) : SettingOption {
    MP4("mp4", "MP4"),
    WEBM("webm", "WebM"),
    BEST_COMPATIBLE("best_compatible", "Best compatible format"),
}

enum class AudioFormat(override val key: String, override val label: String) : SettingOption {
    MP3("mp3", "MP3"),
    M4A("m4a", "M4A"),
    OPUS("opus", "Opus"),
    ORIGINAL("original", "Original audio"),
}

enum class VideoQuality(override val key: String, override val label: String) : SettingOption {
    BEST("best", "Best available"),
    P2160("2160", "2160p"),
    P1440("1440", "1440p"),
    P1080("1080", "1080p"),
    P720("720", "720p"),
    P480("480", "480p"),
    P360("360", "360p"),
    ASK("ask", "Ask every time"),
}

enum class AudioQuality(override val key: String, override val label: String) : SettingOption {
    BEST("best", "Best available"),
    K320("320", "320 kbps"),
    K256("256", "256 kbps"),
    K192("192", "192 kbps"),
    K128("128", "128 kbps"),
    K96("96", "96 kbps"),
}

enum class FrameRatePreference(override val key: String, override val label: String) : SettingOption {
    BEST("best", "Best available"),
    PREFER_60("60", "Prefer 60 FPS"),
    PREFER_30("30", "Prefer 30 FPS"),
}

data class DownloadSettings(
    val downloadType: DownloadType = DownloadType.VIDEO,
    val videoFormat: VideoFormat = VideoFormat.MP4,
    val audioFormat: AudioFormat = AudioFormat.MP3,
    val videoQuality: VideoQuality = VideoQuality.BEST,
    val audioQuality: AudioQuality = AudioQuality.BEST,
    val frameRate: FrameRatePreference = FrameRatePreference.BEST,
    val preferHdr: Boolean = false,
    val preferAndroidCompatibleCodecs: Boolean = true,
    val autoFallbackQuality: Boolean = true,
    val askQualityBeforeEachDownload: Boolean = false,
    val downloadThumbnail: Boolean = true,
    val embedThumbnail: Boolean = true,
    val embedMetadata: Boolean = true,
    val preserveUploadDate: Boolean = true,
)

// ---------------------------------------------------------------------------
// Section 3 — Storage and file management
// ---------------------------------------------------------------------------

enum class FilenameConflict(override val key: String, override val label: String) : SettingOption {
    ADD_NUMBER("add_number", "Automatically add a number"),
    REPLACE("replace", "Replace after confirmation"),
    SKIP("skip", "Skip the download"),
    ASK("ask", "Ask every time"),
}

enum class SubfolderOrganization(override val key: String, override val label: String) : SettingOption {
    NONE("none", "Don't organize"),
    BY_CHANNEL("channel", "By channel"),
    BY_PLAYLIST("playlist", "By playlist"),
    BY_MEDIA_TYPE("media_type", "By media type"),
}

data class StorageSettings(
    /** Tree URI (as string) returned by the system folder picker, or empty when unset. */
    val downloadFolderUri: String = "",
    val videoFolderUri: String = "",
    val audioFolderUri: String = "",
    val tempFolderUri: String = "",
    val warnOnLowSpace: Boolean = true,
    val filenameConflict: FilenameConflict = FilenameConflict.ADD_NUMBER,
    val filenameTemplate: String = "{title}",
    val maxFilenameLength: Int = 120,
    val subfolderOrganization: SubfolderOrganization = SubfolderOrganization.NONE,
)

// ---------------------------------------------------------------------------
// Section 4 — Download behavior
// ---------------------------------------------------------------------------

enum class QueuePosition(override val key: String, override val label: String) : SettingOption {
    TOP("top", "Top of queue"),
    BOTTOM("bottom", "Bottom of queue"),
}

enum class DuplicateDetection(override val key: String, override val label: String) : SettingOption {
    SOURCE_URL("url", "Source URL"),
    MEDIA_ID("id", "Media ID"),
    FILENAME("filename", "Existing filename"),
}

enum class PostDownloadAction(override val key: String, override val label: String) : SettingOption {
    NOTHING("nothing", "Do nothing"),
    OPEN_FILE("open_file", "Open the file"),
    OPEN_DOWNLOADS("open_downloads", "Open the downloads screen"),
    SHARE("share", "Share the file"),
}

data class BehaviorSettings(
    val confirmBeforeDownload: Boolean = false,
    val maxSimultaneousDownloads: Int = 2,
    val maxRetryCount: Int = 3,
    val autoResumeInterrupted: Boolean = true,
    val resumeQueueAfterRestart: Boolean = true,
    val preventDuplicates: Boolean = true,
    val duplicateDetection: DuplicateDetection = DuplicateDetection.SOURCE_URL,
    val newDownloadPosition: QueuePosition = QueuePosition.BOTTOM,
    val keepScreenAwake: Boolean = false,
    val pauseOnBatterySaver: Boolean = true,
    val pauseOnOverheat: Boolean = true,
    val autoRetryOnReconnect: Boolean = true,
    val speedLimitEnabled: Boolean = false,
    val speedLimitKbps: Int = 1024,
    val scheduleEnabled: Boolean = false,
    val scheduleStartMinutes: Int = 0, // minutes from midnight
    val scheduleEndMinutes: Int = 6 * 60,
    val postDownloadAction: PostDownloadAction = PostDownloadAction.NOTHING,
)

// ---------------------------------------------------------------------------
// Section 5 — Network preferences
// ---------------------------------------------------------------------------

enum class NetworkType(override val key: String, override val label: String) : SettingOption {
    WIFI_ONLY("wifi", "Wi-Fi only"),
    WIFI_AND_MOBILE("wifi_mobile", "Wi-Fi and mobile data"),
    ANY("any", "Any network"),
}

enum class ProxyType(override val key: String, override val label: String) : SettingOption {
    DISABLED("disabled", "Disabled"),
    HTTP("http", "HTTP"),
    SOCKS("socks", "SOCKS"),
}

data class NetworkSettings(
    val allowedNetworks: NetworkType = NetworkType.WIFI_AND_MOBILE,
    val allowRoaming: Boolean = false,
    val confirmMobileData: Boolean = true,
    val mobileDataWarningMb: Int = 100,
    val treatMeteredWifiAsMobile: Boolean = true,
    val pauseOnNetworkChange: Boolean = false,
    val retryAfterConnectionLoss: Boolean = true,
    val proxyType: ProxyType = ProxyType.DISABLED,
    val proxyHost: String = "",
    val proxyPort: Int = 0,
    val proxyUsername: String = "",
    /** True when a password is stored in secure storage. The password itself never lives here. */
    val proxyPasswordSet: Boolean = false,
)

// ---------------------------------------------------------------------------
// Section 6 — Subtitles and captions
// ---------------------------------------------------------------------------

enum class SubtitleTypePreference(override val key: String, override val label: String) : SettingOption {
    MANUAL_ONLY("manual", "Manual captions only"),
    ALLOW_GENERATED("generated", "Allow automatically generated"),
    PREFER_MANUAL("prefer_manual", "Prefer manual, fall back to generated"),
}

enum class SubtitleFormat(override val key: String, override val label: String) : SettingOption {
    SRT("srt", "SRT"),
    VTT("vtt", "VTT"),
    BEST("best", "Best available"),
}

data class SubtitleSettings(
    val downloadSubtitles: Boolean = false,
    val preferredLanguage: String = "en",
    val fallbackLanguage: String = "",
    val subtitleType: SubtitleTypePreference = SubtitleTypePreference.PREFER_MANUAL,
    val format: SubtitleFormat = SubtitleFormat.SRT,
    val embedInVideo: Boolean = true,
    val saveAsSeparateFiles: Boolean = false,
    val includeAllLanguages: Boolean = false,
    val addLanguageCodeToFilename: Boolean = true,
)

// ---------------------------------------------------------------------------
// Section 7 — Notifications
// ---------------------------------------------------------------------------

data class NotificationSettings(
    val showProgress: Boolean = true,
    val notifyOnEachComplete: Boolean = true,
    val notifyOnAllComplete: Boolean = true,
    val notifyOnFailure: Boolean = true,
    val sound: Boolean = false,
    val vibration: Boolean = true,
    val showActions: Boolean = true,
    val groupNotifications: Boolean = true,
)

// ---------------------------------------------------------------------------
// Section 8 — Appearance and accessibility
// ---------------------------------------------------------------------------

enum class AppTheme(override val key: String, override val label: String) : SettingOption {
    SYSTEM("system", "Follow system"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
}

data class AppearanceSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = false,
    /** BCP-47 language tag, or empty for "Follow system". */
    val languageTag: String = "",
    val compactList: Boolean = false,
    val showFileSize: Boolean = true,
    val showSpeed: Boolean = true,
    val showEta: Boolean = true,
    val reduceAnimations: Boolean = false,
    val highContrast: Boolean = false,
)

// ---------------------------------------------------------------------------
// Section 9 — History and privacy
// ---------------------------------------------------------------------------

enum class HistoryRetention(override val key: String, override val label: String) : SettingOption {
    FOREVER("forever", "Forever"),
    DAYS_30("30d", "30 days"),
    DAYS_7("7d", "7 days"),
    UNTIL_CLOSE("session", "Until app closes"),
}

data class HistorySettings(
    val keepHistory: Boolean = true,
    val retention: HistoryRetention = HistoryRetention.FOREVER,
    val saveRecentUrls: Boolean = true,
    val saveSearchHistory: Boolean = true,
)

// ---------------------------------------------------------------------------
// Section 10 — Advanced processing
// ---------------------------------------------------------------------------

enum class ProcessingPriority(override val key: String, override val label: String) : SettingOption {
    FASTER("faster", "Faster processing"),
    BALANCED("balanced", "Balanced"),
    LOWER_BATTERY("battery", "Lower battery usage"),
}

enum class DiagnosticLogLevel(override val key: String, override val label: String) : SettingOption {
    OFF("off", "Off"),
    ERRORS("errors", "Errors only"),
    DETAILED("detailed", "Detailed"),
}

data class ProcessingSettings(
    val enableConversion: Boolean = false,
    val deleteSourceAfterConversion: Boolean = false,
    val preserveSourceOnFailure: Boolean = true,
    val preferHardwareAcceleration: Boolean = true,
    val priority: ProcessingPriority = ProcessingPriority.BALANCED,
    val allowBackgroundProcessing: Boolean = true,
    val maxTempStorageMb: Int = 2048,
    val logLevel: DiagnosticLogLevel = DiagnosticLogLevel.ERRORS,
)

// ---------------------------------------------------------------------------
// Aggregate
// ---------------------------------------------------------------------------

/** A full, immutable snapshot of every user-configurable setting. */
data class AppSettings(
    val download: DownloadSettings = DownloadSettings(),
    val storage: StorageSettings = StorageSettings(),
    val behavior: BehaviorSettings = BehaviorSettings(),
    val network: NetworkSettings = NetworkSettings(),
    val subtitles: SubtitleSettings = SubtitleSettings(),
    val notifications: NotificationSettings = NotificationSettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val history: HistorySettings = HistorySettings(),
    val processing: ProcessingSettings = ProcessingSettings(),
) {
    companion object {
        /** The single source of truth for first-launch defaults. */
        val DEFAULTS = AppSettings()
    }
}

/** Categories used for scoped resets (Section 13). */
enum class SettingsCategory { DOWNLOAD, STORAGE, BEHAVIOR, NETWORK, SUBTITLES, NOTIFICATIONS, APPEARANCE, HISTORY, PROCESSING }
