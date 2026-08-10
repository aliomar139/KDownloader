package com.kira.kdownloader.util

import android.content.ContentValues
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.kira.kdownloader.data.AppDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadDirectoryScannerAndroidTest {
    @Test
    fun mediaStoreFileInDefaultDirectoryIsImportedOnce() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)

        val context = ApplicationProvider.getApplicationContext<Context>()
        val database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        var mediaUri: Uri? = null

        try {
            val displayName = "history-scan-${System.currentTimeMillis()}.mp4"
            mediaUri = context.contentResolver.insert(
                MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/KDownloads",
                    )
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                },
            )
            assertNotNull(mediaUri)

            context.contentResolver.openOutputStream(requireNotNull(mediaUri)).use { output ->
                requireNotNull(output).write(byteArrayOf(0, 0, 0, 20, 102, 116, 121, 112))
            }
            context.contentResolver.update(
                requireNotNull(mediaUri),
                ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) },
                null,
                null,
            )

            val firstImportCount = DownloadDirectoryScanner.syncIntoHistory(
                context,
                database.downloadDao(),
                force = true,
            )
            val insertedMediaId = ContentUris.parseId(requireNotNull(mediaUri))
            val imported = database.downloadDao().observeAll().first()
                .singleOrNull { row ->
                    runCatching {
                        ContentUris.parseId(Uri.parse(row.fileUri)) == insertedMediaId
                    }.getOrDefault(false)
                }

            assertNotNull(imported)
            assertEquals("VIDEO", imported?.kind)
            assertEquals(
                0,
                DownloadDirectoryScanner.syncIntoHistory(
                    context,
                    database.downloadDao(),
                    force = true,
                ),
            )
            assertEquals(imported, database.downloadDao().observeAll().first()
                .singleOrNull { row ->
                    runCatching {
                        ContentUris.parseId(Uri.parse(row.fileUri)) == insertedMediaId
                    }.getOrDefault(false)
                })
            check(firstImportCount >= 1)
        } finally {
            mediaUri?.let { uri -> context.contentResolver.delete(uri, null, null) }
            database.close()
        }
    }
}
