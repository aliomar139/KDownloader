package com.kira.kdownloader.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;

/**
 * Fills an image view with a media thumbnail, fetching each one at most once.
 *
 * <p>Whatever a thumbnail ends up coming from — a remote URL or a frame of the file itself — a
 * downscaled copy is kept under a stable cache key, so later binds are served from memory or disk.
 * Nothing is re-fetched when the list is scrolled, the screen reopened, or the app restarted, and
 * thumbnails keep working after a remote URL expires.
 */
public final class ThumbnailBinder {
    private ThumbnailBinder() {}

    /**
     * @param tag       identifies the row this view is bound to, so a late result never lands on a
     *                  recycled view
     * @param cacheKey  stable across restarts; must not change when the remote URL does
     * @param remoteUrl poster URL to try first, if the source provided one
     * @param fileUri   saved media to extract a frame from when there is no usable remote image
     */
    public static void bind(ImageView view, Object tag, String cacheKey, @Nullable String remoteUrl,
                            @Nullable String fileUri, boolean audio, @DrawableRes int fallback, int sizePx) {
        view.setTag(tag);
        Bitmap memory = MediaThumbnails.peek(cacheKey, sizePx);
        if (memory != null) {
            Glide.with(view).clear(view);
            view.setImageBitmap(memory);
            return;
        }
        // A stale request from the recycled row would otherwise land on this one.
        Glide.with(view).clear(view);
        view.setImageResource(fallback);

        Context context = view.getContext().getApplicationContext();
        AppExecutors.io().execute(() -> {
            Bitmap stored = MediaThumbnails.cached(context, cacheKey, sizePx);
            AppExecutors.main().execute(() -> {
                if (!matches(view, tag)) return;
                if (stored != null) {
                    view.setImageBitmap(stored);
                } else if (remoteUrl != null && !remoteUrl.trim().isEmpty()) {
                    fetchRemote(view, tag, cacheKey, remoteUrl, fileUri, audio, fallback, sizePx);
                } else {
                    extractLocal(view, tag, cacheKey, fileUri, audio, sizePx);
                }
            });
        });
    }

    private static void fetchRemote(ImageView view, Object tag, String cacheKey, String remoteUrl,
                                    @Nullable String fileUri, boolean audio, @DrawableRes int fallback, int sizePx) {
        Context context = view.getContext().getApplicationContext();
        RequestBuilder<Drawable> request = Glide.with(view).load(remoteUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL);
        if (!MotionPreferences.reduce(view.getContext())) {
            request = request.transition(DrawableTransitionOptions.withCrossFade(200));
        }
        request.placeholder(fallback).error(fallback).listener(new RequestListener<Drawable>() {
            @Override public boolean onLoadFailed(@Nullable GlideException error, @Nullable Object model,
                                                  @NonNull Target<Drawable> target, boolean first) {
                // Poster URLs expire; the file itself still has a frame to show.
                view.post(() -> {
                    if (matches(view, tag)) extractLocal(view, tag, cacheKey, fileUri, audio, sizePx);
                });
                return false;
            }

            @Override public boolean onResourceReady(@NonNull Drawable resource, @NonNull Object model,
                                                     Target<Drawable> target, @NonNull DataSource source,
                                                     boolean first) {
                if (resource instanceof BitmapDrawable) {
                    Bitmap bitmap = ((BitmapDrawable) resource).getBitmap();
                    AppExecutors.io().execute(() -> MediaThumbnails.store(context, cacheKey, sizePx, bitmap));
                }
                return false;
            }
        }).into(view);
    }

    private static void extractLocal(ImageView view, Object tag, String cacheKey,
                                     @Nullable String fileUri, boolean audio, int sizePx) {
        if (fileUri == null || fileUri.trim().isEmpty()) return;
        Context context = view.getContext().getApplicationContext();
        AppExecutors.io().execute(() -> {
            Bitmap bitmap = MediaThumbnails.load(context, cacheKey, fileUri, audio, sizePx);
            if (bitmap == null) return;
            AppExecutors.main().execute(() -> {
                if (matches(view, tag)) view.setImageBitmap(bitmap);
            });
        });
    }

    private static boolean matches(ImageView view, Object tag) {
        return tag.equals(view.getTag());
    }
}
