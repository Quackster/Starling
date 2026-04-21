package org.oldskooler.vibe.storage.bootstrap;

import org.oldskooler.vibe.storage.bootstrap.seed.registry.DefaultDatabaseSeedRegistry;

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
