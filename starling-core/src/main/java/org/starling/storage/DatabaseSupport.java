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

    private static String joinEscapedIdentifiers(String... identifiers) {
        return Arrays.stream(identifiers)
                .map(identifier -> "`" + escapeIdentifier(identifier) + "`")
                .collect(Collectors.joining(", "));
    }
}
