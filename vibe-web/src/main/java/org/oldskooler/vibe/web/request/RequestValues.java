package org.oldskooler.vibe.web.request;

import org.oldskooler.vibe.web.util.Slugifier;

public final class RequestValues {

    /**
     * Creates a new RequestValues.
     */
    private RequestValues() {}

    /**
     * Returns an empty string for null values.
     * @param value the raw value
     * @return the resulting value
     */
    public static String valueOrEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * Returns a fallback when the raw value is blank.
     * @param value the raw value
     * @param fallback the fallback value
     * @return the resulting value
     */
    public static String valueOrDefault(String value, String fallback) {
        String normalized = valueOrEmpty(value).trim();
        return normalized.isBlank() ? fallback : normalized;
    }

    /**
     * Parses an int with a fallback.
     * @param value the raw value
     * @param fallback the fallback value
     * @return the resulting int
     */
    public static int parseInt(String value, int fallback) {
        try {
            return Integer.parseInt(value);
        } catch (Exception ignored) {
            return fallback;
        }
    }

    /**
     * Normalizes a slug input.
     * @param explicitSlug the explicit slug value
     * @param title the title value
     * @param fallbackPrefix the fallback prefix value
     * @return the resulting slug
     */
    public static String normalizedSlug(String explicitSlug, String title, String fallbackPrefix) {
        String slug = Slugifier.slugify(valueOrEmpty(explicitSlug));
        if (!slug.isBlank()) {
            return slug;
        }

        slug = Slugifier.slugify(title);
        if (!slug.isBlank()) {
            return slug;
        }

        return fallbackPrefix + "-" + System.currentTimeMillis();
    }
}
