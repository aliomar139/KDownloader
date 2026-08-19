package com.kira.kdownloader.ui;

import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.HapticFeedbackConstants;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.BaseTransientBottomBar;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.TextInputEditText;
import com.kira.kdownloader.MainActivity;
import com.kira.kdownloader.R;
import com.kira.kdownloader.engine.DownloadChoice;
import com.kira.kdownloader.engine.MediaInfo;
import com.kira.kdownloader.service.DownloadEvents;
import com.kira.kdownloader.service.DownloadService;
import com.kira.kdownloader.util.FormattingKt;
import com.kira.kdownloader.util.RecentUrls;
import com.kira.kdownloader.util.UrlExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class HomeFragment extends Fragment implements FormatAdapter.Listener {
    private static final String ARG_INITIAL_URL = "initial_url";
    private static final String TAG = "HomeFragment";

    private String initialUrl = "";
    private String clipboardSuggestion;
    private String dismissedLink;
    private String handledCompletionKey;
    private String shownFailureKey;
    private HomeUiState.Loaded loadedState;
    private Map<String, DownloadEvents.State> downloadStates = Collections.emptyMap();
    private HomeViewModel viewModel;
    private ClipboardManager clipboard;
    private ClipboardManager.OnPrimaryClipChangedListener clipboardListener;
    private View root;
    private TextInputEditText urlInput;
    private MaterialButton fetchButton;
    private MaterialButton activeDownloadsButton;
    private View clipboardBanner;
    private TextView clipboardText;
    private View idleState, loadingState, errorState, resultState;
    private TextView errorMessage;
    private LinearLayout recentUrls;
    private ImageView thumbnail;
    private TextView mediaTitle, mediaMeta;
    private FormatAdapter formatAdapter;
    private ObjectAnimator shimmer;
    private BottomSheetDialog downloadsDialog;
    private ActiveDownloadAdapter activeDownloadAdapter;

    public static HomeFragment newInstance(String initialUrl) {
        HomeFragment fragment = new HomeFragment();
        Bundle args = new Bundle();
        args.putString(ARG_INITIAL_URL, initialUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) initialUrl = getArguments().getString(ARG_INITIAL_URL, "");
        viewModel = new ViewModelProvider(this).get(HomeViewModel.class);
        clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
        clipboardListener = this::refreshClipboardSuggestion;
    }

    @NonNull @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                @Nullable ViewGroup container,
                                                @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        root = view;
        urlInput = view.findViewById(R.id.url_input);
        fetchButton = view.findViewById(R.id.fetch_button);
        activeDownloadsButton = view.findViewById(R.id.active_downloads_button);
        clipboardBanner = view.findViewById(R.id.clipboard_banner);
        clipboardText = view.findViewById(R.id.clipboard_text);
        idleState = view.findViewById(R.id.idle_state);
        loadingState = view.findViewById(R.id.loading_state);
        errorState = view.findViewById(R.id.error_state);
        resultState = view.findViewById(R.id.result_state);
        errorMessage = view.findViewById(R.id.error_message);
        recentUrls = view.findViewById(R.id.recent_urls);
        thumbnail = view.findViewById(R.id.media_thumbnail);
        mediaTitle = view.findViewById(R.id.media_title);
        mediaMeta = view.findViewById(R.id.media_meta);

        RecyclerView formats = view.findViewById(R.id.formats_list);
        formats.setLayoutManager(new LinearLayoutManager(requireContext()));
        formatAdapter = new FormatAdapter(this);
        formats.setAdapter(formatAdapter);
        urlInput.setText(initialUrl);
        fetchButton.setOnClickListener(ignored -> fetch());
        urlInput.setOnEditorActionListener((text, actionId, event) -> {
            if (actionId != EditorInfo.IME_ACTION_GO) return false;
            fetch();
            return true;
        });
        view.findViewById(R.id.paste_button).setOnClickListener(ignored -> pasteClipboard(false));
        view.findViewById(R.id.clear_button).setOnClickListener(ignored -> urlInput.setText(""));
        view.findViewById(R.id.retry_button).setOnClickListener(ignored -> fetch());
        view.findViewById(R.id.use_clipboard_button).setOnClickListener(ignored -> pasteClipboard(true));
        view.findViewById(R.id.dismiss_clipboard_button).setOnClickListener(ignored -> {
            dismissedLink = clipboardSuggestion;
            clipboardBanner.setVisibility(View.GONE);
        });
        activeDownloadsButton.setOnClickListener(ignored -> showActiveDownloads());
        ImageButton themeToggle = view.findViewById(R.id.theme_toggle);
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        themeToggle.setImageResource(dark ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
        themeToggle.setOnClickListener(ignored -> ((MainActivity) requireActivity()).toggleTheme());

        viewModel.getState().observe(getViewLifecycleOwner(), this::renderState);
        DownloadEvents.getStates().live().observe(getViewLifecycleOwner(), this::renderDownloads);
        viewModel.warmUp();
        refreshRecentUrls();
    }

    @Override public void onResume() {
        super.onResume();
        clipboard.addPrimaryClipChangedListener(clipboardListener);
        refreshClipboardSuggestion();
    }

    @Override public void onPause() {
        clipboard.removePrimaryClipChangedListener(clipboardListener);
        super.onPause();
    }

    @Override public void onDestroyView() {
        if (shimmer != null) shimmer.cancel();
        if (downloadsDialog != null) downloadsDialog.dismiss();
        root = null;
        super.onDestroyView();
    }

    public void setInitialUrl(String url) {
        initialUrl = url == null ? "" : url;
        if (urlInput != null && !initialUrl.trim().isEmpty()) urlInput.setText(initialUrl);
    }

    private void fetch() { viewModel.fetch(text(urlInput)); }

    private void renderState(HomeUiState state) {
        if (root == null || state == null) return;
        idleState.setVisibility(state instanceof HomeUiState.Idle ? View.VISIBLE : View.GONE);
        loadingState.setVisibility(state instanceof HomeUiState.Loading ? View.VISIBLE : View.GONE);
        errorState.setVisibility(state instanceof HomeUiState.Error ? View.VISIBLE : View.GONE);
        resultState.setVisibility(state instanceof HomeUiState.Loaded ? View.VISIBLE : View.GONE);
        fetchButton.setEnabled(!(state instanceof HomeUiState.Loading));
        if (state instanceof HomeUiState.Loading) startShimmer();
        else if (shimmer != null) shimmer.cancel();
        if (state instanceof HomeUiState.Error) {
            errorMessage.setText(((HomeUiState.Error) state).getMessage());
        } else if (state instanceof HomeUiState.Loaded) {
            loadedState = (HomeUiState.Loaded) state;
            renderLoaded(loadedState);
        } else if (state instanceof HomeUiState.Idle) refreshRecentUrls();
    }

    private void renderLoaded(HomeUiState.Loaded loaded) {
        MediaInfo info = loaded.getInfo();
        mediaTitle.setText(info.getTitle());
        List<String> metadata = new ArrayList<>();
        String host = Uri.parse(loaded.getSourceUrl()).getHost();
        if (host != null) metadata.add(host.startsWith("www.") ? host.substring(4) : host);
        if (info.getUploader() != null && !info.getUploader().trim().isEmpty()) metadata.add(info.getUploader());
        String duration = FormattingKt.formatDuration(info.getDurationSeconds());
        if (!duration.isEmpty()) metadata.add(duration);
        mediaMeta.setText(android.text.TextUtils.join(" · ", metadata));
        if (info.getThumbnailUrl() == null) thumbnail.setImageResource(R.drawable.ic_smart_display);
        else Glide.with(this).load(info.getThumbnailUrl()).placeholder(R.drawable.ic_smart_display)
                .error(R.drawable.ic_smart_display).into(thumbnail);
        formatAdapter.submit(loaded.getSourceUrl(), info.getChoices(), downloadStates);
    }

    private void renderDownloads(Map<String, DownloadEvents.State> states) {
        downloadStates = states;
        if (formatAdapter != null) formatAdapter.updateStates(states);
        List<DownloadEvents.State> active = new ArrayList<>();
        Map.Entry<String, DownloadEvents.State> failure = null, success = null;
        for (Map.Entry<String, DownloadEvents.State> entry : states.entrySet()) {
            DownloadEvents.Phase phase = entry.getValue().getPhase();
            if (phase == DownloadEvents.Phase.PREPARING || phase == DownloadEvents.Phase.RUNNING) active.add(entry.getValue());
            else if (phase == DownloadEvents.Phase.FAILED && failure == null) failure = entry;
            else if (phase == DownloadEvents.Phase.COMPLETED && success == null) success = entry;
        }
        activeDownloadsButton.setVisibility(active.isEmpty() ? View.GONE : View.VISIBLE);
        activeDownloadsButton.setText(active.size() == 1 ? "1 download in progress · View"
                : active.size() + " downloads in progress · View");
        if (activeDownloadAdapter != null) activeDownloadAdapter.submit(active);
        if (active.isEmpty() && downloadsDialog != null) downloadsDialog.dismiss();
        if (failure != null && !failure.getKey().equals(shownFailureKey)) {
            shownFailureKey = failure.getKey();
            showFailure(failure.getKey(), failure.getValue());
        }
        if (success != null && !success.getKey().equals(handledCompletionKey)) {
            handledCompletionKey = success.getKey();
            showCompletion(success.getKey(), success.getValue());
        }
    }

    @Override public void onDownload(DownloadChoice choice) {
        if (loadedState == null) return;
        root.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS);
        String url = loadedState.getSourceUrl();
        String key = DownloadEvents.keyOf(url, choice.getLabel());
        String processId = "dl-" + key.hashCode() + '-' + System.currentTimeMillis();
        MediaInfo info = loadedState.getInfo();
        DownloadEvents.update(key, new DownloadEvents.State(DownloadEvents.Phase.PREPARING, -1,
                info.getTitle(), choice.getKind().name(), null, null, -1L, processId));
        DownloadService.start(requireContext(), url, choice, info.getTitle(), info.getThumbnailUrl(), processId);
        Snackbar.make(root, R.string.download_started, Snackbar.LENGTH_SHORT).show();
    }

    @Override public void onCancel(String processId) { DownloadService.cancel(requireContext(), processId); }

    private void showActiveDownloads() {
        View content = getLayoutInflater().inflate(R.layout.sheet_active_downloads, null);
        RecyclerView list = content.findViewById(R.id.active_downloads_list);
        list.setLayoutManager(new LinearLayoutManager(requireContext()));
        activeDownloadAdapter = new ActiveDownloadAdapter(this::onCancel);
        list.setAdapter(activeDownloadAdapter);
        List<DownloadEvents.State> active = new ArrayList<>();
        for (DownloadEvents.State state : downloadStates.values()) if (state.getPhase() == DownloadEvents.Phase.PREPARING || state.getPhase() == DownloadEvents.Phase.RUNNING) active.add(state);
        activeDownloadAdapter.submit(active);
        downloadsDialog = new BottomSheetDialog(requireContext());
        downloadsDialog.setContentView(content);
        downloadsDialog.setOnDismissListener(ignored -> { downloadsDialog = null; activeDownloadAdapter = null; });
        downloadsDialog.show();
    }

    private void showFailure(String key, DownloadEvents.State state) {
        new MaterialAlertDialogBuilder(requireContext()).setIcon(R.drawable.ic_error_outline)
                .setTitle("Download failed")
                .setMessage(state.getTitle() + (state.getMessage() == null ? "" : "\n\n" + state.getMessage()))
                .setPositiveButton(android.R.string.ok, (dialog, which) -> DownloadEvents.clear(key))
                .setOnCancelListener(dialog -> DownloadEvents.clear(key)).show();
    }

    private void showCompletion(String key, DownloadEvents.State state) {
        Snackbar snackbar = Snackbar.make(root, "Saved · " + state.getTitle(), Snackbar.LENGTH_LONG);
        if (state.getFileUri() != null) snackbar.setAction(R.string.open, ignored -> openMedia(state.getFileUri(), state.getKind()));
        snackbar.addCallback(new BaseTransientBottomBar.BaseCallback<Snackbar>() {
            @Override public void onDismissed(Snackbar transientBottomBar, int event) { DownloadEvents.clear(key); }
        });
        snackbar.show();
    }

    private void openMedia(String fileUri, String kind) {
        Intent intent = new Intent(Intent.ACTION_VIEW).setDataAndType(Uri.parse(fileUri),
                "AUDIO".equals(kind) ? "audio/*" : "video/*");
        try { startActivity(Intent.createChooser(intent, "Open with")); }
        catch (Throwable error) {
            Log.w(TAG, "Could not open media", error);
            Toast.makeText(requireContext(), R.string.couldn_t_open_this_file, Toast.LENGTH_SHORT).show();
        }
    }

    private void refreshRecentUrls() {
        if (recentUrls == null) return;
        recentUrls.removeAllViews();
        for (String url : RecentUrls.all(requireContext())) {
            MaterialButton button = new MaterialButton(requireContext(), null,
                    com.google.android.material.R.attr.materialButtonOutlinedStyle);
            button.setText(url);
            button.setMaxLines(1);
            button.setEllipsize(android.text.TextUtils.TruncateAt.END);
            button.setOnClickListener(ignored -> { urlInput.setText(url); fetch(); });
            recentUrls.addView(button, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }
    }

    private void refreshClipboardSuggestion() {
        if (root == null) return;
        ClipData clip = clipboard.getPrimaryClip();
        String value = clip == null || clip.getItemCount() == 0 ? null
                : String.valueOf(clip.getItemAt(0).coerceToText(requireContext()));
        String candidate = value == null ? null : UrlExtractor.fromText(value);
        clipboardSuggestion = candidate != null && candidate.regionMatches(true, 0, "http", 0, 4)
                ? candidate : null;
        boolean show = clipboardSuggestion != null && !clipboardSuggestion.equals(text(urlInput))
                && !clipboardSuggestion.equals(dismissedLink);
        clipboardBanner.setVisibility(show ? View.VISIBLE : View.GONE);
        if (show) clipboardText.setText("Link on clipboard\n" + clipboardSuggestion);
    }

    private void pasteClipboard(boolean fetchNow) {
        refreshClipboardSuggestion();
        if (clipboardSuggestion != null) {
            urlInput.setText(clipboardSuggestion);
            if (fetchNow) fetch();
            return;
        }
        ClipData clip = clipboard.getPrimaryClip();
        if (clip != null && clip.getItemCount() > 0) urlInput.setText(clip.getItemAt(0).coerceToText(requireContext()));
    }

    private void startShimmer() {
        if (shimmer != null) shimmer.cancel();
        shimmer = ObjectAnimator.ofFloat(root.findViewById(R.id.skeleton_1), View.ALPHA, 0.45f, 1f);
        shimmer.setDuration(850L);
        shimmer.setRepeatMode(ObjectAnimator.REVERSE);
        shimmer.setRepeatCount(ObjectAnimator.INFINITE);
        shimmer.start();
    }

    private static String text(TextView view) { return view.getText() == null ? "" : view.getText().toString(); }
}
