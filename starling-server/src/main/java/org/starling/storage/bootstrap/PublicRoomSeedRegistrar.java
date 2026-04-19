package org.starling.storage.bootstrap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.oldskooler.entity4j.DbContext;
import org.starling.storage.entity.PublicRoomEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PublicRoomSeedRegistrar implements DatabaseSeedRegistrar {

    private static final Logger log = LogManager.getLogger(PublicRoomSeedRegistrar.class);
    private static final List<HolographPublicSpaceCatalog.PublicRoomSeed> DEFAULT_PUBLIC_ROOMS =
            HolographPublicSpaceCatalog.load().publicRooms();

    /**
     * Seeds.
     * @param context the context value
     */
    @Override
    public void seed(DbContext context) {
        try {
            Map<Integer, PublicRoomEntity> roomsById = new HashMap<>();
            for (PublicRoomEntity entity : context.from(PublicRoomEntity.class).toList()) {
                roomsById.put(entity.getId(), entity);
            }

            for (HolographPublicSpaceCatalog.PublicRoomSeed room : DEFAULT_PUBLIC_ROOMS) {
                PublicRoomEntity entity = roomsById.get(room.id());
                boolean isNew = entity == null;
                if (isNew) {
                    entity = new PublicRoomEntity();
                    entity.setId(room.id());
                }

                entity.setCategoryId(room.categoryId());
                entity.setName(room.name());
                entity.setUnitStrId(room.unitStrId());
                entity.setHeightmap(room.heightmap());
                entity.setPort(room.port());
                entity.setDoor(room.door());
                entity.setCasts(room.casts());
                entity.setCurrentUsers(room.currentUsers());
                entity.setMaxUsers(room.maxUsers());
                entity.setUsersInQueue(room.usersInQueue());
                entity.setIsVisible(room.isVisible());
                entity.setNavigatorFilter(room.navigatorFilter());
                entity.setDescription(room.description());

                if (isNew) {
                    context.insert(entity);
                } else {
                    context.update(entity);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to seed public rooms", e);
        }

        log.info("Ensured default public rooms exist");
    }
}
