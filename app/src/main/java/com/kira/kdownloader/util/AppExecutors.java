package com.kira.kdownloader.util;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class AppExecutors {
    private static final ExecutorService IO = Executors.newCachedThreadPool(new NamedThreadFactory());
    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());
    private static final Executor MAIN = MAIN_HANDLER::post;

    private AppExecutors() {
    }

    @NonNull
    public static ExecutorService io() {
        return IO;
    }

    @NonNull
    public static Executor main() {
        return MAIN;
    }

    private static final class NamedThreadFactory implements ThreadFactory {
        private final AtomicInteger nextId = new AtomicInteger(1);

        @Override
        public Thread newThread(@NonNull Runnable runnable) {
            return new Thread(runnable, "kdownloader-io-" + nextId.getAndIncrement());
        }
    }
}
