package org.starling.storage;

import org.oldskooler.entity4j.DbContext;
import org.starling.storage.bootstrap.shared.messenger.SharedMessengerSchemaBootstrap;
import org.starling.storage.bootstrap.shared.permission.SharedRankPermissionBootstrap;

public final class SharedSchemaSupport {

    /**
     * Creates a new SharedSchemaSupport.
     */
    private SharedSchemaSupport() {}

    /**
     * Ensures the shared messenger tables exist.
     * @param context the context value
     */
    public static void ensureMessengerSchema(DbContext context) {
        SharedMessengerSchemaBootstrap.ensureSchema(context);
    }

    /**
     * Ensures the shared rank-permission table exists and is seeded.
     * @param context the database context
     */
    public static void ensureRankPermissionSchema(DbContext context) {
        SharedRankPermissionBootstrap.ensureSchema(context);
    }
}
