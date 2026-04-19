package org.starling.storage;

import org.junit.jupiter.api.Test;
import org.oldskooler.entity4j.dialect.SqlDialect;
import org.oldskooler.entity4j.dialect.types.MySqlDialect;
import org.oldskooler.entity4j.dialect.types.PostgresDialect;
import org.oldskooler.entity4j.dialect.types.SqliteDialect;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSupportDefinitionTest {

    private static final MySqlDialect MYSQL_DIALECT = new MySqlDialect();
    private static final PostgresDialect POSTGRES_DIALECT = new PostgresDialect();
    private static final SqliteDialect SQLITE_DIALECT = new SqliteDialect();

    @Test
    void tableDefinitionBuildsCreateTableSqlWithEscapedIdentifiersAndDefaults() throws Exception {
        DatabaseSupport.TableDefinition definition = DatabaseSupport.table("odd`table")
                .column(DatabaseSupport.column("id", "INT").notNull().autoIncrement())
                .column(DatabaseSupport.column("display`name", "VARCHAR(64)").notNull().defaultValue("O'Reilly"))
                .column(DatabaseSupport.column("published_at", "TIMESTAMP").notNull().defaultExpression("CURRENT_TIMESTAMP"))
                .primaryKey("id");

        String sql = invokeStringMethod(definition, "toCreateTableSql", SqlDialect.class, MYSQL_DIALECT);

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
                () -> invokeStringMethod(definition, "toSql", SqlDialect.class, MYSQL_DIALECT)
        );

        assertTrue(exception.getCause() instanceof IllegalArgumentException);
        assertTrue(exception.getCause().getMessage().contains("Unsupported default value type"));
    }

    @Test
    void tableDefinitionInlinesSqliteAutoIncrementPrimaryKey() throws Exception {
        DatabaseSupport.TableDefinition definition = DatabaseSupport.table("rooms")
                .column(DatabaseSupport.column("id", "INT").notNull().autoIncrement())
                .column(DatabaseSupport.column("name", "TEXT").notNull())
                .primaryKey("id");

        String sql = invokeStringMethod(definition, "toCreateTableSql", SqlDialect.class, SQLITE_DIALECT);

        assertTrue(sql.contains("INTEGER PRIMARY KEY"));
        assertTrue(sql.contains("AUTOINCREMENT"));
        assertFalse(sql.contains("PRIMARY KEY ("));
    }

    @Test
    void addColumnStatementOmitsAfterClauseOutsideMySql() throws Exception {
        Object statement = constructPrivateRecord(
                "org.starling.storage.DatabaseSupport$AddColumnStatement",
                String.class,
                DatabaseSupport.ColumnDefinition.class,
                String.class,
                "rooms",
                DatabaseSupport.column("owner", "VARCHAR(32)").notNull(),
                "caption"
        );

        String mysqlSql = invokeStringMethod(statement, "toSql", SqlDialect.class, MYSQL_DIALECT);
        String postgresSql = invokeStringMethod(statement, "toSql", SqlDialect.class, POSTGRES_DIALECT);

        assertEquals("ALTER TABLE `rooms` ADD COLUMN `owner` VARCHAR(32) NOT NULL AFTER `caption`", mysqlSql);
        assertEquals("ALTER TABLE \"rooms\" ADD COLUMN \"owner\" VARCHAR(32) NOT NULL", postgresSql);
    }

    @Test
    void buildModifyColumnStatementsProducesPostgresPlan() throws Exception {
        DatabaseSupport.ColumnDefinition definition = DatabaseSupport.column("score", "DOUBLE")
                .defaultValue(4)
                .notNull();

        Method method = DatabaseSupport.class.getDeclaredMethod(
                "buildModifyColumnStatements",
                SqlDialect.class,
                String.class,
                DatabaseSupport.ColumnDefinition.class
        );
        method.setAccessible(true);

        List<?> statements = (List<?>) method.invoke(null, POSTGRES_DIALECT, "rooms", definition);
        List<String> sql = statements.stream()
                .map(statement -> invokeUnchecked(statement, POSTGRES_DIALECT))
                .toList();

        assertEquals(List.of(
                "ALTER TABLE \"rooms\" ALTER COLUMN \"score\" TYPE DOUBLE PRECISION",
                "ALTER TABLE \"rooms\" ALTER COLUMN \"score\" SET DEFAULT 4",
                "ALTER TABLE \"rooms\" ALTER COLUMN \"score\" SET NOT NULL"
        ), sql);
    }

    private String invokeStringMethod(Object target, String methodName, Class<?> parameterType, Object argument) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterType);
        method.setAccessible(true);
        return (String) method.invoke(target, argument);
    }

    private Object constructPrivateRecord(String className, Class<?> firstType, Class<?> secondType, Class<?> thirdType,
                                          Object first, Object second, Object third) throws Exception {
        Class<?> clazz = Class.forName(className);
        Constructor<?> constructor = clazz.getDeclaredConstructor(firstType, secondType, thirdType);
        constructor.setAccessible(true);
        return constructor.newInstance(first, second, third);
    }

    private String invokeUnchecked(Object target, SqlDialect dialect) {
        try {
            return invokeStringMethod(target, "toSql", SqlDialect.class, dialect);
        } catch (Exception exception) {
            throw new RuntimeException(exception);
        }
    }
}
