package com.kira.kdownloader.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: DownloadDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).build()
        dao = database.downloadDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertThenObserveReturnsRow() = runBlocking {
        val id = dao.insert(
            DownloadEntity(
                title = "Test",
                sourceUrl = "https://example.com/video",
                kind = "VIDEO",
                formatLabel = "720p",
                fileUri = null,
                thumbnailUrl = null,
                createdAt = 1L,
                status = DownloadStatus.RUNNING,
            ),
        )

        assertEquals("Test", dao.observeAll().first().single().title)

        dao.updateStatusAndUri(id, DownloadStatus.COMPLETED, "content://file/1")
        val updated = dao.getById(id)!!
        assertEquals(DownloadStatus.COMPLETED, updated.status)
        assertEquals("content://file/1", updated.fileUri)
    }

    @Test
    fun getByIdMissingReturnsNull() = runBlocking {
        assertNull(dao.getById(999))
    }

    @Test
    fun deleteByIdRemovesRow() = runBlocking {
        val id = dao.insert(
            DownloadEntity(
                title = "Doomed",
                sourceUrl = "https://example.com/video",
                kind = "AUDIO",
                formatLabel = "Audio (mp3)",
                fileUri = "content://file/2",
                thumbnailUrl = null,
                createdAt = 2L,
                status = DownloadStatus.COMPLETED,
            ),
        )

        dao.deleteById(id)

        assertNull(dao.getById(id))
        assertEquals(0, dao.observeAll().first().size)
    }
}
