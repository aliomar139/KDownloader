package com.kira.kdownloader.ui;

import android.animation.ObjectAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.LinearProgressIndicator;
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
import com.kira.kdownloader.util.MediaOpener;
import com.kira.kdownloader.util.ThumbnailBinder;
import com.kira.kdownloader.util.MotionPreferences;
import com.kira.kdownloader.util.UrlExtractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class HomeFragment extends Fragment implements FormatAdapter.Listener {
    private static final String ARG_INITIAL_URL = "initial_url";
    private static final String TAG = "HomeFragment";

    private String initialUrl = "";
    private String handledCompletionKey;
    private String shownFailureKey;
    private HomeUiState.Loaded loadedState;
    private Map<String, DownloadEvents.State> downloadStates = Collections.emptyMap();
    private HomeViewModel viewModel;
    private ClipboardManager clipboard;
    private View root;
    private TextInputEditText urlInput;
    private MaterialButton fetchButton;
    private MaterialButton activeDownloadsButton;
    private View urlCard;
    private View idleState, loadingState, resultState;
    private StateView errorState;
    private ImageView thumbnail;
    private TextView mediaTitle, mediaMeta;
    private View progressCard;
    private TextView progressTitle, progressPercent, progressDetail;
    private LinearProgressIndicator progressBar;
    private MaterialButton progressCancel;
    private FormatAdapter formatAdapter;
    private ObjectAnimator shimmer;
    private BottomSheetDialog downloadsDialog;
    private ActiveDownloadAdapter activeDownloadAdapter;
    private boolean showingResult;

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
        urlCard = view.findViewById(R.id.url_card);
        idleState = view.findViewById(R.id.idle_state);
        loadingState = view.findViewById(R.id.loading_state);
        errorState = view.findViewById(R.id.error_state);
        resultState = view.findViewById(R.id.result_state);
        thumbnail = view.findViewById(R.id.media_thumbnail);
        mediaTitle = view.findViewById(R.id.media_title);
        mediaMeta = view.findViewById(R.id.media_meta);
        progressCard = view.findViewById(R.id.download_progress_card);
        progressTitle = view.findViewById(R.id.download_progress_title);
        progressPercent = view.findViewById(R.id.download_progress_percent);
        progressDetail = view.findViewById(R.id.download_progress_detail);
        progressBar = view.findViewById(R.id.download_progress_bar);
        progressCancel = view.findViewById(R.id.download_progress_cancel);

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
        view.findViewById(R.id.paste_button).setOnClickListener(ignored -> pasteClipboard());
        view.findViewById(R.id.clear_button).setOnClickListener(ignored -> urlInput.setText(""));
        view.findViewById(R.id.change_link_button).setOnClickListener(ignored -> {
            viewModel.reset();
            urlInput.requestFocus();
        });
        StateView idlePrompt = view.findViewById(R.id.home_idle_prompt);
        idlePrompt.setState(R.drawable.ic_link, getString(R.string.ready_when_you_are), null);
        errorState.setState(R.drawable.ic_error_outline, getString(R.string.something_went_wrong), null);
        errorState.setAction(getString(R.string.try_again), ignored -> fetch());
        activeDownloadsButton.setOnClickListener(ignored -> showActiveDownloads());
        ImageButton themeToggle = view.findViewById(R.id.theme_toggle);
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        themeToggle.setImageResource(dark ? R.drawable.ic_light_mode : R.drawable.ic_dark_mode);
        themeToggle.setOnClickListener(ignored -> ((MainActivity) requireActivity()).toggleTheme());

        viewModel.getState().observe(getViewLifecycleOwner(), this::renderState);
        DownloadEvents.getStates().live().observe(getViewLifecycleOwner(), this::renderDownloads);
        viewModel.warmUp();
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
        showingResult = state instanceof HomeUiState.Loaded;
        urlCard.setVisibility(showingResult ? View.GONE : View.VISIBLE);
        idleState.setVisibility(state instanceof HomeUiState.Idle ? View.VISIBLE : View.GONE);
        loadingState.setVisibility(state instanceof HomeUiState.Loading ? View.VISIBLE : View.GONE);
        errorState.setVisibility(state instanceof HomeUiState.Error ? View.VISIBLE : View.GONE);
        resultState.setVisibility(state instanceof HomeUiState.Loaded ? View.VISIBLE : View.GONE);
        fetchButton.setEnabled(!(state instanceof HomeUiState.Loading));
        if (state instanceof HomeUiState.Loading) startShimmer();
        else if (shimmer != null) shimmer.cancel();
        if (!(state instanceof HomeUiState.Loaded)) loadedState = null;
        if (state instanceof HomeUiState.Error) {
            errorState.setState(R.drawable.ic_error_outline, getString(R.string.something_went_wrong),
                    ((HomeUiState.Error) state).getMessage());
        } else if (state instanceof HomeUiState.Loaded) {
            loadedState = (HomeUiState.Loaded) state;
            renderLoaded(loadedState);
        }
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
        ThumbnailBinder.bind(thumbnail, loaded.getSourceUrl(), "media:" + loaded.getSourceUrl(),
                info.getThumbnailUrl(), null, false, R.drawable.placeholder_video, 256);
        formatAdapter.submit(loaded.getSourceUrl(), info.getChoices(), downloadStates);
        renderProgressCard(downloadStates);
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
        renderProgressCard(states);
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

    /** The panel under the quality list: what is downloading right now, and a way to stop it. */
    private void renderProgressCard(Map<String, DownloadEvents.State> states) {
        if (progressCard == null) return;
        String sourceUrl = loadedState == null ? null : loadedState.getSourceUrl();
        DownloadEvents.State shown = null;
        String shownLabel = null;
        int running = 0;
        if (sourceUrl != null) {
            String prefix = DownloadEvents.keyOf(sourceUrl, "");
            for (Map.Entry<String, DownloadEvents.State> entry : states.entrySet()) {
                DownloadEvents.Phase phase = entry.getValue().getPhase();
                if (phase != DownloadEvents.Phase.PREPARING && phase != DownloadEvents.Phase.RUNNING) continue;
                if (!entry.getKey().startsWith(prefix)) continue;
                running++;
                if (shown == null) {
                    shown = entry.getValue();
                    shownLabel = entry.getKey().substring(prefix.length());
                }
            }
        }
        if (shown == null) {
            progressCard.setVisibility(View.GONE);
            return;
        }
        boolean preparing = shown.getPhase() == DownloadEvents.Phase.PREPARING || shown.getPercent() < 0;
        int percent = Math.max(0, Math.min(100, shown.getPercent()));
        progressCard.setVisibility(View.VISIBLE);
        progressTitle.setText(shownLabel == null || shownLabel.trim().isEmpty()
                ? shown.getTitle() : "Downloading " + shownLabel);
        progressPercent.setText(preparing ? "" : percent + "%");
        progressBar.setIndeterminate(preparing);
        if (!preparing) progressBar.setProgressCompat(percent, true);
        String remaining = FormattingKt.formatEta(shown.getEtaSeconds());
        String eta = preparing ? "Preparing…"
                : remaining.trim().isEmpty() ? "Downloading…" : remaining + " left";
        progressDetail.setText(running > 1 ? eta + " · " + (running - 1) + " more in queue" : eta);
        String processId = shown.getProcessId();
        progressCancel.setEnabled(processId != null);
        progressCancel.setOnClickListener(ignored -> {
            if (processId != null) onCancel(processId);
        });
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
        Uri uri = Uri.parse(fileUri);
        MediaOpener.Result result = MediaOpener.open(
                requireContext(), uri, "AUDIO".equalsIgnoreCase(kind));
        if (result == MediaOpener.Result.LAUNCHED) return;
        Log.w(TAG, "Could not open " + uri + ": " + result);
        int message = result == MediaOpener.Result.NO_APP
                ? R.string.no_app_on_this_device_can_open_this_file
                : R.string.couldn_t_open_this_file;
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show();
    }

    private void pasteClipboard() {
        ClipData clip = clipboard.getPrimaryClip();
        if (clip == null || clip.getItemCount() == 0) return;
        String value = String.valueOf(clip.getItemAt(0).coerceToText(requireContext()));
        String link = UrlExtractor.fromText(value);
        urlInput.setText(link != null && link.regionMatches(true, 0, "http", 0, 4) ? link : value);
        urlInput.setSelection(text(urlInput).length());
    }

    private void startShimmer() {
        if (shimmer != null) shimmer.cancel();
        if (MotionPreferences.reduce(requireContext())) {
            root.findViewById(R.id.skeleton_1).setAlpha(0.75f);
            return;
        }
        shimmer = ObjectAnimator.ofFloat(root.findViewById(R.id.skeleton_1), View.ALPHA, 0.35f, 0.75f);
        shimmer.setDuration(1100L);
        shimmer.setRepeatMode(ObjectAnimator.REVERSE);
        shimmer.setRepeatCount(ObjectAnimator.INFINITE);
        shimmer.start();
    }

    private static String text(TextView view) { return view.getText() == null ? "" : view.getText().toString(); }
}
