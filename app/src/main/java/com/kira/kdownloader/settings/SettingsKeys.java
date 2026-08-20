package com.kira.kdownloader.settings;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SettingsKeys {
    private SettingsKeys() {}

    public static final int CURRENT_VERSION = 2;
    public static final String VERSION = "settings.version";

    public static final String DL_TYPE = "download.type";
    public static final String DL_VIDEO_FORMAT = "download.video_format";
    public static final String DL_AUDIO_FORMAT = "download.audio_format";
    public static final String DL_VIDEO_QUALITY = "download.video_quality";
    public static final String DL_AUDIO_QUALITY = "download.audio_quality";
    public static final String DL_FRAME_RATE = "download.frame_rate";
    public static final String DL_PREFER_HDR = "download.prefer_hdr";
    public static final String DL_ANDROID_CODECS = "download.android_codecs";
    public static final String DL_AUTO_FALLBACK = "download.auto_fallback";
    public static final String DL_ASK_QUALITY = "download.ask_quality";
    public static final String DL_THUMBNAIL = "download.thumbnail";
    public static final String DL_EMBED_THUMBNAIL = "download.embed_thumbnail";
    public static final String DL_EMBED_METADATA = "download.embed_metadata";
    public static final String DL_PRESERVE_DATE = "download.preserve_date";

    public static final String ST_FOLDER = "storage.folder_uri";
    public static final String ST_VIDEO_FOLDER = "storage.video_folder_uri";
    public static final String ST_AUDIO_FOLDER = "storage.audio_folder_uri";
    public static final String ST_TEMP_FOLDER = "storage.temp_folder_uri";
    public static final String ST_WARN_LOW_SPACE = "storage.warn_low_space";
    public static final String ST_CONFLICT = "storage.conflict";
    public static final String ST_TEMPLATE = "storage.filename_template";
    public static final String ST_MAX_NAME_LEN = "storage.max_name_length";
    public static final String ST_SUBFOLDER = "storage.subfolder";

    public static final String BH_CONFIRM = "behavior.confirm_before";
    public static final String BH_MAX_PARALLEL = "behavior.max_parallel";
    public static final String BH_MAX_RETRY = "behavior.max_retry";
    public static final String BH_AUTO_RESUME = "behavior.auto_resume";
    public static final String BH_RESUME_QUEUE = "behavior.resume_queue";
    public static final String BH_PREVENT_DUP = "behavior.prevent_duplicates";
    public static final String BH_DUP_METHOD = "behavior.duplicate_method";
    public static final String BH_QUEUE_POSITION = "behavior.queue_position";
    public static final String BH_KEEP_AWAKE = "behavior.keep_awake";
    public static final String BH_PAUSE_BATTERY = "behavior.pause_battery_saver";
    public static final String BH_PAUSE_HOT = "behavior.pause_overheat";
    public static final String BH_RETRY_RECONNECT = "behavior.retry_reconnect";
    public static final String BH_SPEED_LIMIT_ON = "behavior.speed_limit_enabled";
    public static final String BH_SPEED_LIMIT_KBPS = "behavior.speed_limit_kbps";
    public static final String BH_SCHEDULE_ON = "behavior.schedule_enabled";
    public static final String BH_SCHEDULE_START = "behavior.schedule_start";
    public static final String BH_SCHEDULE_END = "behavior.schedule_end";
    public static final String BH_POST_ACTION = "behavior.post_action";

    public static final String NW_ALLOWED = "network.allowed";
    public static final String NW_ROAMING = "network.roaming";
    public static final String NW_CONFIRM_MOBILE = "network.confirm_mobile";
    public static final String NW_WARN_MB = "network.warn_mb";
    public static final String NW_METERED_AS_MOBILE = "network.metered_as_mobile";
    public static final String NW_PAUSE_ON_CHANGE = "network.pause_on_change";
    public static final String NW_RETRY_LOSS = "network.retry_loss";
    public static final String NW_PROXY_TYPE = "network.proxy_type";
    public static final String NW_PROXY_HOST = "network.proxy_host";
    public static final String NW_PROXY_PORT = "network.proxy_port";
    public static final String NW_PROXY_USER = "network.proxy_user";
    public static final String NW_PROXY_PASSWORD_SET = "network.proxy_password_set";

    public static final String SB_ENABLED = "subtitles.enabled";
    public static final String SB_LANG = "subtitles.language";
    public static final String SB_FALLBACK_LANG = "subtitles.fallback_language";
    public static final String SB_TYPE = "subtitles.type";
    public static final String SB_FORMAT = "subtitles.format";
    public static final String SB_EMBED = "subtitles.embed";
    public static final String SB_SEPARATE = "subtitles.separate_files";
    public static final String SB_ALL_LANGS = "subtitles.all_languages";
    public static final String SB_LANG_IN_NAME = "subtitles.language_in_name";

    public static final String NT_PROGRESS = "notifications.progress";
    public static final String NT_EACH_COMPLETE = "notifications.each_complete";
    public static final String NT_ALL_COMPLETE = "notifications.all_complete";
    public static final String NT_FAILURE = "notifications.failure";
    public static final String NT_SOUND = "notifications.sound";
    public static final String NT_VIBRATION = "notifications.vibration";
    public static final String NT_ACTIONS = "notifications.actions";
    public static final String NT_GROUP = "notifications.group";

    public static final String AP_THEME = "appearance.theme";
    public static final String AP_DYNAMIC_COLOR = "appearance.dynamic_color";
    public static final String AP_LANGUAGE = "appearance.language";
    public static final String AP_COMPACT = "appearance.compact";
    public static final String AP_SHOW_SIZE = "appearance.show_size";
    public static final String AP_SHOW_SPEED = "appearance.show_speed";
    public static final String AP_SHOW_ETA = "appearance.show_eta";
    public static final String AP_REDUCE_ANIM = "appearance.reduce_animations";
    public static final String AP_HIGH_CONTRAST = "appearance.high_contrast";

    public static final String HS_KEEP = "history.keep";
    public static final String HS_RETENTION = "history.retention";
    public static final String HS_RECENT_URLS = "history.recent_urls";
    public static final String HS_SEARCH = "history.search";

    public static final String PR_ENABLE = "processing.enable";
    public static final String PR_DELETE_SOURCE = "processing.delete_source";
    public static final String PR_PRESERVE_ON_FAIL = "processing.preserve_on_fail";
    public static final String PR_HW_ACCEL = "processing.hw_accel";
    public static final String PR_PRIORITY = "processing.priority";
    public static final String PR_BACKGROUND = "processing.background";
    public static final String PR_MAX_TEMP_MB = "processing.max_temp_mb";
    public static final String PR_LOG_LEVEL = "processing.log_level";

    public static final Set<String> SENSITIVE_KEYS;

    static {
        Set<String> keys = new HashSet<>();
        keys.add(NW_PROXY_USER);
        keys.add(NW_PROXY_PASSWORD_SET);
        SENSITIVE_KEYS = Collections.unmodifiableSet(keys);
    }
}
