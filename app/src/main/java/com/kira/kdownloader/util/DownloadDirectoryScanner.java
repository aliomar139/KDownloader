package com.kira.kdownloader.util;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.WorkerThread;
import androidx.annotation.RequiresApi;

import com.kira.kdownloader.data.DownloadDao;
import com.kira.kdownloader.data.DownloadEntity;
import com.kira.kdownloader.data.DownloadStatus;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class DownloadDirectoryScanner {
    private static final String TAG = "DownloadDirectoryScanner";
    private static final String NORMALIZED_DOWNLOAD_ROOT = "download";
    private static final Set<String> DIRECTORY_NAMES = new HashSet<>(Arrays.asList("kdownloader", "kdownloads"));
    private static final List<String> DIRECTORY_QUERY_NAMES = Arrays.asList("KDownloader", "KDownloads");
    private static final Set<String> AUDIO_EXTENSIONS = new HashSet<>(Arrays.asList("aac", "flac", "m4a", "mp3", "ogg", "opus", "wav"));
    private static final Set<String> VIDEO_EXTENSIONS = new HashSet<>(Arrays.asList("avi", "m4v", "mkv", "mov", "mp4", "webm"));
    private static final long MIN_RESCAN_INTERVAL_MS = 30_000L;
    private static final Object SYNC_LOCK = new Object();
    private static volatile long lastScanAtMs;

    private DownloadDirectoryScanner() {}

    @WorkerThread
    public static int syncIntoHistory(Context context, DownloadDao dao) {
        return syncIntoHistory(context, dao, false);
    }

    @WorkerThread
    public static int syncIntoHistory(Context context, DownloadDao dao, boolean force) {
        synchronized (SYNC_LOCK) {
            long now = System.currentTimeMillis();
            if (!force && lastScanAtMs != 0 && now - lastScanAtMs < MIN_RESCAN_INTERVAL_MS) return 0;

            Set<String> existingUris = new HashSet<>(dao.getAllFileUris());
            Set<Long> existingIds = new HashSet<>();
            for (String uri : existingUris) {
                Long id = mediaStoreIdFrom(uri);
                if (id != null) existingIds.add(id);
            }

            List<ScannedMedia> discovered;
            try {
                discovered = scan(context.getApplicationContext());
            } catch (Throwable error) {
                Log.w(TAG, "Could not scan the KDownloads directories", error);
                discovered = new ArrayList<>();
            }

            int inserted = 0;
            for (ScannedMedia media : discovered) {
                String uri = media.entity.getFileUri();
                if (existingIds.contains(media.mediaStoreId) || existingUris.contains(uri)) continue;
                dao.insert(media.entity);
                existingIds.add(media.mediaStoreId);
                if (uri != null) existingUris.add(uri);
                inserted++;
            }
            lastScanAtMs = System.currentTimeMillis();
            return inserted;
        }
    }

    private static List<ScannedMedia> scan(Context context) {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q ? scanScopedStorage(context) : scanLegacyStorage(context);
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private static List<ScannedMedia> scanScopedStorage(Context context) {
        Set<Long> seenIds = new HashSet<>();
        List<ScannedMedia> result = new ArrayList<>();
        List<Uri> collections = Arrays.asList(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MediaStore.Downloads.EXTERNAL_CONTENT_URI);
        for (Uri collection : collections) {
            try {
                for (ScannedMedia media : queryScopedCollection(context, collection)) {
                    if (seenIds.add(media.mediaStoreId)) result.add(media);
                }
            } catch (Throwable error) {
                Log.w(TAG, "Could not query " + collection + " for download history", error);
            }
        }
        return result;
    }

    private static List<ScannedMedia> queryScopedCollection(Context context, Uri collection) {
        String[] projection = {
                MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.RELATIVE_PATH};
        String path = MediaStore.MediaColumns.RELATIVE_PATH;
        String selection = MediaStore.MediaColumns.IS_PENDING + " = 0 AND (" + path + " LIKE ? OR " + path + " LIKE ?)";
        String[] args = {
                Environment.DIRECTORY_DOWNLOADS + "/KDownloader%",
                Environment.DIRECTORY_DOWNLOADS + "/KDownloads%"};
        List<ScannedMedia> result = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(
                collection, projection, selection, args, MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (cursor == null) return result;
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
            int mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.RELATIVE_PATH);
            while (cursor.moveToNext()) {
                if (!isSupportedRelativePath(cursor.getString(pathColumn))) continue;
                ScannedMedia media = scannedMedia(collection, cursor.getLong(idColumn), cursor.getString(nameColumn),
                        cursor.getString(mimeColumn), cursor.getLong(dateColumn));
                if (media != null) result.add(media);
            }
        }
        return result;
    }

    @SuppressWarnings("deprecation")
    private static List<ScannedMedia> scanLegacyStorage(Context context) {
        Uri collection = MediaStore.Files.getContentUri("external");
        String[] projection = {
                MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME,
                MediaStore.MediaColumns.MIME_TYPE, MediaStore.MediaColumns.DATE_ADDED,
                MediaStore.MediaColumns.DATA};
        File downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        String data = MediaStore.MediaColumns.DATA;
        String selection = data + " LIKE ? OR " + data + " LIKE ?";
        String[] args = {
                new File(downloads, "KDownloader").getAbsolutePath() + File.separator + "%",
                new File(downloads, "KDownloads").getAbsolutePath() + File.separator + "%"};
        List<ScannedMedia> result = new ArrayList<>();
        try (Cursor cursor = context.getContentResolver().query(
                collection, projection, selection, args, MediaStore.MediaColumns.DATE_ADDED + " DESC")) {
            if (cursor == null) return result;
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
            int mimeColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
            int dateColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_ADDED);
            int pathColumn = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            while (cursor.moveToNext()) {
                if (!isSupportedLegacyPath(cursor.getString(pathColumn))) continue;
                ScannedMedia media = scannedMedia(collection, cursor.getLong(idColumn), cursor.getString(nameColumn),
                        cursor.getString(mimeColumn), cursor.getLong(dateColumn));
                if (media != null) result.add(media);
            }
        }
        return result;
    }

    private static ScannedMedia scannedMedia(Uri collection, long id, String name, String mimeType, long dateAddedSeconds) {
        if (name == null) return null;
        long createdAt = dateAddedSeconds > 0 ? dateAddedSeconds * 1000L : System.currentTimeMillis();
        DownloadEntity entity = entityFrom(name, mimeType, ContentUris.withAppendedId(collection, id).toString(), createdAt);
        return entity == null ? null : new ScannedMedia(id, entity);
    }

    static boolean isSupportedRelativePath(String relativePath) {
        if (relativePath == null) return false;
        String normalized = trimSlashes(relativePath.replace('\\', '/').toLowerCase(Locale.ROOT));
        String[] segments = normalized.split("/");
        return segments.length >= 2 && NORMALIZED_DOWNLOAD_ROOT.equals(segments[0]) && DIRECTORY_NAMES.contains(segments[1]);
    }

    static boolean isSupportedLegacyPath(String absolutePath) {
        if (absolutePath == null) return false;
        String normalized = absolutePath.replace('\\', '/').toLowerCase(Locale.ROOT);
        String marker = "/" + NORMALIZED_DOWNLOAD_ROOT + "/";
        int markerIndex = normalized.indexOf(marker);
        if (markerIndex < 0) return false;
        String rest = normalized.substring(markerIndex + marker.length());
        int slash = rest.indexOf('/');
        return DIRECTORY_NAMES.contains(slash < 0 ? rest : rest.substring(0, slash));
    }

    static DownloadEntity entityFrom(String displayName, String mimeType, String fileUri, long createdAt) {
        String extension = extension(displayName).toLowerCase(Locale.ROOT);
        String kind;
        if ((mimeType != null && mimeType.startsWith("audio/")) || AUDIO_EXTENSIONS.contains(extension)) kind = "AUDIO";
        else if ((mimeType != null && mimeType.startsWith("video/")) || VIDEO_EXTENSIONS.contains(extension)) kind = "VIDEO";
        else return null;

        int dot = displayName.lastIndexOf('.');
        String title = dot < 0 ? displayName : displayName.substring(0, dot);
        if (title.trim().isEmpty()) title = displayName;
        String label = extension.toUpperCase(Locale.ROOT);
        if (label.trim().isEmpty()) label = "AUDIO".equals(kind) ? "Audio" : "Video";
        return new DownloadEntity(0L, title, "", kind, label, fileUri, null, createdAt, DownloadStatus.COMPLETED);
    }

    private static Long mediaStoreIdFrom(String uriString) {
        try { return ContentUris.parseId(Uri.parse(uriString)); }
        catch (Throwable ignored) { return null; }
    }

    private static String extension(String name) {
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1);
    }

    private static String trimSlashes(String value) {
        int start = 0;
        int end = value.length();
        while (start < end && value.charAt(start) == '/') start++;
        while (end > start && value.charAt(end - 1) == '/') end--;
        return value.substring(start, end);
    }

    private static final class ScannedMedia {
        final long mediaStoreId;
        final DownloadEntity entity;
        ScannedMedia(long mediaStoreId, DownloadEntity entity) {
            this.mediaStoreId = mediaStoreId;
            this.entity = entity;
        }
    }
}
