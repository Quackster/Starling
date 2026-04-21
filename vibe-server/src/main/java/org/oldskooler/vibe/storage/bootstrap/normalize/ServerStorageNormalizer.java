package org.oldskooler.vibe.storage.bootstrap.normalize;

import org.oldskooler.entity4j.DbContext;
import org.oldskooler.vibe.storage.entity.PublicRoomEntity;
import org.oldskooler.vibe.storage.entity.PublicRoomItemEntity;
import org.oldskooler.vibe.storage.entity.RoomEntity;
import org.oldskooler.vibe.storage.entity.RoomModelEntity;
import org.oldskooler.vibe.storage.entity.UserEntity;

public final class ServerStorageNormalizer {

    /**
     * Creates a new ServerStorageNormalizer.
     */
    private ServerStorageNormalizer() {}

    /**
     * Normalizes shared server-side storage data.
     * @param context the database context
     */
    public static void normalize(DbContext context) {
        backfillRoomOwnerIds(context);

        context.from(RoomEntity.class)
                .filter(filter -> filter
                        .open()
                        .isNull(RoomEntity::getModelName)
                        .or()
                        .equals(RoomEntity::getModelName, "")
                        .close())
                .update(setter -> setter.set(RoomEntity::getModelName, "model_a"));
        context.from(RoomEntity.class)
                .filter(filter -> filter.isNull(RoomEntity::getHeightmap))
                .update(setter -> setter.set(RoomEntity::getHeightmap, ""));
        context.from(RoomEntity.class)
                .filter(filter -> filter.isNull(RoomEntity::getWallpaper))
                .update(setter -> setter.set(RoomEntity::getWallpaper, ""));
        context.from(RoomEntity.class)
                .filter(filter -> filter.isNull(RoomEntity::getFloorPattern))
                .update(setter -> setter.set(RoomEntity::getFloorPattern, ""));
        context.from(RoomEntity.class)
                .filter(filter -> filter.isNull(RoomEntity::getLandscape))
                .update(setter -> setter.set(RoomEntity::getLandscape, ""));
        context.from(RoomEntity.class)
                .filter(filter -> filter.isNull(RoomEntity::getDoorPassword))
                .update(setter -> setter.set(RoomEntity::getDoorPassword, ""));
        context.from(RoomModelEntity.class)
                .filter(filter -> filter.isNull(RoomModelEntity::getPublicRoomItems))
                .update(setter -> setter.set(RoomModelEntity::getPublicRoomItems, ""));
        context.from(PublicRoomEntity.class)
                .filter(filter -> filter.isNull(PublicRoomEntity::getHeightmap))
                .update(setter -> setter.set(PublicRoomEntity::getHeightmap, ""));
        context.from(PublicRoomItemEntity.class)
                .filter(filter -> filter.isNull(PublicRoomItemEntity::getBehaviour))
                .update(setter -> setter.set(PublicRoomItemEntity::getBehaviour, ""));
        context.from(PublicRoomItemEntity.class)
                .filter(filter -> filter.isNull(PublicRoomItemEntity::getCurrentProgram))
                .update(setter -> setter.set(PublicRoomItemEntity::getCurrentProgram, ""));
        context.from(UserEntity.class)
                .filter(filter -> filter.notEquals(UserEntity::getIsOnline, 0))
                .update(setter -> setter.set(UserEntity::getIsOnline, 0));
    }

    private static void backfillRoomOwnerIds(DbContext context) {
        for (RoomEntity room : context.from(RoomEntity.class)
                .filter(filter -> filter
                        .open()
                        .isNull(RoomEntity::getOwnerId)
                        .or()
                        .equals(RoomEntity::getOwnerId, 0)
                        .close())
                .toList()) {
            Integer ownerId = context.from(UserEntity.class)
                    .filter(filter -> filter.equalsIgnoreCase(UserEntity::getUsername, room.getOwnerName()))
                    .first()
                    .map(UserEntity::getId)
                    .orElse(null);
            if (ownerId == null) {
                continue;
            }

            room.setOwnerId(ownerId);
            context.update(room);
        }
    }
}
