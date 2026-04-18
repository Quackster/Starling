package org.starling.storage.bootstrap;

import java.util.List;

public final class DatabaseSeedRegistrars {

    private static final List<DatabaseSeedRegistrar> DEFAULTS = List.of(
            new AdminUserSeedRegistrar(),
            new NavigatorCategorySeedRegistrar(),
            new RoomModelSeedRegistrar(),
            new GuestRoomSeedRegistrar(),
            new PublicRoomSeedRegistrar(),
            new PublicRoomItemSeedRegistrar()
    );

    /**
     * Creates a new DatabaseSeedRegistrars.
     */
    private DatabaseSeedRegistrars() {}

    /**
     * Defaultses.
     * @return the resulting defaults
     */
    public static List<DatabaseSeedRegistrar> defaults() {
        return DEFAULTS;
    }
}
