package com.kira.kdownloader.settings.ui;

import android.app.Application;
import android.content.Context;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kira.kdownloader.BuildConfig;
import com.kira.kdownloader.data.AppDatabase;
import com.kira.kdownloader.settings.AppSettings;
import com.kira.kdownloader.settings.AppearanceSettings;
import com.kira.kdownloader.settings.BehaviorSettings;
import com.kira.kdownloader.settings.DownloadSettings;
import com.kira.kdownloader.settings.HistorySettings;
import com.kira.kdownloader.settings.NetworkSettings;
import com.kira.kdownloader.settings.NotificationSettings;
import com.kira.kdownloader.settings.ProcessingSettings;
import com.kira.kdownloader.settings.SettingsCategory;
import com.kira.kdownloader.settings.SettingsRepository;
import com.kira.kdownloader.settings.StorageSettings;
import com.kira.kdownloader.settings.SubtitleSettings;
import com.kira.kdownloader.settings.platform.FolderAccessManager;
import com.kira.kdownloader.settings.platform.SystemStatus;
import com.kira.kdownloader.settings.store.KeystoreSecureStore;
import com.kira.kdownloader.settings.store.SharedPreferencesKeyValueStore;
import com.kira.kdownloader.util.AppExecutors;

import java.io.File;

public final class SettingsViewModel extends AndroidViewModel {
    private static final String HISTORY_CACHE = "history_cache";
    private static final String KEY_RECENT_URLS = "recent_urls";
    private static final String KEY_SEARCH_HISTORY = "search_history";

    private final SettingsRepository repository;
    private final FolderAccessManager folders;
    private final SystemStatus system;
    private final LiveData<AppSettings> settingsLive;
    private final MutableLiveData<SystemStatus.Snapshot> systemStatus;

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        repository = new SettingsRepository(new SharedPreferencesKeyValueStore(application),
                new KeystoreSecureStore(application));
        folders = new FolderAccessManager(application);
        system = new SystemStatus(application);
        settingsLive = repository.observe();
        systemStatus = new MutableLiveData<>(system.snapshot());
    }

    public LiveData<AppSettings> getSettingsLive() { return settingsLive; }
    public AppSettings getSettingsValue() { return repository.read(); }
    public LiveData<SystemStatus.Snapshot> getSystemStatus() { return systemStatus; }
    public FolderAccessManager getFolders() { return folders; }
    public SystemStatus getSystem() { return system; }
    public void refreshStatus() { systemStatus.setValue(system.snapshot()); }

    public void setDownload(DownloadSettings value) { repository.update(s -> s.withDownload(value)); }
    public void setStorage(StorageSettings value) { repository.update(s -> s.withStorage(value)); }
    public void setBehavior(BehaviorSettings value) { repository.update(s -> s.withBehavior(value)); }
    public void setNetwork(NetworkSettings value) { repository.update(s -> s.withNetwork(value)); }
    public void setSubtitles(SubtitleSettings value) { repository.update(s -> s.withSubtitles(value)); }
    public void setNotifications(NotificationSettings value) { repository.update(s -> s.withNotifications(value)); }
    public void setAppearance(AppearanceSettings value) { repository.update(s -> s.withAppearance(value)); }
    public void setHistory(HistorySettings value) { repository.update(s -> s.withHistory(value)); }
    public void setProcessing(ProcessingSettings value) { repository.update(s -> s.withProcessing(value)); }
    public void setProxyPassword(String value) { repository.setProxyPassword(value); }

    public void onFolderSelected(FolderSlot slot, Uri uri) {
        folders.persist(uri);
        StorageSettings storage = repository.read().getStorage();
        String value = uri.toString();
        if (slot == FolderSlot.DOWNLOAD) storage = storage.withDownloadFolderUri(value);
        else if (slot == FolderSlot.VIDEO) storage = storage.withVideoFolderUri(value);
        else if (slot == FolderSlot.AUDIO) storage = storage.withAudioFolderUri(value);
        else storage = storage.withTempFolderUri(value);
        setStorage(storage);
    }

    public void clearFolder(FolderSlot slot) {
        StorageSettings storage = repository.read().getStorage();
        String current;
        if (slot == FolderSlot.DOWNLOAD) current = storage.getDownloadFolderUri();
        else if (slot == FolderSlot.VIDEO) current = storage.getVideoFolderUri();
        else if (slot == FolderSlot.AUDIO) current = storage.getAudioFolderUri();
        else current = storage.getTempFolderUri();
        folders.release(current);
        if (slot == FolderSlot.DOWNLOAD) storage = storage.withDownloadFolderUri("");
        else if (slot == FolderSlot.VIDEO) storage = storage.withVideoFolderUri("");
        else if (slot == FolderSlot.AUDIO) storage = storage.withAudioFolderUri("");
        else storage = storage.withTempFolderUri("");
        setStorage(storage);
    }

    public void resetCategory(SettingsCategory category) { repository.resetCategory(category); }
    public void resetAll() { repository.resetAll(); }
    public String exportJson() { return repository.exportToJson(); }
    public SettingsRepository.ImportResult importJson(String json) { return repository.importFromJson(json); }

    public long recoverableTempBytes() {
        long total = 0;
        for (File file : cacheDirs()) total += dirSize(file);
        return total;
    }

    public long clearTempFiles() {
        long before = recoverableTempBytes();
        for (File dir : cacheDirs()) {
            File[] files = dir.listFiles();
            if (files != null) for (File file : files) deleteTree(file);
        }
        return before;
    }

    public void clearHistory() {
        AppExecutors.io().execute(() -> AppDatabase.get(getApplication()).downloadDao().clearAll());
    }

    public void clearAllAppData() {
        clearHistory();
        AppExecutors.io().execute(this::clearTempFiles);
        resetAll();
    }

    public void clearRecentUrls() { prefs().edit().remove(KEY_RECENT_URLS).apply(); }
    public void clearSearchHistory() { prefs().edit().remove(KEY_SEARCH_HISTORY).apply(); }

    public String buildDiagnostics() {
        AppSettings value = repository.read();
        return "KDownloader diagnostics\n"
                + "app.version=" + BuildConfig.VERSION_NAME + '\n'
                + "app.build=" + BuildConfig.VERSION_CODE + '\n'
                + "ytdlp.bundled=" + BuildConfig.BUNDLED_YTDLP_VERSION + '\n'
                + "android.sdk=" + android.os.Build.VERSION.SDK_INT + '\n'
                + "device.model=" + android.os.Build.MODEL + '\n'
                + "device.manufacturer=" + android.os.Build.MANUFACTURER + '\n'
                + "log.level=" + value.getProcessing().getLogLevel().getKey() + '\n'
                + "network.allowed=" + value.getNetwork().getAllowedNetworks().getKey() + '\n'
                + "proxy.enabled=" + (!"disabled".equals(value.getNetwork().getProxyType().getKey())) + '\n'
                + "notifications.enabled=" + system.notificationsEnabled() + '\n'
                + "battery.exempt=" + system.ignoringBatteryOptimizations() + '\n'
                + "background.restricted=" + system.backgroundRestricted() + '\n';
    }

    public void clearDiagnosticLog() {
        AppExecutors.io().execute(() -> new File(getApplication().getCacheDir(), "diagnostics.log").delete());
    }

    private android.content.SharedPreferences prefs() {
        return getApplication().getSharedPreferences(HISTORY_CACHE, Context.MODE_PRIVATE);
    }
    private File[] cacheDirs() {
        File external = getApplication().getExternalCacheDir();
        return external == null ? new File[]{getApplication().getCacheDir()}
                : new File[]{getApplication().getCacheDir(), external};
    }
    private static long dirSize(File file) {
        if (file.isFile()) return file.length();
        long total = 0;
        File[] children = file.listFiles();
        if (children != null) for (File child : children) total += dirSize(child);
        return total;
    }
    private static boolean deleteTree(File file) {
        File[] children = file.listFiles();
        if (children != null) for (File child : children) deleteTree(child);
        return !file.exists() || file.delete();
    }
}
