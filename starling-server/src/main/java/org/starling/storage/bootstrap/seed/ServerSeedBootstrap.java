package org.starling.storage.bootstrap.seed;

import org.oldskooler.entity4j.DbContext;
import org.starling.storage.bootstrap.DatabaseSeedRegistrar;
import org.starling.storage.bootstrap.seed.registry.DefaultDatabaseSeedRegistry;

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
