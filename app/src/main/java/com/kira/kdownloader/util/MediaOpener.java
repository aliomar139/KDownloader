package com.kira.kdownloader.util;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.Nullable;

/**
 * Opens and shares saved media, reporting why a file could not be handed to another app
 * instead of showing an empty chooser.
 */
public final class MediaOpener {
    private static final String TAG = "MediaOpener";

    public enum Result {
        /** Another app was launched. */
        LAUNCHED,
        /** The row is gone from MediaStore, or its file was deleted outside the app. */
        MISSING,
        /** The file is there, but nothing installed can handle it. */
        NO_APP,
        /** Launching failed for another reason. */
        FAILED
    }

    private MediaOpener() {}


    public static Result open(Context context, Uri uri, boolean isAudio) {
        return launch(context, uri, isAudio, true);
    }

    public static Result share(Context context, Uri uri, boolean isAudio) {
        return launch(context, uri, isAudio, false);
    }

    private static Result launch(Context context, Uri uri, boolean isAudio, boolean view) {
        Uri readable = MediaUris.readable(context, uri, isAudio);
        if (readable == null) return Result.MISSING;
        String mimeType = mimeTypeOf(context, readable, isAudio);
        Intent intent = view
                ? new Intent(Intent.ACTION_VIEW).setDataAndType(readable, mimeType)
                : new Intent(Intent.ACTION_SEND).setType(mimeType).putExtra(Intent.EXTRA_STREAM, readable);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (context.getPackageManager().queryIntentActivities(intent, 0).isEmpty()) return Result.NO_APP;

        Intent chooser = Intent.createChooser(intent, view ? "Open with" : "Share");
        if (!(context instanceof Activity)) chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(chooser);
            return Result.LAUNCHED;
        } catch (ActivityNotFoundException error) {
            Log.w(TAG, "No activity accepted " + mimeType, error);
            return Result.NO_APP;
        } catch (Throwable error) {
            Log.w(TAG, "Could not launch " + readable, error);
            return Result.FAILED;
        }
    }

    /** The best concrete type for a saved file, falling back to its stored display name. */
    public static String mimeTypeOf(Context context, Uri uri, boolean isAudio) {
        return mimeTypeOf(context, uri, isAudio, null);
    }

    public static String mimeTypeOf(Context context, Uri uri, boolean isAudio, @Nullable String nameHint) {
        String providerType = null;
        try { providerType = context.getContentResolver().getType(uri); }
        catch (Throwable ignored) { }
        if (MediaMimeTypes.isPlayable(providerType)) {
            return MediaMimeTypes.forPlayback(providerType, null, isAudio);
        }
        String name = nameHint != null ? nameHint : MediaMimeTypes.displayName(context, uri);
        if (name == null) name = uri.getLastPathSegment();
        return MediaMimeTypes.forPlayback(providerType, name, isAudio);
    }
}
