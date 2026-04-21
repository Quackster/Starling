package org.oldskooler.vibe.storage.bootstrap.seed;

import org.oldskooler.entity4j.DbContext;
import org.oldskooler.vibe.storage.bootstrap.DatabaseSeedRegistrar;
import org.oldskooler.vibe.storage.bootstrap.seed.registry.DefaultDatabaseSeedRegistry;

public final class ServerSeedBootstrap {

    /**
     * Creates a new ServerSeedBootstrap.
     */
    private ServerSeedBootstrap() {}

    /**
     * Seeds the default server data.
     * @param context the database context
     */
    public static void seedDefaults(DbContext context) {
        for (DatabaseSeedRegistrar registrar : DefaultDatabaseSeedRegistry.defaults()) {
            registrar.seed(context);
        }
    }
}
