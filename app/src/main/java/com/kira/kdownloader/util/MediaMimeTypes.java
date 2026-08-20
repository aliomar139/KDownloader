package com.kira.kdownloader.util;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;

import androidx.annotation.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves concrete MIME types for the containers the downloader produces.
 *
 * <p>{@link MimeTypeMap} does not know every container on every device (opus, flac and matroska
 * are missing on several OEM builds), and a wildcard type such as {@code video/*} is worse than
 * useless: MediaStore refuses to classify the row as media, so no thumbnail is ever generated,
 * and {@code ACTION_VIEW} with a wildcard type matches almost no player.
 */
public final class MediaMimeTypes {
    private static final String FALLBACK = "application/octet-stream";
    private static final Map<String, String> BY_EXTENSION;

    static {
        Map<String, String> types = new HashMap<>();
        types.put("aac", "audio/aac");
        types.put("flac", "audio/flac");
        types.put("m4a", "audio/mp4");
        types.put("m4b", "audio/mp4");
        types.put("mp3", "audio/mpeg");
        types.put("oga", "audio/ogg");
        types.put("ogg", "audio/ogg");
        types.put("opus", "audio/ogg");
        types.put("wav", "audio/x-wav");
        types.put("weba", "audio/webm");
        types.put("3gp", "video/3gpp");
        types.put("avi", "video/x-msvideo");
        types.put("flv", "video/x-flv");
        types.put("m4v", "video/x-m4v");
        types.put("mkv", "video/x-matroska");
        types.put("mov", "video/quicktime");
        types.put("mp4", "video/mp4");
        types.put("mpg", "video/mpeg");
        types.put("ts", "video/mp2t");
        types.put("webm", "video/webm");
        BY_EXTENSION = Collections.unmodifiableMap(types);
    }

    private MediaMimeTypes() {}

    /** True when the type names one concrete format rather than a wildcard or opaque blob. */
    public static boolean isPlayable(@Nullable String mimeType) {
        if (mimeType == null) return false;
        String value = mimeType.trim().toLowerCase(Locale.ROOT);
        return !value.contains("*") && (value.startsWith("audio/") || value.startsWith("video/"));
    }

    /** The type to store in MediaStore for a file. Never a wildcard: MediaStore rejects those. */
    public static String forStorage(String fileName) {
        String fromName = fromFileName(fileName);
        return fromName != null ? fromName : FALLBACK;
    }

    /**
     * The type to hand to {@code ACTION_VIEW} / {@code ACTION_SEND}. Prefers what the provider
     * reports, falls back to the file extension, and only then to a wildcard.
     */
    public static String forPlayback(@Nullable String providerType, @Nullable String fileName, boolean isAudio) {
        if (isPlayable(providerType)) return providerType.trim().toLowerCase(Locale.ROOT);
        String fromName = fromFileName(fileName);
        if (isPlayable(fromName)) return fromName;
        return isAudio ? "audio/*" : "video/*";
    }

    @Nullable
    public static String fromFileName(@Nullable String fileName) {
        String extension = extension(fileName);
        if (extension.isEmpty()) return null;
        String known = BY_EXTENSION.get(extension);
        if (known != null) return known;
        String guessed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        return isPlayable(guessed) ? guessed.toLowerCase(Locale.ROOT) : null;
    }

    /** The display name MediaStore holds for a row, or {@code null} when the row is gone. */
    @Nullable
    public static String displayName(Context context, Uri uri) {
        try (Cursor cursor = context.getContentResolver().query(
                uri, new String[]{MediaStore.MediaColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) return cursor.getString(0);
        } catch (Throwable ignored) {
        }
        return null;
    }

    private static String extension(@Nullable String fileName) {
        if (fileName == null) return "";
        String name = fileName.trim();
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
