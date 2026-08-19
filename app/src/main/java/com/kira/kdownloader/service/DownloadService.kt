package com.kira.kdownloader.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.kira.kdownloader.MainActivity
import com.kira.kdownloader.data.AppDatabase
import com.kira.kdownloader.data.DownloadEntity
import com.kira.kdownloader.data.DownloadStatus
import com.kira.kdownloader.engine.DownloadChoice
import com.kira.kdownloader.engine.DownloadOutputSelector
import com.kira.kdownloader.engine.DownloaderRepository
import com.kira.kdownloader.engine.EngineException
import com.kira.kdownloader.engine.ExtractorOptions
import com.kira.kdownloader.engine.FormatSelector
import com.kira.kdownloader.util.MediaStoreWriter
import com.yausername.youtubedl_android.YoutubeDL
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.io.File

class DownloadService : Service() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val repository by lazy { DownloaderRepository(applicationContext) }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            intent.getStringExtra(EXTRA_PROCESS_ID)?.let(repository::cancel)
            return START_NOT_STICKY
        }

        val url = intent?.getStringExtra(EXTRA_URL) ?: return stopInvalidStart(startId)
        val title = intent.getStringExtra(EXTRA_TITLE) ?: url
        val thumbnailUrl = intent.getStringExtra(EXTRA_THUMBNAIL)
        val kind = intent.getStringExtra(EXTRA_KIND)
            ?.let { runCatching { FormatSelector.Kind.valueOf(it) }.getOrNull() }
            ?: return stopInvalidStart(startId)
        val label = intent.getStringExtra(EXTRA_LABEL).orEmpty()
        val selector = intent.getStringExtra(EXTRA_SELECTOR) ?: return stopInvalidStart(startId)
        val headerKeys = intent.getStringArrayListExtra(EXTRA_HEADER_KEYS).orEmpty()
        val headerValues = intent.getStringArrayListExtra(EXTRA_HEADER_VALUES).orEmpty()
        val choice = DownloadChoice(
            label,
            kind,
            selector,
            intent.getStringExtra(EXTRA_DIRECT_URL),
            headerKeys.zip(headerValues).toMap(),
            null,
        )
        // Prefer the caller-supplied id so the UI can cancel this exact download; fall back to a
        // generated one for older callers.
        val processId = intent.getStringExtra(EXTRA_PROCESS_ID)
            ?: "download-${System.currentTimeMillis()}-$startId"

        createNotificationChannel()
        startForeground(
            NOTIFICATION_ID,
            buildNotification(title, processId, progress = 0, indeterminate = true),
        )
        runDownload(startId, processId, url, title, thumbnailUrl, choice)
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun stopInvalidStart(startId: Int): Int {
        stopSelf(startId)
        return START_NOT_STICKY
    }

    private fun runDownload(
        startId: Int,
        processId: String,
        url: String,
        title: String,
        thumbnailUrl: String?,
        choice: DownloadChoice,
    ) {
        serviceScope.launch {
            val dao = AppDatabase.get(applicationContext).downloadDao()
            val eventKey = DownloadEvents.keyOf(url, choice.label)
            var rowId: Long? = null
            var outputDirectory: File? = null

            DownloadEvents.update(
                eventKey,
                DownloadEvents.State(
                    phase = DownloadEvents.Phase.PREPARING,
                    title = title,
                    kind = choice.kind.name,
                    processId = processId,
                ),
            )
            try {
                val insertedId = dao.insert(
                    DownloadEntity(
                        0L,
                        title,
                        url,
                        choice.kind.name,
                        choice.label,
                        null,
                        thumbnailUrl,
                        System.currentTimeMillis(),
                        DownloadStatus.RUNNING,
                    ),
                )
                rowId = insertedId
                val tempDirectory = File(cacheDir, "download-$insertedId").apply {
                    check(mkdirs() || isDirectory) { "Could not create temporary directory" }
                }
                outputDirectory = tempDirectory

                repository.init()
                val onProgress: (Float, Long, String) -> Unit = { progress, etaSeconds, _ ->
                    val percent = progress.toInt()
                    updateNotification(title, processId, percent.coerceIn(0, 100))
                    DownloadEvents.update(
                        eventKey,
                        DownloadEvents.State(
                            phase = DownloadEvents.Phase.RUNNING,
                            percent = percent,
                            title = title,
                            kind = choice.kind.name,
                            etaSeconds = etaSeconds,
                            processId = processId,
                        ),
                    )
                }
                val outputFile = executeWithFallbacks(
                    url = url,
                    choice = choice,
                    outputDirectory = tempDirectory,
                    outputTitle = title,
                    processId = processId,
                    onProgress = onProgress,
                )
                val outputUri = MediaStoreWriter.publish(
                    applicationContext,
                    outputFile,
                    choice.kind == FormatSelector.Kind.AUDIO,
                )

                dao.updateStatusAndUri(
                    insertedId,
                    DownloadStatus.COMPLETED,
                    outputUri.toString(),
                )
                DownloadEvents.update(
                    eventKey,
                    DownloadEvents.State(
                        phase = DownloadEvents.Phase.COMPLETED,
                        percent = 100,
                        title = title,
                        kind = choice.kind.name,
                        fileUri = outputUri.toString(),
                    ),
                )
            } catch (error: Throwable) {
                if (isCanceled(error)) {
                    // User-initiated cancel: drop the record entirely so the row returns to idle
                    // (the download button reappears) instead of showing a failure.
                    Log.i(TAG, "Download canceled by user")
                    rowId?.let { dao.deleteById(it) }
                    DownloadEvents.clear(eventKey)
                } else {
                    Log.e(TAG, "Download failed", error)
                    rowId?.let { dao.updateStatusAndUri(it, DownloadStatus.FAILED, null) }
                    DownloadEvents.update(
                        eventKey,
                        DownloadEvents.State(
                            phase = DownloadEvents.Phase.FAILED,
                            title = title,
                            kind = choice.kind.name,
                            message = error.message?.takeIf(String::isNotBlank),
                        ),
                    )
                }
            } finally {
                outputDirectory?.deleteRecursively()
                stopForegroundCompat()
                stopSelf(startId)
            }
        }
    }

    private suspend fun executeWithFallbacks(
        url: String,
        choice: DownloadChoice,
        outputDirectory: File,
        outputTitle: String,
        processId: String,
        onProgress: (progress: Float, etaSeconds: Long, line: String) -> Unit,
    ): File {
        val attempts = buildDownloadAttempts(url, choice)
        var lastError: Throwable? = null

        attempts.forEachIndexed { index, attempt ->
            if (index > 0) clearTemporaryDirectory(outputDirectory)

            try {
                val request = repository.buildRequest(
                    url = url,
                    choice = attempt.choice,
                    outputDirectory = outputDirectory,
                    outputTitle = outputTitle,
                    useTikTokAppInfo = attempt.useTikTokAppInfo,
                )
                repository.execute(request, processId, onProgress)

                DownloadOutputSelector.select(
                    outputDirectory.listFiles().orEmpty().toList(),
                    choice.kind,
                )?.let { return it }

                val producedNames = outputDirectory.listFiles()
                    .orEmpty()
                    .joinToString { it.name }
                throw EngineException(
                    if (choice.kind == FormatSelector.Kind.VIDEO) {
                        "The site did not produce a complete video with audio"
                    } else {
                        "The site did not produce a completed audio file"
                    } + producedNames.takeIf(String::isNotBlank)?.let { ": $it" }.orEmpty(),
                )
            } catch (error: Throwable) {
                if (isCanceled(error)) throw error
                lastError = error
                if (index + 1 < attempts.size) {
                    Log.w(
                        TAG,
                        "Download attempt ${index + 1} failed; trying a compatible fallback",
                        error,
                    )
                }
            }
        }

        throw lastError ?: EngineException("Download failed before an attempt could start")
    }

    private fun buildDownloadAttempts(
        url: String,
        choice: DownloadChoice,
    ): List<DownloadAttempt> = buildList {
        fun addAttempt(attemptChoice: DownloadChoice, useTikTokAppInfo: Boolean) {
            val attempt = DownloadAttempt(attemptChoice, useTikTokAppInfo)
            if (attempt !in this) add(attempt)
        }

        addAttempt(choice, useTikTokAppInfo = true)

        val extractedChoice = choice.withDirect(null, emptyMap())
        if (!choice.directUrl.isNullOrBlank()) {
            addAttempt(extractedChoice, useTikTokAppInfo = true)
        }
        if (ExtractorOptions.isTikTokUrl(url)) {
            addAttempt(extractedChoice, useTikTokAppInfo = false)
        }

        if (
            choice.kind == FormatSelector.Kind.VIDEO &&
            choice.formatSelector != FormatSelector.FALLBACK_VIDEO_SELECTOR
        ) {
            val genericVideoChoice = extractedChoice.withFormatSelector(
                FormatSelector.FALLBACK_VIDEO_SELECTOR,
            )
            addAttempt(genericVideoChoice, useTikTokAppInfo = true)
            if (ExtractorOptions.isTikTokUrl(url)) {
                addAttempt(genericVideoChoice, useTikTokAppInfo = false)
            }
        }
    }

    private fun clearTemporaryDirectory(directory: File) {
        directory.listFiles().orEmpty().forEach { file ->
            check(file.deleteRecursively()) {
                "Could not clear the temporary download before retrying"
            }
        }
    }

    private fun isCanceled(error: Throwable): Boolean =
        generateSequence(error) { it.cause }.any { it is YoutubeDL.CanceledException }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                manager.createNotificationChannel(
                    NotificationChannel(
                        CHANNEL_ID,
                        "Downloads",
                        NotificationManager.IMPORTANCE_LOW,
                    ),
                )
            }
        }
    }

    private fun buildNotification(
        title: String,
        processId: String,
        progress: Int,
        indeterminate: Boolean,
    ): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cancelIntent = PendingIntent.getService(
            this,
            processId.hashCode(),
            Intent(this, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_PROCESS_ID, processId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(if (indeterminate) "Preparing download…" else "Downloading… $progress%")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(openAppIntent)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setProgress(100, progress, indeterminate)
            .addAction(0, "Cancel", cancelIntent)
            .build()
    }

    private fun updateNotification(title: String, processId: String, progress: Int) {
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            buildNotification(title, processId, progress, indeterminate = false),
        )
    }

    private fun stopForegroundCompat() {
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    companion object {
        private const val TAG = "DownloadService"
        private const val CHANNEL_ID = "downloads"
        private const val NOTIFICATION_ID = 42
        private const val ACTION_CANCEL = "com.kira.kdownloader.action.CANCEL"
        private const val EXTRA_URL = "url"
        private const val EXTRA_TITLE = "title"
        private const val EXTRA_THUMBNAIL = "thumbnail"
        private const val EXTRA_KIND = "kind"
        private const val EXTRA_LABEL = "label"
        private const val EXTRA_SELECTOR = "selector"
        private const val EXTRA_DIRECT_URL = "directUrl"
        private const val EXTRA_HEADER_KEYS = "headerKeys"
        private const val EXTRA_HEADER_VALUES = "headerValues"
        private const val EXTRA_PROCESS_ID = "processId"

        fun start(
            context: Context,
            url: String,
            choice: DownloadChoice,
            title: String,
            thumbnailUrl: String?,
            processId: String,
        ) {
            val intent = Intent(context, DownloadService::class.java).apply {
                putExtra(EXTRA_URL, url)
                putExtra(EXTRA_TITLE, title)
                putExtra(EXTRA_THUMBNAIL, thumbnailUrl)
                putExtra(EXTRA_KIND, choice.kind.name)
                putExtra(EXTRA_LABEL, choice.label)
                putExtra(EXTRA_SELECTOR, choice.formatSelector)
                putExtra(EXTRA_DIRECT_URL, choice.directUrl)
                putExtra(EXTRA_PROCESS_ID, processId)
                putStringArrayListExtra(
                    EXTRA_HEADER_KEYS,
                    ArrayList(choice.httpHeaders.keys),
                )
                putStringArrayListExtra(
                    EXTRA_HEADER_VALUES,
                    ArrayList(choice.httpHeaders.values),
                )
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** Cancels the running download identified by [processId]. */
        fun cancel(context: Context, processId: String) {
            val intent = Intent(context, DownloadService::class.java).apply {
                action = ACTION_CANCEL
                putExtra(EXTRA_PROCESS_ID, processId)
            }
            context.startService(intent)
        }
    }

    private data class DownloadAttempt(
        val choice: DownloadChoice,
        val useTikTokAppInfo: Boolean,
    )
}
