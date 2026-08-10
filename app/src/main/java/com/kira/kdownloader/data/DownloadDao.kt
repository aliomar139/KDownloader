package com.kira.kdownloader.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    @Query("SELECT * FROM downloads ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<DownloadEntity>>

    @Insert
    suspend fun insert(entity: DownloadEntity): Long

    @Query("UPDATE downloads SET status = :status, fileUri = :fileUri WHERE id = :id")
    suspend fun updateStatusAndUri(id: Long, status: DownloadStatus, fileUri: String?)

    @Query("SELECT * FROM downloads WHERE id = :id")
    suspend fun getById(id: Long): DownloadEntity?

    @Query("SELECT fileUri FROM downloads WHERE fileUri IS NOT NULL")
    suspend fun getAllFileUris(): List<String>

    @Query("DELETE FROM downloads WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** Clears all download history rows. Does not touch the downloaded media files (Section 9). */
    @Query("DELETE FROM downloads")
    suspend fun clearAll()
}
