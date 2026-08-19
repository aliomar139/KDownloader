package com.kira.kdownloader.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    LiveData<List<DownloadEntity>> observeAll();

    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    List<DownloadEntity> getAllSync();

    @Insert long insert(DownloadEntity entity);

    @Query("UPDATE downloads SET status = :status, fileUri = :fileUri WHERE id = :id")
    void updateStatusAndUri(long id, DownloadStatus status, String fileUri);

    @Query("SELECT * FROM downloads WHERE id = :id") DownloadEntity getById(long id);
    @Query("SELECT fileUri FROM downloads WHERE fileUri IS NOT NULL") List<String> getAllFileUris();
    @Query("DELETE FROM downloads WHERE id = :id") void deleteById(long id);
    @Query("DELETE FROM downloads") void clearAll();
}
