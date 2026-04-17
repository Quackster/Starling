package org.starling.storage.bootstrap;

import org.oldskooler.entity4j.DbContext;

@FunctionalInterface
public interface DatabaseSeedRegistrar {
    void seed(DbContext context);
}
