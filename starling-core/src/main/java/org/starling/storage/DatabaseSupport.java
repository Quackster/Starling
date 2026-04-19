package org.starling.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.starling.config.DatabaseConfig;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

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
     * Escapes an identifier for use in raw SQL.
     * @param identifier the identifier value
     * @return the escaped identifier
     */
    public static String escapeIdentifier(String identifier) {
        return identifier.replace("`", "``");
    }
}
