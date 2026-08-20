package com.kira.kdownloader.util;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public final class MediaStoreWriter {
    private MediaStoreWriter() {}

    public static Uri publish(Context context, File sourceFile, boolean isAudio) throws IOException {
        if (!sourceFile.isFile()) throw new IllegalArgumentException("Output file does not exist: " + sourceFile.getName());
        ContentResolver resolver = context.getContentResolver();
        String extension = extension(sourceFile.getName()).toLowerCase(Locale.ROOT);
        String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (mimeType == null) mimeType = isAudio ? "audio/*" : "video/*";
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
            return uri;
        } catch (Throwable error) {
            resolver.delete(uri, null, null);
            if (error instanceof IOException) throw (IOException) error;
            if (error instanceof RuntimeException) throw (RuntimeException) error;
            throw new RuntimeException(error);
        }
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }
}
