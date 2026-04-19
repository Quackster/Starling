package org.starling.storage.bootstrap;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class BootstrapSqlSupport {

    private static final String INSERT = "INSERT";
    private static final String INTO = "INTO";
    private static final String VALUES = "VALUES";

    /**
     * Creates a new BootstrapSqlSupport.
     */
    private BootstrapSqlSupport() {}

    /**
     * Reads bundled sql.
     * @param owner the owner value
     * @param resourcePath the resource path value
     * @return the resulting read bundled sql
     */
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

    /**
     * Parses insert rows.
     * @param sql the sql value
     * @param tableName the table name value
     * @param resourcePath the resource path value
     * @return the resulting parse insert rows
     */
    static List<List<String>> parseInsertRows(String sql, String tableName, String resourcePath) {
        List<List<String>> rows = new ArrayList<>();
        int searchFrom = 0;

        while (searchFrom < sql.length()) {
            int statementStart = findInsertStatementStart(sql, tableName, searchFrom);
            if (statementStart < 0) {
                break;
            }

            int valuesStart = findValuesStart(sql, statementStart + INSERT.length(), resourcePath);
            int statementEnd = findStatementEnd(sql, valuesStart, resourcePath);
            rows.addAll(parseTuples(sql.substring(valuesStart, statementEnd), resourcePath));
            searchFrom = statementEnd + 1;
        }
        return rows;
    }

    /**
     * Parses int.
     * @param row the row value
     * @param index the index value
     * @return the resulting parse int
     */
    static int parseInt(List<String> row, int index) {
        String value = row.get(index);
        if (value == null || value.isBlank() || value.equalsIgnoreCase("NULL")) {
            return 0;
        }
        return (int) Math.round(Double.parseDouble(value));
    }

    /**
     * Parses double.
     * @param row the row value
     * @param index the index value
     * @return the resulting parse double
     */
    static double parseDouble(List<String> row, int index) {
        String value = row.get(index);
        if (value == null || value.isBlank() || value.equalsIgnoreCase("NULL")) {
            return 0;
        }
        return Double.parseDouble(value);
    }

    /**
     * Parses string.
     * @param row the row value
     * @param index the index value
     * @return the resulting parse string
     */
    static String parseString(List<String> row, int index) {
        String value = parseNullableString(row, index);
        return value == null ? "" : value;
    }

    /**
     * Parses nullable string.
     * @param row the row value
     * @param index the index value
     * @return the resulting parse nullable string
     */
    static String parseNullableString(List<String> row, int index) {
        String value = row.get(index);
        if (value == null || value.equalsIgnoreCase("NULL")) {
            return null;
        }
        return value;
    }

    /**
     * Defaults string.
     * @param value the value value
     * @return the resulting default string
     */
    static String defaultString(String value) {
        return value == null ? "" : value;
    }

    /**
     * Normalizes.
     * @param value the value value
     * @return the resulting normalize
     */
    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * Parses tuples.
     * @param valuesBlock the values block value
     * @param resourcePath the resource path value
     * @return the resulting parse tuples
     */
    private static int findValuesStart(String sql, int startIndex, String resourcePath) {
        int depth = 0;
        boolean inString = false;

        for (int index = startIndex; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (inString) {
                if (current == '\\' && index + 1 < sql.length()) {
                    index++;
                    continue;
                }
                if (current == '\'') {
                    if (index + 1 < sql.length() && sql.charAt(index + 1) == '\'') {
                        index++;
                    } else {
                        inString = false;
                    }
                }
                continue;
            }

            if (current == '\'') {
                inString = true;
                continue;
            }
            if (current == '(') {
                depth++;
                continue;
            }
            if (current == ')' && depth > 0) {
                depth--;
                continue;
            }
            if (depth == 0 && sql.regionMatches(true, index, VALUES, 0, VALUES.length())) {
                return index + VALUES.length();
            }
        }

        throw new IllegalStateException("Missing VALUES clause in " + resourcePath);
    }

    private static int findInsertStatementStart(String sql, String tableName, int searchFrom) {
        for (int index = searchFrom; index < sql.length(); index++) {
            if (!matchesKeyword(sql, index, INSERT)) {
                continue;
            }

            int cursor = skipWhitespace(sql, index + INSERT.length());
            if (!matchesKeyword(sql, cursor, INTO)) {
                continue;
            }

            cursor = skipWhitespace(sql, cursor + INTO.length());
            if (!matchesTableName(sql, cursor, tableName)) {
                continue;
            }

            return index;
        }

        return -1;
    }

    private static boolean matchesKeyword(String sql, int startIndex, String keyword) {
        if (startIndex < 0 || startIndex + keyword.length() > sql.length()) {
            return false;
        }
        if (!sql.regionMatches(true, startIndex, keyword, 0, keyword.length())) {
            return false;
        }
        return isKeywordBoundary(sql, startIndex - 1)
                && isKeywordBoundary(sql, startIndex + keyword.length());
    }

    private static boolean matchesTableName(String sql, int startIndex, String tableName) {
        if (startIndex < 0 || startIndex >= sql.length()) {
            return false;
        }

        char first = sql.charAt(startIndex);
        if (first == '`' || first == '"') {
            int endIndex = sql.indexOf(first, startIndex + 1);
            return endIndex > startIndex
                    && sql.substring(startIndex + 1, endIndex).equalsIgnoreCase(tableName);
        }

        if (startIndex + tableName.length() > sql.length()) {
            return false;
        }

        return sql.regionMatches(true, startIndex, tableName, 0, tableName.length())
                && isKeywordBoundary(sql, startIndex - 1)
                && isKeywordBoundary(sql, startIndex + tableName.length());
    }

    private static int skipWhitespace(String sql, int startIndex) {
        int index = startIndex;
        while (index < sql.length() && Character.isWhitespace(sql.charAt(index))) {
            index++;
        }
        return index;
    }

    private static boolean isKeywordBoundary(String sql, int index) {
        if (index < 0 || index >= sql.length()) {
            return true;
        }

        char current = sql.charAt(index);
        return !Character.isLetterOrDigit(current) && current != '_';
    }

    private static int findStatementEnd(String sql, int startIndex, String resourcePath) {
        int depth = 0;
        boolean inString = false;

        for (int index = startIndex; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (inString) {
                if (current == '\\' && index + 1 < sql.length()) {
                    index++;
                    continue;
                }
                if (current == '\'') {
                    if (index + 1 < sql.length() && sql.charAt(index + 1) == '\'') {
                        index++;
                    } else {
                        inString = false;
                    }
                }
                continue;
            }

            if (current == '\'') {
                inString = true;
                continue;
            }
            if (current == '(') {
                depth++;
                continue;
            }
            if (current == ')' && depth > 0) {
                depth--;
                continue;
            }
            if (current == ';' && depth == 0) {
                return index;
            }
        }

        throw new IllegalStateException("Missing statement terminator in " + resourcePath);
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

    /**
     * Appends decoded escaped character.
     * @param builder the builder value
     * @param next the next value
     */
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
