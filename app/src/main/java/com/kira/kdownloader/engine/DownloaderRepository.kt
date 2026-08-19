package com.kira.kdownloader.engine

import android.content.Context
import android.util.Log
import com.kira.kdownloader.BuildConfig
import com.kira.kdownloader.R
import com.yausername.ffmpeg.FFmpeg
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.SecureRandom
import java.util.concurrent.atomic.AtomicBoolean

data class MediaInfo(
    val title: String,
    val thumbnailUrl: String?,
    val choices: List<DownloadChoice>,
    /** Duration in seconds, if known (0/absent when the extractor doesn't report it). */
    val durationSeconds: Int = 0,
    val uploader: String? = null,
)

class EngineException(message: String, cause: Throwable? = null) : Exception(message, cause)

class DownloaderRepository(context: Context) {
    private val appContext = context.applicationContext
    private val backgroundScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val updateInFlight = AtomicBoolean(false)

    suspend fun init() = withContext(Dispatchers.IO) {
        initMutex.withLock {
            if (!initialized) {
                try {
                    YoutubeDL.getInstance().init(appContext)
                    FFmpeg.getInstance().init(appContext)
                    val installedVersion = ensureCurrentYtDlpInstalled()
                    registerBundledYtDlpVersionIfNeeded(installedVersion)
                    initialized = true
                } catch (error: Throwable) {
                    throw EngineException("Failed to initialize download engine", error)
                }
            }
        }

        // Run the self-update in the background so it never delays fetching formats. The bundled
        // yt-dlp already works; an update just refreshes it opportunistically. At most one runs at
        // a time, and it's rescheduled on the next init once finished (its own 24h throttle decides
        // whether to actually hit the network).
        if (updateInFlight.compareAndSet(false, true)) {
            backgroundScope.launch {
                try {
                    updateEngineIfNeeded()
                } finally {
                    updateInFlight.set(false)
                }
            }
        }
    }

    suspend fun fetchInfo(url: String): MediaInfo = withContext(Dispatchers.IO) {
        val tikTokStrategies = if (ExtractorOptions.isTikTokUrl(url)) {
            listOf(true, false)
        } else {
            listOf(true)
        }
        var lastError: Throwable? = null

        for (useTikTokAppInfo in tikTokStrategies) {
            try {
                val infoRequest = baseRequest(url, useTikTokAppInfo).apply {
                    // getInfo adds --dump-json. Keep stdout JSON-only so first-run component
                    // messages cannot break Jackson's parser on slower/older devices.
                    addOption("--quiet")
                    addOption("--no-warnings")
                    addOption("--no-progress")
                }
                val info = YoutubeDL.getInstance().getInfo(infoRequest)
                val formats = info.formats.orEmpty().map { format ->
                    FormatInput(
                        format.formatId.orEmpty(),
                        format.ext.orEmpty(),
                        format.height.takeIf { it > 0 },
                        format.vcodec,
                        format.acodec,
                        format.url,
                        format.httpHeaders.orEmpty(),
                        // yt-dlp reports exact `filesize` or `filesize_approx`; prefer the former.
                        format.fileSize.takeIf { it > 0 }
                            ?: format.fileSizeApproximate.takeIf { it > 0 },
                    )
                }

                return@withContext MediaInfo(
                    title = info.title ?: url,
                    thumbnailUrl = info.thumbnail,
                    choices = FormatSelector.choices(formats, url),
                    durationSeconds = info.duration,
                    uploader = info.uploader,
                )
            } catch (error: Throwable) {
                lastError = error
                if (useTikTokAppInfo && tikTokStrategies.size > 1) {
                    Log.w(TAG, "TikTok mobile extraction failed; retrying web extraction", error)
                }
            }
        }

        val error = lastError ?: error("No media information strategy was attempted")
        throw EngineException(readableErrorMessage(url, error), error)
    }

    fun buildRequest(
        url: String,
        choice: DownloadChoice,
        outputDirectory: File,
        outputTitle: String,
        useTikTokAppInfo: Boolean = true,
    ): YoutubeDLRequest {
        val directUrl = choice.directUrl
        if (!directUrl.isNullOrBlank()) {
            return YoutubeDLRequest(directUrl).apply {
                addOption(
                    "-o",
                    File(outputDirectory, "${safeOutputTitle(outputTitle)}.%(ext)s").absolutePath,
                )
                choice.httpHeaders.forEach { (name, value) ->
                    if (SAFE_HEADER_NAME.matches(name) && !value.contains('\r') && !value.contains('\n')) {
                        addOption("--add-header", "$name:$value")
                    }
                }
                if (choice.kind == FormatSelector.Kind.AUDIO) {
                    addOption("-x")
                    addOption("--audio-format", "mp3")
                }
            }
        }

        return baseRequest(url, useTikTokAppInfo).apply {
            addOption(
                "-o",
                File(outputDirectory, "${safeOutputTitle(outputTitle)}.%(ext)s").absolutePath,
            )
            addOption("-f", choice.formatSelector)

            if (choice.kind == FormatSelector.Kind.AUDIO) {
                addOption("-x")
                addOption("--audio-format", "mp3")
            } else {
                addOption("--merge-output-format", "mp4")
            }
        }
    }

    private fun safeOutputTitle(title: String): String = title
        .replace(INVALID_FILE_NAME_CHARACTERS, "_")
        .trim(' ', '.')
        .take(MAX_OUTPUT_TITLE_LENGTH)
        .ifBlank { "download" }

    suspend fun execute(
        request: YoutubeDLRequest,
        processId: String,
        onProgress: (progress: Float, etaSeconds: Long, line: String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        try {
            YoutubeDL.getInstance().execute(request, processId) { progress, etaSeconds, line ->
                onProgress(progress, etaSeconds, line)
            }
        } catch (error: Throwable) {
            throw EngineException(error.message ?: "Download failed", error)
        }
    }

    fun cancel(processId: String): Boolean = YoutubeDL.getInstance().destroyProcessById(processId)

    private fun baseRequest(
        url: String,
        useTikTokAppInfo: Boolean = true,
    ) = YoutubeDLRequest(url).apply {
        addOption("--no-playlist")
        // The EJS remote component is a YouTube concern. Requesting it for every site adds a
        // needless GitHub dependency and can contaminate --dump-json output on first use.
        if (ExtractorOptions.isYouTubeUrl(url)) {
            addOption("--remote-components", "ejs:github")
        }
        if (useTikTokAppInfo && ExtractorOptions.isTikTokUrl(url)) {
            addOption(
                "--extractor-args",
                ExtractorOptions.tikTokAppInfo(tikTokInstallIds()),
            )
        }
    }

    private fun tikTokInstallIds(): List<String> {
        val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val storedIds = preferences.getString(TIKTOK_INSTALL_IDS_KEY, null)
            ?.split(',')
            ?.filter(TIKTOK_INSTALL_ID_PATTERN::matches)
            .orEmpty()
        if (storedIds.size == TIKTOK_INSTALL_ID_COUNT) return storedIds

        val generatedIds = buildSet {
            while (size < TIKTOK_INSTALL_ID_COUNT) {
                add(buildString {
                    append("73")
                    repeat(17) { append(secureRandom.nextInt(10)) }
                })
            }
        }.toList()
        preferences.edit()
            .putString(TIKTOK_INSTALL_IDS_KEY, generatedIds.joinToString(","))
            .apply()
        return generatedIds
    }

    private fun ensureCurrentYtDlpInstalled(): String {
        val installedVersion = readInstalledYtDlpVersion()
        if (
            !YtDlpVersionPolicy.shouldInstallBundled(
                installedVersion,
                BuildConfig.BUNDLED_YTDLP_VERSION,
            )
        ) {
            return installedVersion.orEmpty()
        }

        Log.i(
            TAG,
            "Replacing stale yt-dlp ${installedVersion ?: "unknown"} with bundled " +
                BuildConfig.BUNDLED_YTDLP_VERSION,
        )
        installBundledYtDlp()
        val verifiedVersion = readInstalledYtDlpVersion()
        check(verifiedVersion == BuildConfig.BUNDLED_YTDLP_VERSION) {
            "Bundled yt-dlp verification failed: found ${verifiedVersion ?: "unknown"}"
        }
        return verifiedVersion
    }

    private fun readInstalledYtDlpVersion(): String? = runCatching {
        YoutubeDL.getInstance().execute(
            YoutubeDLRequest(emptyList()).addOption("--version"),
        ).out.lineSequence().firstOrNull(String::isNotBlank)?.trim()
    }.onFailure { error ->
        Log.w(TAG, "Could not read the installed yt-dlp version", error)
    }.getOrNull()

    private fun installBundledYtDlp() {
        val ytdlpDirectory = File(
            appContext.noBackupFilesDir,
            "${YoutubeDL.baseName}/${YoutubeDL.ytdlpDirName}",
        ).apply {
            check(mkdirs() || isDirectory) { "Could not create the yt-dlp directory" }
        }
        val target = File(ytdlpDirectory, YoutubeDL.ytdlpBin)
        val temporary = File(ytdlpDirectory, "${YoutubeDL.ytdlpBin}.new")

        try {
            appContext.resources.openRawResource(R.raw.ytdlp).use { input ->
                temporary.outputStream().use(input::copyTo)
            }
            check(temporary.length() > 0L) { "Bundled yt-dlp resource is empty" }
            if (target.exists()) check(target.delete()) { "Could not replace stale yt-dlp" }
            if (!temporary.renameTo(target)) {
                temporary.copyTo(target, overwrite = true)
                check(temporary.delete()) { "Could not remove temporary yt-dlp copy" }
            }
        } finally {
            if (temporary.exists() && !temporary.delete()) {
                Log.w(TAG, "Could not remove ${temporary.absolutePath}")
            }
        }
    }

    private fun registerBundledYtDlpVersionIfNeeded(installedVersion: String) {
        val ytdlpPreferences = appContext.getSharedPreferences(
            YTDLP_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        )

        if (installedVersion != BuildConfig.BUNDLED_YTDLP_VERSION) {
            // A newer version installed by the updater must not be downgraded or relabeled.
            return
        }

        if (
            ytdlpPreferences.getString(YTDLP_VERSION_KEY, null) == installedVersion &&
            appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
                .contains(LAST_UPDATE_ATTEMPT)
        ) {
            return
        }

        ytdlpPreferences.edit()
            .putString(YTDLP_VERSION_KEY, BuildConfig.BUNDLED_YTDLP_VERSION)
            .putString(YTDLP_VERSION_NAME_KEY, "yt-dlp ${BuildConfig.BUNDLED_YTDLP_VERSION}")
            .apply()
        appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
            .edit()
            .putLong(LAST_UPDATE_ATTEMPT, System.currentTimeMillis())
            .apply()
        Log.i(TAG, "Using bundled yt-dlp ${BuildConfig.BUNDLED_YTDLP_VERSION}")
    }

    private fun readableErrorMessage(url: String, error: Throwable): String {
        val details = generateSequence(error) { it.cause }
            .mapNotNull { it.message?.takeIf(String::isNotBlank) }
            .joinToString("\n")
        if (
            ExtractorOptions.isTikTokUrl(url) &&
            ("expecting value" in details.lowercase() || "column 1" in details.lowercase())
        ) {
            return "TikTok returned an invalid response. Check the connection and try again."
        }
        return error.message?.takeIf(String::isNotBlank) ?: "Could not fetch media info"
    }

    private suspend fun updateEngineIfNeeded() {
        val preferences = appContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - preferences.getLong(LAST_UPDATE_ATTEMPT, 0L) < UPDATE_INTERVAL_MS) return

        updateMutex.withLock {
            val currentTime = System.currentTimeMillis()
            if (
                currentTime - preferences.getLong(LAST_UPDATE_ATTEMPT, 0L) <
                UPDATE_INTERVAL_MS
            ) {
                return@withLock
            }

            var lastError: Throwable? = null
            repeat(MAX_UPDATE_ATTEMPTS) { attempt ->
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(
                        appContext,
                        YoutubeDL.UpdateChannel.STABLE,
                    )
                    preferences.edit().putLong(LAST_UPDATE_ATTEMPT, currentTime).apply()
                    Log.i(
                        TAG,
                        "yt-dlp is up to date: ${YoutubeDL.getInstance().versionName(appContext)}",
                    )
                    return@withLock
                } catch (error: Throwable) {
                    lastError = error
                    if (attempt + 1 < MAX_UPDATE_ATTEMPTS) {
                        delay(UPDATE_RETRY_DELAY_MS * (attempt + 1))
                    }
                }
            }

            // Preserve the bundled version while offline, but leave the timestamp unset so the
            // next fetch retries instead of suppressing updates for a full day.
            preferences.edit().remove(LAST_UPDATE_ATTEMPT).apply()
            Log.w(TAG, "Could not update yt-dlp after $MAX_UPDATE_ATTEMPTS attempts", lastError)
        }
    }

    companion object {
        private const val TAG = "DownloaderRepository"
        private const val PREFERENCES_NAME = "download_engine"
        private const val LAST_UPDATE_ATTEMPT = "last_update_attempt"
        private const val YTDLP_PREFERENCES_NAME = "youtubedl-android"
        private const val YTDLP_VERSION_KEY = "dlpVersion"
        private const val YTDLP_VERSION_NAME_KEY = "dlpVersionName"
        private const val TIKTOK_INSTALL_IDS_KEY = "tiktok_install_ids"
        private const val TIKTOK_INSTALL_ID_COUNT = 3
        private const val UPDATE_INTERVAL_MS = 24 * 60 * 60 * 1000L
        private const val MAX_UPDATE_ATTEMPTS = 3
        private const val UPDATE_RETRY_DELAY_MS = 750L
        private const val MAX_OUTPUT_TITLE_LENGTH = 120

        private val initMutex = Mutex()
        private val updateMutex = Mutex()
        private val secureRandom = SecureRandom()
        private val TIKTOK_INSTALL_ID_PATTERN = Regex("\\d{19}")
        private val SAFE_HEADER_NAME = Regex("[A-Za-z0-9-]+")
        private val INVALID_FILE_NAME_CHARACTERS = Regex("[\\\\/:*?\"<>|\\u0000-\\u001F]")

        @Volatile
        private var initialized = false
    }
}
