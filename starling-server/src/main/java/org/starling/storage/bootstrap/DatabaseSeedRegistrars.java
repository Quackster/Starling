package org.starling.storage.bootstrap;

import org.starling.storage.bootstrap.seed.registry.DefaultDatabaseSeedRegistry;

import java.util.List;

public final class DatabaseSeedRegistrars {

    /**
     * Creates a new DatabaseSeedRegistrars.
     */
    private DatabaseSeedRegistrars() {}

    /**
     * Defaultses.
     * @return the resulting defaults
     */
    public static List<DatabaseSeedRegistrar> defaults() {
        return DefaultDatabaseSeedRegistry.defaults();
    }
}
