package org.starling.json;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Shared Gson-backed JSON helpers used across Starling modules.
 */
public final class GsonSupport {

    private static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();
    private static final String LEGACY_SECURE_PREFIX = "/*-secure-\n";
    private static final String LEGACY_SECURE_SUFFIX = "\n */";

    private GsonSupport() {
    }

    /**
     * Serializes a value to JSON.
     * @param value the value to serialize
     * @return the JSON string
     */
    public static String toJson(Object value) {
        return GSON.toJson(value);
    }

    /**
     * Serializes a value to JSON using an explicit type.
     * @param value the value to serialize
     * @param type the target type
     * @return the JSON string
     */
    public static String toJson(Object value, Type type) {
        return GSON.toJson(value, type);
    }

    /**
     * Parses JSON into an object using an explicit type.
     * @param json the source JSON
     * @param type the target type
     * @param <T> the decoded type
     * @return the decoded value
     */
    public static <T> T fromJson(String json, Type type) {
        return GSON.fromJson(json, type);
    }

    /**
     * Returns the generic {@link List} type for the given element type.
     * @param elementType the element class
     * @return the list type
     */
    public static Type listType(Class<?> elementType) {
        return TypeToken.getParameterized(List.class, elementType).getType();
    }

    /**
     * Wraps a JSON payload in the legacy secure-array envelope expected by
     * older Habbo web callbacks.
     * @param value the value to serialize inside the wrapper
     * @return the wrapped payload
     */
    public static String toLegacySecureJson(Object value) {
        return LEGACY_SECURE_PREFIX + toJson(value) + LEGACY_SECURE_SUFFIX;
    }
}
