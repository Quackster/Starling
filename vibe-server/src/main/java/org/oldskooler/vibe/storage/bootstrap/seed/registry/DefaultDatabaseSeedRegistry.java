package org.oldskooler.vibe.storage.bootstrap.seed.registry;

import org.oldskooler.vibe.storage.bootstrap.AdminUserSeedRegistrar;
import org.oldskooler.vibe.storage.bootstrap.DatabaseSeedRegistrar;
import org.oldskooler.vibe.storage.bootstrap.GuestRoomSeedRegistrar;
import org.oldskooler.vibe.storage.bootstrap.NavigatorCategorySeedRegistrar;
import org.oldskooler.vibe.storage.bootstrap.PublicRoomItemSeedRegistrar;
import org.oldskooler.vibe.storage.bootstrap.PublicRoomSeedRegistrar;
import org.oldskooler.vibe.storage.bootstrap.RoomModelSeedRegistrar;

import java.util.List;

public final class DefaultDatabaseSeedRegistry {

    private static final List<DatabaseSeedRegistrar> DEFAULTS = List.of(
            new AdminUserSeedRegistrar(),
            new NavigatorCategorySeedRegistrar(),
            new RoomModelSeedRegistrar(),
            new GuestRoomSeedRegistrar(),
            new PublicRoomSeedRegistrar(),
            new PublicRoomItemSeedRegistrar()
    );

    /**
     * Creates a new DefaultDatabaseSeedRegistry.
     */
    private DefaultDatabaseSeedRegistry() {}

    /**
     * Returns the default server seed registrars.
     * @return the seed registrars
     */
    public static List<DatabaseSeedRegistrar> defaults() {
        return DEFAULTS;
    }
}
