package com.kira.kdownloader.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kira.kdownloader.engine.DownloaderRepository
import com.kira.kdownloader.engine.MediaInfo
import com.kira.kdownloader.util.RecentUrls
import com.kira.kdownloader.util.UrlExtractor
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface HomeUiState {
    data object Idle : HomeUiState
    data object Loading : HomeUiState
    data class Loaded(val sourceUrl: String, val info: MediaInfo) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = DownloaderRepository(application)
    private val mutableState = MutableStateFlow<HomeUiState>(HomeUiState.Idle)
    private var fetchJob: Job? = null
    private var warmed = false

    // Short-lived cache of fetched formats, keyed by normalized URL. The TTL keeps it useful for
    // immediate retries / re-picks while avoiding serving direct media URLs after they've expired.
    private data class CachedInfo(val info: MediaInfo, val atMs: Long)
    private val infoCache = LinkedHashMap<String, CachedInfo>()

    val state: StateFlow<HomeUiState> = mutableState.asStateFlow()

    /** Initialize the engine ahead of the first fetch so its one-time cost is already paid. */
    fun warmUp() {
        if (warmed) return
        warmed = true
        viewModelScope.launch(Dispatchers.IO) { runCatching { repository.init() } }
    }

    fun fetch(url: String) {
        val normalizedUrl = UrlExtractor.fromText(url)
        if (normalizedUrl.isEmpty()) {
            mutableState.value = HomeUiState.Error("Enter a media URL first")
            return
        }

        cachedInfo(normalizedUrl)?.let { info ->
            fetchJob?.cancel()
            mutableState.value = HomeUiState.Loaded(sourceUrl = normalizedUrl, info = info)
            return
        }

        fetchJob?.cancel()
        fetchJob = viewModelScope.launch {
            mutableState.value = HomeUiState.Loading
            try {
                val info = withContext(Dispatchers.IO) {
                    repository.init()
                    repository.fetchInfo(normalizedUrl)
                }
                RecentUrls.add(getApplication<Application>(), normalizedUrl)
                putCache(normalizedUrl, info)
                mutableState.value = HomeUiState.Loaded(
                    sourceUrl = normalizedUrl,
                    info = info,
                )
            } catch (error: Throwable) {
                mutableState.value = HomeUiState.Error(
                    error.message?.takeIf(String::isNotBlank) ?: "Failed to fetch media info",
                )
            }
        }
    }

    private fun cachedInfo(normalizedUrl: String): MediaInfo? =
        infoCache[normalizedUrl]
            ?.takeIf { System.currentTimeMillis() - it.atMs < CACHE_TTL_MS }
            ?.info

    private fun putCache(normalizedUrl: String, info: MediaInfo) {
        infoCache[normalizedUrl] = CachedInfo(info, System.currentTimeMillis())
        while (infoCache.size > MAX_CACHE_ENTRIES) {
            infoCache.remove(infoCache.keys.first())
        }
    }

    override fun onCleared() {
        repository.close()
        super.onCleared()
    }

    private companion object {
        const val CACHE_TTL_MS = 5 * 60_000L
        const val MAX_CACHE_ENTRIES = 20
    }
}
