package com.kira.kdownloader.util

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.kira.kdownloader.data.DownloadDao
import com.kira.kdownloader.data.DownloadEntity
import com.kira.kdownloader.data.DownloadStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File

object DownloadDirectoryScanner {
    private const val TAG = "DownloadDirectoryScanner"
    private const val NORMALIZED_DOWNLOAD_ROOT = "download"
    private val directoryNames = setOf("kdownloader", "kdownloads")
    private val directoryQueryNames = listOf("KDownloader", "KDownloads")

    private val audioExtensions = setOf("aac", "flac", "m4a", "mp3", "ogg", "opus", "wav")
    private val videoExtensions = setOf("avi", "m4v", "mkv", "mov", "mp4", "webm")
    private val syncMutex = Mutex()

    /** Skip re-scanning if the last scan finished within this window (unless forced). */
    private const val MIN_RESCAN_INTERVAL_MS = 30_000L

    @Volatile
    private var lastScanAtMs = 0L

    private data class ScannedMedia(
        val mediaStoreId: Long,
        val entity: DownloadEntity,
    )

    /**
     * Scans the KDownloads directories and inserts any media not already in history.
     *
     * Cheap to call often: results are throttled to at most once per [MIN_RESCAN_INTERVAL_MS], so
     * returning to the screen (ON_RESUME) doesn't trigger a fresh MediaStore query each time. Pass
     * [force] = true when the caller knows something changed (e.g. media permission just granted).
     */
    suspend fun syncIntoHistory(context: Context, dao: DownloadDao, force: Boolean = false): Int =
        withContext(Dispatchers.IO) {
            syncMutex.withLock {
                val now = System.currentTimeMillis()
                if (!force && lastScanAtMs != 0L && now - lastScanAtMs < MIN_RESCAN_INTERVAL_MS) {
                    return@withLock 0
                }

                val existingUris = dao.getAllFileUris().toHashSet()
                val existingMediaStoreIds = existingUris
                    .mapNotNull(::mediaStoreIdFrom)
                    .toHashSet()
                val discovered = runCatching { scan(context.applicationContext) }
                    .onFailure { Log.w(TAG, "Could not scan the KDownloads directories", it) }
                    .getOrDefault(emptyList())
                    .filterNot { media ->
                        media.mediaStoreId in existingMediaStoreIds ||
                            media.entity.fileUri in existingUris
                    }

                discovered.forEach { media ->
                    dao.insert(media.entity)
                    existingMediaStoreIds.add(media.mediaStoreId)
                    media.entity.fileUri?.let(existingUris::add)
                }
                lastScanAtMs = System.currentTimeMillis()
                discovered.size
            }
        }

    private fun scan(context: Context): List<ScannedMedia> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            scanScopedStorage(context)
        } else {
            scanLegacyStorage(context)
        }

    private fun scanScopedStorage(context: Context): List<ScannedMedia> {
        val seenIds = mutableSetOf<Long>()
        val results = mutableListOf<ScannedMedia>()
        val collections = listOf(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        )

        collections.forEach { collection ->
            runCatching { queryScopedCollection(context, collection) }
                .onFailure { error ->
                    Log.w(TAG, "Could not query $collection for download history", error)
                }
                .getOrDefault(emptyList())
                .forEach { media ->
                    if (seenIds.add(media.mediaStoreId)) results.add(media)
                }
        }
        return results
    }

    private fun queryScopedCollection(context: Context, collection: Uri): List<ScannedMedia> {
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.RELATIVE_PATH,
        )
        val selection = buildString {
            append("${MediaStore.MediaColumns.IS_PENDING} = 0 AND (")
            append(
                directoryQueryNames.joinToString(" OR ") {
                    "${MediaStore.MediaColumns.RELATIVE_PATH} LIKE ?"
                },
            )
            append(")")
        }
        val selectionArgs = directoryQueryNames
            .map { name -> "${Environment.DIRECTORY_DOWNLOADS}/$name%" }
            .toTypedArray()

        val results = mutableListOf<ScannedMedia>()
        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH)

            while (cursor.moveToNext()) {
                if (!isSupportedRelativePath(cursor.getString(pathColumn))) continue
                scannedMedia(
                    collection = collection,
                    id = cursor.getLong(idColumn),
                    displayName = cursor.getString(nameColumn),
                    mimeType = cursor.getString(mimeColumn),
                    dateAddedSeconds = cursor.getLong(dateColumn),
                )?.let(results::add)
            }
        }
        return results
    }

    @Suppress("DEPRECATION")
    private fun scanLegacyStorage(context: Context): List<ScannedMedia> {
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.DATE_ADDED,
            MediaStore.MediaColumns.DATA,
        )
        val downloadsDirectory = Environment.getExternalStoragePublicDirectory(
            Environment.DIRECTORY_DOWNLOADS,
        )
        val selection = directoryQueryNames.joinToString(" OR ") {
            "${MediaStore.MediaColumns.DATA} LIKE ?"
        }
        val selectionArgs = directoryQueryNames
            .map { name -> File(downloadsDirectory, name).absolutePath + File.separator + "%" }
            .toTypedArray()

        val results = mutableListOf<ScannedMedia>()
        context.contentResolver.query(
            collection,
            projection,
            selection,
            selectionArgs,
            "${MediaStore.MediaColumns.DATE_ADDED} DESC",
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID)
            val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME)
            val mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)
            val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)

            while (cursor.moveToNext()) {
                if (!isSupportedLegacyPath(cursor.getString(pathColumn))) continue
                scannedMedia(
                    collection = collection,
                    id = cursor.getLong(idColumn),
                    displayName = cursor.getString(nameColumn),
                    mimeType = cursor.getString(mimeColumn),
                    dateAddedSeconds = cursor.getLong(dateColumn),
                )?.let(results::add)
            }
        }
        return results
    }

    private fun scannedMedia(
        collection: Uri,
        id: Long,
        displayName: String?,
        mimeType: String?,
        dateAddedSeconds: Long,
    ): ScannedMedia? {
        val name = displayName ?: return null
        val createdAt = dateAddedSeconds
            .takeIf { it > 0 }
            ?.times(1_000L)
            ?: System.currentTimeMillis()
        val uri = ContentUris.withAppendedId(collection, id).toString()
        val entity = entityFrom(name, mimeType, uri, createdAt) ?: return null
        return ScannedMedia(id, entity)
    }

    internal fun isSupportedRelativePath(relativePath: String?): Boolean {
        val normalized = relativePath
            ?.replace('\\', '/')
            ?.trim('/')
            ?.lowercase()
            ?: return false
        val segments = normalized.split('/')
        return segments.size >= 2 &&
            segments[0] == NORMALIZED_DOWNLOAD_ROOT &&
            segments[1] in directoryNames
    }

    internal fun isSupportedLegacyPath(absolutePath: String?): Boolean {
        val normalized = absolutePath
            ?.replace('\\', '/')
            ?.lowercase()
            ?: return false
        val marker = "/$NORMALIZED_DOWNLOAD_ROOT/"
        val directoryAndFile = normalized.substringAfter(marker, missingDelimiterValue = "")
        return directoryAndFile.substringBefore('/') in directoryNames
    }

    private fun mediaStoreIdFrom(uriString: String): Long? = runCatching {
        ContentUris.parseId(Uri.parse(uriString))
    }.getOrNull()

    internal fun entityFrom(
        displayName: String,
        mimeType: String?,
        fileUri: String,
        createdAt: Long,
    ): DownloadEntity? {
        val extension = displayName.substringAfterLast('.', "").lowercase()
        val kind = when {
            mimeType?.startsWith("audio/") == true || extension in audioExtensions -> "AUDIO"
            mimeType?.startsWith("video/") == true || extension in videoExtensions -> "VIDEO"
            else -> return null
        }
        val title = displayName.substringBeforeLast('.', displayName).ifBlank { displayName }

        return DownloadEntity(
            title = title,
            sourceUrl = "",
            kind = kind,
            formatLabel = extension.uppercase().ifBlank {
                if (kind == "AUDIO") "Audio" else "Video"
            },
            fileUri = fileUri,
            thumbnailUrl = null,
            createdAt = createdAt,
            status = DownloadStatus.COMPLETED,
        )
    }
}
