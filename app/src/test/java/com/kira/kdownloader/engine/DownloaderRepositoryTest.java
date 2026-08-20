package com.kira.kdownloader.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.LongNode;
import com.fasterxml.jackson.databind.node.NullNode;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class DownloaderRepositoryTest {
    @Test public void nullableMetadataNumbersFallBackSafely() {
        assertEquals(0L, DownloaderRepository.positiveLong(NullNode.getInstance()));
        assertEquals(0, DownloaderRepository.positiveInt(NullNode.getInstance()));
        assertEquals(Integer.MAX_VALUE,
                DownloaderRepository.positiveInt(LongNode.valueOf(Long.MAX_VALUE)));
    }

    @Test public void parsesNullableMetadataForAllSupportedSites() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        String json = "{\"title\":\"Example\",\"duration\":null,\"formats\":["
                + "{\"format_id\":\"audio\",\"ext\":\"m4a\",\"height\":null,"
                + "\"filesize\":null,\"filesize_approx\":null,\"vcodec\":\"none\",\"acodec\":\"aac\"},"
                + "{\"format_id\":\"video\",\"ext\":\"mp4\",\"height\":720,"
                + "\"filesize\":null,\"filesize_approx\":9000,\"vcodec\":\"h264\",\"acodec\":\"aac\"}]}";
        String[] urls = {
                "https://www.youtube.com/watch?v=example",
                "https://www.instagram.com/reel/example/",
                "https://www.tiktok.com/@user/video/1",
                "https://www.facebook.com/reel/1"
        };
        for (String url : urls) {
            MediaInfo info = DownloaderRepository.mediaInfoFromJson(mapper.readTree(json), url);
            assertEquals("Example", info.getTitle());
            assertEquals(0, info.getDurationSeconds());
            assertFalse(info.getChoices().isEmpty());
        }
    }
}
