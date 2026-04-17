package org.starling.message.support;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HandlerParsing {

    private HandlerParsing() {}

    public static int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    public static int parseRoomId(String rawRoomId) {
        if (rawRoomId == null) {
            return 0;
        }

        String normalized = rawRoomId.trim();
        if (normalized.startsWith("f_")) {
            normalized = normalized.substring(2);
        }
        return parseIntOrDefault(normalized, 0);
    }

    public static String sanitizeRoomName(String roomName) {
        if (roomName == null) {
            return "";
        }
        return roomName.replace("/", "").trim();
    }

    public static String[] normalizeLines(String rawBody) {
        if (rawBody == null || rawBody.isEmpty()) {
            return new String[0];
        }
        return rawBody.replace("\n", "").split("\r");
    }

    public static Map<String, String> parseKeyValues(String[] lines, int startIndex) {
        Map<String, String> values = new LinkedHashMap<>();
        for (int i = startIndex; i < lines.length; i++) {
            String line = lines[i];
            if (line == null || line.isBlank()) {
                continue;
            }

            int separator = line.indexOf('=');
            if (separator <= 0) {
                continue;
            }

            values.put(line.substring(0, separator).trim(), line.substring(separator + 1));
        }
        return values;
    }
}
