package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.RoomRightEntity;

public final class RoomRightDao {

    private RoomRightDao() {}

    public static boolean exists(int roomId, int userId) {
        return EntityContext.withContext(context -> context.from(RoomRightEntity.class)
                .filter(filter -> filter
                        .equals(RoomRightEntity::getRoomId, roomId)
                        .equals(RoomRightEntity::getUserId, userId))
                .limit(1)
                .first()
                .isPresent());
    }

    public static void deleteByRoomId(int roomId) {
        EntityContext.inTransaction(context -> {
            context.from(RoomRightEntity.class)
                    .filter(filter -> filter.equals(RoomRightEntity::getRoomId, roomId))
                    .delete();
            return null;
        });
    }
}
