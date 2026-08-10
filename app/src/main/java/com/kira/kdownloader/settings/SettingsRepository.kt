package com.kira.kdownloader.settings

import com.kira.kdownloader.settings.store.InMemorySecureStore
import com.kira.kdownloader.settings.store.KeyValueStore
import com.kira.kdownloader.settings.store.SecureStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Single source of truth for reading, observing, and persisting [AppSettings].
 *
 * Responsibilities:
 *  - Runs migrations once on construction (Section 14).
 *  - Reads a fully-populated, validated [AppSettings] snapshot ([read]) — missing or malformed
 *    values fall back to documented defaults so it can never throw.
 *  - Exposes a reactive [observe] flow that re-reads on any change (Section 1 "apply immediately").
 *  - Clamps and validates every value on write so an out-of-range value can never be stored
 *    (Section 15 "Invalid values cannot be saved").
 *  - Keeps the proxy password only in the Keystore-backed [SecureStore] (Section 5), never in the
 *    exportable store.
 *  - Provides scoped resets (Section 13) and JSON export/import that excludes secrets (Section 12).
 */
class SettingsRepository(
    private val store: KeyValueStore,
    private val secure: SecureStore = InMemorySecureStore(),
) {
    init {
        val fresh = !store.contains(SettingsKeys.VERSION)
        SettingsMigration.migrateIfNeeded(store)
        // Fully materialize defaults once, at startup, so later single-field edits touch exactly one
        // key instead of rewriting the entire preference set (and firing a listener storm).
        if (fresh) {
            store.edit { encodeFields(AppSettings.DEFAULTS, EditorSink(this)) }
        }
    }

    // ---- Read ----------------------------------------------------------------

    fun read(): AppSettings = decode(StoreSource(store)).let { decoded ->
        decoded.copy(
            // Derived from a plain-store marker kept in sync by [setProxyPassword]. This keeps the
            // encrypted Keystore off the hot read path (read runs on the main thread per emission).
            network = decoded.network.copy(
                proxyPasswordSet = store.getBoolean(SettingsKeys.NW_PROXY_PASSWORD_SET, false),
            ),
        )
    }

    fun observe(): Flow<AppSettings> = store.changes
        .onStart { emit(null) }
        .map { read() }
        .distinctUntilChanged()

    // ---- Write ---------------------------------------------------------------

    /** Persists a sanitized snapshot, writing only the keys whose values actually changed. */
    fun save(settings: AppSettings) {
        val clean = sanitize(settings)
        store.edit { encodeFields(clean, DiffEditorSink(this)) }
    }

    /** Read-modify-write convenience: `update { it.copy(...) }`. */
    fun update(transform: (AppSettings) -> AppSettings) = save(transform(read()))

    // ---- Proxy password (secure) --------------------------------------------

    /**
     * Stores or clears the proxy password. A null/blank value removes it. The plaintext lives only
     * in secure storage; a boolean marker is written to the regular store purely so observers are
     * notified and the derived [NetworkSettings.proxyPasswordSet] refreshes.
     */
    fun setProxyPassword(password: String?) {
        if (password.isNullOrEmpty()) {
            secure.remove(SecureKeys.PROXY_PASSWORD)
            store.edit { putBoolean(SettingsKeys.NW_PROXY_PASSWORD_SET, false) }
        } else {
            secure.put(SecureKeys.PROXY_PASSWORD, password)
            store.edit { putBoolean(SettingsKeys.NW_PROXY_PASSWORD_SET, true) }
        }
    }

    /** For runtime use by the download engine only — never surfaced to the UI. */
    fun proxyPassword(): String? = secure.get(SecureKeys.PROXY_PASSWORD)

    // ---- Reset (Section 13) --------------------------------------------------

    fun resetCategory(category: SettingsCategory) {
        val d = AppSettings.DEFAULTS
        val current = read()
        val reset = when (category) {
            SettingsCategory.DOWNLOAD -> current.copy(download = d.download)
            SettingsCategory.STORAGE -> current.copy(storage = d.storage)
            SettingsCategory.BEHAVIOR -> current.copy(behavior = d.behavior)
            SettingsCategory.NETWORK -> {
                setProxyPassword(null)
                current.copy(network = d.network)
            }
            SettingsCategory.SUBTITLES -> current.copy(subtitles = d.subtitles)
            SettingsCategory.NOTIFICATIONS -> current.copy(notifications = d.notifications)
            SettingsCategory.APPEARANCE -> current.copy(appearance = d.appearance)
            SettingsCategory.HISTORY -> current.copy(history = d.history)
            SettingsCategory.PROCESSING -> current.copy(processing = d.processing)
        }
        save(reset)
    }

    /** Resets every preference to defaults. Does not touch downloaded files or history rows. */
    fun resetAll() {
        setProxyPassword(null)
        save(AppSettings.DEFAULTS)
    }

    // ---- Export / Import (Section 9 / 12) ------------------------------------

    /** Serializes current settings to JSON, excluding all sensitive values. */
    fun exportToJson(): String {
        val map = LinkedHashMap<String, String>()
        map[SettingsKeys.VERSION] = SettingsKeys.CURRENT_VERSION.toString()
        encodeFields(sanitize(read()), MapSink(map))
        return SettingsCodec.encode(map)
    }

    sealed interface ImportResult {
        data class Success(val applied: Int) : ImportResult
        data class Failure(val reason: String) : ImportResult
    }

    /**
     * Imports settings from a previously exported file. Tolerant by design: unknown keys are
     * ignored, malformed values fall back to defaults, and credentials are never imported.
     */
    fun importFromJson(json: String): ImportResult {
        val map = SettingsCodec.decode(json)
            ?: return ImportResult.Failure("This file is not a valid settings export.")
        if (map.isEmpty()) return ImportResult.Failure("The settings file is empty.")

        val imported = decode(MapSource(map))
        // Never carry credentials across an import; the password marker is left as-is.
        save(imported)
        return ImportResult.Success(map.count { it.key != SettingsKeys.VERSION })
    }

    // ---- Sanitization --------------------------------------------------------

    /** Clamps every numeric/textual field into its documented valid range. */
    private fun sanitize(s: AppSettings): AppSettings {
        val templateValid = FilenameTemplate.validate(s.storage.filenameTemplate) is FilenameTemplate.Validation.Valid
        return s.copy(
            storage = s.storage.copy(
                filenameTemplate = if (templateValid) s.storage.filenameTemplate else "{title}",
                maxFilenameLength = s.storage.maxFilenameLength
                    .coerceIn(FilenameTemplate.MIN_LENGTH, FilenameTemplate.MAX_LENGTH),
            ),
            behavior = s.behavior.copy(
                maxSimultaneousDownloads = s.behavior.maxSimultaneousDownloads.coerceIn(1, 5),
                maxRetryCount = s.behavior.maxRetryCount.coerceIn(0, 10),
                speedLimitKbps = s.behavior.speedLimitKbps.coerceAtLeast(1),
                scheduleStartMinutes = s.behavior.scheduleStartMinutes.coerceIn(0, 1439),
                scheduleEndMinutes = s.behavior.scheduleEndMinutes.coerceIn(0, 1439),
            ),
            network = s.network.copy(
                proxyPort = s.network.proxyPort.coerceIn(0, 65535),
                mobileDataWarningMb = s.network.mobileDataWarningMb.coerceAtLeast(1),
            ),
            processing = s.processing.copy(
                maxTempStorageMb = s.processing.maxTempStorageMb.coerceAtLeast(1),
            ),
        )
    }

    // ---- Field mapping (single definition, reused by read/import/export/save) -

    private fun decode(src: Source): AppSettings = AppSettings(
        download = DownloadSettings(
            downloadType = src.option(SettingsKeys.DL_TYPE, DownloadType.entries.toTypedArray(), DownloadType.VIDEO),
            videoFormat = src.option(SettingsKeys.DL_VIDEO_FORMAT, VideoFormat.entries.toTypedArray(), VideoFormat.MP4),
            audioFormat = src.option(SettingsKeys.DL_AUDIO_FORMAT, AudioFormat.entries.toTypedArray(), AudioFormat.MP3),
            videoQuality = src.option(SettingsKeys.DL_VIDEO_QUALITY, VideoQuality.entries.toTypedArray(), VideoQuality.BEST),
            audioQuality = src.option(SettingsKeys.DL_AUDIO_QUALITY, AudioQuality.entries.toTypedArray(), AudioQuality.BEST),
            frameRate = src.option(SettingsKeys.DL_FRAME_RATE, FrameRatePreference.entries.toTypedArray(), FrameRatePreference.BEST),
            preferHdr = src.bool(SettingsKeys.DL_PREFER_HDR, false),
            preferAndroidCompatibleCodecs = src.bool(SettingsKeys.DL_ANDROID_CODECS, true),
            autoFallbackQuality = src.bool(SettingsKeys.DL_AUTO_FALLBACK, true),
            askQualityBeforeEachDownload = src.bool(SettingsKeys.DL_ASK_QUALITY, false),
            downloadThumbnail = src.bool(SettingsKeys.DL_THUMBNAIL, true),
            embedThumbnail = src.bool(SettingsKeys.DL_EMBED_THUMBNAIL, true),
            embedMetadata = src.bool(SettingsKeys.DL_EMBED_METADATA, true),
            preserveUploadDate = src.bool(SettingsKeys.DL_PRESERVE_DATE, true),
        ),
        storage = StorageSettings(
            downloadFolderUri = src.str(SettingsKeys.ST_FOLDER, ""),
            videoFolderUri = src.str(SettingsKeys.ST_VIDEO_FOLDER, ""),
            audioFolderUri = src.str(SettingsKeys.ST_AUDIO_FOLDER, ""),
            tempFolderUri = src.str(SettingsKeys.ST_TEMP_FOLDER, ""),
            warnOnLowSpace = src.bool(SettingsKeys.ST_WARN_LOW_SPACE, true),
            filenameConflict = src.option(SettingsKeys.ST_CONFLICT, FilenameConflict.entries.toTypedArray(), FilenameConflict.ADD_NUMBER),
            filenameTemplate = src.str(SettingsKeys.ST_TEMPLATE, "{title}"),
            maxFilenameLength = src.int(SettingsKeys.ST_MAX_NAME_LEN, 120),
            subfolderOrganization = src.option(SettingsKeys.ST_SUBFOLDER, SubfolderOrganization.entries.toTypedArray(), SubfolderOrganization.NONE),
        ),
        behavior = BehaviorSettings(
            confirmBeforeDownload = src.bool(SettingsKeys.BH_CONFIRM, false),
            maxSimultaneousDownloads = src.int(SettingsKeys.BH_MAX_PARALLEL, 2),
            maxRetryCount = src.int(SettingsKeys.BH_MAX_RETRY, 3),
            autoResumeInterrupted = src.bool(SettingsKeys.BH_AUTO_RESUME, true),
            resumeQueueAfterRestart = src.bool(SettingsKeys.BH_RESUME_QUEUE, true),
            preventDuplicates = src.bool(SettingsKeys.BH_PREVENT_DUP, true),
            duplicateDetection = src.option(SettingsKeys.BH_DUP_METHOD, DuplicateDetection.entries.toTypedArray(), DuplicateDetection.SOURCE_URL),
            newDownloadPosition = src.option(SettingsKeys.BH_QUEUE_POSITION, QueuePosition.entries.toTypedArray(), QueuePosition.BOTTOM),
            keepScreenAwake = src.bool(SettingsKeys.BH_KEEP_AWAKE, false),
            pauseOnBatterySaver = src.bool(SettingsKeys.BH_PAUSE_BATTERY, true),
            pauseOnOverheat = src.bool(SettingsKeys.BH_PAUSE_HOT, true),
            autoRetryOnReconnect = src.bool(SettingsKeys.BH_RETRY_RECONNECT, true),
            speedLimitEnabled = src.bool(SettingsKeys.BH_SPEED_LIMIT_ON, false),
            speedLimitKbps = src.int(SettingsKeys.BH_SPEED_LIMIT_KBPS, 1024),
            scheduleEnabled = src.bool(SettingsKeys.BH_SCHEDULE_ON, false),
            scheduleStartMinutes = src.int(SettingsKeys.BH_SCHEDULE_START, 0),
            scheduleEndMinutes = src.int(SettingsKeys.BH_SCHEDULE_END, 360),
            postDownloadAction = src.option(SettingsKeys.BH_POST_ACTION, PostDownloadAction.entries.toTypedArray(), PostDownloadAction.NOTHING),
        ),
        network = NetworkSettings(
            allowedNetworks = src.option(SettingsKeys.NW_ALLOWED, NetworkType.entries.toTypedArray(), NetworkType.WIFI_AND_MOBILE),
            allowRoaming = src.bool(SettingsKeys.NW_ROAMING, false),
            confirmMobileData = src.bool(SettingsKeys.NW_CONFIRM_MOBILE, true),
            mobileDataWarningMb = src.int(SettingsKeys.NW_WARN_MB, 100),
            treatMeteredWifiAsMobile = src.bool(SettingsKeys.NW_METERED_AS_MOBILE, true),
            pauseOnNetworkChange = src.bool(SettingsKeys.NW_PAUSE_ON_CHANGE, false),
            retryAfterConnectionLoss = src.bool(SettingsKeys.NW_RETRY_LOSS, true),
            proxyType = src.option(SettingsKeys.NW_PROXY_TYPE, ProxyType.entries.toTypedArray(), ProxyType.DISABLED),
            proxyHost = src.str(SettingsKeys.NW_PROXY_HOST, ""),
            proxyPort = src.int(SettingsKeys.NW_PROXY_PORT, 0),
            proxyUsername = src.str(SettingsKeys.NW_PROXY_USER, ""),
            proxyPasswordSet = false,
        ),
        subtitles = SubtitleSettings(
            downloadSubtitles = src.bool(SettingsKeys.SB_ENABLED, false),
            preferredLanguage = src.str(SettingsKeys.SB_LANG, "en"),
            fallbackLanguage = src.str(SettingsKeys.SB_FALLBACK_LANG, ""),
            subtitleType = src.option(SettingsKeys.SB_TYPE, SubtitleTypePreference.entries.toTypedArray(), SubtitleTypePreference.PREFER_MANUAL),
            format = src.option(SettingsKeys.SB_FORMAT, SubtitleFormat.entries.toTypedArray(), SubtitleFormat.SRT),
            embedInVideo = src.bool(SettingsKeys.SB_EMBED, true),
            saveAsSeparateFiles = src.bool(SettingsKeys.SB_SEPARATE, false),
            includeAllLanguages = src.bool(SettingsKeys.SB_ALL_LANGS, false),
            addLanguageCodeToFilename = src.bool(SettingsKeys.SB_LANG_IN_NAME, true),
        ),
        notifications = NotificationSettings(
            showProgress = src.bool(SettingsKeys.NT_PROGRESS, true),
            notifyOnEachComplete = src.bool(SettingsKeys.NT_EACH_COMPLETE, true),
            notifyOnAllComplete = src.bool(SettingsKeys.NT_ALL_COMPLETE, true),
            notifyOnFailure = src.bool(SettingsKeys.NT_FAILURE, true),
            sound = src.bool(SettingsKeys.NT_SOUND, false),
            vibration = src.bool(SettingsKeys.NT_VIBRATION, true),
            showActions = src.bool(SettingsKeys.NT_ACTIONS, true),
            groupNotifications = src.bool(SettingsKeys.NT_GROUP, true),
        ),
        appearance = AppearanceSettings(
            theme = src.option(SettingsKeys.AP_THEME, AppTheme.entries.toTypedArray(), AppTheme.SYSTEM),
            dynamicColor = src.bool(SettingsKeys.AP_DYNAMIC_COLOR, false),
            languageTag = src.str(SettingsKeys.AP_LANGUAGE, ""),
            compactList = src.bool(SettingsKeys.AP_COMPACT, false),
            showFileSize = src.bool(SettingsKeys.AP_SHOW_SIZE, true),
            showSpeed = src.bool(SettingsKeys.AP_SHOW_SPEED, true),
            showEta = src.bool(SettingsKeys.AP_SHOW_ETA, true),
            reduceAnimations = src.bool(SettingsKeys.AP_REDUCE_ANIM, false),
            highContrast = src.bool(SettingsKeys.AP_HIGH_CONTRAST, false),
        ),
        history = HistorySettings(
            keepHistory = src.bool(SettingsKeys.HS_KEEP, true),
            retention = src.option(SettingsKeys.HS_RETENTION, HistoryRetention.entries.toTypedArray(), HistoryRetention.FOREVER),
            saveRecentUrls = src.bool(SettingsKeys.HS_RECENT_URLS, true),
            saveSearchHistory = src.bool(SettingsKeys.HS_SEARCH, true),
        ),
        processing = ProcessingSettings(
            enableConversion = src.bool(SettingsKeys.PR_ENABLE, false),
            deleteSourceAfterConversion = src.bool(SettingsKeys.PR_DELETE_SOURCE, false),
            preserveSourceOnFailure = src.bool(SettingsKeys.PR_PRESERVE_ON_FAIL, true),
            preferHardwareAcceleration = src.bool(SettingsKeys.PR_HW_ACCEL, true),
            priority = src.option(SettingsKeys.PR_PRIORITY, ProcessingPriority.entries.toTypedArray(), ProcessingPriority.BALANCED),
            allowBackgroundProcessing = src.bool(SettingsKeys.PR_BACKGROUND, true),
            maxTempStorageMb = src.int(SettingsKeys.PR_MAX_TEMP_MB, 2048),
            logLevel = src.option(SettingsKeys.PR_LOG_LEVEL, DiagnosticLogLevel.entries.toTypedArray(), DiagnosticLogLevel.ERRORS),
        ),
    )

    private fun encodeFields(s: AppSettings, sink: Sink) {
        with(s.download) {
            sink.str(SettingsKeys.DL_TYPE, downloadType.key)
            sink.str(SettingsKeys.DL_VIDEO_FORMAT, videoFormat.key)
            sink.str(SettingsKeys.DL_AUDIO_FORMAT, audioFormat.key)
            sink.str(SettingsKeys.DL_VIDEO_QUALITY, videoQuality.key)
            sink.str(SettingsKeys.DL_AUDIO_QUALITY, audioQuality.key)
            sink.str(SettingsKeys.DL_FRAME_RATE, frameRate.key)
            sink.bool(SettingsKeys.DL_PREFER_HDR, preferHdr)
            sink.bool(SettingsKeys.DL_ANDROID_CODECS, preferAndroidCompatibleCodecs)
            sink.bool(SettingsKeys.DL_AUTO_FALLBACK, autoFallbackQuality)
            sink.bool(SettingsKeys.DL_ASK_QUALITY, askQualityBeforeEachDownload)
            sink.bool(SettingsKeys.DL_THUMBNAIL, downloadThumbnail)
            sink.bool(SettingsKeys.DL_EMBED_THUMBNAIL, embedThumbnail)
            sink.bool(SettingsKeys.DL_EMBED_METADATA, embedMetadata)
            sink.bool(SettingsKeys.DL_PRESERVE_DATE, preserveUploadDate)
        }
        with(s.storage) {
            sink.str(SettingsKeys.ST_FOLDER, downloadFolderUri)
            sink.str(SettingsKeys.ST_VIDEO_FOLDER, videoFolderUri)
            sink.str(SettingsKeys.ST_AUDIO_FOLDER, audioFolderUri)
            sink.str(SettingsKeys.ST_TEMP_FOLDER, tempFolderUri)
            sink.bool(SettingsKeys.ST_WARN_LOW_SPACE, warnOnLowSpace)
            sink.str(SettingsKeys.ST_CONFLICT, filenameConflict.key)
            sink.str(SettingsKeys.ST_TEMPLATE, filenameTemplate)
            sink.int(SettingsKeys.ST_MAX_NAME_LEN, maxFilenameLength)
            sink.str(SettingsKeys.ST_SUBFOLDER, subfolderOrganization.key)
        }
        with(s.behavior) {
            sink.bool(SettingsKeys.BH_CONFIRM, confirmBeforeDownload)
            sink.int(SettingsKeys.BH_MAX_PARALLEL, maxSimultaneousDownloads)
            sink.int(SettingsKeys.BH_MAX_RETRY, maxRetryCount)
            sink.bool(SettingsKeys.BH_AUTO_RESUME, autoResumeInterrupted)
            sink.bool(SettingsKeys.BH_RESUME_QUEUE, resumeQueueAfterRestart)
            sink.bool(SettingsKeys.BH_PREVENT_DUP, preventDuplicates)
            sink.str(SettingsKeys.BH_DUP_METHOD, duplicateDetection.key)
            sink.str(SettingsKeys.BH_QUEUE_POSITION, newDownloadPosition.key)
            sink.bool(SettingsKeys.BH_KEEP_AWAKE, keepScreenAwake)
            sink.bool(SettingsKeys.BH_PAUSE_BATTERY, pauseOnBatterySaver)
            sink.bool(SettingsKeys.BH_PAUSE_HOT, pauseOnOverheat)
            sink.bool(SettingsKeys.BH_RETRY_RECONNECT, autoRetryOnReconnect)
            sink.bool(SettingsKeys.BH_SPEED_LIMIT_ON, speedLimitEnabled)
            sink.int(SettingsKeys.BH_SPEED_LIMIT_KBPS, speedLimitKbps)
            sink.bool(SettingsKeys.BH_SCHEDULE_ON, scheduleEnabled)
            sink.int(SettingsKeys.BH_SCHEDULE_START, scheduleStartMinutes)
            sink.int(SettingsKeys.BH_SCHEDULE_END, scheduleEndMinutes)
            sink.str(SettingsKeys.BH_POST_ACTION, postDownloadAction.key)
        }
        with(s.network) {
            sink.str(SettingsKeys.NW_ALLOWED, allowedNetworks.key)
            sink.bool(SettingsKeys.NW_ROAMING, allowRoaming)
            sink.bool(SettingsKeys.NW_CONFIRM_MOBILE, confirmMobileData)
            sink.int(SettingsKeys.NW_WARN_MB, mobileDataWarningMb)
            sink.bool(SettingsKeys.NW_METERED_AS_MOBILE, treatMeteredWifiAsMobile)
            sink.bool(SettingsKeys.NW_PAUSE_ON_CHANGE, pauseOnNetworkChange)
            sink.bool(SettingsKeys.NW_RETRY_LOSS, retryAfterConnectionLoss)
            sink.str(SettingsKeys.NW_PROXY_TYPE, proxyType.key)
            sink.str(SettingsKeys.NW_PROXY_HOST, proxyHost)
            sink.int(SettingsKeys.NW_PROXY_PORT, proxyPort)
            sink.str(SettingsKeys.NW_PROXY_USER, proxyUsername)
            // proxyPasswordSet is derived from secure storage; never persisted here.
        }
        with(s.subtitles) {
            sink.bool(SettingsKeys.SB_ENABLED, downloadSubtitles)
            sink.str(SettingsKeys.SB_LANG, preferredLanguage)
            sink.str(SettingsKeys.SB_FALLBACK_LANG, fallbackLanguage)
            sink.str(SettingsKeys.SB_TYPE, subtitleType.key)
            sink.str(SettingsKeys.SB_FORMAT, format.key)
            sink.bool(SettingsKeys.SB_EMBED, embedInVideo)
            sink.bool(SettingsKeys.SB_SEPARATE, saveAsSeparateFiles)
            sink.bool(SettingsKeys.SB_ALL_LANGS, includeAllLanguages)
            sink.bool(SettingsKeys.SB_LANG_IN_NAME, addLanguageCodeToFilename)
        }
        with(s.notifications) {
            sink.bool(SettingsKeys.NT_PROGRESS, showProgress)
            sink.bool(SettingsKeys.NT_EACH_COMPLETE, notifyOnEachComplete)
            sink.bool(SettingsKeys.NT_ALL_COMPLETE, notifyOnAllComplete)
            sink.bool(SettingsKeys.NT_FAILURE, notifyOnFailure)
            sink.bool(SettingsKeys.NT_SOUND, sound)
            sink.bool(SettingsKeys.NT_VIBRATION, vibration)
            sink.bool(SettingsKeys.NT_ACTIONS, showActions)
            sink.bool(SettingsKeys.NT_GROUP, groupNotifications)
        }
        with(s.appearance) {
            sink.str(SettingsKeys.AP_THEME, theme.key)
            sink.bool(SettingsKeys.AP_DYNAMIC_COLOR, dynamicColor)
            sink.str(SettingsKeys.AP_LANGUAGE, languageTag)
            sink.bool(SettingsKeys.AP_COMPACT, compactList)
            sink.bool(SettingsKeys.AP_SHOW_SIZE, showFileSize)
            sink.bool(SettingsKeys.AP_SHOW_SPEED, showSpeed)
            sink.bool(SettingsKeys.AP_SHOW_ETA, showEta)
            sink.bool(SettingsKeys.AP_REDUCE_ANIM, reduceAnimations)
            sink.bool(SettingsKeys.AP_HIGH_CONTRAST, highContrast)
        }
        with(s.history) {
            sink.bool(SettingsKeys.HS_KEEP, keepHistory)
            sink.str(SettingsKeys.HS_RETENTION, retention.key)
            sink.bool(SettingsKeys.HS_RECENT_URLS, saveRecentUrls)
            sink.bool(SettingsKeys.HS_SEARCH, saveSearchHistory)
        }
        with(s.processing) {
            sink.bool(SettingsKeys.PR_ENABLE, enableConversion)
            sink.bool(SettingsKeys.PR_DELETE_SOURCE, deleteSourceAfterConversion)
            sink.bool(SettingsKeys.PR_PRESERVE_ON_FAIL, preserveSourceOnFailure)
            sink.bool(SettingsKeys.PR_HW_ACCEL, preferHardwareAcceleration)
            sink.str(SettingsKeys.PR_PRIORITY, priority.key)
            sink.bool(SettingsKeys.PR_BACKGROUND, allowBackgroundProcessing)
            sink.int(SettingsKeys.PR_MAX_TEMP_MB, maxTempStorageMb)
            sink.str(SettingsKeys.PR_LOG_LEVEL, logLevel.key)
        }
    }

    // ---- Source / Sink abstractions -----------------------------------------

    private interface Source {
        fun str(key: String, default: String): String
        fun int(key: String, default: Int): Int
        fun bool(key: String, default: Boolean): Boolean
    }

    private fun <T> Source.option(key: String, values: Array<T>, default: T): T
        where T : Enum<T>, T : SettingOption = optionFromKey(values, str(key, default.key), default)

    private class StoreSource(private val store: KeyValueStore) : Source {
        override fun str(key: String, default: String) = store.getString(key, default)
        override fun int(key: String, default: Int) = store.getInt(key, default)
        override fun bool(key: String, default: Boolean) = store.getBoolean(key, default)
    }

    private class MapSource(private val map: Map<String, String>) : Source {
        override fun str(key: String, default: String) = map[key] ?: default
        override fun int(key: String, default: Int) = map[key]?.toIntOrNull() ?: default
        override fun bool(key: String, default: Boolean) = map[key]?.toBooleanStrictOrNull() ?: default
    }

    private interface Sink {
        fun str(key: String, value: String)
        fun int(key: String, value: Int)
        fun bool(key: String, value: Boolean)
    }

    private class EditorSink(private val editor: KeyValueStore.Editor) : Sink {
        override fun str(key: String, value: String) { editor.putString(key, value) }
        override fun int(key: String, value: Int) { editor.putInt(key, value) }
        override fun bool(key: String, value: Boolean) { editor.putBoolean(key, value) }
    }

    /** Writes only keys whose stored value differs, so one toggle fires one change notification. */
    private inner class DiffEditorSink(private val editor: KeyValueStore.Editor) : Sink {
        override fun str(key: String, value: String) {
            if (store.getString(key, UNSET) != value) editor.putString(key, value)
        }
        override fun int(key: String, value: Int) {
            if (store.getInt(key, Int.MIN_VALUE) != value) editor.putInt(key, value)
        }
        override fun bool(key: String, value: Boolean) {
            if (store.getBoolean(key, !value) != value) editor.putBoolean(key, value)
        }
    }

    private class MapSink(private val map: MutableMap<String, String>) : Sink {
        override fun str(key: String, value: String) { map[key] = value }
        override fun int(key: String, value: Int) { map[key] = value.toString() }
        override fun bool(key: String, value: Boolean) { map[key] = value.toString() }
    }

    private companion object {
        /** Sentinel default that no real string value equals, used for change detection. */
        const val UNSET = "\u0000__kdl_unset__"
    }
}
