package org.oldskooler.vibe.web.feature.shared.page.navigation;

import java.util.Arrays;
import java.util.List;

public final class NavigationSelectionCodec {

    /**
     * Creates a new NavigationSelectionCodec.
     */
    private NavigationSelectionCodec() {}

    /**
     * Parses a comma-delimited selection list.
     * @param csv the raw csv value
     * @return the parsed values
     */
    public static List<String> values(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }

        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .toList();
    }

    /**
     * Encodes a selection list into csv.
     * @param values the values
     * @return the resulting csv
     */
    public static String toCsv(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }

        return values.stream()
                .map(value -> value == null ? "" : value.trim())
                .filter(value -> !value.isBlank())
                .distinct()
                .collect(java.util.stream.Collectors.joining(","));
    }

    /**
     * Returns a stable selection token for a sub navigation link.
     * @param groupKey the group key
     * @param linkKey the link key
     * @return the token
     */
    public static String subLinkToken(String groupKey, String linkKey) {
        return normalized(groupKey) + "::" + normalized(linkKey);
    }

    private static String normalized(String value) {
        return value == null ? "" : value.trim();
    }
}
