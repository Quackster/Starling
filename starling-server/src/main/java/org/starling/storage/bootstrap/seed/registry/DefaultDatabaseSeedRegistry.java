package org.starling.storage.bootstrap.seed.registry;

import org.starling.storage.bootstrap.AdminUserSeedRegistrar;
import org.starling.storage.bootstrap.DatabaseSeedRegistrar;
import org.starling.storage.bootstrap.GuestRoomSeedRegistrar;
import org.starling.storage.bootstrap.NavigatorCategorySeedRegistrar;
import org.starling.storage.bootstrap.PublicRoomItemSeedRegistrar;
import org.starling.storage.bootstrap.PublicRoomSeedRegistrar;
import org.starling.storage.bootstrap.RoomModelSeedRegistrar;

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
