package com.kira.kdownloader.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.util.LruCache;
import android.util.Size;

import androidx.annotation.WorkerThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class MediaThumbnails {
    private static final String DISK_DIR = "media_thumbs";
    private static final int JPEG_QUALITY = 85;
    private static final Object DISK_LOCK = new Object();
    private static final LruCache<String, Bitmap> MEMORY_CACHE = new LruCache<String, Bitmap>(
            Math.max((int) (Runtime.getRuntime().maxMemory() / 1024 / 8), 4 * 1024)) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount() / 1024; }
    };

    private MediaThumbnails() {}

    public static Bitmap peek(String uriString) { return peek(uriString, 256); }
    public static Bitmap peek(String uriString, int sizePx) { return MEMORY_CACHE.get(keyFor(uriString, sizePx)); }

    @WorkerThread
    public static Bitmap load(Context context, String uriString, boolean isAudio) {
        return load(context, uriString, isAudio, 256);
    }

    @WorkerThread
    public static Bitmap load(Context context, String uriString, boolean isAudio, int sizePx) {
        String key = keyFor(uriString, sizePx);
        Bitmap cached = MEMORY_CACHE.get(key);
        if (cached != null) return cached;
        Bitmap bitmap = readFromDisk(context, key);
        if (bitmap == null) {
            try {
                Uri uri = Uri.parse(uriString);
                bitmap = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                        ? context.getContentResolver().loadThumbnail(uri, new Size(sizePx, sizePx), null)
                        : legacyThumbnail(context, uri, isAudio);
            } catch (Throwable ignored) {
                bitmap = null;
            }
            if (bitmap != null) writeToDisk(context, key, bitmap);
        }
        if (bitmap != null) MEMORY_CACHE.put(key, bitmap);
        return bitmap;
    }

    private static String keyFor(String uriString, int sizePx) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(
                    (uriString + "@" + sizePx).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte value : digest) result.append(String.format("%02x", value & 0xff));
            return result.toString();
        } catch (Exception error) {
            throw new IllegalStateException(error);
        }
    }

    private static File cacheFile(Context context, String key) {
        File directory = new File(context.getCacheDir(), DISK_DIR);
        directory.mkdirs();
        return new File(directory, key + ".jpg");
    }

    private static Bitmap readFromDisk(Context context, String key) {
        File file = cacheFile(context, key);
        if (!file.exists()) return null;
        try { return BitmapFactory.decodeFile(file.getAbsolutePath()); }
        catch (Throwable ignored) { return null; }
    }

    private static void writeToDisk(Context context, String key, Bitmap bitmap) {
        synchronized (DISK_LOCK) {
            try (OutputStream output = new FileOutputStream(cacheFile(context, key))) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output);
            } catch (Throwable ignored) {}
        }
    }

    private static Bitmap legacyThumbnail(Context context, Uri uri, boolean isAudio) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        try {
            retriever.setDataSource(context, uri);
            if (!isAudio) return retriever.getFrameAtTime(0);
            byte[] picture = retriever.getEmbeddedPicture();
            return picture == null ? null : BitmapFactory.decodeByteArray(picture, 0, picture.length);
        } catch (Throwable ignored) {
            return null;
        } finally {
            try { retriever.release(); } catch (Throwable ignored) {}
        }
    }
}
