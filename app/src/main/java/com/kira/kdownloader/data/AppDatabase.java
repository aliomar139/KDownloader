package com.kira.kdownloader.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {DownloadEntity.class}, version = 2, exportSchema = false)
@TypeConverters(Converters.class)
public abstract class AppDatabase extends RoomDatabase {
    private static volatile AppDatabase instance;
    private static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE `downloads_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `sourceUrl` TEXT NOT NULL, `kind` TEXT NOT NULL, `formatLabel` TEXT NOT NULL, `fileUri` TEXT, `thumbnailUrl` TEXT, `createdAt` INTEGER NOT NULL, `status` TEXT NOT NULL)");
            database.execSQL("INSERT INTO `downloads_new` (`id`, `title`, `sourceUrl`, `kind`, `formatLabel`, `fileUri`, `thumbnailUrl`, `createdAt`, `status`) SELECT `id`, COALESCE(`title`, ''), COALESCE(`sourceUrl`, ''), COALESCE(`kind`, ''), COALESCE(`formatLabel`, ''), `fileUri`, `thumbnailUrl`, `createdAt`, COALESCE(`status`, 'FAILED') FROM `downloads`");
            database.execSQL("DROP TABLE `downloads`");
            database.execSQL("ALTER TABLE `downloads_new` RENAME TO `downloads`");
        }
    };

    public abstract DownloadDao downloadDao();

    public static AppDatabase get(Context context) {
        AppDatabase current = instance;
        if (current != null) return current;
        synchronized (AppDatabase.class) {
            if (instance == null) {
                instance = Room.databaseBuilder(
                        context.getApplicationContext(), AppDatabase.class, "kdownloader.db")
                        .addMigrations(MIGRATION_1_2)
                        .build();
            }
            return instance;
        }
    }
}
