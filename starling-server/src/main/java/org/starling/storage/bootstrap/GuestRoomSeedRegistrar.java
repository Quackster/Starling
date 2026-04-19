package org.starling.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.starling.game.room.layout.RoomLayoutRegistry;
import org.starling.storage.entity.UserEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GuestRoomSeedRegistrar implements DatabaseSeedRegistrar {

    private static final Logger log = LogManager.getLogger(GuestRoomSeedRegistrar.class);
    private static final List<RoomSeed> DEFAULT_ROOMS = List.of(
            createRoomSeed(1, 28, "admin", "Sunset Lounge", "A laid-back lounge for first-time visitors.", "model_a", 0, "", 7, 25, 50, 1, 1, 0, 0, "", 0),
            createRoomSeed(2, 6, "admin", "Trading Corner", "Swap furniture and meet other traders.", "model_b", 0, "", 4, 20, 50, 1, 1, 0, 0, "", 0),
            createRoomSeed(3, 33, "admin", "Pixel Plaza", "A busy social room with a central dance floor.", "model_c", 0, "", 11, 25, 50, 1, 1, 0, 0, "", 0),
            createRoomSeed(4, 29, "admin", "Rooftop Chill", "Quiet seating, skyline views, and open chat.", "model_d", 2, "starling", 2, 15, 40, 1, 1, 0, 0, "", 0)
    );

    /**
     * Seeds.
     * @param context the context value
     */
    @Override
    public void seed(DbContext context) {
        Map<String, Integer> ownerIds = new HashMap<>();
        for (RoomSeed room : DEFAULT_ROOMS) {
            ownerIds.computeIfAbsent(room.ownerName(), ownerName -> context.from(UserEntity.class)
                    .filter(filter -> filter.equals(UserEntity::getUsername, ownerName))
                    .first()
                    .map(UserEntity::getId)
                    .orElseThrow(() -> new IllegalStateException("Guest room owner not found: " + ownerName)));
        }

        try {
            Map<Integer, GuestRoomSeedEntity> roomsById = new HashMap<>();
            for (GuestRoomSeedEntity entity : context.from(GuestRoomSeedEntity.class).toList()) {
                roomsById.put(entity.getId(), entity);
            }

            for (RoomSeed room : DEFAULT_ROOMS) {
                GuestRoomSeedEntity entity = roomsById.get(room.id());
                boolean isNew = entity == null;
                if (isNew) {
                    entity = new GuestRoomSeedEntity();
                    entity.setId(room.id());
                }

                entity.setCategoryId(room.categoryId());
                entity.setOwnerId(ownerIds.get(room.ownerName()));
                entity.setOwnerName(room.ownerName());
                entity.setName(room.name());
                entity.setDescription(room.description());
                entity.setModelName(room.modelName());
                entity.setHeightmap(room.heightmap());
                entity.setWallpaper(room.wallpaper());
                entity.setFloorPattern(room.floorPattern());
                entity.setLandscape(room.landscape());
                entity.setDoorMode(room.doorMode());
                entity.setDoorPassword(room.doorPassword());
                entity.setCurrentUsers(room.currentUsers());
                entity.setMaxUsers(room.maxUsers());
                entity.setAbsoluteMaxUsers(room.absoluteMaxUsers());
                entity.setShowOwnerName(room.showOwnerName());
                entity.setAllowTrading(room.allowTrading());
                entity.setAllowOthersMoveFurniture(room.allowOthersMoveFurniture());
                entity.setAlertState(room.alertState());
                entity.setNavigatorFilter(room.navigatorFilter());
                entity.setPort(room.port());

                if (isNew) {
                    context.insert(entity);
                } else {
                    context.update(entity);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed rooms", e);
        }

        log.info("Ensured default guest rooms exist");
    }

    /**
     * Creates room seed.
     * @param id the id value
     * @param categoryId the category id value
     * @param ownerName the owner name value
     * @param name the name value
     * @param description the description value
     * @param modelName the model name value
     * @param doorMode the door mode value
     * @param doorPassword the door password value
     * @param currentUsers the current users value
     * @param maxUsers the max users value
     * @param absoluteMaxUsers the absolute max users value
     * @param showOwnerName the show owner name value
     * @param allowTrading the allow trading value
     * @param allowOthersMoveFurniture the allow others move furniture value
     * @param alertState the alert state value
     * @param navigatorFilter the navigator filter value
     * @param port the port value
     * @return the resulting create room seed
     */
    private static RoomSeed createRoomSeed(
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
        return new RoomSeed(
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

    private record RoomSeed(
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
