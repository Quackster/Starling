package org.starling.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.oldskooler.entity4j.dialect.SqlDialectType;
import org.oldskooler.entity4j.transaction.Transaction;
import org.starling.config.ServerConfig;

import java.sql.DriverManager;
import java.util.Objects;
import java.util.function.Function;

public final class EntityContext {

    private static final Logger log = LogManager.getLogger(EntityContext.class);
    private static ServerConfig config;

    /**
     * Creates a new EntityContext.
     */
    private EntityContext() {}

    /**
     * Inits.
     * @param serverConfig the server config value
     */
    public static void init(ServerConfig serverConfig) {
        config = Objects.requireNonNull(serverConfig, "serverConfig");

        try {
            Class.forName("org.mariadb.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("MariaDB JDBC driver is not available", e);
        }

        log.info("Entity4j configured for {}", config.jdbcUrl());
    }

    /**
     * Returns whether initialized.
     * @return whether initialized
     */
    public static boolean isInitialized() {
        return config != null;
    }

    /**
     * Opens context.
     * @return the result of this operation
     */
    public static DbContext openContext() {
        if (config == null) {
            throw new IllegalStateException("EntityContext has not been initialized");
        }

        try {
            return new DbContext(
                    DriverManager.getConnection(
                            config.jdbcUrl(),
                            config.dbUsername(),
                            config.dbPassword()
                    ),
                    SqlDialectType.MYSQL
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to open Entity4j context", e);
        }
    }

    /**
     * Returns a copy with context.
     * @param action the action value
     * @return the result of this operation
     */
    public static <T> T withContext(Function<DbContext, T> action) {
        Objects.requireNonNull(action, "action");

        try (DbContext context = openContext()) {
            return action.apply(context);
        }
    }

    /**
     * Ins transaction.
     * @param action the action value
     * @return the result of this operation
     */
    public static <T> T inTransaction(Function<DbContext, T> action) {
        Objects.requireNonNull(action, "action");

        try (DbContext context = openContext();
             Transaction transaction = context.beginTransaction()) {
            T result = action.apply(context);
            transaction.commit();
            return result;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to execute database transaction", e);
        }
    }

    /**
     * Shutdowns.
     */
    public static void shutdown() {
        config = null;
    }
}
