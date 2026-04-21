package org.oldskooler.vibe.storage.dao;

import org.oldskooler.vibe.storage.EntityContext;
import org.oldskooler.vibe.storage.entity.RoomRightEntity;

public final class RoomRightDao {

    /**
     * Creates a new RoomRightDao.
     */
    private RoomRightDao() {}

    /**
     * Existses.
     * @param roomId the room id value
     * @param userId the user id value
     * @return the result of this operation
     */
    public static boolean exists(int roomId, int userId) {
        return EntityContext.withContext(context -> context.from(RoomRightEntity.class)
                .filter(filter -> filter
                        .equals(RoomRightEntity::getRoomId, roomId)
                        .equals(RoomRightEntity::getUserId, userId))
                .limit(1)
                .first()
                .isPresent());
    }

    /**
     * Deletes by room id.
     * @param roomId the room id value
     */
    public static void deleteByRoomId(int roomId) {
        EntityContext.inTransaction(context -> {
            context.from(RoomRightEntity.class)
                    .filter(filter -> filter.equals(RoomRightEntity::getRoomId, roomId))
                    .delete();
            return null;
        });
    }
}
