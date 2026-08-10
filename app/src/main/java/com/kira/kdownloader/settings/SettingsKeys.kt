package com.kira.kdownloader.settings

/**
 * Stable storage keys for every persisted preference.
 *
 * These strings are a public contract: they appear in [SharedPreferences], in exported settings
 * files, and in migrations. Never rename one without adding a migration — doing so silently resets
 * a user's preference. New keys can be added freely; unknown keys are ignored on read (Section 14).
 */
object SettingsKeys {
    /** Bumped whenever a migration is required. See [SettingsMigration]. */
    const val CURRENT_VERSION = 2

    const val VERSION = "settings.version"

    // Download
    const val DL_TYPE = "download.type"
    const val DL_VIDEO_FORMAT = "download.video_format"
    const val DL_AUDIO_FORMAT = "download.audio_format"
    const val DL_VIDEO_QUALITY = "download.video_quality"
    const val DL_AUDIO_QUALITY = "download.audio_quality"
    const val DL_FRAME_RATE = "download.frame_rate"
    const val DL_PREFER_HDR = "download.prefer_hdr"
    const val DL_ANDROID_CODECS = "download.android_codecs"
    const val DL_AUTO_FALLBACK = "download.auto_fallback"
    const val DL_ASK_QUALITY = "download.ask_quality"
    const val DL_THUMBNAIL = "download.thumbnail"
    const val DL_EMBED_THUMBNAIL = "download.embed_thumbnail"
    const val DL_EMBED_METADATA = "download.embed_metadata"
    const val DL_PRESERVE_DATE = "download.preserve_date"

    // Storage
    const val ST_FOLDER = "storage.folder_uri"
    const val ST_VIDEO_FOLDER = "storage.video_folder_uri"
    const val ST_AUDIO_FOLDER = "storage.audio_folder_uri"
    const val ST_TEMP_FOLDER = "storage.temp_folder_uri"
    const val ST_WARN_LOW_SPACE = "storage.warn_low_space"
    const val ST_CONFLICT = "storage.conflict"
    const val ST_TEMPLATE = "storage.filename_template"
    const val ST_MAX_NAME_LEN = "storage.max_name_length"
    const val ST_SUBFOLDER = "storage.subfolder"

    // Behavior
    const val BH_CONFIRM = "behavior.confirm_before"
    const val BH_MAX_PARALLEL = "behavior.max_parallel"
    const val BH_MAX_RETRY = "behavior.max_retry"
    const val BH_AUTO_RESUME = "behavior.auto_resume"
    const val BH_RESUME_QUEUE = "behavior.resume_queue"
    const val BH_PREVENT_DUP = "behavior.prevent_duplicates"
    const val BH_DUP_METHOD = "behavior.duplicate_method"
    const val BH_QUEUE_POSITION = "behavior.queue_position"
    const val BH_KEEP_AWAKE = "behavior.keep_awake"
    const val BH_PAUSE_BATTERY = "behavior.pause_battery_saver"
    const val BH_PAUSE_HOT = "behavior.pause_overheat"
    const val BH_RETRY_RECONNECT = "behavior.retry_reconnect"
    const val BH_SPEED_LIMIT_ON = "behavior.speed_limit_enabled"
    const val BH_SPEED_LIMIT_KBPS = "behavior.speed_limit_kbps"
    const val BH_SCHEDULE_ON = "behavior.schedule_enabled"
    const val BH_SCHEDULE_START = "behavior.schedule_start"
    const val BH_SCHEDULE_END = "behavior.schedule_end"
    const val BH_POST_ACTION = "behavior.post_action"

    // Network
    const val NW_ALLOWED = "network.allowed"
    const val NW_ROAMING = "network.roaming"
    const val NW_CONFIRM_MOBILE = "network.confirm_mobile"
    const val NW_WARN_MB = "network.warn_mb"
    const val NW_METERED_AS_MOBILE = "network.metered_as_mobile"
    const val NW_PAUSE_ON_CHANGE = "network.pause_on_change"
    const val NW_RETRY_LOSS = "network.retry_loss"
    const val NW_PROXY_TYPE = "network.proxy_type"
    const val NW_PROXY_HOST = "network.proxy_host"
    const val NW_PROXY_PORT = "network.proxy_port"
    const val NW_PROXY_USER = "network.proxy_user"
    /** Marker only; the password lives in secure storage under [SecureKeys.PROXY_PASSWORD]. */
    const val NW_PROXY_PASSWORD_SET = "network.proxy_password_set"

    // Subtitles
    const val SB_ENABLED = "subtitles.enabled"
    const val SB_LANG = "subtitles.language"
    const val SB_FALLBACK_LANG = "subtitles.fallback_language"
    const val SB_TYPE = "subtitles.type"
    const val SB_FORMAT = "subtitles.format"
    const val SB_EMBED = "subtitles.embed"
    const val SB_SEPARATE = "subtitles.separate_files"
    const val SB_ALL_LANGS = "subtitles.all_languages"
    const val SB_LANG_IN_NAME = "subtitles.language_in_name"

    // Notifications
    const val NT_PROGRESS = "notifications.progress"
    const val NT_EACH_COMPLETE = "notifications.each_complete"
    const val NT_ALL_COMPLETE = "notifications.all_complete"
    const val NT_FAILURE = "notifications.failure"
    const val NT_SOUND = "notifications.sound"
    const val NT_VIBRATION = "notifications.vibration"
    const val NT_ACTIONS = "notifications.actions"
    const val NT_GROUP = "notifications.group"

    // Appearance
    const val AP_THEME = "appearance.theme"
    const val AP_DYNAMIC_COLOR = "appearance.dynamic_color"
    const val AP_LANGUAGE = "appearance.language"
    const val AP_COMPACT = "appearance.compact"
    const val AP_SHOW_SIZE = "appearance.show_size"
    const val AP_SHOW_SPEED = "appearance.show_speed"
    const val AP_SHOW_ETA = "appearance.show_eta"
    const val AP_REDUCE_ANIM = "appearance.reduce_animations"
    const val AP_HIGH_CONTRAST = "appearance.high_contrast"

    // History
    const val HS_KEEP = "history.keep"
    const val HS_RETENTION = "history.retention"
    const val HS_RECENT_URLS = "history.recent_urls"
    const val HS_SEARCH = "history.search"

    // Processing
    const val PR_ENABLE = "processing.enable"
    const val PR_DELETE_SOURCE = "processing.delete_source"
    const val PR_PRESERVE_ON_FAIL = "processing.preserve_on_fail"
    const val PR_HW_ACCEL = "processing.hw_accel"
    const val PR_PRIORITY = "processing.priority"
    const val PR_BACKGROUND = "processing.background"
    const val PR_MAX_TEMP_MB = "processing.max_temp_mb"
    const val PR_LOG_LEVEL = "processing.log_level"

    /** Keys whose values are sensitive and must be excluded from exports (Section 9 / 12). */
    val SENSITIVE_KEYS = setOf(NW_PROXY_USER, NW_PROXY_PASSWORD_SET)
}

/** Keys for the Keystore-backed secure store. */
object SecureKeys {
    const val PROXY_PASSWORD = "proxy_password"
}
