package org.oldskooler.vibe.storage;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.oldskooler.entity4j.transaction.Transaction;
import org.oldskooler.vibe.config.ServerConfig;
import org.oldskooler.vibe.storage.bootstrap.schema.ServerSchemaBootstrap;
import org.oldskooler.vibe.storage.bootstrap.seed.ServerSeedBootstrap;

public final class DatabaseBootstrap {

    private static final Logger log = LogManager.getLogger(DatabaseBootstrap.class);

    /**
     * Creates a new DatabaseBootstrap.
     */
    private DatabaseBootstrap() {}

    /**
     * Ensures database.
     * @param config the config value
     */
    public static void ensureDatabase(ServerConfig config) {
        DatabaseSupport.ensureDatabase(config.database());
    }

    /**
     * Ensures schema.
     * @param config the config value
     */
    public static void ensureSchema(ServerConfig config) {
        try (DbContext context = EntityContext.openContext()) {
            ServerSchemaBootstrap.ensure(context);
            log.info("Ensured navigator schema extensions exist");
        } catch (Exception e) {
            log.error("Failed to ensure schema extensions: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Seeds defaults.
     */
    public static void seedDefaults() {
        try (DbContext context = EntityContext.openContext();
             Transaction transaction = context.beginTransaction()) {
            ServerSeedBootstrap.seedDefaults(context);
            transaction.commit();
        } catch (Exception e) {
            log.error("Failed to seed default data: {}", e.getMessage(), e);
            throw new RuntimeException(e);
        }
    }
}
