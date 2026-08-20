package com.kira.kdownloader.util;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.kira.kdownloader.data.AppDatabase;
import com.kira.kdownloader.data.DownloadEntity;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.OutputStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;

@RunWith(AndroidJUnit4.class)
public class DownloadDirectoryScannerAndroidTest {
    @Test public void mediaStoreFileInDefaultDirectoryIsImportedOnce() throws Exception {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q);
        Context context = ApplicationProvider.getApplicationContext();
        AppDatabase database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        Uri mediaUri = null;
        try {
            String displayName = "history-scan-" + System.currentTimeMillis() + ".mp4";
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, displayName);
            values.put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4");
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/KDownloads");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
            mediaUri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            assertNotNull(mediaUri);
            try (OutputStream output = context.getContentResolver().openOutputStream(mediaUri)) {
                assertNotNull(output);
                output.write(new byte[]{0, 0, 0, 20, 102, 116, 121, 112});
            }
            ContentValues complete = new ContentValues();
            complete.put(MediaStore.MediaColumns.IS_PENDING, 0);
            context.getContentResolver().update(mediaUri, complete, null, null);

            int firstImportCount = DownloadDirectoryScanner.syncIntoHistory(context, database.downloadDao(), true);
            long insertedMediaId = ContentUris.parseId(mediaUri);
            DownloadEntity imported = findByMediaId(database, insertedMediaId);
            assertNotNull(imported);
            assertEquals("VIDEO", imported.getKind());
            assertEquals(0, DownloadDirectoryScanner.syncIntoHistory(context, database.downloadDao(), true));
            assertEquals(imported, findByMediaId(database, insertedMediaId));
            if (firstImportCount < 1) throw new AssertionError("Expected at least one imported row");
        } finally {
            if (mediaUri != null) context.getContentResolver().delete(mediaUri, null, null);
            database.close();
        }
    }

    private DownloadEntity findByMediaId(AppDatabase database, long mediaId) {
        for (DownloadEntity row : database.downloadDao().getAllSync()) {
            try {
                if (row.getFileUri() != null && ContentUris.parseId(Uri.parse(row.getFileUri())) == mediaId) return row;
            } catch (RuntimeException ignored) {}
        }
        return null;
    }
}
