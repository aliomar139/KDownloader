package com.kira.kdownloader.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.LruCache;
import android.util.Size;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class MediaThumbnails {
    private static final String DISK_DIR = "media_thumbs";
    private static final int JPEG_QUALITY = 85;
    private static final long FAILURE_RETRY_MS = 5 * 60_000L;
    private static final Object DISK_LOCK = new Object();
    private static final LruCache<String, Bitmap> MEMORY_CACHE = new LruCache<String, Bitmap>(
            Math.max((int) (Runtime.getRuntime().maxMemory() / 1024 / 8), 4 * 1024)) {
        @Override protected int sizeOf(String key, Bitmap value) { return value.getByteCount() / 1024; }
    };
    /** Keys that could not be decoded, so scrolling does not re-run the extractor on every bind. */
    private static final LruCache<String, Long> FAILURES = new LruCache<>(512);

    private MediaThumbnails() {}

    /** The in-memory copy for a cache key, if one is already loaded. Safe on the main thread. */
    public static Bitmap peek(String cacheKey, int sizePx) { return MEMORY_CACHE.get(keyFor(cacheKey, sizePx)); }

    /** The memory or on-disk copy for a cache key. Never touches the network or the media file. */
    @WorkerThread
    public static Bitmap cached(Context context, String cacheKey, int sizePx) {
        String key = keyFor(cacheKey, sizePx);
        Bitmap memory = MEMORY_CACHE.get(key);
        if (memory != null) return memory;
        Bitmap bitmap = readFromDisk(context, key, sizePx);
        if (bitmap != null) MEMORY_CACHE.put(key, bitmap);
        return bitmap;
    }

    /** Keeps a downscaled copy of an already-loaded image so it is never fetched again. */
    @WorkerThread
    public static void store(Context context, String cacheKey, int sizePx, Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) return;
        String key = keyFor(cacheKey, sizePx);
        Bitmap copy = scale(copyOf(bitmap), sizePx);
        if (copy == null) return;
        writeToDisk(context, key, copy);
        MEMORY_CACHE.put(key, copy);
        FAILURES.remove(key);
    }

    @WorkerThread
    public static Bitmap load(Context context, String cacheKey, String uriString, boolean isAudio, int sizePx) {
        String key = keyFor(cacheKey, sizePx);
        Bitmap cached = MEMORY_CACHE.get(key);
        if (cached != null) return cached;
        Bitmap bitmap = readFromDisk(context, key, sizePx);
        if (bitmap == null) {
            if (recentlyFailed(key)) return null;
            Uri uri = uriOf(uriString);
            if (uri == null) return null;
            // The stored address can be one the app may no longer read; find one that works.
            Uri readable = MediaUris.readable(context, uri, isAudio);
            if (readable == null) {
                FAILURES.put(key, System.currentTimeMillis());
                return null;
            }
            bitmap = scale(extract(context, readable, isAudio, sizePx), sizePx);
            if (bitmap == null) {
                FAILURES.put(key, System.currentTimeMillis());
                return null;
            }
            writeToDisk(context, key, bitmap);
        }
        FAILURES.remove(key);
        MEMORY_CACHE.put(key, bitmap);
        return bitmap;
    }

    /**
     * MediaStore only serves thumbnails for rows it classified as media, and only for some
     * containers, so every provider failure falls through to decoding the file ourselves.
     */
    @Nullable
    private static Bitmap extract(Context context, Uri uri, boolean isAudio, int sizePx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                Bitmap thumbnail = context.getContentResolver()
                        .loadThumbnail(uri, new Size(sizePx, sizePx), null);
                if (thumbnail != null) return thumbnail;
            } catch (Throwable ignored) {
            }
        }
        return retrieverThumbnail(context, uri, isAudio, sizePx);
    }

    @Nullable
    private static Bitmap copyOf(Bitmap bitmap) {
        try {
            Bitmap.Config config = bitmap.getConfig() == null ? Bitmap.Config.ARGB_8888 : bitmap.getConfig();
            return bitmap.copy(config, false);
        } catch (Throwable ignored) {
            return null;
        }
    }

    @Nullable
    private static Uri uriOf(String uriString) {
        if (uriString == null || uriString.trim().isEmpty()) return null;
        try { return Uri.parse(uriString); }
        catch (Throwable ignored) { return null; }
    }

    private static boolean recentlyFailed(String key) {
        Long failedAt = FAILURES.get(key);
        if (failedAt == null) return false;
        if (System.currentTimeMillis() - failedAt < FAILURE_RETRY_MS) return true;
        FAILURES.remove(key);
        return false;
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

    private static Bitmap readFromDisk(Context context, String key, int sizePx) {
        File file = cacheFile(context, key);
        if (!file.exists()) return null;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            options.inJustDecodeBounds = false;
            options.inSampleSize = sampleSize(options.outWidth, options.outHeight, sizePx);
            Bitmap bitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
            if (bitmap == null) file.delete();
            return bitmap;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void writeToDisk(Context context, String key, Bitmap bitmap) {
        synchronized (DISK_LOCK) {
            try (OutputStream output = new FileOutputStream(cacheFile(context, key))) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output);
            } catch (Throwable ignored) {}
        }
    }

    private static int sampleSize(int width, int height, int sizePx) {
        int sample = 1;
        while (sizePx > 0 && width / (sample * 2) >= sizePx && height / (sample * 2) >= sizePx) sample *= 2;
        return sample;
    }

    @Nullable
    private static Bitmap scale(@Nullable Bitmap bitmap, int sizePx) {
        if (bitmap == null || sizePx <= 0) return bitmap;
        int longest = Math.max(bitmap.getWidth(), bitmap.getHeight());
        if (longest <= sizePx * 2) return bitmap;
        float ratio = (float) sizePx / longest;
        int width = Math.max(1, Math.round(bitmap.getWidth() * ratio));
        int height = Math.max(1, Math.round(bitmap.getHeight() * ratio));
        try {
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, width, height, true);
            if (scaled != bitmap) bitmap.recycle();
            return scaled;
        } catch (Throwable ignored) {
            return bitmap;
        }
    }

    @Nullable
    private static Bitmap retrieverThumbnail(Context context, Uri uri, boolean isAudio, int sizePx) {
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        ParcelFileDescriptor descriptor = null;
        try {
            try {
                retriever.setDataSource(context, uri);
            } catch (Throwable contentSourceFailed) {
                descriptor = context.getContentResolver().openFileDescriptor(uri, "r");
                if (descriptor == null) return null;
                retriever.setDataSource(descriptor.getFileDescriptor());
            }
            Bitmap embedded = embeddedPicture(retriever);
            if (isAudio) return embedded;
            Bitmap frame = firstFrame(retriever, sizePx);
            return frame != null ? frame : embedded;
        } catch (Throwable ignored) {
            return null;
        } finally {
            try { retriever.release(); } catch (Throwable ignored) {}
            if (descriptor != null) {
                try { descriptor.close(); } catch (Throwable ignored) {}
            }
        }
    }

    @Nullable
    private static Bitmap embeddedPicture(MediaMetadataRetriever retriever) {
        try {
            byte[] picture = retriever.getEmbeddedPicture();
            return picture == null ? null : BitmapFactory.decodeByteArray(picture, 0, picture.length);
        } catch (Throwable ignored) {
            return null;
        }
    }

    /** Some containers have no frame at position zero, so fall back to any representative frame. */
    @Nullable
    private static Bitmap firstFrame(MediaMetadataRetriever retriever, int sizePx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            try {
                Bitmap scaled = retriever.getScaledFrameAtTime(
                        -1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC, sizePx, sizePx);
                if (scaled != null) return scaled;
            } catch (Throwable ignored) {
            }
        }
        try {
            Bitmap frame = retriever.getFrameAtTime(-1, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            if (frame != null) return frame;
        } catch (Throwable ignored) {
        }
        try { return retriever.getFrameAtTime(); }
        catch (Throwable ignored) { return null; }
    }
}
