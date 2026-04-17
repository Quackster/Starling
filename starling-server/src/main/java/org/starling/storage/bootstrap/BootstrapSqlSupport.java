package org.starling.storage.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class BootstrapSqlSupport {

    private BootstrapSqlSupport() {}

    static String readBundledSql(Class<?> owner, String resourcePath) {
        try (InputStream input = owner.getClassLoader().getResourceAsStream(resourcePath)) {
            if (input == null) {
                throw new IllegalStateException("Bundled bootstrap resource is missing: " + resourcePath);
            }
            return new String(input.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read bundled bootstrap resource " + resourcePath, e);
        }
    }

    static List<List<String>> parseInsertRows(String sql, String tableName, String resourcePath) {
        Matcher matcher = Pattern.compile(
                "INSERT INTO `" + Pattern.quote(tableName) + "` .*? VALUES\\s*(.*?);",
                Pattern.DOTALL)
                .matcher(sql);

        if (!matcher.find()) {
            return Collections.emptyList();
        }

        return parseTuples(matcher.group(1), resourcePath);
    }

    static int parseInt(List<String> row, int index) {
        String value = row.get(index);
        if (value == null || value.isBlank() || value.equalsIgnoreCase("NULL")) {
            return 0;
        }
        return (int) Math.round(Double.parseDouble(value));
    }

    static double parseDouble(List<String> row, int index) {
        String value = row.get(index);
        if (value == null || value.isBlank() || value.equalsIgnoreCase("NULL")) {
            return 0;
        }
        return Double.parseDouble(value);
    }

    static String parseString(List<String> row, int index) {
        String value = parseNullableString(row, index);
        return value == null ? "" : value;
    }

    static String parseNullableString(List<String> row, int index) {
        String value = row.get(index);
        if (value == null || value.equalsIgnoreCase("NULL")) {
            return null;
        }
        return value;
    }

    static String defaultString(String value) {
        return value == null ? "" : value;
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static List<List<String>> parseTuples(String valuesBlock, String resourcePath) {
        List<List<String>> tuples = new ArrayList<>();
        List<String> fields = null;
        StringBuilder field = new StringBuilder();
        boolean inString = false;
        int depth = 0;

        for (int index = 0; index < valuesBlock.length(); index++) {
            char current = valuesBlock.charAt(index);
            if (inString) {
                if (current == '\\' && index + 1 < valuesBlock.length()) {
                    appendDecodedEscapedCharacter(field, valuesBlock.charAt(index + 1));
                    index++;
                    continue;
                }

                if (current == '\'') {
                    if (index + 1 < valuesBlock.length() && valuesBlock.charAt(index + 1) == '\'') {
                        field.append('\'');
                        index++;
                    } else {
                        inString = false;
                    }
                    continue;
                }

                field.append(current);
                continue;
            }

            if (current == '\'') {
                inString = true;
                continue;
            }

            if (current == '(') {
                depth++;
                if (depth == 1) {
                    fields = new ArrayList<>();
                    field.setLength(0);
                    continue;
                }
            }

            if (current == ')') {
                depth--;
                if (depth == 0) {
                    if (fields == null) {
                        throw new IllegalStateException("Malformed SQL tuple data in " + resourcePath);
                    }
                    fields.add(field.toString().trim());
                    tuples.add(fields);
                    field.setLength(0);
                    continue;
                }
            }

            if (current == ',' && depth == 1) {
                if (fields == null) {
                    throw new IllegalStateException("Malformed SQL tuple data in " + resourcePath);
                }
                fields.add(field.toString().trim());
                field.setLength(0);
                continue;
            }

            if (depth >= 1) {
                field.append(current);
            }
        }

        return tuples;
    }

    private static void appendDecodedEscapedCharacter(StringBuilder builder, char next) {
        switch (next) {
            case 'r' -> builder.append('\r');
            case 'n' -> builder.append('\n');
            case 't' -> builder.append('\t');
            case '\\' -> builder.append('\\');
            case '\'' -> builder.append('\'');
            default -> {
                builder.append('\\');
                builder.append(next);
            }
        }
    }
}
