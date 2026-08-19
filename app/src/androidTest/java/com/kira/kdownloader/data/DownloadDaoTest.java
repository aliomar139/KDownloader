package com.kira.kdownloader.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class DownloadDaoTest {
    private AppDatabase database;
    private DownloadDao dao;

    @Before public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class).build();
        dao = database.downloadDao();
    }

    @After public void tearDown() { database.close(); }

    @Test public void insertThenObserveReturnsRow() {
        long id = dao.insert(new DownloadEntity(0L, "Test", "https://example.com/video", "VIDEO",
                "720p", null, null, 1L, DownloadStatus.RUNNING));
        assertEquals("Test", dao.getAllSync().get(0).getTitle());
        dao.updateStatusAndUri(id, DownloadStatus.COMPLETED, "content://file/1");
        DownloadEntity updated = dao.getById(id);
        assertEquals(DownloadStatus.COMPLETED, updated.getStatus());
        assertEquals("content://file/1", updated.getFileUri());
    }

    @Test public void getByIdMissingReturnsNull() { assertNull(dao.getById(999)); }

    @Test public void deleteByIdRemovesRow() {
        long id = dao.insert(new DownloadEntity(0L, "Doomed", "https://example.com/video", "AUDIO",
                "Audio (mp3)", "content://file/2", null, 2L, DownloadStatus.COMPLETED));
        dao.deleteById(id);
        assertNull(dao.getById(id));
        assertEquals(0, dao.getAllSync().size());
    }
}
