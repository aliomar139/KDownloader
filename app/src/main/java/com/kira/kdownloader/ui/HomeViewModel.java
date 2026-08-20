package com.kira.kdownloader.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.kira.kdownloader.engine.DownloaderRepository;
import com.kira.kdownloader.engine.MediaInfo;
import com.kira.kdownloader.util.AppExecutors;
import com.kira.kdownloader.util.UrlExtractor;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

public final class HomeViewModel extends AndroidViewModel {
    private static final long CACHE_TTL_MS = 5L * 60L * 1000L;
    private static final int MAX_CACHE_ENTRIES = 20;

    private final DownloaderRepository repository;
    private final MutableLiveData<HomeUiState> state =
            new MutableLiveData<>(HomeUiState.Idle.INSTANCE);
    private final LinkedHashMap<String, CachedInfo> infoCache = new LinkedHashMap<>();
    private final AtomicInteger fetchGeneration = new AtomicInteger();
    private Future<?> fetchFuture;
    private boolean warmed;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new DownloaderRepository(application);
    }

    public LiveData<HomeUiState> getState() {
        return state;
    }

    public synchronized void warmUp() {
        if (warmed) return;
        warmed = true;
        AppExecutors.io().execute(() -> {
            try {
                repository.init();
            } catch (Throwable ignored) {
                // Fetch reports initialization errors to the UI when the user actually requests it.
            }
        });
    }

    public synchronized void fetch(String text) {
        String url = UrlExtractor.fromText(text);
        if (url.isEmpty()) {
            state.setValue(new HomeUiState.Error("Enter a media URL first"));
            return;
        }

        MediaInfo cached = cachedInfo(url);
        if (cached != null) {
            cancelFetch();
            state.setValue(new HomeUiState.Loaded(url, cached));
            return;
        }

        cancelFetch();
        int generation = fetchGeneration.incrementAndGet();
        state.setValue(HomeUiState.Loading.INSTANCE);
        fetchFuture = AppExecutors.io().submit(() -> {
            try {
                repository.init();
                MediaInfo info = repository.fetchInfo(url);
                if (generation != fetchGeneration.get() || Thread.currentThread().isInterrupted()) return;
                putCache(url, info);
                state.postValue(new HomeUiState.Loaded(url, info));
            } catch (Throwable error) {
                if (generation != fetchGeneration.get() || Thread.currentThread().isInterrupted()) return;
                String message = error.getMessage();
                state.postValue(new HomeUiState.Error(
                        message == null || message.trim().isEmpty()
                                ? "Failed to fetch media info" : message));
            }
        });
    }

    public synchronized void reset() {
        cancelFetch();
        state.setValue(HomeUiState.Idle.INSTANCE);
    }

    private synchronized void cancelFetch() {
        fetchGeneration.incrementAndGet();
        if (fetchFuture != null) fetchFuture.cancel(true);
        fetchFuture = null;
    }

    private synchronized MediaInfo cachedInfo(String url) {
        CachedInfo cached = infoCache.get(url);
        if (cached == null || System.currentTimeMillis() - cached.atMs >= CACHE_TTL_MS) return null;
        return cached.info;
    }

    private synchronized void putCache(String url, MediaInfo info) {
        infoCache.put(url, new CachedInfo(info, System.currentTimeMillis()));
        while (infoCache.size() > MAX_CACHE_ENTRIES) {
            String first = infoCache.keySet().iterator().next();
            infoCache.remove(first);
        }
    }

    @Override protected void onCleared() {
        cancelFetch();
        repository.close();
        super.onCleared();
    }

    private static final class CachedInfo {
        private final MediaInfo info;
        private final long atMs;

        private CachedInfo(MediaInfo info, long atMs) {
            this.info = info;
            this.atMs = atMs;
        }
    }
}
