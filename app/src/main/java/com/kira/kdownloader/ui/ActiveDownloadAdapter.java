package com.kira.kdownloader.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;
import com.kira.kdownloader.R;
import com.kira.kdownloader.service.DownloadEvents;
import com.kira.kdownloader.util.FormattingKt;

import java.util.Collections;
import java.util.List;

final class ActiveDownloadAdapter extends RecyclerView.Adapter<ActiveDownloadAdapter.Holder> {
    interface CancelListener { void onCancel(String processId); }

    private final CancelListener listener;
    private List<DownloadEvents.State> downloads = Collections.emptyList();

    ActiveDownloadAdapter(CancelListener listener) { this.listener = listener; }

    void submit(List<DownloadEvents.State> downloads) {
        this.downloads = downloads;
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_active_download, parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        DownloadEvents.State state = downloads.get(position);
        boolean preparing = state.getPhase() == DownloadEvents.Phase.PREPARING
                || state.getPercent() < 0;
        holder.title.setText(state.getTitle().trim().isEmpty() ? "Preparing…" : state.getTitle());
        String eta = FormattingKt.formatEta(state.getEtaSeconds());
        holder.status.setText(preparing ? "Preparing…" : state.getPercent() + "%"
                + (eta.isEmpty() ? "" : " · " + eta + " left"));
        holder.progress.setIndeterminate(preparing);
        if (!preparing) holder.progress.setProgressCompat(
                Math.max(0, Math.min(100, state.getPercent())), true);
        holder.cancel.setEnabled(state.getProcessId() != null);
        holder.cancel.setOnClickListener(view -> {
            if (state.getProcessId() != null) listener.onCancel(state.getProcessId());
        });
    }

    @Override public int getItemCount() { return downloads.size(); }

    static final class Holder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView status;
        private final ImageButton cancel;
        private final LinearProgressIndicator progress;

        Holder(View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.download_title);
            status = itemView.findViewById(R.id.download_status);
            cancel = itemView.findViewById(R.id.cancel_download);
            progress = itemView.findViewById(R.id.download_progress);
        }
    }
}
