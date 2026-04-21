package org.oldskooler.vibe.json;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GsonSupportTest {

    @Test
    void serializesLegacyJsonWithoutHtmlEscaping() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("message", "Don't <escape> me");
        values.put("count", 2);

        assertEquals(
                "{\"message\":\"Don't <escape> me\",\"count\":2}",
                GsonSupport.toJson(values)
        );
    }

    @Test
    void wrapsLegacySecureJsonPayloads() {
        List<Map<String, Object>> values = List.of(Map.of("name", "Alex"));

        assertEquals(
                "/*-secure-\n[{\"name\":\"Alex\"}]\n */",
                GsonSupport.toLegacySecureJson(values)
        );
    }
}
