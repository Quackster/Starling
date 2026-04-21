package org.oldskooler.vibe.storage.bootstrap.seed.data;

import org.oldskooler.vibe.game.room.layout.RoomLayoutRegistry;

import java.util.List;

public final class GuestRoomSeedCatalog {

    private static final List<GuestRoomSeed> ROOMS = List.of(
            createRoomSeed(1, 28, "admin", "Sunset Lounge", "A laid-back lounge for first-time visitors.", "model_a", 0, "", 7, 25, 50, 1, 1, 0, 0, "", 0),
            createRoomSeed(2, 6, "admin", "Trading Corner", "Swap furniture and meet other traders.", "model_b", 0, "", 4, 20, 50, 1, 1, 0, 0, "", 0),
            createRoomSeed(3, 33, "admin", "Pixel Plaza", "A busy social room with a central dance floor.", "model_c", 0, "", 11, 25, 50, 1, 1, 0, 0, "", 0),
            createRoomSeed(4, 29, "admin", "Rooftop Chill", "Quiet seating, skyline views, and open chat.", "model_d", 2, "vibe", 2, 15, 40, 1, 1, 0, 0, "", 0)
    );

    /**
     * Creates a new GuestRoomSeedCatalog.
     */
    private GuestRoomSeedCatalog() {}

    /**
     * Returns the default guest room seeds.
     * @return the guest room seeds
     */
    public static List<GuestRoomSeed> rooms() {
        return ROOMS;
    }

    private static GuestRoomSeed createRoomSeed(
            int id,
            int categoryId,
            String ownerName,
            String name,
            String description,
            String modelName,
            int doorMode,
            String doorPassword,
            int currentUsers,
            int maxUsers,
            int absoluteMaxUsers,
            int showOwnerName,
            int allowTrading,
            int allowOthersMoveFurniture,
            int alertState,
            String navigatorFilter,
            int port
    ) {
        RoomLayoutRegistry.RoomVisuals visuals = RoomLayoutRegistry.builtinPrivateRoom(modelName);
        return new GuestRoomSeed(
                id,
                categoryId,
                ownerName,
                name,
                description,
                visuals.marker(),
                visuals.heightmap(),
                visuals.wallpaper(),
                visuals.floorPattern(),
                visuals.landscape(),
                doorMode,
                doorPassword,
                currentUsers,
                maxUsers,
                absoluteMaxUsers,
                showOwnerName,
                allowTrading,
                allowOthersMoveFurniture,
                alertState,
                navigatorFilter,
                port
        );
    }

    public record GuestRoomSeed(
            int id,
            int categoryId,
            String ownerName,
            String name,
            String description,
            String modelName,
            String heightmap,
            String wallpaper,
            String floorPattern,
            String landscape,
            int doorMode,
            String doorPassword,
            int currentUsers,
            int maxUsers,
            int absoluteMaxUsers,
            int showOwnerName,
            int allowTrading,
            int allowOthersMoveFurniture,
            int alertState,
            String navigatorFilter,
            int port
    ) {}
}
