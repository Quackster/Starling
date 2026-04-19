package org.starling.storage;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSupportDefinitionTest {

    @Test
    void tableDefinitionBuildsCreateTableSqlWithEscapedIdentifiersAndDefaults() throws Exception {
        DatabaseSupport.TableDefinition definition = DatabaseSupport.table("odd`table")
                .column(DatabaseSupport.column("id", "INT").notNull().autoIncrement())
                .column(DatabaseSupport.column("display`name", "VARCHAR(64)").notNull().defaultValue("O'Reilly"))
                .column(DatabaseSupport.column("published_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"))
                .primaryKey("id");

        String sql = invokeStringMethod(definition, "toCreateTableSql");

        assertEquals("""
                CREATE TABLE `odd``table` (
                    `id` INT NOT NULL AUTO_INCREMENT,
                    `display``name` VARCHAR(64) NOT NULL DEFAULT 'O''Reilly',
                    `published_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                    PRIMARY KEY (`id`)
                ) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci
                """.stripTrailing(), sql);
    }

    @Test
    void columnDefinitionRejectsUnsupportedDefaultValueTypes() throws Exception {
        DatabaseSupport.ColumnDefinition definition = DatabaseSupport.column("metadata", "TEXT")
                .defaultValue(Boolean.TRUE);

        InvocationTargetException exception = assertThrows(
                InvocationTargetException.class,
                () -> invokeStringMethod(definition, "toSql")
        );

        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertTrue(exception.getCause().getMessage().contains("Unsupported default value type"));
    }

    private String invokeStringMethod(Object target, String methodName) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName);
        method.setAccessible(true);
        return (String) method.invoke(target);
    }
}
