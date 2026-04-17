package org.starling.storage.bootstrap;

import org.oldskooler.entity4j.DbContext;

@FunctionalInterface
public interface DatabaseSeedRegistrar {
    /**
     * Seeds.
     * @param context the context value
     */
    void seed(DbContext context);
}
