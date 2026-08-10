package com.kira.kdownloader.settings.platform

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.util.Locale

/**
 * Wraps the Storage Access Framework for folder selection and health (Section 3, Section 11).
 *
 * The app never requests broad `WRITE_EXTERNAL_STORAGE`; instead the user picks a folder with the
 * system picker and we persist the returned tree URI permission so it survives restarts. This class
 * also detects when that access has been revoked and reports readable names and free space.
 */
class FolderAccessManager(context: Context) {
    private val appContext = context.applicationContext

    /** Persists read+write permission for a tree URI returned by [Intent.ACTION_OPEN_DOCUMENT_TREE]. */
    fun persist(uri: Uri) {
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { appContext.contentResolver.takePersistableUriPermission(uri, flags) }
            .onFailure { Log.w(TAG, "Could not persist permission for $uri", it) }
    }

    /** Releases a previously persisted permission (e.g. when the user clears the folder). */
    fun release(uriString: String) {
        if (uriString.isEmpty()) return
        val uri = uriString.toUri() ?: return
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        runCatching { appContext.contentResolver.releasePersistableUriPermission(uri, flags) }
    }

    /** True when we still hold a persisted, writable grant for [uriString]. */
    fun hasAccess(uriString: String): Boolean {
        if (uriString.isEmpty()) return false
        val uri = uriString.toUri() ?: return false
        val stillGranted = appContext.contentResolver.persistedUriPermissions.any {
            it.uri == uri && it.isWritePermission
        }
        if (!stillGranted) return false
        return runCatching {
            DocumentFile.fromTreeUri(appContext, uri)?.canWrite() == true
        }.getOrDefault(false)
    }

    /** A human-readable folder name, or a decoded fallback derived from the URI. */
    fun displayName(uriString: String): String {
        if (uriString.isEmpty()) return "Not set"
        val uri = uriString.toUri() ?: return "Unknown folder"
        val documentName = runCatching { DocumentFile.fromTreeUri(appContext, uri)?.name }.getOrNull()
        if (!documentName.isNullOrBlank()) return documentName
        // Fall back to the last path segment of the tree id, e.g. "primary:Download/Videos".
        val decoded = Uri.decode(uri.toString())
        return decoded.substringAfterLast(':').substringAfterLast('/').ifBlank { "Selected folder" }
    }

    /** Available bytes on the primary shared storage volume (best-effort; SAF hides real paths). */
    fun availableBytes(): Long = runCatching {
        val dir = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Environment.getExternalStorageDirectory() ?: Environment.getDataDirectory()
        } else {
            Environment.getDataDirectory()
        }
        val stat = StatFs(dir.path)
        stat.availableBlocksLong * stat.blockSizeLong
    }.getOrDefault(-1L)

    fun totalBytes(): Long = runCatching {
        val dir = Environment.getExternalStorageDirectory() ?: Environment.getDataDirectory()
        val stat = StatFs(dir.path)
        stat.blockCountLong * stat.blockSizeLong
    }.getOrDefault(-1L)

    private fun String.toUri(): Uri? = runCatching { Uri.parse(this) }.getOrNull()

    companion object {
        private const val TAG = "FolderAccessManager"

        /** Formats a byte count into a compact human string (e.g. "3.4 GB"). */
        fun formatBytes(bytes: Long): String {
            if (bytes < 0) return "Unknown"
            if (bytes < 1024) return "$bytes B"
            val units = arrayOf("KB", "MB", "GB", "TB")
            var value = bytes.toDouble() / 1024.0
            var unit = 0
            while (value >= 1024.0 && unit < units.lastIndex) {
                value /= 1024.0
                unit++
            }
            return String.format(Locale.US, "%.1f %s", value, units[unit])
        }
    }
}
