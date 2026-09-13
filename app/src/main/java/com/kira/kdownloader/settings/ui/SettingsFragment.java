package com.kira.kdownloader.settings.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.card.MaterialCardView;
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
    private enum Route { HOME, DOWNLOADS, STORAGE, APPEARANCE, NETWORK, ADVANCED_ABOUT }

    private SettingsViewModel vm;
    private MaterialToolbar toolbar;
    private LinearLayout content;
    private LinearLayout currentGroup;
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
        vm = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
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
        if (vm == null) {
            vm = new androidx.lifecycle.ViewModelProvider(requireActivity()).get(SettingsViewModel.class);
        }
        toolbar.setNavigationOnClickListener(ignored -> show(Route.HOME));
        vm.getSettingsLive().observe(getViewLifecycleOwner(), ignored -> render());
        vm.getSystemStatus().observe(getViewLifecycleOwner(), ignored -> {
            if (route == Route.ADVANCED_ABOUT) render();
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
        currentGroup = null;
        AppSettings settings = vm.getSettingsValue();
        toolbar.setNavigationIcon(route == Route.HOME ? null : getDrawable(R.drawable.ic_arrow_back));
        toolbar.setTitle(title(route));
        toolbar.setTitleTextAppearance(requireContext(), route == Route.HOME
                ? R.style.TextAppearance_KDownloader_Display
                : R.style.TextAppearance_KDownloader_HeadlineSmall);
        switch (route) {
            case HOME: home(); break;
            case DOWNLOADS: downloads(settings.getDownload(), settings.getSubtitles()); break;
            case STORAGE: storage(settings.getStorage()); break;
            case APPEARANCE: appearance(settings.getAppearance()); break;
            case NETWORK: network(settings.getNetwork()); break;
            case ADVANCED_ABOUT: advancedAbout(settings.getBehavior(), settings.getProcessing(), settings.getHistory(), settings.getNotifications()); break;
        }
    }

    private void home() {
        category("Downloads & Quality", "Resolution, audio format, subtitles & metadata", R.drawable.ic_video_library, Route.DOWNLOADS);
        category("Storage & Files", "Download folder and cache management", R.drawable.ic_storage, Route.STORAGE);
        category("Appearance", "Theme, dynamic colors, language and motion", R.drawable.ic_palette, Route.APPEARANCE);
        category("Network & Connection", "Wi-Fi, mobile data limits and proxy", R.drawable.ic_wifi, Route.NETWORK);
        category("Advanced & About", "Queue, background status, app & engine info", R.drawable.ic_tune, Route.ADVANCED_ABOUT);
    }

    private void downloads(DownloadSettings d, SubtitleSettings s) {
        group("Media Quality & Formats");
        choice("Default video quality", VideoQuality.values(), d.getVideoQuality(), true, v -> vm.setDownload(d.withVideoQuality(v)));
        choice("Video format", VideoFormat.values(), d.getVideoFormat(), true, v -> vm.setDownload(d.withVideoFormat(v)));
        choice("Default audio quality", AudioQuality.values(), d.getAudioQuality(), true, v -> vm.setDownload(d.withAudioQuality(v)));
        choice("Audio format", AudioFormat.values(), d.getAudioFormat(), true, v -> vm.setDownload(d.withAudioFormat(v)));
        toggle("Ask quality before each download", "Show format selection dialog every time", d.getAskQualityBeforeEachDownload(), true, v -> vm.setDownload(d.withAskQualityBeforeEachDownload(v)));
        toggle("Auto fallback quality", "Use closest available quality if requested is missing", d.getAutoFallbackQuality(), true, v -> vm.setDownload(d.withAutoFallbackQuality(v)));

        group("Subtitles");
        toggle("Download subtitles", "Fetch subtitles when available", s.getDownloadSubtitles(), true, v -> vm.setSubtitles(s.withDownloadSubtitles(v)));
        if (s.getDownloadSubtitles()) {
            languageTagChoice("Preferred subtitle language", s.getPreferredLanguage(), false, true, v -> vm.setSubtitles(s.withPreferredLanguage(v)));
            choice("Subtitle format", SubtitleFormat.values(), s.getFormat(), true, v -> vm.setSubtitles(s.withFormat(v)));
            toggle("Embed in video", "Mux subtitles directly into video container", s.getEmbedInVideo(), true, v -> vm.setSubtitles(s.withEmbedInVideo(v)));
        }

        group("Metadata & Artwork");
        toggle("Download thumbnail", null, d.getDownloadThumbnail(), true, v -> vm.setDownload(d.withDownloadThumbnail(v)));
        toggle("Embed thumbnail in audio", null, d.getEmbedThumbnail(), d.getDownloadThumbnail(), v -> vm.setDownload(d.withEmbedThumbnail(v)));
        toggle("Embed tags and metadata", "Title, artist, album and track info", d.getEmbedMetadata(), true, v -> vm.setDownload(d.withEmbedMetadata(v)));
    }

    private void storage(StorageSettings s) {
        group("Download Folder");
        folder("Download location", FolderSlot.DOWNLOAD, s.getDownloadFolderUri());
        note("Available storage: " + FolderAccessManager.formatBytes(vm.getFolders().availableBytes()));
        toggle("Low storage warning", "Warn before starting downloads if space is low", s.getWarnOnLowSpace(), true, v -> vm.setStorage(s.withWarnOnLowSpace(v)));

        group("Storage Management");
        action("Clear temporary files", "Recoverable cache: " + FolderAccessManager.formatBytes(vm.recoverableTempBytes()), R.drawable.ic_delete, () -> confirm("Clear temporary files?", "This removes temporary cache files only. Completed downloads are never deleted.", () -> AppExecutors.io().execute(vm::clearTempFiles)));
    }

    private void appearance(AppearanceSettings a) {
        group("Theme & Colors");
        choice("Theme", AppTheme.values(), a.getTheme(), true, v -> vm.setAppearance(a.withTheme(v)));
        toggle("Dynamic color", "Use Material You colors matching system wallpaper", a.getDynamicColor(), true, v -> vm.setAppearance(a.withDynamicColor(v)));
        languageChoice(a);

        group("Display & Accessibility");
        toggle("Show file size in lists", null, a.getShowFileSize(), true, v -> vm.setAppearance(a.withShowFileSize(v)));
        toggle("Show download speed", null, a.getShowSpeed(), true, v -> vm.setAppearance(a.withShowSpeed(v)));
        toggle("Show estimated completion time", null, a.getShowEta(), true, v -> vm.setAppearance(a.withShowEta(v)));
        toggle("High contrast", "Boost contrast for text and controls", a.getHighContrast(), true, v -> vm.setAppearance(a.withHighContrast(v)));
        toggle("Reduce animations", "Minimize transitions and motion effects", a.getReduceAnimations(), true, v -> vm.setAppearance(a.withReduceAnimations(v)));
    }

    private void network(NetworkSettings n) {
        group("Connection");
        choice("Allowed networks", NetworkType.values(), n.getAllowedNetworks(), true, v -> vm.setNetwork(n.withAllowedNetworks(v)));
        toggle("Allow roaming", "Download over roaming networks", n.getAllowRoaming(), true, v -> vm.setNetwork(n.withAllowRoaming(v)));
        toggle("Confirm on mobile data", "Ask for confirmation before using cellular data", n.getConfirmMobileData(), true, v -> vm.setNetwork(n.withConfirmMobileData(v)));

        group("Proxy (Optional)");
        choice("Proxy type", ProxyType.values(), n.getProxyType(), true, v -> vm.setNetwork(n.withProxyType(v)));
        boolean proxy = n.getProxyType() != ProxyType.DISABLED;
        if (proxy) {
            text("Host", n.getProxyHost(), false, false, v -> vm.setNetwork(n.withProxyHost(v.trim())), true, null, "", value -> !value.trim().isEmpty() && !ProxyValidator.isValidHost(value) ? "Enter a valid host or IP" : null);
            text("Port", n.getProxyPort() == 0 ? "" : Integer.toString(n.getProxyPort()), true, false, v -> vm.setNetwork(n.withProxyPort(Integer.parseInt(v))), true, null, "1-65535", value -> { try { return ProxyValidator.isValidPort(Integer.parseInt(value)) ? null : "Port must be 1-65535"; } catch (NumberFormatException e) { return "Port must be 1-65535"; } });
            text("Username (optional)", n.getProxyUsername(), false, false, v -> vm.setNetwork(n.withProxyUsername(v)), true, null, "", value -> null);
            text("Password (optional)", "", false, true, vm::setProxyPassword, true, n.getProxyPasswordSet() ? "•••••• (stored securely)" : "Not set", "", value -> null);
            View test = action("Test connection", null, 0, () -> testProxy(n.getProxyHost(), n.getProxyPort()));
            boolean canTest = ProxyValidator.isValidHost(n.getProxyHost()) && ProxyValidator.isValidPort(n.getProxyPort());
            test.setEnabled(canTest);
            test.setAlpha(canTest ? 1f : .38f);
        }
    }

    private void advancedAbout(BehaviorSettings b, ProcessingSettings p, HistorySettings h, NotificationSettings notif) {
        group("Queue & Background");
        slider("Simultaneous downloads", b.getMaxSimultaneousDownloads(), 1, 5, v -> vm.setBehavior(b.withMaxSimultaneousDownloads(v)));
        toggle("Auto-resume downloads", "Resume interrupted downloads when connection returns", b.getAutoResumeInterrupted(), true, v -> vm.setBehavior(b.withAutoResumeInterrupted(v)));
        toggle("Show download notifications", null, notif.getShowProgress(), true, v -> vm.setNotifications(notif.withShowProgress(v)));
        SystemStatus.Snapshot s = vm.getSystemStatus().getValue();
        if (s == null) s = new SystemStatus.Snapshot(false, false, false, false);
        status("Background activity", !s.getBackgroundRestricted(), "Allowed", "Restricted", "Open settings", () -> launch(vm.getSystem().appDetailsSettingsIntent()));
        status("Battery optimization exemption", s.getIgnoringBatteryOptimizations(), "Granted", "Not granted", "Open settings", () -> launch(vm.getSystem().batteryOptimizationSettingsIntent()));

        group("Backup & Data");
        action("Clear download history", null, R.drawable.ic_delete, () -> confirm("Clear download history?", "Downloaded files are not deleted.", vm::clearHistory));
        action("Export settings", "Save preferences to a file", 0, () -> settingsDocument.launch("kdownloader-settings.json"));
        action("Import settings", "Restore preferences from a file", 0, () -> openDocument.launch(new String[]{"application/json", "text/plain"}));
        action("Export diagnostics", "Logs to help diagnose issues", 0, () -> diagnosticsDocument.launch("kdownloader-diagnostics.txt"));

        group("Reset");
        action("Reset all settings", "Restore default configuration", R.drawable.ic_restart_alt, () -> confirm("Reset all settings?", "Restores all settings to their default values. Downloaded files and history are never deleted.", vm::resetAll));

        group("About");
        action("App version", "KDownloader " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")", 0, () -> {});
        action("Download engine", "yt-dlp " + BuildConfig.BUNDLED_YTDLP_VERSION, 0, () -> {});
        action("Open source licenses", null, 0, () -> openUrl("https://github.com/yt-dlp/yt-dlp"));
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
        addPreference(new LabeledChoicePreferenceView(requireContext(),title,options,selected,enabled,change));
    }

    private <T extends Enum<T> & SettingOption> void choice(String title, T[] values, T selected,
                                                              boolean enabled, Consumer<T> change) {
        addPreference(new SingleChoicePreferenceView<>(requireContext(),title,values,selected,enabled,change));
    }

    private void toggle(String title, String subtitle, boolean checked, boolean enabled, Consumer<Boolean> change) {
        addPreference(new SwitchPreferenceView(requireContext(),title,subtitle,checked,enabled,change));
    }

    private View action(String title, String subtitle, int icon, Runnable click) {
        ClickablePreferenceView row=new ClickablePreferenceView(requireContext(),title,subtitle,icon,click);addPreference(row);return row;
    }

    private void text(String title,String value,boolean numeric,boolean password,Consumer<String> change,
                      boolean enabled,String summary,String placeholder,Function<String,String> validate){
        addPreference(new TextEntryPreferenceView(requireContext(),title,value,numeric,password,enabled,summary,placeholder,validate,change));
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
        addPreference(new IntSliderPreferenceView(requireContext(),title,value,min,max,change));
    }

    private void group(String title){
        content.addView(new PreferenceGroupTitleView(requireContext(),title));
        startGroup();
    }
    private void note(String text){addPreference(new PreferenceNoteView(requireContext(),text));}

    private void startGroup() {
        MaterialCardView card = (MaterialCardView) getLayoutInflater().inflate(
                R.layout.view_settings_group, content, false);
        currentGroup = card.findViewById(R.id.settings_group_rows);
        content.addView(card);
    }

    private void addPreference(View preference) {
        if (currentGroup == null) startGroup();
        if (currentGroup.getChildCount() > 0) {
            getLayoutInflater().inflate(R.layout.view_settings_divider, currentGroup, true);
        }
        currentGroup.addView(preference, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
    }
    private void status(String title,boolean ok,String okText,String notOkText,String actionText,Runnable action){action(title,ok?okText:notOkText+" — tap to "+actionText,ok?R.drawable.ic_check_circle:R.drawable.ic_error_outline,action);}
    private void confirm(String title,String message,Runnable action){ConfirmDialogView.show(requireContext(),title,message,action);}
    private void launch(Intent intent){if(intent==null)return;try{startActivity(intent);}catch(Throwable e){Toast.makeText(requireContext(),"No app can handle this action",Toast.LENGTH_SHORT).show();}}
    private void openUrl(String url){launch(new Intent(Intent.ACTION_VIEW,Uri.parse(url)));}
    private android.graphics.drawable.Drawable getDrawable(int id){return ContextCompat.getDrawable(requireContext(),id);}
    private String title(Route r) {
        switch (r) {
            case HOME: return "Settings";
            case DOWNLOADS: return "Downloads & Quality";
            case STORAGE: return "Storage & Files";
            case APPEARANCE: return "Appearance";
            case NETWORK: return "Network & Connection";
            case ADVANCED_ABOUT: return "Advanced & About";
            default: throw new IllegalArgumentException("Unknown settings route: " + r);
        }
    }
    private int dp(int value){return Math.round(value*getResources().getDisplayMetrics().density);}
    private void write(Uri uri,String text){try(OutputStream out=requireContext().getContentResolver().openOutputStream(uri)){if(out!=null)out.write(text.getBytes(StandardCharsets.UTF_8));}catch(Exception e){AppExecutors.main().execute(()->Toast.makeText(requireContext(),"Export failed",Toast.LENGTH_SHORT).show());}}
    private void importSettings(Uri uri){try(InputStream in=requireContext().getContentResolver().openInputStream(uri)){if(in==null)return;ByteArrayOutputStream bytes=new ByteArrayOutputStream();byte[] buffer=new byte[4096];int count;while((count=in.read(buffer))!=-1)bytes.write(buffer,0,count);String text=new String(bytes.toByteArray(),StandardCharsets.UTF_8);SettingsRepository.ImportResult result=vm.importJson(text);AppExecutors.main().execute(()->Toast.makeText(requireContext(),result.toString(),Toast.LENGTH_LONG).show());}catch(Exception e){AppExecutors.main().execute(()->Toast.makeText(requireContext(),"Could not read the file",Toast.LENGTH_SHORT).show());}}
}
