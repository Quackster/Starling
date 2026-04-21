package org.oldskooler.vibe.web.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Slugifier {

    /**
     * Creates a new Slugifier.
     */
    private Slugifier() {}

    /**
     * Slugifies the given input.
     * @param input the input value
     * @return the resulting slug
     */
    public static String slugify(String input) {
        if (input == null || input.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");

        return normalized;
    }
}
