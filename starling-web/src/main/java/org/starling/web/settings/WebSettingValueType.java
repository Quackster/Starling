package org.starling.web.settings;

public enum WebSettingValueType {
    TEXT,
    PASSWORD,
    NUMBER,
    URL,
    PATH;

    /**
     * Resolves a persisted setting type.
     * @param value the stored value
     * @return the resulting type
     */
    public static WebSettingValueType fromValue(String value) {
        if (value == null || value.isBlank()) {
            return TEXT;
        }

        try {
            return WebSettingValueType.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return TEXT;
        }
    }
}
