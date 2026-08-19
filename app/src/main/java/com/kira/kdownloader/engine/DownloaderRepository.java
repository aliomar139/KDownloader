package com.kira.kdownloader.engine;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.WorkerThread;

import com.kira.kdownloader.BuildConfig;
import com.kira.kdownloader.R;
import com.yausername.ffmpeg.FFmpeg;
import com.yausername.youtubedl_android.YoutubeDL;
import com.yausername.youtubedl_android.YoutubeDLRequest;
import com.yausername.youtubedl_android.mapper.VideoFormat;
import com.yausername.youtubedl_android.mapper.VideoInfo;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Pattern;

import kotlin.Unit;

public final class DownloaderRepository implements AutoCloseable {
    private static final String TAG = "DownloaderRepository";
    private static final String PREFERENCES_NAME = "download_engine";
    private static final String LAST_UPDATE_ATTEMPT = "last_update_attempt";
    private static final String YTDLP_PREFERENCES_NAME = "youtubedl-android";
    private static final String YTDLP_VERSION_KEY = "dlpVersion";
    private static final String YTDLP_VERSION_NAME_KEY = "dlpVersionName";
    private static final String TIKTOK_INSTALL_IDS_KEY = "tiktok_install_ids";
    private static final int TIKTOK_INSTALL_ID_COUNT = 3;
    private static final long UPDATE_INTERVAL_MS = 24L * 60L * 60L * 1000L;
    private static final int MAX_UPDATE_ATTEMPTS = 3;
    private static final long UPDATE_RETRY_DELAY_MS = 750L;
    private static final int MAX_OUTPUT_TITLE_LENGTH = 120;

    private static final ReentrantLock INIT_LOCK = new ReentrantLock();
    private static final ReentrantLock UPDATE_LOCK = new ReentrantLock();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final Pattern TIKTOK_INSTALL_ID_PATTERN = Pattern.compile("\\d{19}");
    private static final Pattern SAFE_HEADER_NAME = Pattern.compile("[A-Za-z0-9-]+");
    private static final Pattern INVALID_FILE_NAME_CHARACTERS =
            Pattern.compile("[\\\\/:*?\"<>|\\x00-\\x1F]");

    private static volatile boolean initialized;

    private final Context appContext;
    private final ExecutorService backgroundExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean updateInFlight = new AtomicBoolean();

    @FunctionalInterface
    public interface ProgressListener {
        void onProgress(float progress, long etaSeconds, String line);
    }

    public DownloaderRepository(Context context) {
        appContext = context.getApplicationContext();
    }

    @WorkerThread
    public void init() throws EngineException {
        INIT_LOCK.lock();
        try {
            if (!initialized) {
                try {
                    YoutubeDL.getInstance().init(appContext);
                    FFmpeg.getInstance().init(appContext);
                    String installedVersion = ensureCurrentYtDlpInstalled();
                    registerBundledYtDlpVersionIfNeeded(installedVersion);
                    initialized = true;
                } catch (Throwable error) {
                    throw new EngineException("Failed to initialize download engine", error);
                }
            }
        } finally {
            INIT_LOCK.unlock();
        }

        if (updateInFlight.compareAndSet(false, true)) {
            backgroundExecutor.execute(() -> {
                try {
                    updateEngineIfNeeded();
                } finally {
                    updateInFlight.set(false);
                }
            });
        }
    }

    @WorkerThread
    public MediaInfo fetchInfo(String url) throws EngineException {
        boolean retryWithoutTikTokAppInfo = ExtractorOptions.isTikTokUrl(url);
        Throwable lastError = null;
        int strategyCount = retryWithoutTikTokAppInfo ? 2 : 1;

        for (int index = 0; index < strategyCount; index++) {
            boolean useTikTokAppInfo = index == 0;
            try {
                YoutubeDLRequest request = baseRequest(url, useTikTokAppInfo)
                        .addOption("--quiet")
                        .addOption("--no-warnings")
                        .addOption("--no-progress");
                VideoInfo info = YoutubeDL.getInstance().getInfo(request);
                List<FormatInput> formats = new ArrayList<>();
                List<VideoFormat> sourceFormats = info.getFormats();
                if (sourceFormats != null) {
                    for (VideoFormat format : sourceFormats) {
                        long exactSize = format.getFileSize();
                        long approximateSize = format.getFileSizeApproximate();
                        Long size = exactSize > 0 ? exactSize
                                : approximateSize > 0 ? approximateSize : null;
                        formats.add(new FormatInput(
                                valueOrEmpty(format.getFormatId()),
                                valueOrEmpty(format.getExt()),
                                format.getHeight() > 0 ? format.getHeight() : null,
                                format.getVcodec(),
                                format.getAcodec(),
                                format.getUrl(),
                                format.getHttpHeaders() == null
                                        ? Collections.emptyMap() : format.getHttpHeaders(),
                                size
                        ));
                    }
                }
                return new MediaInfo(
                        info.getTitle() == null ? url : info.getTitle(),
                        info.getThumbnail(),
                        FormatSelector.choices(formats, url),
                        info.getDuration(),
                        info.getUploader()
                );
            } catch (Throwable error) {
                lastError = error;
                if (useTikTokAppInfo && retryWithoutTikTokAppInfo) {
                    Log.w(TAG, "TikTok mobile extraction failed; retrying web extraction", error);
                }
            }
        }

        if (lastError == null) lastError = new IllegalStateException("No media information strategy was attempted");
        throw new EngineException(readableErrorMessage(url, lastError), lastError);
    }

    public YoutubeDLRequest buildRequest(String url, DownloadChoice choice, File outputDirectory,
                                         String outputTitle) {
        return buildRequest(url, choice, outputDirectory, outputTitle, true);
    }

    public YoutubeDLRequest buildRequest(String url, DownloadChoice choice, File outputDirectory,
                                         String outputTitle, boolean useTikTokAppInfo) {
        String outputPath = new File(
                outputDirectory, safeOutputTitle(outputTitle) + ".%(ext)s").getAbsolutePath();
        String directUrl = choice.getDirectUrl();
        if (directUrl != null && !directUrl.trim().isEmpty()) {
            YoutubeDLRequest request = new YoutubeDLRequest(directUrl).addOption("-o", outputPath);
            for (Map.Entry<String, String> header : choice.getHttpHeaders().entrySet()) {
                String name = header.getKey();
                String value = header.getValue();
                if (SAFE_HEADER_NAME.matcher(name).matches()
                        && value.indexOf('\r') < 0 && value.indexOf('\n') < 0) {
                    request.addOption("--add-header", name + ":" + value);
                }
            }
            addOutputOptions(request, choice);
            return request;
        }

        YoutubeDLRequest request = baseRequest(url, useTikTokAppInfo)
                .addOption("-o", outputPath)
                .addOption("-f", choice.getFormatSelector());
        addOutputOptions(request, choice);
        return request;
    }

    private static void addOutputOptions(YoutubeDLRequest request, DownloadChoice choice) {
        if (choice.getKind() == FormatSelector.Kind.AUDIO) {
            request.addOption("-x").addOption("--audio-format", "mp3");
        } else {
            request.addOption("--merge-output-format", "mp4");
        }
    }

    @WorkerThread
    public void execute(YoutubeDLRequest request, String processId,
                        ProgressListener onProgress) throws EngineException {
        try {
            YoutubeDL.getInstance().execute(request, processId, (progress, etaSeconds, line) -> {
                onProgress.onProgress(progress, etaSeconds, line == null ? "" : line);
                return Unit.INSTANCE;
            });
        } catch (Throwable error) {
            String message = error.getMessage();
            throw new EngineException(message == null ? "Download failed" : message, error);
        }
    }

    public boolean cancel(String processId) {
        return YoutubeDL.getInstance().destroyProcessById(processId);
    }

    private YoutubeDLRequest baseRequest(String url, boolean useTikTokAppInfo) {
        YoutubeDLRequest request = new YoutubeDLRequest(url).addOption("--no-playlist");
        if (ExtractorOptions.isYouTubeUrl(url)) {
            request.addOption("--remote-components", "ejs:github");
        }
        if (useTikTokAppInfo && ExtractorOptions.isTikTokUrl(url)) {
            request.addOption("--extractor-args", ExtractorOptions.tikTokAppInfo(tikTokInstallIds()));
        }
        return request;
    }

    private List<String> tikTokInstallIds() {
        SharedPreferences preferences = appContext.getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
        String stored = preferences.getString(TIKTOK_INSTALL_IDS_KEY, null);
        List<String> storedIds = new ArrayList<>();
        if (stored != null) {
            for (String id : stored.split(",")) {
                if (TIKTOK_INSTALL_ID_PATTERN.matcher(id).matches()) storedIds.add(id);
            }
        }
        if (storedIds.size() == TIKTOK_INSTALL_ID_COUNT) return storedIds;

        LinkedHashSet<String> generated = new LinkedHashSet<>();
        while (generated.size() < TIKTOK_INSTALL_ID_COUNT) {
            StringBuilder id = new StringBuilder("73");
            for (int index = 0; index < 17; index++) id.append(SECURE_RANDOM.nextInt(10));
            generated.add(id.toString());
        }
        List<String> result = new ArrayList<>(generated);
        preferences.edit().putString(TIKTOK_INSTALL_IDS_KEY, String.join(",", result)).apply();
        return result;
    }

    private String ensureCurrentYtDlpInstalled() throws IOException {
        String installedVersion = readInstalledYtDlpVersion();
        if (!YtDlpVersionPolicy.shouldInstallBundled(
                installedVersion, BuildConfig.BUNDLED_YTDLP_VERSION)) {
            return installedVersion == null ? "" : installedVersion;
        }

        Log.i(TAG, "Replacing stale yt-dlp "
                + (installedVersion == null ? "unknown" : installedVersion)
                + " with bundled " + BuildConfig.BUNDLED_YTDLP_VERSION);
        installBundledYtDlp();
        String verifiedVersion = readInstalledYtDlpVersion();
        if (!BuildConfig.BUNDLED_YTDLP_VERSION.equals(verifiedVersion)) {
            throw new IllegalStateException("Bundled yt-dlp verification failed: found "
                    + (verifiedVersion == null ? "unknown" : verifiedVersion));
        }
        return verifiedVersion;
    }

    private String readInstalledYtDlpVersion() {
        try {
            String output = YoutubeDL.getInstance().execute(
                    new YoutubeDLRequest(Collections.emptyList()).addOption("--version")).getOut();
            for (String line : output.split("\\R")) {
                if (!line.trim().isEmpty()) return line.trim();
            }
        } catch (Throwable error) {
            Log.w(TAG, "Could not read the installed yt-dlp version", error);
        }
        return null;
    }

    private void installBundledYtDlp() throws IOException {
        File directory = new File(appContext.getNoBackupFilesDir(),
                YoutubeDL.baseName + "/" + YoutubeDL.ytdlpDirName);
        if (!directory.mkdirs() && !directory.isDirectory()) {
            throw new IOException("Could not create the yt-dlp directory");
        }
        File target = new File(directory, YoutubeDL.ytdlpBin);
        File temporary = new File(directory, YoutubeDL.ytdlpBin + ".new");

        try {
            try (InputStream input = appContext.getResources().openRawResource(R.raw.ytdlp);
                 FileOutputStream output = new FileOutputStream(temporary)) {
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
            }
            if (temporary.length() <= 0) throw new IOException("Bundled yt-dlp resource is empty");
            if (target.exists() && !target.delete()) throw new IOException("Could not replace stale yt-dlp");
            if (!temporary.renameTo(target)) {
                copyFile(temporary, target);
                if (!temporary.delete()) throw new IOException("Could not remove temporary yt-dlp copy");
            }
        } finally {
            if (temporary.exists() && !temporary.delete()) {
                Log.w(TAG, "Could not remove " + temporary.getAbsolutePath());
            }
        }
    }

    private static void copyFile(File source, File target) throws IOException {
        try (InputStream input = new java.io.FileInputStream(source);
             FileOutputStream output = new FileOutputStream(target)) {
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        }
    }

    private void registerBundledYtDlpVersionIfNeeded(String installedVersion) {
        SharedPreferences ytdlpPreferences = appContext.getSharedPreferences(
                YTDLP_PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (!BuildConfig.BUNDLED_YTDLP_VERSION.equals(installedVersion)) return;

        SharedPreferences enginePreferences = appContext.getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
        if (installedVersion.equals(ytdlpPreferences.getString(YTDLP_VERSION_KEY, null))
                && enginePreferences.contains(LAST_UPDATE_ATTEMPT)) return;

        ytdlpPreferences.edit()
                .putString(YTDLP_VERSION_KEY, BuildConfig.BUNDLED_YTDLP_VERSION)
                .putString(YTDLP_VERSION_NAME_KEY,
                        "yt-dlp " + BuildConfig.BUNDLED_YTDLP_VERSION)
                .apply();
        enginePreferences.edit().putLong(LAST_UPDATE_ATTEMPT,
                System.currentTimeMillis()).apply();
        Log.i(TAG, "Using bundled yt-dlp " + BuildConfig.BUNDLED_YTDLP_VERSION);
    }

    private static String readableErrorMessage(String url, Throwable error) {
        StringBuilder details = new StringBuilder();
        for (Throwable current = error; current != null; current = current.getCause()) {
            String message = current.getMessage();
            if (message != null && !message.trim().isEmpty()) details.append(message).append('\n');
        }
        String lower = details.toString().toLowerCase(Locale.ROOT);
        if (ExtractorOptions.isTikTokUrl(url)
                && (lower.contains("expecting value") || lower.contains("column 1"))) {
            return "TikTok returned an invalid response. Check the connection and try again.";
        }
        String message = error.getMessage();
        return message == null || message.trim().isEmpty()
                ? "Could not fetch media info" : message;
    }

    private void updateEngineIfNeeded() {
        SharedPreferences preferences = appContext.getSharedPreferences(
                PREFERENCES_NAME, Context.MODE_PRIVATE);
        long now = System.currentTimeMillis();
        if (now - preferences.getLong(LAST_UPDATE_ATTEMPT, 0L) < UPDATE_INTERVAL_MS) return;

        UPDATE_LOCK.lock();
        try {
            long currentTime = System.currentTimeMillis();
            if (currentTime - preferences.getLong(LAST_UPDATE_ATTEMPT, 0L)
                    < UPDATE_INTERVAL_MS) return;

            Throwable lastError = null;
            for (int attempt = 0; attempt < MAX_UPDATE_ATTEMPTS; attempt++) {
                try {
                    YoutubeDL.getInstance().updateYoutubeDL(
                            appContext, YoutubeDL.UpdateChannel._STABLE);
                    preferences.edit().putLong(LAST_UPDATE_ATTEMPT, currentTime).apply();
                    Log.i(TAG, "yt-dlp is up to date: "
                            + YoutubeDL.getInstance().versionName(appContext));
                    return;
                } catch (Throwable error) {
                    lastError = error;
                    if (attempt + 1 < MAX_UPDATE_ATTEMPTS) {
                        try {
                            Thread.sleep(UPDATE_RETRY_DELAY_MS * (attempt + 1));
                        } catch (InterruptedException interrupted) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }

            preferences.edit().remove(LAST_UPDATE_ATTEMPT).apply();
            Log.w(TAG, "Could not update yt-dlp after " + MAX_UPDATE_ATTEMPTS
                    + " attempts", lastError);
        } finally {
            UPDATE_LOCK.unlock();
        }
    }

    private static String safeOutputTitle(String title) {
        String safe = INVALID_FILE_NAME_CHARACTERS.matcher(title).replaceAll("_");
        int start = 0;
        int end = safe.length();
        while (start < end && (safe.charAt(start) == ' ' || safe.charAt(start) == '.')) start++;
        while (end > start && (safe.charAt(end - 1) == ' ' || safe.charAt(end - 1) == '.')) end--;
        safe = safe.substring(start, end);
        if (safe.length() > MAX_OUTPUT_TITLE_LENGTH) safe = safe.substring(0, MAX_OUTPUT_TITLE_LENGTH);
        return safe.isEmpty() ? "download" : safe;
    }

    private static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    @Override public void close() {
        backgroundExecutor.shutdownNow();
    }
}
