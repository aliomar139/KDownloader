package com.kira.kdownloader.ui;

import android.graphics.Bitmap;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.android.material.color.MaterialColors;
import com.kira.kdownloader.R;
import com.kira.kdownloader.data.DownloadEntity;
import com.kira.kdownloader.data.DownloadStatus;
import com.kira.kdownloader.util.AppExecutors;
import com.kira.kdownloader.util.MediaThumbnails;
import com.kira.kdownloader.util.MotionPreferences;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

final class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    interface Listener {
        void onOpen(DownloadEntity download);
        void onDetails(DownloadEntity download);
    }

    private static final int HEADER = 0;
    private static final int DOWNLOAD = 1;
    private final Listener listener;
    private final DateFormat dateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    private final List<Object> items = new ArrayList<>();

    HistoryAdapter(Listener listener) { this.listener = listener; }

    void submit(List<DownloadEntity> downloads) {
        items.clear();
        String lastBucket = null;
        for (DownloadEntity download : downloads) {
            String bucket = dateBucket(download.getCreatedAt());
            if (!bucket.equals(lastBucket)) {
                items.add(bucket);
                lastBucket = bucket;
            }
            items.add(download);
        }
        notifyDataSetChanged();
    }

    DownloadEntity downloadAt(int position) {
        if (position < 0 || position >= items.size()) return null;
        Object item = items.get(position);
        return item instanceof DownloadEntity ? (DownloadEntity) item : null;
    }

    @Override public int getItemViewType(int position) {
        return items.get(position) instanceof String ? HEADER : DOWNLOAD;
    }

    @NonNull @Override public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (type == HEADER) return new HeaderHolder(inflater.inflate(R.layout.item_history_header, parent, false));
        return new DownloadHolder(inflater.inflate(R.layout.item_history, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull RecyclerView.ViewHolder raw, int position) {
        Object item = items.get(position);
        if (raw instanceof HeaderHolder) {
            ((HeaderHolder) raw).text.setText((String) item);
            return;
        }
        DownloadEntity download = (DownloadEntity) item;
        DownloadHolder holder = (DownloadHolder) raw;
        holder.title.setText(download.getTitle());
        holder.meta.setText(download.getFormatLabel() + " · "
                + dateFormat.format(new Date(download.getCreatedAt())));
        bindStatus(holder.status, download.getStatus());
        bindThumbnail(holder.thumbnail, download);
        holder.itemView.setOnClickListener(view -> {
            if (download.getStatus() == DownloadStatus.COMPLETED && download.getFileUri() != null) {
                listener.onOpen(download);
            }
        });
        holder.itemView.setOnLongClickListener(view -> {
            listener.onDetails(download);
            return true;
        });
        holder.actions.setOnClickListener(view -> listener.onDetails(download));
    }

    @Override public int getItemCount() { return items.size(); }

    private static void bindStatus(TextView view, DownloadStatus status) {
        String label;
        int backgroundAttr;
        int contentAttr;
        if (status == DownloadStatus.COMPLETED) {
            label = "Completed";
            backgroundAttr = R.attr.statusSuccessContainer;
            contentAttr = R.attr.colorOnStatusSuccessContainer;
        } else if (status == DownloadStatus.RUNNING) {
            label = "Running";
            backgroundAttr = com.google.android.material.R.attr.colorPrimaryContainer;
            contentAttr = com.google.android.material.R.attr.colorOnPrimaryContainer;
        } else {
            label = "Failed";
            backgroundAttr = com.google.android.material.R.attr.colorErrorContainer;
            contentAttr = com.google.android.material.R.attr.colorOnErrorContainer;
        }
        view.setText(label);
        view.setTextColor(MaterialColors.getColor(view, contentAttr));
        Drawable background = view.getBackground().mutate();
        background.setTint(MaterialColors.getColor(view, backgroundAttr));
        view.setBackground(background);
        int icon = status == DownloadStatus.COMPLETED ? R.drawable.ic_check_circle
                : status == DownloadStatus.RUNNING ? R.drawable.ic_download : R.drawable.ic_error_outline;
        Drawable statusIcon = view.getContext().getDrawable(icon).mutate();
        int size = Math.round(12 * view.getResources().getDisplayMetrics().density);
        statusIcon.setBounds(0, 0, size, size);
        view.setCompoundDrawableTintList(ColorStateList.valueOf(MaterialColors.getColor(view, contentAttr)));
        view.setCompoundDrawablePadding(Math.round(4 * view.getResources().getDisplayMetrics().density));
        view.setCompoundDrawables(statusIcon, null, null, null);
    }

    private static void bindThumbnail(ImageView view, DownloadEntity download) {
        boolean audio = "AUDIO".equalsIgnoreCase(download.getKind());
        int fallback = audio ? R.drawable.placeholder_audio : R.drawable.placeholder_video;
        view.setTag(download.getId());
        view.setImageResource(fallback);
        if (download.getThumbnailUrl() != null) {
            RequestBuilder<Drawable> request = Glide.with(view).load(download.getThumbnailUrl());
            if (!MotionPreferences.reduce(view.getContext())) {
                request = request.transition(DrawableTransitionOptions.withCrossFade(200));
            }
            request.placeholder(fallback).error(fallback).into(view);
        } else if (download.getFileUri() != null) {
            Bitmap cached = MediaThumbnails.peek(download.getFileUri(), 128);
            if (cached != null) view.setImageBitmap(cached);
            else AppExecutors.io().execute(() -> {
                Bitmap bitmap = MediaThumbnails.load(view.getContext(), download.getFileUri(), audio, 128);
                AppExecutors.main().execute(() -> {
                    if (Long.valueOf(download.getId()).equals(view.getTag()) && bitmap != null) view.setImageBitmap(bitmap);
                });
            });
        }
    }

    private static String dateBucket(long createdAt) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long today = calendar.getTimeInMillis();
        long day = 86_400_000L;
        if (createdAt >= today) return "Today";
        if (createdAt >= today - day) return "Yesterday";
        if (createdAt >= today - 7L * day) return "Earlier this week";
        return "Earlier";
    }

    private static final class HeaderHolder extends RecyclerView.ViewHolder {
        private final TextView text;
        HeaderHolder(View view) { super(view); text = view.findViewById(R.id.history_header); }
    }

    private static final class DownloadHolder extends RecyclerView.ViewHolder {
        private final ImageView thumbnail;
        private final TextView title, status, meta;
        private final ImageButton actions;
        DownloadHolder(View view) {
            super(view);
            thumbnail = view.findViewById(R.id.history_thumbnail);
            title = view.findViewById(R.id.history_title);
            status = view.findViewById(R.id.history_status);
            meta = view.findViewById(R.id.history_meta);
            actions = view.findViewById(R.id.history_actions);
        }
    }
}
