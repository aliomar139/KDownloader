package com.kira.kdownloader.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.kira.kdownloader.R;
import com.kira.kdownloader.engine.DownloadChoice;
import com.kira.kdownloader.engine.FormatSelector;
import com.kira.kdownloader.service.DownloadEvents;
import com.kira.kdownloader.util.FormattingKt;

import java.util.Collections;
import java.util.List;
import java.util.Map;

final class FormatAdapter extends RecyclerView.Adapter<FormatAdapter.Holder> {
    interface Listener {
        void onDownload(DownloadChoice choice);
        void onCancel(String processId);
    }

    private final Listener listener;
    private List<DownloadChoice> choices = Collections.emptyList();
    private Map<String, DownloadEvents.State> states = Collections.emptyMap();
    private String sourceUrl = "";
    private DownloadChoice recommended;

    FormatAdapter(Listener listener) {
        this.listener = listener;
    }

    void submit(String sourceUrl, List<DownloadChoice> choices,
                Map<String, DownloadEvents.State> states) {
        this.sourceUrl = sourceUrl;
        this.choices = choices;
        this.states = states;
        recommended = null;
        for (DownloadChoice choice : choices) {
            if (choice.getKind() == FormatSelector.Kind.VIDEO) {
                recommended = choice;
                break;
            }
        }
        notifyDataSetChanged();
    }

    void updateStates(Map<String, DownloadEvents.State> states) {
        this.states = states;
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_format, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        DownloadChoice choice = choices.get(position);
        DownloadEvents.State state = states.get(DownloadEvents.keyOf(sourceUrl, choice.getLabel()));
        boolean active = state != null && (state.getPhase() == DownloadEvents.Phase.PREPARING
                || state.getPhase() == DownloadEvents.Phase.RUNNING);
        boolean audio = choice.getKind() == FormatSelector.Kind.AUDIO;

        holder.icon.setImageResource(audio ? R.drawable.ic_music_note : R.drawable.ic_videocam);
        holder.title.setText(choice.getLabel());
        holder.badge.setVisibility(choice.equals(recommended) ? View.VISIBLE : View.GONE);
        String base = audio ? "MP3 · audio only" : "MP4 · video + audio";
        String size = FormattingKt.formatBytes(choice.getApproxBytes());
        holder.summary.setText(size.isEmpty() ? base : base + " · ~" + size);
        holder.action.setImageResource(active ? R.drawable.ic_close : R.drawable.ic_download);
        holder.status.setVisibility(active ? View.VISIBLE : View.GONE);
        holder.progress.setVisibility(active ? View.VISIBLE : View.GONE);
        if (active) {
            boolean preparing = state.getPhase() == DownloadEvents.Phase.PREPARING
                    || state.getPercent() < 0;
            holder.status.setText(preparing ? "Preparing…" : state.getPercent() + "%");
            holder.progress.setIndeterminate(preparing);
            if (!preparing) holder.progress.setProgressCompat(
                    Math.max(0, Math.min(100, state.getPercent())), true);
            holder.action.setOnClickListener(view -> {
                if (state.getProcessId() != null) listener.onCancel(state.getProcessId());
            });
            holder.itemView.setOnClickListener(null);
        } else {
            holder.action.setOnClickListener(view -> listener.onDownload(choice));
            holder.itemView.setOnClickListener(view -> listener.onDownload(choice));
        }
    }

    @Override public int getItemCount() { return choices.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        private final ImageView icon;
        private final TextView title;
        private final TextView badge;
        private final TextView summary;
        private final TextView status;
        private final ImageButton action;
        private final LinearProgressIndicator progress;

        Holder(View itemView) {
            super(itemView);
            icon = itemView.findViewById(R.id.format_icon);
            title = itemView.findViewById(R.id.format_title);
            badge = itemView.findViewById(R.id.format_badge);
            summary = itemView.findViewById(R.id.format_summary);
            status = itemView.findViewById(R.id.format_status);
            action = itemView.findViewById(R.id.format_action);
            progress = itemView.findViewById(R.id.format_progress);
        }
    }
}
