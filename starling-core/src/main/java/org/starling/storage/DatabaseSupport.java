package org.starling.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Arrays;
import java.util.stream.Collectors;

public final class DatabaseSupport {

    private static final Logger log = LogManager.getLogger(DatabaseSupport.class);

    /**
     * Creates a new DatabaseSupport.
     */
    private DatabaseSupport() {}

    /**
     * Ensures the configured database exists.
     * @param config the database config value
     */
    public static void ensureDatabase(DatabaseConfig config) {
        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MariaDB JDBC driver is not available", e);
        }

        String createDatabaseSql = "CREATE DATABASE IF NOT EXISTS `" + escapeIdentifier(config.dbName())
                + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci";

        try (Connection connection = DriverManager.getConnection(
                config.adminJdbcUrl(),
                config.dbUsername(),
                config.dbPassword()
        );
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(createDatabaseSql);
            log.info("Ensured database '{}' exists", config.dbName());
        } catch (Exception e) {
            log.error("Failed to create database '{}': {}", config.dbName(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Drops the configured database if it exists.
     * @param config the database config value
     */
    public static void dropDatabaseIfExists(DatabaseConfig config) {
        String dropDatabaseSql = "DROP DATABASE IF EXISTS `" + escapeIdentifier(config.dbName()) + "`";

        try (Connection connection = DriverManager.getConnection(
                config.adminJdbcUrl(),
                config.dbUsername(),
                config.dbPassword()
        );
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(dropDatabaseSql);
            log.info("Dropped database '{}' if it existed", config.dbName());
        } catch (Exception e) {
            log.error("Failed to drop database '{}': {}", config.dbName(), e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensures an index exists.
     * @param connection the connection value
     * @param tableName the table name value
     * @param indexName the index name value
     * @param unique whether the index is unique
     * @param columns the index column names
     */
    public static void ensureIndex(Connection connection, String tableName, String indexName, boolean unique, String... columns) {
        if (indexExists(connection, tableName, indexName)) {
            return;
        }

        String createIndexSql = "CREATE "
                + (unique ? "UNIQUE " : "")
                + "INDEX `" + escapeIdentifier(indexName)
                + "` ON `" + escapeIdentifier(tableName)
                + "` (" + joinEscapedIdentifiers(columns) + ")";

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createIndexSql);
            log.info("Ensured index '{}' exists on '{}'", indexName, tableName);
        } catch (Exception e) {
            log.error("Failed to create index '{}' on '{}': {}", indexName, tableName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensures a unique index exists.
     * @param connection the connection value
     * @param tableName the table name value
     * @param indexName the index name value
     * @param columns the index column names
     */
    public static void ensureUniqueIndex(Connection connection, String tableName, String indexName, String... columns) {
        ensureIndex(connection, tableName, indexName, true, columns);
    }

    /**
     * Ensures a table exists.
     * @param connection the connection value
     * @param tableName the table name value
     * @param createTableSql the create table sql value
     */
    public static void ensureTable(Connection connection, String tableName, String createTableSql) {
        if (tableExists(connection, tableName)) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(createTableSql);
            log.info("Ensured table '{}' exists", tableName);
        } catch (Exception e) {
            log.error("Failed to create table '{}': {}", tableName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Ensures a column exists.
     * @param connection the connection value
     * @param tableName the table name value
     * @param columnName the column name value
     * @param columnDefinition the sql column definition
     * @param afterColumn the column that should precede the added column
     */
    public static void ensureColumn(Connection connection, String tableName, String columnName, String columnDefinition, String afterColumn) {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }

        String addColumnSql = "ALTER TABLE `"
                + escapeIdentifier(tableName)
                + "` ADD COLUMN `"
                + escapeIdentifier(columnName)
                + "` "
                + columnDefinition
                + (afterColumn == null || afterColumn.isBlank()
                ? ""
                : " AFTER `" + escapeIdentifier(afterColumn) + "`");

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(addColumnSql);
            log.info("Ensured column '{}.{}' exists", tableName, columnName);
        } catch (Exception e) {
            log.error("Failed to add column '{}.{}': {}", tableName, columnName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Modifies a column.
     * @param connection the connection value
     * @param tableName the table name value
     * @param columnName the column name value
     * @param columnDefinition the sql column definition
     */
    public static void modifyColumn(Connection connection, String tableName, String columnName, String columnDefinition) {
        String modifyColumnSql = "ALTER TABLE `"
                + escapeIdentifier(tableName)
                + "` MODIFY COLUMN `"
                + escapeIdentifier(columnName)
                + "` "
                + columnDefinition;

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(modifyColumnSql);
            log.info("Modified column '{}.{}'", tableName, columnName);
        } catch (Exception e) {
            log.error("Failed to modify column '{}.{}': {}", tableName, columnName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Escapes an identifier for use in raw SQL.
     * @param identifier the identifier value
     * @return the escaped identifier
     */
    public static String escapeIdentifier(String identifier) {
        return identifier.replace("`", "``");
    }

    private static boolean indexExists(Connection connection, String tableName, String indexName) {
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), null, tableName, false, false)) {
                while (indexes.next()) {
                    String existingIndexName = indexes.getString("INDEX_NAME");
                    if (existingIndexName != null && existingIndexName.equalsIgnoreCase(indexName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to inspect indexes for '{}': {}", tableName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static boolean tableExists(Connection connection, String tableName) {
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet tables = metadata.getTables(connection.getCatalog(), null, tableName, new String[]{"TABLE"})) {
                while (tables.next()) {
                    String existingTableName = tables.getString("TABLE_NAME");
                    if (existingTableName != null && existingTableName.equalsIgnoreCase(tableName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to inspect table '{}': {}", tableName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) {
        try {
            DatabaseMetaData metadata = connection.getMetaData();
            try (ResultSet columns = metadata.getColumns(connection.getCatalog(), null, tableName, columnName)) {
                while (columns.next()) {
                    String existingColumnName = columns.getString("COLUMN_NAME");
                    if (existingColumnName != null && existingColumnName.equalsIgnoreCase(columnName)) {
                        return true;
                    }
                }
            }
            return false;
        } catch (Exception e) {
            log.error("Failed to inspect column '{}.{}': {}", tableName, columnName, e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    private static String joinEscapedIdentifiers(String... identifiers) {
        return Arrays.stream(identifiers)
                .map(identifier -> "`" + escapeIdentifier(identifier) + "`")
                .collect(Collectors.joining(", "));
    }
}
