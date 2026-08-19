package com.kira.kdownloader.settings.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import com.kira.kdownloader.data.AppDatabase
import com.kira.kdownloader.settings.AppSettings
import com.kira.kdownloader.settings.AppearanceSettings
import com.kira.kdownloader.settings.BehaviorSettings
import com.kira.kdownloader.settings.DownloadSettings
import com.kira.kdownloader.settings.HistorySettings
import com.kira.kdownloader.settings.NetworkSettings
import com.kira.kdownloader.settings.NotificationSettings
import com.kira.kdownloader.settings.ProcessingSettings
import com.kira.kdownloader.settings.SettingsCategory
import com.kira.kdownloader.settings.SettingsRepository
import com.kira.kdownloader.settings.StorageSettings
import com.kira.kdownloader.settings.SubtitleSettings
import com.kira.kdownloader.settings.platform.FolderAccessManager
import com.kira.kdownloader.settings.platform.SystemStatus
import com.kira.kdownloader.settings.store.KeystoreSecureStore
import com.kira.kdownloader.settings.store.SharedPreferencesKeyValueStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

/** Folder slots the user can configure (Section 3). */
enum class FolderSlot { DOWNLOAD, VIDEO, AUDIO, TEMP }

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(
        SharedPreferencesKeyValueStore(application),
        KeystoreSecureStore(application),
    )

    val folders = FolderAccessManager(application)
    val system = SystemStatus(application)

    val settingsLive = repository.observe()
    private val settingsState = MutableStateFlow(repository.read())
    private val settingsObserver = Observer<AppSettings> { settingsState.value = it }
    val settings: StateFlow<AppSettings> = settingsState.asStateFlow()

    private val statusState = MutableStateFlow(system.snapshot())
    val systemStatus: StateFlow<SystemStatus.Snapshot> = statusState.asStateFlow()

    init {
        settingsLive.observeForever(settingsObserver)
    }

    override fun onCleared() {
        settingsLive.removeObserver(settingsObserver)
        super.onCleared()
    }

    fun refreshStatus() {
        statusState.value = system.snapshot()
    }

    // ---- Grouped updates (each persists immediately) -------------------------

    fun setDownload(value: DownloadSettings) = repository.update { it.withDownload(value) }
    fun setStorage(value: StorageSettings) = repository.update { it.withStorage(value) }
    fun setBehavior(value: BehaviorSettings) = repository.update { it.withBehavior(value) }
    fun setNetwork(value: NetworkSettings) = repository.update { it.withNetwork(value) }
    fun setSubtitles(value: SubtitleSettings) = repository.update { it.withSubtitles(value) }
    fun setNotifications(value: NotificationSettings) = repository.update { it.withNotifications(value) }
    fun setAppearance(value: AppearanceSettings) = repository.update { it.withAppearance(value) }
    fun setHistory(value: HistorySettings) = repository.update { it.withHistory(value) }
    fun setProcessing(value: ProcessingSettings) = repository.update { it.withProcessing(value) }

    // ---- Proxy credentials ---------------------------------------------------

    fun setProxyPassword(password: String?) = repository.setProxyPassword(password)

    // ---- Folders (SAF) -------------------------------------------------------

    fun onFolderSelected(slot: FolderSlot, uri: Uri) {
        folders.persist(uri)
        val value = uri.toString()
        val storage = settings.value.storage
        setStorage(
            when (slot) {
                FolderSlot.DOWNLOAD -> storage.withDownloadFolderUri(value)
                FolderSlot.VIDEO -> storage.withVideoFolderUri(value)
                FolderSlot.AUDIO -> storage.withAudioFolderUri(value)
                FolderSlot.TEMP -> storage.withTempFolderUri(value)
            },
        )
    }

    fun clearFolder(slot: FolderSlot) {
        val storage = settings.value.storage
        val current = when (slot) {
            FolderSlot.DOWNLOAD -> storage.downloadFolderUri
            FolderSlot.VIDEO -> storage.videoFolderUri
            FolderSlot.AUDIO -> storage.audioFolderUri
            FolderSlot.TEMP -> storage.tempFolderUri
        }
        folders.release(current)
        setStorage(
            when (slot) {
                FolderSlot.DOWNLOAD -> storage.withDownloadFolderUri("")
                FolderSlot.VIDEO -> storage.withVideoFolderUri("")
                FolderSlot.AUDIO -> storage.withAudioFolderUri("")
                FolderSlot.TEMP -> storage.withTempFolderUri("")
            },
        )
    }

    // ---- Reset (Section 13) --------------------------------------------------

    fun resetCategory(category: SettingsCategory) = repository.resetCategory(category)
    fun resetAll() = repository.resetAll()

    // ---- Export / Import (Section 9) -----------------------------------------

    fun exportJson(): String = repository.exportToJson()

    fun importJson(json: String): SettingsRepository.ImportResult = repository.importFromJson(json)

    // ---- Destructive maintenance actions -------------------------------------

    /** Size of removable temporary/cache data (Section 3 "Clear temporary files"). */
    suspend fun recoverableTempBytes(): Long = withContext(Dispatchers.IO) {
        cacheDirs().sumOf { dirSize(it) }
    }

    /** Deletes temporary/cache files. Never touches completed downloads (Section 3). */
    suspend fun clearTempFiles(): Long = withContext(Dispatchers.IO) {
        val before = cacheDirs().sumOf { dirSize(it) }
        cacheDirs().forEach { dir -> dir.listFiles()?.forEach { it.deleteRecursively() } }
        before
    }

    fun clearHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { AppDatabase.get(getApplication()).downloadDao().clearAll() }
        }
    }

    /** Resets settings and clears caches + history. Downloaded media files are left in place. */
    fun clearAllAppData() {
        clearHistory()
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { cacheDirs().forEach { d -> d.listFiles()?.forEach { it.deleteRecursively() } } }
        }
        resetAll()
    }

    /** Clears the cached recently-used URLs (Section 9). Does not affect download history. */
    fun clearRecentUrls() {
        getApplication<Application>()
            .getSharedPreferences(HISTORY_CACHE, android.content.Context.MODE_PRIVATE)
            .edit().remove(KEY_RECENT_URLS).apply()
    }

    /** Clears the cached search history (Section 9). */
    fun clearSearchHistory() {
        getApplication<Application>()
            .getSharedPreferences(HISTORY_CACHE, android.content.Context.MODE_PRIVATE)
            .edit().remove(KEY_SEARCH_HISTORY).apply()
    }

    /**
     * Builds a diagnostic report that deliberately excludes credentials, private URLs, folder
     * contents, and PII (Section 12). Only non-identifying app/device metadata is included.
     */
    fun buildDiagnostics(): String {
        val s = settings.value
        return buildString {
            appendLine("KDownloader diagnostics")
            appendLine("app.version=${com.kira.kdownloader.BuildConfig.VERSION_NAME}")
            appendLine("app.build=${com.kira.kdownloader.BuildConfig.VERSION_CODE}")
            appendLine("ytdlp.bundled=${com.kira.kdownloader.BuildConfig.BUNDLED_YTDLP_VERSION}")
            appendLine("android.sdk=${android.os.Build.VERSION.SDK_INT}")
            appendLine("device.model=${android.os.Build.MODEL}")
            appendLine("device.manufacturer=${android.os.Build.MANUFACTURER}")
            appendLine("log.level=${s.processing.logLevel.key}")
            appendLine("network.allowed=${s.network.allowedNetworks.key}")
            appendLine("proxy.enabled=${s.network.proxyType.key != "disabled"}")
            appendLine("notifications.enabled=${system.notificationsEnabled()}")
            appendLine("battery.exempt=${system.ignoringBatteryOptimizations()}")
            appendLine("background.restricted=${system.backgroundRestricted()}")
        }
    }

    fun clearDiagnosticLog() {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching { File(getApplication<Application>().cacheDir, "diagnostics.log").delete() }
        }
    }

    private fun cacheDirs(): List<File> {
        val app = getApplication<Application>()
        return listOfNotNull(app.cacheDir, app.externalCacheDir)
    }

    private fun dirSize(dir: File): Long =
        dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }

    private companion object {
        const val HISTORY_CACHE = "history_cache"
        const val KEY_RECENT_URLS = "recent_urls"
        const val KEY_SEARCH_HISTORY = "search_history"
    }
}
