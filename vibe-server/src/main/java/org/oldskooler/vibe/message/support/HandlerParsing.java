package org.oldskooler.vibe.message.support;

import java.util.LinkedHashMap;
import java.util.Map;

public final class HandlerParsing {

    /**
     * Creates a new HandlerParsing.
     */
    private HandlerParsing() {}

    /**
     * Parses int or default.
     * @param value the value value
     * @param defaultValue the default value value
     * @return the resulting parse int or default
     */
    public static int parseIntOrDefault(String value, int defaultValue) {
        try {
            return Integer.parseInt(value == null ? "" : value.trim());
        } catch (Exception ignored) {
            return defaultValue;
        }
    }

    /**
     * Parses room id.
     * @param rawRoomId the raw room id value
     * @return the resulting parse room id
     */
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

    /**
     * Sanitizes room name.
     * @param roomName the room name value
     * @return the resulting sanitize room name
     */
    public static String sanitizeRoomName(String roomName) {
        if (roomName == null) {
            return "";
        }
        return roomName.replace("/", "").trim();
    }

    /**
     * Normalizes lines.
     * @param rawBody the raw body value
     * @return the resulting normalize lines
     */
    public static String[] normalizeLines(String rawBody) {
        if (rawBody == null || rawBody.isEmpty()) {
            return new String[0];
        }
        return rawBody.replace("\n", "").split("\r");
    }

    /**
     * Parses key values.
     * @param lines the lines value
     * @param startIndex the start index value
     * @return the resulting parse key values
     */
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
