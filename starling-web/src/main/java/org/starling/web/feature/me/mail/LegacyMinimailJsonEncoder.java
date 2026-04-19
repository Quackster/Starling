package org.starling.web.feature.me.mail;

import java.util.List;
import java.util.Map;

public final class LegacyMinimailJsonEncoder {

    /**
     * Encodes the minimail X-JSON header payload.
     * @param values the values to encode
     * @return the encoded object
     */
    public String object(Map<String, Object> values) {
        StringBuilder builder = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (!first) {
                builder.append(',');
            }
            first = false;
            builder.append('"').append(escapeJson(entry.getKey())).append('"').append(':');
            appendJsonValue(builder, entry.getValue());
        }
        builder.append('}');
        return builder.toString();
    }

    /**
     * Encodes recipient suggestions in the legacy secure wrapper.
     * @param values the values to encode
     * @return the wrapped array
     */
    public String secureArray(List<Map<String, Object>> values) {
        StringBuilder builder = new StringBuilder();
        builder.append("/*-secure-\n");
        builder.append(array(values));
        builder.append("\n */");
        return builder.toString();
    }

    private String array(List<Map<String, Object>> values) {
        StringBuilder builder = new StringBuilder("[");
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                builder.append(',');
            }
            builder.append(object(values.get(index)));
        }
        builder.append(']');
        return builder.toString();
    }

    private void appendJsonValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value);
            return;
        }

        builder.append('"').append(escapeJson(String.valueOf(value))).append('"');
    }

    private String escapeJson(String value) {
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < value.length(); index++) {
            char current = value.charAt(index);
            switch (current) {
                case '\\' -> builder.append("\\\\");
                case '"' -> builder.append("\\\"");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> builder.append(current);
            }
        }
        return builder.toString();
    }
}
