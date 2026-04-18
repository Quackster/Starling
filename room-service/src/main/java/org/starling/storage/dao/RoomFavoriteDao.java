package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.RoomFavoriteEntity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

public final class RoomFavoriteDao {

    /**
     * Creates a new RoomFavoriteDao.
     */
    private RoomFavoriteDao() {}

    /**
     * Finds by user id.
     * @param userId the user id value
     * @return the resulting find by user id
     */
    public static List<RoomFavoriteEntity> findByUserId(int userId) {
        return EntityContext.withContext(context -> context.from(RoomFavoriteEntity.class)
                .filter(filter -> filter.equals(RoomFavoriteEntity::getUserId, userId))
                .orderBy(order -> order
                        .col(RoomFavoriteEntity::getCreatedAt).asc()
                        .col(RoomFavoriteEntity::getRoomId).asc())
                .toList());
    }

    /**
     * Counts by user id.
     * @param userId the user id value
     * @return the result of this operation
     */
    public static long countByUserId(int userId) {
        return EntityContext.withContext(context -> context.from(RoomFavoriteEntity.class)
                .filter(filter -> filter.equals(RoomFavoriteEntity::getUserId, userId))
                .count());
    }

    /**
     * Existses.
     * @param userId the user id value
     * @param roomType the room type value
     * @param roomId the room id value
     * @return the result of this operation
     */
    public static boolean exists(int userId, int roomType, int roomId) {
        return EntityContext.withContext(context -> context.from(RoomFavoriteEntity.class)
                .filter(filter -> filter
                        .equals(RoomFavoriteEntity::getUserId, userId)
                        .equals(RoomFavoriteEntity::getRoomType, roomType)
                        .equals(RoomFavoriteEntity::getRoomId, roomId))
                .count() > 0);
    }

    /**
     * Adds favorite.
     * @param userId the user id value
     * @param roomType the room type value
     * @param roomId the room id value
     */
    public static void addFavorite(int userId, int roomType, int roomId) {
        EntityContext.inTransaction(context -> {
            RoomFavoriteEntity favorite = new RoomFavoriteEntity();
            favorite.setUserId(userId);
            favorite.setRoomType(roomType);
            favorite.setRoomId(roomId);
            favorite.setCreatedAt(Timestamp.from(Instant.now()));
            context.insert(favorite);
            return null;
        });
    }

    /**
     * Removes favorite.
     * @param userId the user id value
     * @param roomType the room type value
     * @param roomId the room id value
     */
    public static void removeFavorite(int userId, int roomType, int roomId) {
        EntityContext.inTransaction(context -> {
            context.from(RoomFavoriteEntity.class)
                    .filter(filter -> filter
                            .equals(RoomFavoriteEntity::getUserId, userId)
                            .equals(RoomFavoriteEntity::getRoomType, roomType)
                            .equals(RoomFavoriteEntity::getRoomId, roomId))
                    .delete();
            return null;
        });
    }

    /**
     * Deletes by private room id.
     * @param roomId the room id value
     */
    public static void deleteByPrivateRoomId(int roomId) {
        deleteByRoom(0, roomId);
    }

    /**
     * Deletes by public room id.
     * @param roomId the room id value
     */
    public static void deleteByPublicRoomId(int roomId) {
        deleteByRoom(1, roomId);
    }

    /**
     * Deletes by room.
     * @param roomType the room type value
     * @param roomId the room id value
     */
    private static void deleteByRoom(int roomType, int roomId) {
        EntityContext.inTransaction(context -> {
            context.from(RoomFavoriteEntity.class)
                    .filter(filter -> filter
                            .equals(RoomFavoriteEntity::getRoomType, roomType)
                            .equals(RoomFavoriteEntity::getRoomId, roomId))
                    .delete();
            return null;
        });
    }
}
