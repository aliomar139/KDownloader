package com.kira.kdownloader.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class DownloadStatus { RUNNING, COMPLETED, FAILED }

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUrl: String,
    val kind: String,
    val formatLabel: String,
    val fileUri: String?,
    val thumbnailUrl: String?,
    val createdAt: Long,
    val status: DownloadStatus,
)
