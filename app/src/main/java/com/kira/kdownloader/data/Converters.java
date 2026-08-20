package com.kira.kdownloader.data;

import androidx.room.TypeConverter;

public final class Converters {
    @TypeConverter public DownloadStatus toStatus(String value) { return DownloadStatus.valueOf(value); }
    @TypeConverter public String fromStatus(DownloadStatus status) { return status.name(); }
}
