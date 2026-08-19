package com.kira.kdownloader.util;

import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public final class StateHolder<T> {
    private final MutableLiveData<T> live;
    private volatile T value;

    public StateHolder(@NonNull T initial) {
        value = initial;
        live = new MutableLiveData<>(initial);
    }

    @NonNull
    public T get() {
        return value;
    }

    public void set(@NonNull T next) {
        value = next;
        if (Looper.myLooper() == Looper.getMainLooper()) {
            live.setValue(next);
        } else {
            live.postValue(next);
        }
    }

    public synchronized void update(@NonNull Transform<T> transform) {
        set(transform.apply(value));
    }

    @NonNull
    public LiveData<T> live() {
        return live;
    }

    public interface Transform<T> {
        @NonNull
        T apply(@NonNull T current);
    }
}
