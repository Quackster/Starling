package org.starling.storage.bootstrap;

import java.util.List;

public final class DatabaseSeedRegistrars {

    private static final List<DatabaseSeedRegistrar> DEFAULTS = List.of(
            new AdminUserSeedRegistrar(),
            new NavigatorCategorySeedRegistrar(),
            new RoomModelSeedRegistrar(),
            new GuestRoomSeedRegistrar(),
            new PublicRoomSeedRegistrar()
    );

    private DatabaseSeedRegistrars() {}

    public static List<DatabaseSeedRegistrar> defaults() {
        return DEFAULTS;
    }
}
