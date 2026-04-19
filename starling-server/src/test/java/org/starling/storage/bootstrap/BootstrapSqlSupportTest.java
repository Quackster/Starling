package org.starling.storage.bootstrap;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class BootstrapSqlSupportTest {

    @Test
    void parseInsertRowsHandlesEscapedStringsNullsAndMultipleTuples() {
        String sql = """
                INSERT INTO `rooms` VALUES
                (1, 'Lobby', 'Line 1\\nLine 2', NULL, 4.5),
                (2, 'Bob''s Cafe', 'Comma, bracket () and tab\\ttext', 'owner', 8);
                """;

        List<List<String>> rows = BootstrapSqlSupport.parseInsertRows(sql, "rooms", "test.sql");

        assertEquals(2, rows.size());
        assertEquals(1, BootstrapSqlSupport.parseInt(rows.get(0), 0));
        assertEquals("Lobby", BootstrapSqlSupport.parseString(rows.get(0), 1));
        assertEquals("Line 1\nLine 2", BootstrapSqlSupport.parseString(rows.get(0), 2));
        assertNull(BootstrapSqlSupport.parseNullableString(rows.get(0), 3));
        assertEquals(5, BootstrapSqlSupport.parseInt(rows.get(0), 4));

        assertEquals(2, BootstrapSqlSupport.parseInt(rows.get(1), 0));
        assertEquals("Bob's Cafe", BootstrapSqlSupport.parseString(rows.get(1), 1));
        assertEquals("Comma, bracket () and tab\ttext", BootstrapSqlSupport.parseString(rows.get(1), 2));
        assertEquals("owner", BootstrapSqlSupport.parseNullableString(rows.get(1), 3));
        assertEquals(8.0, BootstrapSqlSupport.parseDouble(rows.get(1), 4));
    }

    @Test
    void parseInsertRowsCollectsRowsAcrossMultipleInsertStatements() {
        String sql = """
                INSERT INTO `room_models` VALUES (1, 'a');
                INSERT INTO `room_models` VALUES (2, 'b'), (3, 'c');
                INSERT INTO `rooms` VALUES (9, 'ignored');
                """;

        List<List<String>> rows = BootstrapSqlSupport.parseInsertRows(sql, "room_models", "test.sql");

        assertEquals(List.of(
                List.of("1", "a"),
                List.of("2", "b"),
                List.of("3", "c")
        ), rows);
    }

    @Test
    void parseInsertRowsHandlesColumnListsAndFlexibleWhitespace() {
        String sql = """
                INSERT
                  INTO `room_models` (`id`, `name`)
                  VALUES (1, 'a');

                insert    into    room_models
                values
                (2, 'b');
                """;

        List<List<String>> rows = BootstrapSqlSupport.parseInsertRows(sql, "room_models", "test.sql");

        assertEquals(List.of(
                List.of("1", "a"),
                List.of("2", "b")
        ), rows);
    }
}
