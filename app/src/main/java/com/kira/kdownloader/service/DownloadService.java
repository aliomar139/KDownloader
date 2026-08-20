package com.kira.kdownloader.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import com.kira.kdownloader.MainActivity;
import com.kira.kdownloader.data.AppDatabase;
import com.kira.kdownloader.data.DownloadDao;
import com.kira.kdownloader.data.DownloadEntity;
import com.kira.kdownloader.data.DownloadStatus;
import com.kira.kdownloader.engine.DownloadChoice;
import com.kira.kdownloader.engine.DownloadOutputSelector;
import com.kira.kdownloader.engine.DownloaderRepository;
import com.kira.kdownloader.engine.EngineException;
import com.kira.kdownloader.engine.FormatSelector;
import com.kira.kdownloader.util.MediaStoreWriter;
import com.yausername.youtubedl_android.YoutubeDL;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DownloadService extends Service {
    private static final String TAG = "DownloadService";
    private static final String CHANNEL_ID = "downloads";
    private static final int NOTIFICATION_ID = 42;
    private static final String ACTION_CANCEL = "com.kira.kdownloader.action.CANCEL";
    private static final String EXTRA_URL = "url";
    private static final String EXTRA_TITLE = "title";
    private static final String EXTRA_THUMBNAIL = "thumbnail";
    private static final String EXTRA_KIND = "kind";
    private static final String EXTRA_LABEL = "label";
    private static final String EXTRA_SELECTOR = "selector";
    private static final String EXTRA_DIRECT_URL = "directUrl";
    private static final String EXTRA_HEADER_KEYS = "headerKeys";
    private static final String EXTRA_HEADER_VALUES = "headerValues";
    private static final String EXTRA_PROCESS_ID = "processId";

    private final ExecutorService serviceExecutor = Executors.newCachedThreadPool();
    private DownloaderRepository repository;

    @Override public void onCreate() {
        super.onCreate();
        repository = new DownloaderRepository(getApplicationContext());
    }

    @Nullable @Override public IBinder onBind(Intent intent) {
        return null;
    }

    @Override public int onStartCommand(@Nullable Intent intent, int flags, int startId) {
        if (intent != null && ACTION_CANCEL.equals(intent.getAction())) {
            String processId = intent.getStringExtra(EXTRA_PROCESS_ID);
            if (processId != null) repository.cancel(processId);
            return START_NOT_STICKY;
        }
        if (intent == null) return stopInvalidStart(startId);

        String url = intent.getStringExtra(EXTRA_URL);
        if (url == null) return stopInvalidStart(startId);
        String title = intent.getStringExtra(EXTRA_TITLE);
        if (title == null) title = url;
        String thumbnailUrl = intent.getStringExtra(EXTRA_THUMBNAIL);
        FormatSelector.Kind kind;
        try {
            String kindName = intent.getStringExtra(EXTRA_KIND);
            if (kindName == null) return stopInvalidStart(startId);
            kind = FormatSelector.Kind.valueOf(kindName);
        } catch (IllegalArgumentException error) {
            return stopInvalidStart(startId);
        }
        String label = intent.getStringExtra(EXTRA_LABEL);
        if (label == null) label = "";
        String selector = intent.getStringExtra(EXTRA_SELECTOR);
        if (selector == null) return stopInvalidStart(startId);

        ArrayList<String> headerKeys = intent.getStringArrayListExtra(EXTRA_HEADER_KEYS);
        ArrayList<String> headerValues = intent.getStringArrayListExtra(EXTRA_HEADER_VALUES);
        Map<String, String> headers = new LinkedHashMap<>();
        if (headerKeys != null && headerValues != null) {
            int count = Math.min(headerKeys.size(), headerValues.size());
            for (int index = 0; index < count; index++) {
                headers.put(headerKeys.get(index), headerValues.get(index));
            }
        }
        DownloadChoice choice = new DownloadChoice(
                label, kind, selector, intent.getStringExtra(EXTRA_DIRECT_URL), headers, null);
        String processId = intent.getStringExtra(EXTRA_PROCESS_ID);
        if (processId == null) processId = "download-" + System.currentTimeMillis() + '-' + startId;

        createNotificationChannel();
        startForeground(NOTIFICATION_ID, buildNotification(title, processId, 0, true));
        runDownload(startId, processId, url, title, thumbnailUrl, choice);
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        serviceExecutor.shutdownNow();
        if (repository != null) repository.close();
        super.onDestroy();
    }

    private int stopInvalidStart(int startId) {
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    private void runDownload(int startId, String processId, String url, String title,
                             String thumbnailUrl, DownloadChoice choice) {
        serviceExecutor.execute(() -> {
            DownloadDao dao = AppDatabase.get(getApplicationContext()).downloadDao();
            String eventKey = DownloadEvents.keyOf(url, choice.getLabel());
            Long rowId = null;
            File outputDirectory = null;

            DownloadEvents.update(eventKey, new DownloadEvents.State(
                    DownloadEvents.Phase.PREPARING, -1, title, choice.getKind().name(),
                    null, null, -1L, processId));
            try {
                long insertedId = dao.insert(new DownloadEntity(
                        0L, title, url, choice.getKind().name(), choice.getLabel(), null,
                        thumbnailUrl, System.currentTimeMillis(), DownloadStatus.RUNNING));
                rowId = insertedId;
                File tempDirectory = new File(getCacheDir(), "download-" + insertedId);
                if (!tempDirectory.mkdirs() && !tempDirectory.isDirectory()) {
                    throw new IllegalStateException("Could not create temporary directory");
                }
                outputDirectory = tempDirectory;

                repository.init();
                File outputFile = executeWithFallbacks(
                        url, choice, tempDirectory, title, processId,
                        (progress, etaSeconds, ignored) -> {
                            int percent = (int) progress;
                            updateNotification(title, processId, Math.max(0, Math.min(100, percent)));
                            DownloadEvents.update(eventKey, new DownloadEvents.State(
                                    DownloadEvents.Phase.RUNNING, percent, title,
                                    choice.getKind().name(), null, null, etaSeconds, processId));
                        });
                Uri outputUri = MediaStoreWriter.publish(getApplicationContext(), outputFile);

                dao.updateStatusAndUri(insertedId, DownloadStatus.COMPLETED, outputUri.toString());
                DownloadEvents.update(eventKey, new DownloadEvents.State(
                        DownloadEvents.Phase.COMPLETED, 100, title, choice.getKind().name(),
                        outputUri.toString(), null, -1L, null));
            } catch (Throwable error) {
                if (isCanceled(error)) {
                    Log.i(TAG, "Download canceled by user");
                    if (rowId != null) dao.deleteById(rowId);
                    DownloadEvents.clear(eventKey);
                } else {
                    Log.e(TAG, "Download failed", error);
                    if (rowId != null) dao.updateStatusAndUri(rowId, DownloadStatus.FAILED, null);
                    String message = error.getMessage();
                    if (message != null && message.trim().isEmpty()) message = null;
                    DownloadEvents.update(eventKey, new DownloadEvents.State(
                            DownloadEvents.Phase.FAILED, -1, title, choice.getKind().name(),
                            null, message, -1L, null));
                }
            } finally {
                if (outputDirectory != null) deleteTree(outputDirectory);
                stopForeground(STOP_FOREGROUND_REMOVE);
                stopSelf(startId);
            }
        });
    }

    private File executeWithFallbacks(String url, DownloadChoice choice, File outputDirectory,
                                      String outputTitle, String processId,
                                      DownloaderRepository.ProgressListener onProgress)
            throws EngineException {
        List<DownloadChoice> attempts = buildDownloadAttempts(choice);
        Throwable lastError = null;

        for (int index = 0; index < attempts.size(); index++) {
            DownloadChoice attempt = attempts.get(index);
            if (index > 0) clearTemporaryDirectory(outputDirectory);
            try {
                repository.execute(repository.buildRequest(
                        url, attempt, outputDirectory, outputTitle), processId, onProgress);

                File[] files = outputDirectory.listFiles();
                List<File> outputs = files == null
                        ? Collections.emptyList() : java.util.Arrays.asList(files);
                File selected = DownloadOutputSelector.select(outputs, choice.getKind());
                if (selected != null) return selected;

                StringBuilder producedNames = new StringBuilder();
                for (File file : outputs) {
                    if (producedNames.length() > 0) producedNames.append(", ");
                    producedNames.append(file.getName());
                }
                String message = choice.getKind() == FormatSelector.Kind.VIDEO
                        ? "The site did not produce a complete video with audio"
                        : "The site did not produce a completed audio file";
                if (producedNames.length() > 0) message += ": " + producedNames;
                throw new EngineException(message);
            } catch (Throwable error) {
                if (isCanceled(error)) rethrow(error);
                lastError = error;
                if (index + 1 < attempts.size()) {
                    Log.w(TAG, "Download attempt " + (index + 1)
                            + " failed; trying a compatible fallback", error);
                }
            }
        }

        if (lastError instanceof EngineException) throw (EngineException) lastError;
        throw new EngineException(lastError == null
                ? "Download failed before an attempt could start"
                : messageOrDefault(lastError, "Download failed"), lastError);
    }

    private static void rethrow(Throwable error) throws EngineException {
        if (error instanceof EngineException) throw (EngineException) error;
        throw new EngineException(messageOrDefault(error, "Download canceled"), error);
    }

    private static List<DownloadChoice> buildDownloadAttempts(DownloadChoice choice) {
        List<DownloadChoice> attempts = new ArrayList<>();
        addAttempt(attempts, choice);

        DownloadChoice extractedChoice = choice.withDirect(null, Collections.emptyMap());
        if (choice.getDirectUrl() != null && !choice.getDirectUrl().trim().isEmpty()) {
            addAttempt(attempts, extractedChoice);
        }

        if (choice.getKind() == FormatSelector.Kind.VIDEO
                && !FormatSelector.FALLBACK_VIDEO_SELECTOR.equals(choice.getFormatSelector())) {
            DownloadChoice genericVideoChoice = extractedChoice.withFormatSelector(
                    FormatSelector.FALLBACK_VIDEO_SELECTOR);
            addAttempt(attempts, genericVideoChoice);
        }
        return attempts;
    }

    private static void addAttempt(List<DownloadChoice> attempts, DownloadChoice choice) {
        if (!attempts.contains(choice)) attempts.add(choice);
    }

    private static void clearTemporaryDirectory(File directory) {
        File[] files = directory.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (!deleteTree(file)) {
                throw new IllegalStateException(
                        "Could not clear the temporary download before retrying");
            }
        }
    }

    private static boolean deleteTree(File file) {
        File[] children = file.listFiles();
        if (children != null) {
            for (File child : children) if (!deleteTree(child)) return false;
        }
        return !file.exists() || file.delete();
    }

    private static boolean isCanceled(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof YoutubeDL.CanceledException) return true;
        }
        return false;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        NotificationManager manager = getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "Downloads", NotificationManager.IMPORTANCE_LOW));
        }
    }

    private Notification buildNotification(String title, String processId, int progress,
                                           boolean indeterminate) {
        PendingIntent openAppIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent cancel = new Intent(this, DownloadService.class)
                .setAction(ACTION_CANCEL)
                .putExtra(EXTRA_PROCESS_ID, processId);
        PendingIntent cancelIntent = PendingIntent.getService(
                this, processId.hashCode(), cancel,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(title)
                .setContentText(indeterminate ? "Preparing download\u2026"
                        : "Downloading\u2026 " + progress + '%')
                .setSmallIcon(android.R.drawable.stat_sys_download)
                .setContentIntent(openAppIntent)
                .setOnlyAlertOnce(true)
                .setOngoing(true)
                .setProgress(100, progress, indeterminate)
                .addAction(0, "Cancel", cancelIntent)
                .build();
    }

    private void updateNotification(String title, String processId, int progress) {
        getSystemService(NotificationManager.class).notify(
                NOTIFICATION_ID, buildNotification(title, processId, progress, false));
    }

    public static void start(Context context, String url, DownloadChoice choice, String title,
                             String thumbnailUrl, String processId) {
        Intent intent = new Intent(context, DownloadService.class)
                .putExtra(EXTRA_URL, url)
                .putExtra(EXTRA_TITLE, title)
                .putExtra(EXTRA_THUMBNAIL, thumbnailUrl)
                .putExtra(EXTRA_KIND, choice.getKind().name())
                .putExtra(EXTRA_LABEL, choice.getLabel())
                .putExtra(EXTRA_SELECTOR, choice.getFormatSelector())
                .putExtra(EXTRA_DIRECT_URL, choice.getDirectUrl())
                .putExtra(EXTRA_PROCESS_ID, processId);
        intent.putStringArrayListExtra(EXTRA_HEADER_KEYS,
                new ArrayList<>(choice.getHttpHeaders().keySet()));
        intent.putStringArrayListExtra(EXTRA_HEADER_VALUES,
                new ArrayList<>(choice.getHttpHeaders().values()));
        ContextCompat.startForegroundService(context, intent);
    }

    public static void cancel(Context context, String processId) {
        Intent intent = new Intent(context, DownloadService.class)
                .setAction(ACTION_CANCEL)
                .putExtra(EXTRA_PROCESS_ID, processId);
        context.startService(intent);
    }

    private static String messageOrDefault(Throwable error, String fallback) {
        String message = error.getMessage();
        return message == null || message.trim().isEmpty() ? fallback : message;
    }

}
