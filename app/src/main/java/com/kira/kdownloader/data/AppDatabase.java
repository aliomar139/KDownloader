package com.kira.kdownloader.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {DownloadEntity.class}, version = 1, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;
    public abstract DownloadDao downloadDao();

    public static AppDatabase get(Context context) {
        AppDatabase current = instance;
        if (current != null) return current;
        synchronized (AppDatabase.class) {
            if (instance == null) {
                instance = Room.databaseBuilder(
                        context.getApplicationContext(), AppDatabase.class, "kdownloader.db").build();
            }
            return instance;
        }
    }
}
