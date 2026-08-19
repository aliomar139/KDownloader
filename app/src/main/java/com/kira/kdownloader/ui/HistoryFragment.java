package com.kira.kdownloader.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.kira.kdownloader.MainActivity;
import com.kira.kdownloader.R;
import com.kira.kdownloader.data.AppDatabase;
import com.kira.kdownloader.data.DownloadDao;
import com.kira.kdownloader.data.DownloadEntity;
import com.kira.kdownloader.data.DownloadStatus;
import com.kira.kdownloader.util.AppExecutors;
import com.kira.kdownloader.util.DownloadDirectoryScanner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public final class HistoryFragment extends Fragment implements HistoryAdapter.Listener {
    private static final String TAG = "HistoryFragment";
    private enum Filter { ALL, VIDEO, AUDIO }

    private final ActivityResultLauncher<String[]> mediaPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> scan(true));
    private DownloadDao dao;
    private HistoryAdapter adapter;
    private TextView countView, emptyView;
    private RecyclerView listView;
    private TextInputEditText searchView;
    private List<DownloadEntity> all = Collections.emptyList();
    private Filter filter = Filter.ALL;
    private boolean newestFirst = true;

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dao = AppDatabase.get(requireContext()).downloadDao();
    }

    @NonNull @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                @Nullable ViewGroup container,
                                                @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        countView = view.findViewById(R.id.history_count);
        emptyView = view.findViewById(R.id.history_empty);
        listView = view.findViewById(R.id.history_list);
        searchView = view.findViewById(R.id.history_search);
        adapter = new HistoryAdapter(this);
        listView.setLayoutManager(new LinearLayoutManager(requireContext()));
        listView.setAdapter(adapter);
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override public boolean onMove(@NonNull RecyclerView recyclerView,
                                            @NonNull RecyclerView.ViewHolder a,
                                            @NonNull RecyclerView.ViewHolder b) { return false; }
            @Override public void onSwiped(@NonNull RecyclerView.ViewHolder holder, int direction) {
                DownloadEntity download = adapter.downloadAt(holder.getBindingAdapterPosition());
                adapter.notifyItemChanged(holder.getBindingAdapterPosition());
                if (download != null) confirmDelete(download);
            }
        }).attachToRecyclerView(listView);

        searchView.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence text, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence text, int start, int before, int count) { applyFilters(); }
            @Override public void afterTextChanged(Editable editable) {}
        });
        ChipGroup filters = view.findViewById(R.id.history_filters);
        filters.setOnCheckedStateChangeListener((group, checkedIds) -> {
            int id = checkedIds.isEmpty() ? R.id.filter_all : checkedIds.get(0);
            filter = id == R.id.filter_audio ? Filter.AUDIO
                    : id == R.id.filter_video ? Filter.VIDEO : Filter.ALL;
            applyFilters();
        });
        view.findViewById(R.id.history_sort).setOnClickListener(ignored -> {
            newestFirst = !newestFirst;
            applyFilters();
        });
        view.findViewById(R.id.history_more).setOnClickListener(this::showMoreMenu);
        ImageButton theme = view.findViewById(R.id.history_theme_toggle);
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        theme.setImageResource(dark ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
        theme.setOnClickListener(ignored -> ((MainActivity) requireActivity()).toggleTheme());

        dao.observeAll().observe(getViewLifecycleOwner(), downloads -> {
            all = downloads;
            countView.setText(downloads.isEmpty() ? "No downloads yet"
                    : downloads.size() + " download" + (downloads.size() == 1 ? "" : "s"));
            applyFilters();
        });
        requestMediaPermissions();
        scan(false);
    }

    @Override public void onResume() {
        super.onResume();
        if (dao != null) scan(false);
    }

    private void applyFilters() {
        if (adapter == null) return;
        String query = searchView.getText() == null ? ""
                : searchView.getText().toString().trim().toLowerCase(Locale.ROOT);
        List<DownloadEntity> visible = new ArrayList<>();
        for (DownloadEntity entry : all) {
            boolean audio = "AUDIO".equalsIgnoreCase(entry.getKind());
            if (filter == Filter.AUDIO && !audio || filter == Filter.VIDEO && audio) continue;
            if (!query.isEmpty() && !entry.getTitle().toLowerCase(Locale.ROOT).contains(query)
                    && !entry.getSourceUrl().toLowerCase(Locale.ROOT).contains(query)) continue;
            visible.add(entry);
        }
        if (!newestFirst) Collections.reverse(visible);
        adapter.submit(visible);
        listView.setVisibility(visible.isEmpty() ? View.GONE : View.VISIBLE);
        emptyView.setVisibility(visible.isEmpty() ? View.VISIBLE : View.GONE);
        emptyView.setText(all.isEmpty() ? "No downloads yet" : "No downloads match your search.");
    }

    private void showMoreMenu(View anchor) {
        PopupMenu menu = new PopupMenu(requireContext(), anchor);
        menu.getMenu().add("Clear all history");
        menu.setOnMenuItemClickListener(item -> {
            confirmClearAll();
            return true;
        });
        menu.show();
    }

    private void confirmClearAll() {
        new MaterialAlertDialogBuilder(requireContext()).setIcon(R.drawable.ic_delete)
                .setTitle("Clear all history?")
                .setMessage("This removes every entry from your history. The downloaded files themselves are not deleted.")
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.clear_all, (dialog, which) ->
                        AppExecutors.io().execute(dao::clearAll)).show();
    }

    private void confirmDelete(DownloadEntity download) {
        boolean fileAvailable = download.getStatus() == DownloadStatus.COMPLETED
                && download.getFileUri() != null;
        CheckBox deleteFile = new CheckBox(requireContext());
        deleteFile.setText("Also delete the file from storage");
        deleteFile.setVisibility(fileAvailable ? View.VISIBLE : View.GONE);
        new MaterialAlertDialogBuilder(requireContext()).setIcon(R.drawable.ic_delete)
                .setTitle(R.string.delete_download)
                .setMessage(download.getTitle() + "\n\n" + (fileAvailable
                        ? "This removes the entry from your history."
                        : "No saved file is linked to this entry; only the history record will be removed."))
                .setView(deleteFile).setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.delete, (dialog, which) -> delete(download,
                        fileAvailable && deleteFile.isChecked())).show();
    }

    private void delete(DownloadEntity download, boolean deleteFile) {
        AppExecutors.io().execute(() -> {
            if (deleteFile && download.getFileUri() != null) {
                try { requireContext().getContentResolver().delete(Uri.parse(download.getFileUri()), null, null); }
                catch (Throwable ignored) { }
            }
            dao.deleteById(download.getId());
        });
    }

    @Override public void onOpen(DownloadEntity download) {
        if (download.getFileUri() == null) {
            Toast.makeText(requireContext(), R.string.this_download_has_no_saved_file, Toast.LENGTH_SHORT).show();
            return;
        }
        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(download.getFileUri()),
                "AUDIO".equals(download.getKind()) ? "audio/*" : "video/*");
        try { startActivity(Intent.createChooser(intent, "Open with")); }
        catch (Throwable error) {
            Log.w(TAG, "Could not open " + download.getTitle(), error);
            Toast.makeText(requireContext(), R.string.couldn_t_open_this_file, Toast.LENGTH_SHORT).show();
        }
    }

    @Override public void onDetails(DownloadEntity download) {
        LinearLayout actions = new LinearLayout(requireContext());
        actions.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (20 * getResources().getDisplayMetrics().density);
        actions.setPadding(padding, padding, padding, padding);
        TextView title = new TextView(requireContext());
        title.setText(download.getTitle());
        title.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_TitleMedium);
        actions.addView(title);
        BottomSheetDialog sheet = new BottomSheetDialog(requireContext());
        if (download.getStatus() == DownloadStatus.COMPLETED && download.getFileUri() != null) {
            addAction(actions, "Play", () -> { sheet.dismiss(); onOpen(download); });
            addAction(actions, "Share", () -> { sheet.dismiss(); share(download); });
        }
        if (download.getStatus() == DownloadStatus.FAILED && !download.getSourceUrl().trim().isEmpty()) {
            addAction(actions, "Download again", () -> {
                sheet.dismiss();
                ((MainActivity) requireActivity()).openHome(download.getSourceUrl());
            });
        }
        addAction(actions, "Delete", () -> { sheet.dismiss(); confirmDelete(download); });
        sheet.setContentView(actions);
        sheet.show();
    }

    private void share(DownloadEntity download) {
        if (download.getFileUri() == null) return;
        Intent intent = new Intent(Intent.ACTION_SEND)
                .setType("AUDIO".equals(download.getKind()) ? "audio/*" : "video/*")
                .putExtra(Intent.EXTRA_STREAM, Uri.parse(download.getFileUri()));
        try { startActivity(Intent.createChooser(intent, "Share")); }
        catch (Throwable error) {
            Log.w(TAG, "Could not share " + download.getTitle(), error);
            Toast.makeText(requireContext(), R.string.no_app_available_to_share_this_file, Toast.LENGTH_SHORT).show();
        }
    }

    private void addAction(LinearLayout parent, String label, Runnable action) {
        MaterialButton button = new MaterialButton(requireContext());
        button.setText(label);
        button.setOnClickListener(ignored -> action.run());
        parent.addView(button, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }

    private void requestMediaPermissions() {
        String[] permissions = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                ? new String[]{Manifest.permission.READ_MEDIA_AUDIO, Manifest.permission.READ_MEDIA_VIDEO}
                : new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        List<String> missing = new ArrayList<>();
        for (String permission : permissions) if (ContextCompat.checkSelfPermission(
                requireContext(), permission) != PackageManager.PERMISSION_GRANTED) missing.add(permission);
        if (!missing.isEmpty()) mediaPermissionLauncher.launch(missing.toArray(new String[0]));
    }

    private void scan(boolean force) {
        AppExecutors.io().execute(() -> DownloadDirectoryScanner.syncIntoHistory(requireContext(), dao, force));
    }
}
