package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.RoomFavoriteEntity;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

public final class RoomFavoriteDao {

    private RoomFavoriteDao() {}

    public static List<RoomFavoriteEntity> findByUserId(int userId) {
        return EntityContext.withContext(context -> context.from(RoomFavoriteEntity.class)
                .filter(filter -> filter.equals(RoomFavoriteEntity::getUserId, userId))
                .orderBy(order -> order
                        .col(RoomFavoriteEntity::getCreatedAt).asc()
                        .col(RoomFavoriteEntity::getRoomId).asc())
                .toList());
    }

    public static long countByUserId(int userId) {
        return EntityContext.withContext(context -> context.from(RoomFavoriteEntity.class)
                .filter(filter -> filter.equals(RoomFavoriteEntity::getUserId, userId))
                .count());
    }

    public static boolean exists(int userId, int roomType, int roomId) {
        return EntityContext.withContext(context -> context.from(RoomFavoriteEntity.class)
                .filter(filter -> filter
                        .equals(RoomFavoriteEntity::getUserId, userId)
                        .equals(RoomFavoriteEntity::getRoomType, roomType)
                        .equals(RoomFavoriteEntity::getRoomId, roomId))
                .count() > 0);
    }

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

    public static void deleteByPrivateRoomId(int roomId) {
        deleteByRoom(0, roomId);
    }

    public static void deleteByPublicRoomId(int roomId) {
        deleteByRoom(1, roomId);
    }

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
