package com.kira.kdownloader.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public final class MediaStoreWriter {
    private MediaStoreWriter() {}

    public static Uri publish(Context context, File sourceFile) throws IOException {
        if (!sourceFile.isFile()) throw new IllegalArgumentException("Output file does not exist: " + sourceFile.getName());
        ContentResolver resolver = context.getContentResolver();
        String mimeType = MediaMimeTypes.forStorage(sourceFile.getName());
        Uri collection = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? MediaStore.Downloads.EXTERNAL_CONTENT_URI : MediaStore.Files.getContentUri("external");

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, sourceFile.getName());
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/KDownloader");
            values.put(MediaStore.MediaColumns.IS_PENDING, 1);
        } else {
            File outputDirectory = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "KDownloader");
            outputDirectory.mkdirs();
            values.put(MediaStore.MediaColumns.DATA, new File(outputDirectory, sourceFile.getName()).getPath());
        }

        Uri uri = resolver.insert(collection, values);
        if (uri == null) throw new IllegalStateException("MediaStore insert returned null");
        try {
            try (OutputStream output = resolver.openOutputStream(uri, "w"); InputStream input = new FileInputStream(sourceFile)) {
                if (output == null) throw new IllegalArgumentException("Could not open MediaStore output stream");
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues complete = new ContentValues();
                complete.put(MediaStore.MediaColumns.IS_PENDING, 0);
                resolver.update(uri, complete, null, null);
            }
            if (!sourceFile.delete()) throw new IllegalStateException("Published file but could not delete temporary output");
            // Keep the audio/video address: the downloads one stops working once the app is
            // reinstalled and MediaStore forgets that this row belonged to us.
            return MediaMimeTypes.isPlayable(mimeType)
                    ? MediaUris.inMediaCollection(uri, mimeType.startsWith("audio/"))
                    : uri;
        } catch (Throwable error) {
            resolver.delete(uri, null, null);
            if (error instanceof IOException) throw (IOException) error;
            if (error instanceof RuntimeException) throw (RuntimeException) error;
            throw new RuntimeException(error);
        }
    }
}
