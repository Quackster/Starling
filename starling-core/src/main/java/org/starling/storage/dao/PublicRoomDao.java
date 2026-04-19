package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.PublicRoomEntity;

import java.util.Collections;
import java.util.List;

public final class PublicRoomDao {

    /**
     * Creates a new PublicRoomDao.
     */
    private PublicRoomDao() {}

    /**
     * Finds visible by category id.
     * @param categoryId the category id value
     * @return the resulting find visible by category id
     */
    public static List<PublicRoomEntity> findVisibleByCategoryId(int categoryId) {
        return EntityContext.withContext(context -> context.from(PublicRoomEntity.class)
                .filter(filter -> filter
                        .equals(PublicRoomEntity::getCategoryId, categoryId)
                        .notEquals(PublicRoomEntity::getIsVisible, 0))
                .orderBy(order -> order
                        .col(PublicRoomEntity::getCurrentUsers).desc()
                        .col(PublicRoomEntity::getId).asc())
                .toList());
    }

    /**
     * Finds by id.
     * @param roomId the room id value
     * @return the resulting find by id
     */
    public static PublicRoomEntity findById(int roomId) {
        return EntityContext.withContext(context -> context.from(PublicRoomEntity.class)
                .filter(filter -> filter.equals(PublicRoomEntity::getId, roomId))
                .first()
                .orElse(null));
    }

    /**
     * Finds by port.
     * @param port the port value
     * @return the resulting find by port
     */
    public static PublicRoomEntity findByPort(int port) {
        return EntityContext.withContext(context -> context.from(PublicRoomEntity.class)
                .filter(filter -> filter
                        .equals(PublicRoomEntity::getPort, port)
                        .notEquals(PublicRoomEntity::getIsVisible, 0))
                .limit(1)
                .first()
                .orElse(null));
    }

    /**
     * Finds by ids.
     * @param roomIds the room ids value
     * @return the resulting find by ids
     */
    public static List<PublicRoomEntity> findByIds(List<Integer> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyList();
        }

        return EntityContext.withContext(context -> context.from(PublicRoomEntity.class)
                .filter(filter -> filter
                        .in(PublicRoomEntity::getId, roomIds)
                        .notEquals(PublicRoomEntity::getIsVisible, 0))
                .orderBy(order -> order
                        .col(PublicRoomEntity::getCurrentUsers).desc()
                        .col(PublicRoomEntity::getId).asc())
                .toList());
    }

    /**
     * Resets current users.
     */
    public static void resetCurrentUsers() {
        EntityContext.inTransaction(context -> {
            context.from(PublicRoomEntity.class)
                    .filter(filter -> filter.notEquals(PublicRoomEntity::getId, 0))
                    .update(setter -> setter.set(PublicRoomEntity::getCurrentUsers, 0));
            return null;
        });
    }

    /**
     * Saves current users.
     * @param roomId the room id value
     * @param currentUsers the current users value
     */
    public static void saveCurrentUsers(int roomId, int currentUsers) {
        int persistedCurrentUsers = Math.max(currentUsers, 0);
        EntityContext.inTransaction(context -> {
            context.from(PublicRoomEntity.class)
                    .filter(filter -> filter.equals(PublicRoomEntity::getId, roomId))
                    .update(setter -> setter.set(PublicRoomEntity::getCurrentUsers, persistedCurrentUsers));
            return null;
        });
    }
}
