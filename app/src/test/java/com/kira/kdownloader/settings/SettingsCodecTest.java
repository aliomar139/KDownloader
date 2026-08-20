package com.kira.kdownloader.settings;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class SettingsCodecTest {
    @Test public void roundTripsAFlatStringMap() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("download.type", "audio");
        values.put("storage.filename_template", "{title} - {channel}");
        values.put("behavior.max_parallel", "3");
        assertEquals(values, SettingsCodec.decode(SettingsCodec.encode(values)));
    }

    @Test public void excludesSensitiveKeys() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(SettingsKeys.NW_PROXY_USER, "secret_user");
        values.put(SettingsKeys.NW_PROXY_HOST, "proxy.example.com");
        String json = SettingsCodec.encode(values);
        assertFalse(json.contains("secret_user"));
        assertTrue(json.contains("proxy.example.com"));
    }

    @Test public void escapesSpecialCharacters() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("k", "line1\nline2 \"quoted\" \\slash");
        assertEquals(values, SettingsCodec.decode(SettingsCodec.encode(values)));
    }

    @Test public void decodeCoercesScalarJsonValuesToStrings() {
        Map<String, String> decoded = SettingsCodec.decode("{\"a\": 42, \"b\": true, \"c\": null}");
        assertEquals("42", decoded.get("a"));
        assertEquals("true", decoded.get("b"));
        assertEquals("", decoded.get("c"));
    }

    @Test public void decodeReturnsNullForNonObjectInput() {
        assertNull(SettingsCodec.decode("not json"));
        assertNull(SettingsCodec.decode("[1,2,3]"));
    }
}
