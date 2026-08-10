package com.kira.kdownloader.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.util.LruCache
import android.util.Size
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest

/**
 * Produces a small thumbnail for a locally stored media file (a MediaStore `content://` URI).
 *
 * Files discovered on the device (as opposed to ones downloaded in-app) have no remote thumbnail
 * URL, so the history list showed a generic icon for them. This extracts a real thumbnail from the
 * file itself — a video frame or an audio track's embedded artwork.
 *
 * Extraction (`loadThumbnail` / `MediaMetadataRetriever`) is expensive, so results are cached at
 * two levels:
 *  - an in-memory [LruCache] sized by bitmap bytes, so scrolling never re-decodes a visible item;
 *  - a persistent disk cache under the app's cache dir, so thumbnails survive memory eviction and
 *    app restarts and are never re-extracted from the source file more than once.
 */
object MediaThumbnails {

    // Cap the in-memory cache at 1/8 of the app's available heap, measured in bytes rather than a
    // fixed entry count — a handful of large frames could otherwise blow the budget.
    private val memoryCache = object : LruCache<String, Bitmap>(
        (Runtime.getRuntime().maxMemory() / 1024 / 8).toInt().coerceAtLeast(4 * 1024),
    ) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount / 1024
    }

    // Serializes disk writes per key so two concurrent loads of the same URI don't clobber the file.
    private val diskLock = Mutex()
    private const val DISK_DIR = "media_thumbs"
    private const val JPEG_QUALITY = 85

    /**
     * Synchronous, non-blocking lookup of an already-decoded thumbnail. Returns null if it isn't in
     * the in-memory cache. Safe to call during composition — lets the UI paint a cached thumbnail
     * immediately (e.g. when returning to the history page) instead of flashing a placeholder.
     */
    fun peek(uriString: String, sizePx: Int = 256): Bitmap? =
        memoryCache.get(keyFor(uriString, sizePx))

    suspend fun load(
        context: Context,
        uriString: String,
        isAudio: Boolean,
        sizePx: Int = 256,
    ): Bitmap? {
        val key = keyFor(uriString, sizePx)
        memoryCache.get(key)?.let { return it }

        val bitmap = withContext(Dispatchers.IO) {
            // Disk tier: reuse a previously extracted thumbnail if one exists.
            readFromDisk(context, key)?.let { return@withContext it }

            val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return@withContext null
            val extracted = runCatching {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    context.contentResolver.loadThumbnail(uri, Size(sizePx, sizePx), null)
                } else {
                    legacyThumbnail(context, uri, isAudio)
                }
            }.getOrNull()

            extracted?.also { writeToDisk(context, key, it) }
        }

        bitmap?.let { memoryCache.put(key, it) }
        return bitmap
    }

    private fun keyFor(uriString: String, sizePx: Int): String {
        val digest = MessageDigest.getInstance("SHA-1")
            .digest("$uriString@$sizePx".toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun cacheFile(context: Context, key: String): File {
        val dir = File(context.cacheDir, DISK_DIR).apply { mkdirs() }
        return File(dir, "$key.jpg")
    }

    private fun readFromDisk(context: Context, key: String): Bitmap? {
        val file = cacheFile(context, key)
        if (!file.exists()) return null
        return runCatching { BitmapFactory.decodeFile(file.absolutePath) }.getOrNull()
    }

    private suspend fun writeToDisk(context: Context, key: String, bitmap: Bitmap) {
        diskLock.withLock {
            runCatching {
                cacheFile(context, key).outputStream().use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                }
            }
        }
    }

    private fun legacyThumbnail(context: Context, uri: Uri, isAudio: Boolean): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            if (isAudio) {
                retriever.embeddedPicture?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
            } else {
                retriever.getFrameAtTime(0)
            }
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { retriever.release() }
        }
    }
}
