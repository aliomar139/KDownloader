package com.kira.kdownloader.settings.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.kira.kdownloader.BuildConfig;
import com.kira.kdownloader.R;
import com.kira.kdownloader.settings.*;
import com.kira.kdownloader.settings.platform.FolderAccessManager;
import com.kira.kdownloader.settings.platform.LanguageManager;
import com.kira.kdownloader.settings.platform.SystemStatus;
import com.kira.kdownloader.settings.ui.components.*;
import com.kira.kdownloader.util.AppExecutors;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public final class SettingsFragment extends Fragment {
    private enum Route { HOME, DOWNLOAD, STORAGE, BEHAVIOR, NETWORK, SUBTITLES,
        NOTIFICATIONS, APPEARANCE, HISTORY, ADVANCED, PERMISSIONS, RESET, ABOUT }

    private SettingsViewModel vm;
    private MaterialToolbar toolbar;
    private LinearLayout content;
    private Route route = Route.HOME;
    private OnBackPressedCallback backCallback;
    private FolderSlot pendingFolder;

    private final ActivityResultLauncher<Uri> folderPicker = registerForActivityResult(
            new ActivityResultContracts.OpenDocumentTree(), uri -> {
                if (uri != null && pendingFolder != null) vm.onFolderSelected(pendingFolder, uri);
            });
    private final ActivityResultLauncher<String> settingsDocument = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/json"), uri -> {
                if (uri == null) return;
                AppExecutors.io().execute(() -> write(uri, vm.exportJson()));
            });
    private final ActivityResultLauncher<String> diagnosticsDocument = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/plain"), uri -> {
                if (uri == null) return;
                AppExecutors.io().execute(() -> write(uri, vm.buildDiagnostics()));
            });
    private final ActivityResultLauncher<String[]> openDocument = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri == null) return;
                AppExecutors.io().execute(() -> importSettings(uri));
            });

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vm = ((com.kira.kdownloader.MainActivity) requireActivity()).getSettingsViewModel();
        if (savedInstanceState != null) {
            try { route = Route.valueOf(savedInstanceState.getString("route", Route.HOME.name())); }
            catch (IllegalArgumentException ignored) { route = Route.HOME; }
        }
        backCallback = new OnBackPressedCallback(route != Route.HOME) {
            @Override public void handleOnBackPressed() { show(Route.HOME); }
        };
        requireActivity().getOnBackPressedDispatcher().addCallback(this, backCallback);
    }

    @NonNull @Override public View onCreateView(@NonNull LayoutInflater inflater,
                                                @Nullable ViewGroup container,
                                                @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        toolbar = view.findViewById(R.id.settings_toolbar);
        content = view.findViewById(R.id.settings_content);
        toolbar.setNavigationOnClickListener(ignored -> show(Route.HOME));
        vm.getSettingsLive().observe(getViewLifecycleOwner(), ignored -> render());
        vm.getSystemStatus().observe(getViewLifecycleOwner(), ignored -> {
            if (route == Route.PERMISSIONS) render();
        });
        render();
    }

    @Override public void onResume() {
        super.onResume();
        vm.refreshStatus();
    }

    @Override public void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putString("route", route.name());
        super.onSaveInstanceState(outState);
    }

    private void show(Route next) {
        route = next;
        backCallback.setEnabled(route != Route.HOME);
        vm.refreshStatus();
        render();
    }

    private void render() {
        if (content == null) return;
        content.removeAllViews();
        AppSettings settings = vm.getSettingsValue();
        toolbar.setNavigationIcon(route == Route.HOME ? null : getDrawable(R.drawable.ic_arrow_back));
        toolbar.setTitle(title(route));
        switch (route) {
            case HOME: home(); break;
            case DOWNLOAD: download(settings.getDownload()); break;
            case STORAGE: storage(settings.getStorage()); break;
            case BEHAVIOR: behavior(settings.getBehavior()); break;
            case NETWORK: network(settings.getNetwork()); break;
            case SUBTITLES: subtitles(settings.getSubtitles()); break;
            case NOTIFICATIONS: notifications(settings.getNotifications()); break;
            case APPEARANCE: appearance(settings.getAppearance()); break;
            case HISTORY: history(settings.getHistory()); break;
            case ADVANCED: advanced(settings.getProcessing()); break;
            case PERMISSIONS: permissions(); break;
            case RESET: reset(); break;
            case ABOUT: about(); break;
        }
    }

    private void home() {
        category("Download preferences", "Type, format, quality, metadata", R.drawable.ic_video_library, Route.DOWNLOAD);
        category("Storage & files", "Folders, filenames, temporary files", R.drawable.ic_storage, Route.STORAGE);
        category("Download behavior", "Queue, retries, power, scheduling", R.drawable.ic_play_arrow, Route.BEHAVIOR);
        category("Network", "Allowed networks, mobile data, proxy", R.drawable.ic_wifi, Route.NETWORK);
        category("Subtitles & captions", "Languages, type, format, embedding", R.drawable.ic_subtitles, Route.SUBTITLES);
        category("Notifications", "Progress, completion, actions", R.drawable.ic_notifications, Route.NOTIFICATIONS);
        category("Appearance & accessibility", "Theme, language, list, motion", R.drawable.ic_palette, Route.APPEARANCE);
        category("History & privacy", "Retention, clearing, export/import", R.drawable.ic_history_filled, Route.HISTORY);
        category("Advanced processing", "Conversion, hardware, logging", R.drawable.ic_tune, Route.ADVANCED);
        category("Permissions & status", "Notifications, battery, background", R.drawable.ic_security, Route.PERMISSIONS);
        category("Reset", "Restore settings to defaults", R.drawable.ic_restart_alt, Route.RESET);
        category("About & support", "Version, licenses, help", R.drawable.ic_info, Route.ABOUT);
    }

    private void download(DownloadSettings d) {
        group("Type and format");
        choice("Download type", DownloadType.values(), d.getDownloadType(), true, v -> vm.setDownload(d.withDownloadType(v)));
        choice("Video format", VideoFormat.values(), d.getVideoFormat(), d.getDownloadType()!=DownloadType.AUDIO_ONLY, v -> vm.setDownload(d.withVideoFormat(v)));
        choice("Audio format", AudioFormat.values(), d.getAudioFormat(), true, v -> vm.setDownload(d.withAudioFormat(v)));
        group("Quality");
        choice("Default video quality", VideoQuality.values(), d.getVideoQuality(), d.getDownloadType()!=DownloadType.AUDIO_ONLY, v -> vm.setDownload(d.withVideoQuality(v)));
        choice("Default audio quality", AudioQuality.values(), d.getAudioQuality(), true, v -> vm.setDownload(d.withAudioQuality(v)));
        choice("Frame rate preference", FrameRatePreference.values(), d.getFrameRate(), d.getDownloadType()!=DownloadType.AUDIO_ONLY, v -> vm.setDownload(d.withFrameRate(v)));
        toggle("Prefer HDR when available", null, d.getPreferHdr(), true, v -> vm.setDownload(d.withPreferHdr(v)));
        toggle("Prefer Android-compatible codecs", "Choose codecs that play on most Android media players", d.getPreferAndroidCompatibleCodecs(), true, v -> vm.setDownload(d.withPreferAndroidCompatibleCodecs(v)));
        toggle("Automatically fall back", "Use the closest available quality or format", d.getAutoFallbackQuality(), true, v -> vm.setDownload(d.withAutoFallbackQuality(v)));
        toggle("Ask before each download", "Show a quality selection dialog every time", d.getAskQualityBeforeEachDownload(), true, v -> vm.setDownload(d.withAskQualityBeforeEachDownload(v)));
        group("Metadata");
        toggle("Download thumbnail when available", null, d.getDownloadThumbnail(), true, v -> vm.setDownload(d.withDownloadThumbnail(v)));
        toggle("Embed thumbnail in audio files", null, d.getEmbedThumbnail(), d.getDownloadThumbnail(), v -> vm.setDownload(d.withEmbedThumbnail(v)));
        toggle("Embed title, artist, album and more", null, d.getEmbedMetadata(), true, v -> vm.setDownload(d.withEmbedMetadata(v)));
        toggle("Preserve source upload date", "When supported by the source", d.getPreserveUploadDate(), true, v -> vm.setDownload(d.withPreserveUploadDate(v)));
    }

    private void storage(StorageSettings s) {
        group("Folders");
        folder("Default download folder", FolderSlot.DOWNLOAD, s.getDownloadFolderUri());
        folder("Video folder (optional)", FolderSlot.VIDEO, s.getVideoFolderUri());
        folder("Audio folder (optional)", FolderSlot.AUDIO, s.getAudioFolderUri());
        folder("Temporary folder (optional)", FolderSlot.TEMP, s.getTempFolderUri());
        note("Available storage: " + FolderAccessManager.formatBytes(vm.getFolders().availableBytes())
                + ". Folders are chosen with the Android system picker and access is remembered across restarts.");
        toggle("Warn before download if space is low", null, s.getWarnOnLowSpace(), true, v -> vm.setStorage(s.withWarnOnLowSpace(v)));
        group("File names");
        choice("On filename conflict", FilenameConflict.values(), s.getFilenameConflict(), true, v -> vm.setStorage(s.withFilenameConflict(v)));
        text("Filename template", s.getFilenameTemplate(), false, false,
                v -> vm.setStorage(s.withFilenameTemplate(v)), true, s.getFilenameTemplate(), "",
                value -> { FilenameTemplate.Validation result=FilenameTemplate.validate(value);return result instanceof FilenameTemplate.Validation.Invalid?((FilenameTemplate.Validation.Invalid)result).getReason():null; });
        List<String> variables=new ArrayList<>();for(String variable:FilenameTemplate.VARIABLES)variables.add("{"+variable+"}");
        note("Variables: "+String.join(" ",variables));
        note("Example: "+FilenameTemplate.example(s.getFilenameTemplate(),s.getMaxFilenameLength())+".mp4");
        slider("Maximum filename length", s.getMaxFilenameLength(), FilenameTemplate.MIN_LENGTH, FilenameTemplate.MAX_LENGTH, v -> vm.setStorage(s.withMaxFilenameLength(v)));
        note("Characters Android does not allow in filenames are removed or replaced.");
        choice("Organize into subfolders", SubfolderOrganization.values(), s.getSubfolderOrganization(), true, v -> vm.setStorage(s.withSubfolderOrganization(v)));
        group("Temporary files");
        action("Clear temporary files", "Recoverable: "+FolderAccessManager.formatBytes(vm.recoverableTempBytes()), 0, () -> confirm("Clear temporary files?", "This removes intermediate and cache files only. Completed downloads are never deleted.", () -> AppExecutors.io().execute(vm::clearTempFiles)));
    }

    private void behavior(BehaviorSettings b) {
        group("Queue");
        toggle("Ask for confirmation before downloading", null, b.getConfirmBeforeDownload(), true, v -> vm.setBehavior(b.withConfirmBeforeDownload(v)));
        slider("Maximum simultaneous downloads", b.getMaxSimultaneousDownloads(), 1, 5, v -> vm.setBehavior(b.withMaxSimultaneousDownloads(v)));
        slider("Maximum retries for failed downloads", b.getMaxRetryCount(), 0, 10, v -> vm.setBehavior(b.withMaxRetryCount(v)));
        choice("Add new downloads to", QueuePosition.values(), b.getNewDownloadPosition(), true, v -> vm.setBehavior(b.withNewDownloadPosition(v)));
        group("Resume and duplicates");
        toggle("Automatically resume interrupted downloads", null, b.getAutoResumeInterrupted(), true, v -> vm.setBehavior(b.withAutoResumeInterrupted(v)));
        toggle("Resume queued downloads after restart", null, b.getResumeQueueAfterRestart(), true, v -> vm.setBehavior(b.withResumeQueueAfterRestart(v)));
        toggle("Prevent duplicate downloads", null, b.getPreventDuplicates(), true, v -> vm.setBehavior(b.withPreventDuplicates(v)));
        choice("Duplicate detection", DuplicateDetection.values(), b.getDuplicateDetection(), b.getPreventDuplicates(), v -> vm.setBehavior(b.withDuplicateDetection(v)));
        group("Power and reliability");
        toggle("Keep screen awake", null, b.getKeepScreenAwake(), true, v -> vm.setBehavior(b.withKeepScreenAwake(v)));
        toggle("Pause on battery saver", null, b.getPauseOnBatterySaver(), true, v -> vm.setBehavior(b.withPauseOnBatterySaver(v)));
        toggle("Pause if device overheats", null, b.getPauseOnOverheat(), true, v -> vm.setBehavior(b.withPauseOnOverheat(v)));
        toggle("Auto-retry on reconnect", null, b.getAutoRetryOnReconnect(), true, v -> vm.setBehavior(b.withAutoRetryOnReconnect(v)));
        group("Limits and scheduling");
        toggle("Speed limit", null, b.getSpeedLimitEnabled(), true, v -> vm.setBehavior(b.withSpeedLimitEnabled(v)));
        text("Speed limit (KB/s)", Integer.toString(b.getSpeedLimitKbps()), true, false, v -> vm.setBehavior(b.withSpeedLimitKbps(Integer.parseInt(v))), b.getSpeedLimitEnabled(), null, "", SettingsFragment::positiveIntegerError);
        toggle("Only download within a time window", null, b.getScheduleEnabled(), true, v -> vm.setBehavior(b.withScheduleEnabled(v)));
        text("Window start (HH:MM)", formatMinutes(b.getScheduleStartMinutes()), false, false, v -> vm.setBehavior(b.withScheduleStartMinutes(parseMinutes(v))), b.getScheduleEnabled(), null, "", SettingsFragment::timeError);
        text("Window end (HH:MM)", formatMinutes(b.getScheduleEndMinutes()), false, false, v -> vm.setBehavior(b.withScheduleEndMinutes(parseMinutes(v))), b.getScheduleEnabled(), null, "", SettingsFragment::timeError);
        choice("After downloads finish", PostDownloadAction.values(), b.getPostDownloadAction(), true, v -> vm.setBehavior(b.withPostDownloadAction(v)));
    }

    private void network(NetworkSettings n) {
        group("Connection");
        choice("Allowed networks", NetworkType.values(), n.getAllowedNetworks(), true, v -> vm.setNetwork(n.withAllowedNetworks(v)));
        toggle("Allow roaming", null, n.getAllowRoaming(), true, v -> vm.setNetwork(n.withAllowRoaming(v)));
        toggle("Confirm mobile data", null, n.getConfirmMobileData(), true, v -> vm.setNetwork(n.withConfirmMobileData(v)));
        text("Mobile data warning threshold (MB)", Integer.toString(n.getMobileDataWarningMb()), true, false, v -> vm.setNetwork(n.withMobileDataWarningMb(Integer.parseInt(v))), true, null, "", SettingsFragment::positiveIntegerError);
        toggle("Treat metered Wi-Fi as mobile", null, n.getTreatMeteredWifiAsMobile(), true, v -> vm.setNetwork(n.withTreatMeteredWifiAsMobile(v)));
        toggle("Pause on network change", null, n.getPauseOnNetworkChange(), true, v -> vm.setNetwork(n.withPauseOnNetworkChange(v)));
        toggle("Retry after connection loss", null, n.getRetryAfterConnectionLoss(), true, v -> vm.setNetwork(n.withRetryAfterConnectionLoss(v)));
        group("Proxy");
        choice("Proxy type", ProxyType.values(), n.getProxyType(), true, v -> vm.setNetwork(n.withProxyType(v)));
        boolean proxy = n.getProxyType() != ProxyType.DISABLED;
        text("Host", n.getProxyHost(), false, false, v -> vm.setNetwork(n.withProxyHost(v.trim())), proxy, null, "", value -> !value.trim().isEmpty()&&!ProxyValidator.isValidHost(value)?"Enter a valid host or IP":null);
        text("Port", n.getProxyPort()==0?"":Integer.toString(n.getProxyPort()), true, false, v -> vm.setNetwork(n.withProxyPort(Integer.parseInt(v))), proxy, null, "1-65535", value -> {try{return ProxyValidator.isValidPort(Integer.parseInt(value))?null:"Port must be 1-65535";}catch(NumberFormatException e){return "Port must be 1-65535";}});
        text("Username (optional)", n.getProxyUsername(), false, false, v -> vm.setNetwork(n.withProxyUsername(v)), proxy, null, "", value -> null);
        text("Password (optional)", "", false, true, vm::setProxyPassword, proxy, n.getProxyPasswordSet()?"•••••• (stored securely)":"Not set", "", value -> null);
        note("Proxy passwords are stored using Android Keystore-backed encryption and are never included in exports.");
        View test=action("Test connection", null, 0, () -> testProxy(n.getProxyHost(),n.getProxyPort()));
        boolean canTest=proxy&&ProxyValidator.isValidHost(n.getProxyHost())&&ProxyValidator.isValidPort(n.getProxyPort());
        test.setEnabled(canTest);test.setAlpha(canTest?1f:.45f);
    }

    private void subtitles(SubtitleSettings s) {
        toggle("Download subtitles", null, s.getDownloadSubtitles(), true, v -> vm.setSubtitles(s.withDownloadSubtitles(v)));
        boolean on = s.getDownloadSubtitles();
        languageTagChoice("Preferred language",s.getPreferredLanguage(),false,on,v->vm.setSubtitles(s.withPreferredLanguage(v)));
        languageTagChoice("Fallback language",s.getFallbackLanguage(),true,on,v->vm.setSubtitles(s.withFallbackLanguage(v)));
        choice("Subtitle type", SubtitleTypePreference.values(), s.getSubtitleType(), on, v -> vm.setSubtitles(s.withSubtitleType(v)));
        choice("Subtitle format", SubtitleFormat.values(), s.getFormat(), on, v -> vm.setSubtitles(s.withFormat(v)));
        toggle("Embed in video", null, s.getEmbedInVideo(), on, v -> vm.setSubtitles(s.withEmbedInVideo(v)));
        toggle("Save as separate files", null, s.getSaveAsSeparateFiles(), on, v -> vm.setSubtitles(s.withSaveAsSeparateFiles(v)));
        toggle("Include all languages", null, s.getIncludeAllLanguages(), on, v -> vm.setSubtitles(s.withIncludeAllLanguages(v)));
        toggle("Add language code to filename", null, s.getAddLanguageCodeToFilename(), on, v -> vm.setSubtitles(s.withAddLanguageCodeToFilename(v)));
    }

    private void notifications(NotificationSettings n) {
        toggle("Show progress notification", null, n.getShowProgress(), true, v -> vm.setNotifications(n.withShowProgress(v)));
        toggle("Notify when each download completes", null, n.getNotifyOnEachComplete(), true, v -> vm.setNotifications(n.withNotifyOnEachComplete(v)));
        toggle("Notify when all downloads complete", null, n.getNotifyOnAllComplete(), true, v -> vm.setNotifications(n.withNotifyOnAllComplete(v)));
        toggle("Notify on failure", null, n.getNotifyOnFailure(), true, v -> vm.setNotifications(n.withNotifyOnFailure(v)));
        toggle("Sound", null, n.getSound(), true, v -> vm.setNotifications(n.withSound(v)));
        toggle("Vibration", null, n.getVibration(), true, v -> vm.setNotifications(n.withVibration(v)));
        toggle("Show notification actions", null, n.getShowActions(), true, v -> vm.setNotifications(n.withShowActions(v)));
        toggle("Group notifications", null, n.getGroupNotifications(), true, v -> vm.setNotifications(n.withGroupNotifications(v)));
    }

    private void appearance(AppearanceSettings a) {
        choice("Theme", AppTheme.values(), a.getTheme(), true, v -> vm.setAppearance(a.withTheme(v)));
        toggle("Dynamic color", null, a.getDynamicColor(), true, v -> vm.setAppearance(a.withDynamicColor(v)));
        languageChoice(a);
        toggle("Compact list", null, a.getCompactList(), true, v -> vm.setAppearance(a.withCompactList(v)));
        toggle("Show file size", null, a.getShowFileSize(), true, v -> vm.setAppearance(a.withShowFileSize(v)));
        toggle("Show speed", null, a.getShowSpeed(), true, v -> vm.setAppearance(a.withShowSpeed(v)));
        toggle("Show ETA", null, a.getShowEta(), true, v -> vm.setAppearance(a.withShowEta(v)));
        toggle("Reduce animations", null, a.getReduceAnimations(), true, v -> vm.setAppearance(a.withReduceAnimations(v)));
        toggle("High contrast", null, a.getHighContrast(), true, v -> vm.setAppearance(a.withHighContrast(v)));
    }

    private void history(HistorySettings h) {
        toggle("Keep download history", null, h.getKeepHistory(), true, v -> vm.setHistory(h.withKeepHistory(v)));
        choice("Retention", HistoryRetention.values(), h.getRetention(), h.getKeepHistory(), v -> vm.setHistory(h.withRetention(v)));
        toggle("Save recent URLs", null, h.getSaveRecentUrls(), true, v -> vm.setHistory(h.withSaveRecentUrls(v)));
        toggle("Save search history", null, h.getSaveSearchHistory(), true, v -> vm.setHistory(h.withSaveSearchHistory(v)));
        group("Clear data");
        action("Clear download history", null, R.drawable.ic_delete, () -> confirm("Clear download history?", "Downloaded files are not deleted.", vm::clearHistory));
        action("Clear recent URLs", null, 0, vm::clearRecentUrls);
        action("Clear search history", null, 0, vm::clearSearchHistory);
        group("Backup");
        action("Export settings to a file", null, 0, () -> settingsDocument.launch("kdownloader-settings.json"));
        action("Import settings from a file", null, 0, () -> openDocument.launch(new String[]{"application/json","text/plain"}));
        note("Exported files never contain passwords, tokens, or other credentials.");
    }

    private void advanced(ProcessingSettings p) {
        toggle("Enable post-download conversion", null, p.getEnableConversion(), true, v -> vm.setProcessing(p.withEnableConversion(v)));
        boolean on = p.getEnableConversion();
        toggle("Delete source files after successful conversion", null, p.getDeleteSourceAfterConversion(), on, v -> vm.setProcessing(p.withDeleteSourceAfterConversion(v)));
        toggle("Preserve source files when conversion fails", null, p.getPreserveSourceOnFailure(), on, v -> vm.setProcessing(p.withPreserveSourceOnFailure(v)));
        toggle("Prefer hardware acceleration", null, p.getPreferHardwareAcceleration(), on, v -> vm.setProcessing(p.withPreferHardwareAcceleration(v)));
        choice("Processing priority", ProcessingPriority.values(), p.getPriority(), on, v -> vm.setProcessing(p.withPriority(v)));
        toggle("Allow background processing", null, p.getAllowBackgroundProcessing(), on, v -> vm.setProcessing(p.withAllowBackgroundProcessing(v)));
        text("Maximum temporary storage (MB)", Integer.toString(p.getMaxTempStorageMb()), true, false, v -> vm.setProcessing(p.withMaxTempStorageMb(Integer.parseInt(v))), on, null, "", SettingsFragment::positiveIntegerError);
        group("Diagnostics");
        choice("Diagnostic logging", DiagnosticLogLevel.values(), p.getLogLevel(), true, v -> vm.setProcessing(p.withLogLevel(v)));
        action("Export diagnostic log", null, 0, () -> diagnosticsDocument.launch("kdownloader-diagnostics.txt"));
        action("Clear diagnostic log", null, 0, vm::clearDiagnosticLog);
    }

    private void permissions() {
        SystemStatus.Snapshot s = vm.getSystemStatus().getValue();
        if (s == null) s = new SystemStatus.Snapshot(false,false,false,false);
        String folderUri=vm.getSettingsValue().getStorage().getDownloadFolderUri();
        boolean folderOk=!folderUri.isEmpty()&&vm.getFolders().hasAccess(folderUri);
        status("Notifications",s.getNotificationsEnabled(),"Granted","Not granted","Open settings",()->launch(vm.getSystem().notificationSettingsIntent()));
        status("Download folder access",folderOk,folderUri.isEmpty()?"Not set":"Granted","Not granted","Manage",()->launch(vm.getSystem().appDetailsSettingsIntent()));
        status("Battery optimization exemption",s.getIgnoringBatteryOptimizations(),"Granted","Not granted","Open settings",()->launch(vm.getSystem().batteryOptimizationSettingsIntent()));
        status("Background activity",!s.getBackgroundRestricted(),"Allowed","Restricted","Open settings",()->launch(vm.getSystem().appDetailsSettingsIntent()));
        status("Media access when needed",s.getHasMediaAccess(),"Granted","Not granted","Open settings",()->launch(vm.getSystem().appDetailsSettingsIntent()));
        note(vm.getSystem().canDownloadReliablyInBackground()
                ? "This app can continue downloads reliably in the background."
                : "Background downloads may be interrupted. Exempt the app from battery optimization for reliable long downloads.");
        note("Long downloads run in a foreground service with an ongoing notification.");
        action("Re-check status", null, R.drawable.ic_refresh, vm::refreshStatus);
    }

    private void reset() {
        action("Reset download preferences",null,R.drawable.ic_restart_alt,()->confirm("Reset download preferences","Restores download type, format, quality, and metadata options to their defaults.",()->vm.resetCategory(SettingsCategory.DOWNLOAD)));
        action("Reset network preferences",null,R.drawable.ic_restart_alt,()->confirm("Reset network preferences","Restores network and proxy settings to their defaults and clears the saved proxy password.",()->vm.resetCategory(SettingsCategory.NETWORK)));
        action("Reset appearance preferences",null,R.drawable.ic_restart_alt,()->confirm("Reset appearance preferences","Restores theme, language, list, and motion options to their defaults.",()->vm.resetCategory(SettingsCategory.APPEARANCE)));
        action("Reset all settings",null,R.drawable.ic_restart_alt,()->confirm("Reset all settings","Restores every setting to its default. Downloaded files and history are NOT deleted.",vm::resetAll));
        note("Resetting settings never deletes downloaded files or history.");
    }

    private void about() {
        group("About");
        action("App", "KDownloader " + BuildConfig.VERSION_NAME, 0, () -> {});
        action("Build number", Integer.toString(BuildConfig.VERSION_CODE), 0, () -> {});
        action("Download engine", "yt-dlp " + BuildConfig.BUNDLED_YTDLP_VERSION, 0, () -> {});
        note("Updates are delivered through your app store. There is no in-app updater.");
        group("Legal");
        action("Open source licenses", null, 0, () -> openUrl("https://github.com/yt-dlp/yt-dlp"));
        action("Privacy policy", null, 0, () -> openUrl("https://example.com/privacy"));
        action("Terms of service", null, 0, () -> openUrl("https://example.com/terms"));
        group("Support");
        action("Help & FAQ", null, 0, () -> openUrl("https://example.com/help"));
        action("Report a problem", null, 0, () -> launch(new Intent(Intent.ACTION_SENDTO,
                Uri.parse("mailto:support@example.com")).putExtra(Intent.EXTRA_SUBJECT,
                "KDownloader " + BuildConfig.VERSION_NAME + " problem report")));
        action("Source code", null, 0, () -> openUrl("https://github.com/"));
        note("Diagnostic exports exclude credentials, private URLs, folder contents, and other personal information.");
    }

    private void category(String title, String subtitle, int icon, Route target) { action(title, subtitle, icon, () -> show(target)); }
    private void folder(String title, FolderSlot slot, String uri) {
        String summary = vm.getFolders().displayName(uri);
        if (!uri.isEmpty() && !vm.getFolders().hasAccess(uri)) summary += " — access revoked";
        if(uri.isEmpty())summary="Not set — tap to choose";
        else if(!vm.getFolders().hasAccess(uri))summary=vm.getFolders().displayName(uri)+" — access lost, tap to reselect";
        action(title, summary, R.drawable.ic_storage, () -> { pendingFolder=slot; folderPicker.launch(null); });
        if (!uri.isEmpty()) action("Clear \"" + title + "\"", null, R.drawable.ic_close, () -> vm.clearFolder(slot));
    }

    private void languageChoice(AppearanceSettings a) {
        View row = action("Language", LanguageManager.displayName(a.getLanguageTag()), 0, () -> {
            List<LanguageManager.Language> languages = LanguageManager.SUPPORTED;
            String[] labels = new String[languages.size()];
            int checked = 0;
            for(int i=0;i<labels.length;i++){labels[i]=languages.get(i).getDisplay();if(languages.get(i).getTag().equals(a.getLanguageTag()))checked=i;}
            new MaterialAlertDialogBuilder(requireContext()).setTitle("Language")
                    .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                        vm.setAppearance(a.withLanguageTag(languages.get(which).getTag())); dialog.dismiss();
                    }).show();
        });
    }

    private void languageTagChoice(String title,String selected,boolean includeNone,boolean enabled,Consumer<String> change){
        List<LabeledChoicePreferenceView.Option> options=new ArrayList<>();
        if(includeNone)options.add(new LabeledChoicePreferenceView.Option("","None"));
        for(LanguageManager.Language language:LanguageManager.SUPPORTED)if(!language.getTag().isEmpty())options.add(new LabeledChoicePreferenceView.Option(language.getTag(),language.getDisplay()));
        content.addView(new LabeledChoicePreferenceView(requireContext(),title,options,selected,enabled,change));
    }

    private <T extends Enum<T> & SettingOption> void choice(String title, T[] values, T selected,
                                                              boolean enabled, Consumer<T> change) {
        content.addView(new SingleChoicePreferenceView<>(requireContext(),title,values,selected,enabled,change));
    }

    private void toggle(String title, String subtitle, boolean checked, boolean enabled, Consumer<Boolean> change) {
        content.addView(new SwitchPreferenceView(requireContext(),title,subtitle,checked,enabled,change));
    }

    private View action(String title, String subtitle, int icon, Runnable click) {
        ClickablePreferenceView row=new ClickablePreferenceView(requireContext(),title,subtitle,icon,click);content.addView(row);return row;
    }

    private void text(String title,String value,boolean numeric,boolean password,Consumer<String> change){text(title,value,numeric,password,change,true);}
    private void text(String title,String value,boolean numeric,boolean password,Consumer<String> change,boolean enabled){
        View row=action(title,password&&!value.isEmpty()?"••••••••":value,0,()->{
            EditText input=new EditText(requireContext());input.setText(value);input.setSelectAllOnFocus(true);
            if(numeric)input.setInputType(InputType.TYPE_CLASS_NUMBER);if(password)input.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);
            new MaterialAlertDialogBuilder(requireContext()).setTitle(title).setView(input).setNegativeButton(R.string.cancel,null)
                    .setPositiveButton(android.R.string.ok,(d,w)->change.accept(input.getText().toString())).show();
        });row.setEnabled(enabled);row.setAlpha(enabled?1f:.45f);
    }

    private void text(String title,String value,boolean numeric,boolean password,Consumer<String> change,
                      boolean enabled,String summary,String placeholder,Function<String,String> validate){
        content.addView(new TextEntryPreferenceView(requireContext(),title,value,numeric,password,enabled,summary,placeholder,validate,change));
    }

    private static String positiveIntegerError(String value){try{return Integer.parseInt(value)>=1?null:"Enter a value of 1 or more";}catch(NumberFormatException e){return "Enter a value of 1 or more";}}
    private static String timeError(String value){return parseMinutesOrNull(value)==null?"Use 24-hour HH:MM":null;}
    private static String formatMinutes(int minutes){int value=Math.max(0,Math.min(1439,minutes));return String.format(java.util.Locale.US,"%02d:%02d",value/60,value%60);}
    private static Integer parseMinutesOrNull(String text){String[] parts=text.trim().split(":",-1);if(parts.length!=2)return null;try{int hour=Integer.parseInt(parts[0]),minute=Integer.parseInt(parts[1]);return hour>=0&&hour<=23&&minute>=0&&minute<=59?hour*60+minute:null;}catch(NumberFormatException e){return null;}}
    private static int parseMinutes(String text){Integer value=parseMinutesOrNull(text);if(value==null)throw new IllegalArgumentException("Invalid time");return value;}

    private void testProxy(String host,int port){
        Toast.makeText(requireContext(),"Testing…",Toast.LENGTH_SHORT).show();
        AppExecutors.io().execute(()->{String result;try(Socket socket=new Socket()){socket.connect(new InetSocketAddress(host,port),5000);result="Connection succeeded";}catch(Exception e){result="Connection failed: "+(e.getMessage()==null?"unreachable":e.getMessage());}String message=result;AppExecutors.main().execute(()->{if(isAdded())Toast.makeText(requireContext(),message,Toast.LENGTH_LONG).show();});});
    }

    private void slider(String title,int value,int min,int max,Consumer<Integer> change){
        content.addView(new IntSliderPreferenceView(requireContext(),title,value,min,max,change));
    }

    private void group(String title){content.addView(new PreferenceGroupTitleView(requireContext(),title));}
    private void note(String text){content.addView(new PreferenceNoteView(requireContext(),text));}
    private void status(String title,boolean ok,String okText,String notOkText,String actionText,Runnable action){action(title,ok?okText:notOkText+" — tap to "+actionText,ok?R.drawable.ic_check_circle:R.drawable.ic_error_outline,action);}
    private void confirm(String title,String message,Runnable action){ConfirmDialogView.show(requireContext(),title,message,action);}
    private void launch(Intent intent){if(intent==null)return;try{startActivity(intent);}catch(Throwable e){Toast.makeText(requireContext(),"No app can handle this action",Toast.LENGTH_SHORT).show();}}
    private void openUrl(String url){launch(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
    private android.graphics.drawable.Drawable getDrawable(int id){return ContextCompat.getDrawable(requireContext(),id);}
    private String title(Route r){
        switch(r){
            case HOME:return "Settings";
            case DOWNLOAD:return "Download preferences";
            case STORAGE:return "Storage & files";
            case BEHAVIOR:return "Download behavior";
            case NETWORK:return "Network";
            case SUBTITLES:return "Subtitles & captions";
            case NOTIFICATIONS:return "Notifications";
            case APPEARANCE:return "Appearance & accessibility";
            case HISTORY:return "History & privacy";
            case ADVANCED:return "Advanced processing";
            case PERMISSIONS:return "Permissions & status";
            case RESET:return "Reset";
            case ABOUT:return "About & support";
            default:throw new IllegalArgumentException("Unknown settings route: "+r);
        }
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void write(Uri uri,String text){try(OutputStream out=requireContext().getContentResolver().openOutputStream(uri)){if(out!=null)out.write(text.getBytes(StandardCharsets.UTF_8));}catch(Exception e){AppExecutors.main().execute(()->Toast.makeText(requireContext(),"Export failed",Toast.LENGTH_SHORT).show());}}
    private void importSettings(Uri uri){try(InputStream in=requireContext().getContentResolver().openInputStream(uri)){if(in==null)return;ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1)bytes.write(buffer,0,count);String text=new String(bytes.toByteArray(),StandardCharsets.UTF_8);SettingsRepository.ImportResult result=vm.importJson(text);AppExecutors.main().execute(()->Toast.makeText(requireContext(),result.toString(),Toast.LENGTH_LONG).show());}catch(Exception e){AppExecutors.main().execute(()->Toast.makeText(requireContext(),"Could not read the file",Toast.LENGTH_SHORT).show());}}
}
