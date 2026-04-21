package org.oldskooler.vibe.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.oldskooler.vibe.storage.bootstrap.seed.data.GuestRoomSeedCatalog;
import org.oldskooler.vibe.storage.entity.UserEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class GuestRoomSeedRegistrar implements DatabaseSeedRegistrar {

    private static final Logger log = LogManager.getLogger(GuestRoomSeedRegistrar.class);
    private static final List<GuestRoomSeedCatalog.GuestRoomSeed> DEFAULT_ROOMS = GuestRoomSeedCatalog.rooms();

    /**
     * Seeds.
     * @param context the context value
     */
    @Override
    public void seed(DbContext context) {
        Map<String, Integer> ownerIds = new HashMap<>();
        for (GuestRoomSeedCatalog.GuestRoomSeed room : DEFAULT_ROOMS) {
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

            for (GuestRoomSeedCatalog.GuestRoomSeed room : DEFAULT_ROOMS) {
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
}
