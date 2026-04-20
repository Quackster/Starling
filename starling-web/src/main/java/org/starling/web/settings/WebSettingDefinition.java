package org.starling.web.settings;

public record WebSettingDefinition(
        String key,
        String category,
        String label,
        String description,
        WebSettingValueType valueType,
        boolean secret,
        int sortOrder,
        String defaultValue
) {

    /**
     * Returns the normalized default value.
     * @return the default value, never null
     */
    public String normalizedDefaultValue() {
        return defaultValue == null ? "" : defaultValue;
    }
}
