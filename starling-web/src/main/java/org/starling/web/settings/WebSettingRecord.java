package org.starling.web.settings;

public record WebSettingRecord(
        String key,
        String category,
        String label,
        String description,
        WebSettingValueType valueType,
        boolean secret,
        int sortOrder,
        String value
) {

    /**
     * Returns the stored value or an empty string when absent.
     * @return the normalized value
     */
    public String normalizedValue() {
        return value == null ? "" : value;
    }
}
