package com.kira.kdownloader.data;

import android.content.Context;
import android.database.Cursor;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    @Test public void schemaRetainsKotlinNullability() {
        Map<String, Integer> expected = new HashMap<>();
        expected.put("id", 1);
        expected.put("title", 1);
        expected.put("sourceUrl", 1);
        expected.put("kind", 1);
        expected.put("formatLabel", 1);
        expected.put("fileUri", 0);
        expected.put("thumbnailUrl", 0);
        expected.put("createdAt", 1);
        expected.put("status", 1);

        try (Cursor cursor = database.getOpenHelper().getReadableDatabase()
                .query("PRAGMA table_info(downloads)")) {
            int nameColumn = cursor.getColumnIndexOrThrow("name");
            int notNullColumn = cursor.getColumnIndexOrThrow("notnull");
            while (cursor.moveToNext()) {
                String name = cursor.getString(nameColumn);
                Integer notNull = expected.remove(name);
                assertNotNull("Unexpected column " + name, notNull);
                assertEquals("Wrong nullability for " + name,
                        notNull.intValue(), cursor.getInt(notNullColumn));
            }
        }
        assertTrue("Missing columns " + expected.keySet(), expected.isEmpty());
    }

    @Test public void deleteByIdRemovesRow() {
        long id = dao.insert(new DownloadEntity(0L, "Doomed", "https://example.com/video", "AUDIO",
                "Audio (mp3)", "content://file/2", null, 2L, DownloadStatus.COMPLETED));
        dao.deleteById(id);
        assertNull(dao.getById(id));
        assertEquals(0, dao.getAllSync().size());
    }
}
