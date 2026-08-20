package com.kira.kdownloader.util;

import android.content.ContentUris;
import android.content.Context;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps saved-file URIs usable for the lifetime of the file rather than the lifetime of the
 * install.
 *
 * <p>Files are published into {@code MediaStore.Downloads}, and MediaProvider only lets an app
 * read <em>its own</em> rows in that collection: reading another app's download needs the storage
 * access framework. Ownership is dropped when the app is uninstalled, so after a reinstall every
 * previously downloaded file becomes unreadable through the URI history stored for it, even though
 * the file is still on disk. The same row addressed through the audio or video collection is
 * readable with the media permissions the app already holds, so that is the form worth keeping.
 */
public final class MediaUris {
    private static final String TAG = "MediaUris";
    private static final String MEDIA_AUTHORITY = "media";

    private MediaUris() {}

    /** The same row addressed through the audio or video collection, when the URI names one. */
    public static Uri inMediaCollection(Uri uri, boolean audio) {
        if (uri == null || !MEDIA_AUTHORITY.equals(uri.getAuthority())) return uri;
        List<String> segments = uri.getPathSegments();
        if (segments.size() < 2) return uri;
        long id;
        try {
            id = ContentUris.parseId(uri);
        } catch (Throwable ignored) {
            return uri;
        }
        if (id < 0) return uri;
        String volume = segments.get(0);
        try {
            Uri collection = audio
                    ? MediaStore.Audio.Media.getContentUri(volume)
                    : MediaStore.Video.Media.getContentUri(volume);
            return ContentUris.withAppendedId(collection, id);
        } catch (Throwable ignored) {
            return uri;
        }
    }

    /** Every address worth trying for a saved file, most likely to work first. */
    public static List<Uri> candidates(Uri uri, boolean audio) {
        List<Uri> result = new ArrayList<>(3);
        add(result, inMediaCollection(uri, audio));
        add(result, inMediaCollection(uri, !audio));
        add(result, uri);
        return result;
    }

    /**
     * The first address the file can actually be read through, or {@code null} when the file is
     * genuinely gone.
     */
    @Nullable
    public static Uri readable(Context context, Uri uri, boolean audio) {
        if (uri == null) return null;
        for (Uri candidate : candidates(uri, audio)) {
            if (canRead(context, candidate)) return candidate;
        }
        Log.d(TAG, "No readable address for " + uri);
        return null;
    }

    private static boolean canRead(Context context, Uri uri) {
        try (ParcelFileDescriptor descriptor = context.getContentResolver().openFileDescriptor(uri, "r")) {
            return descriptor != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void add(List<Uri> uris, Uri uri) {
        if (uri != null && !uris.contains(uri)) uris.add(uri);
    }
}
