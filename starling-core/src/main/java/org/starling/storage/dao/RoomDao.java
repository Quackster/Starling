package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.RoomEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RoomDao {

    /**
     * Creates a new RoomDao.
     */
    private RoomDao() {}

    /**
     * Finds by category id.
     * @param categoryId the category id value
     * @return the resulting find by category id
     */
    public static List<RoomEntity> findByCategoryId(int categoryId) {
        return EntityContext.withContext(context -> context.from(RoomEntity.class)
                .filter(filter -> filter.equals(RoomEntity::getCategoryId, categoryId))
                .orderBy(order -> order
                        .col(RoomEntity::getCurrentUsers).desc()
                        .col(RoomEntity::getId).asc())
                .toList());
    }

    /**
     * Finds by owner.
     * @param ownerName the owner name value
     * @return the resulting find by owner
     */
    public static List<RoomEntity> findByOwner(String ownerName) {
        return EntityContext.withContext(context -> context.from(RoomEntity.class)
                .filter(filter -> filter.equalsIgnoreCase(RoomEntity::getOwnerName, ownerName))
                .orderBy(order -> order.col(RoomEntity::getId).asc())
                .toList());
    }

    /**
     * Finds by ids.
     * @param roomIds the room ids value
     * @return the resulting find by ids
     */
    public static List<RoomEntity> findByIds(List<Integer> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return Collections.emptyList();
        }

        return EntityContext.withContext(context -> context.from(RoomEntity.class)
                .filter(filter -> filter.in(RoomEntity::getId, roomIds))
                .orderBy(order -> order
                        .col(RoomEntity::getCurrentUsers).desc()
                        .col(RoomEntity::getId).asc())
                .toList());
    }

    /**
     * Searches.
     * @param query the query value
     * @return the result of this operation
     */
    public static List<RoomEntity> search(String query) {
        String normalizedQuery = query == null ? "" : query.trim().toLowerCase();
        if (normalizedQuery.isEmpty()) {
            return Collections.emptyList();
        }

        String pattern = "%" + normalizedQuery + "%";

        return EntityContext.withContext(context -> context.from(RoomEntity.class)
                .filter(filter -> filter
                        .open()
                        .like(RoomEntity::getName, pattern)
                        .or()
                        .like(RoomEntity::getDescription, pattern)
                        .or()
                        .like(RoomEntity::getOwnerName, pattern)
                        .close())
                .orderBy(order -> order
                        .col(RoomEntity::getCurrentUsers).desc()
                        .col(RoomEntity::getId).asc())
                .limit(25)
                .toList());
    }

    /**
     * Finds by id.
     * @param roomId the room id value
     * @return the resulting find by id
     */
    public static RoomEntity findById(int roomId) {
        return EntityContext.withContext(context -> context.from(RoomEntity.class)
                .filter(filter -> filter.equals(RoomEntity::getId, roomId))
                .first()
                .orElse(null));
    }

    /**
     * Finds recommended.
     * @param limit the limit value
     * @return the resulting find recommended
     */
    public static List<RoomEntity> findRecommended(int limit) {
        List<Integer> recommendedIds = RecommendedItemDao.listIds("room", null, limit);
        if (recommendedIds.isEmpty()) {
            return findTopRated(limit);
        }
        return findByIdsInOrder(recommendedIds);
    }

    /**
     * Finds top-rated rooms.
     * @param limit the limit value
     * @return the resulting room list
     */
    public static List<RoomEntity> findTopRated(int limit) {
        return EntityContext.withContext(context -> context.from(RoomEntity.class)
                .orderBy(order -> order
                        .col(RoomEntity::getCurrentUsers).desc()
                        .col(RoomEntity::getId).asc())
                .limit(limit)
                .toList());
    }

    /**
     * Saves.
     * @param room the room value
     * @return the result of this operation
     */
    public static RoomEntity save(RoomEntity room) {
        return EntityContext.inTransaction(context -> {
            if (room.getId() > 0) {
                context.update(room);
            } else {
                context.insert(room);
            }
            return room;
        });
    }

    /**
     * Deletes.
     * @param roomId the room id value
     */
    public static void delete(int roomId) {
        EntityContext.inTransaction(context -> {
            context.from(RoomEntity.class)
                    .filter(filter -> filter.equals(RoomEntity::getId, roomId))
                    .delete();
            return null;
        });
    }

    /**
     * Resets current users.
     */
    public static void resetCurrentUsers() {
        EntityContext.inTransaction(context -> {
            context.from(RoomEntity.class)
                    .filter(filter -> filter.notEquals(RoomEntity::getId, 0))
                    .update(setter -> setter.set(RoomEntity::getCurrentUsers, 0));
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
            context.from(RoomEntity.class)
                    .filter(filter -> filter.equals(RoomEntity::getId, roomId))
                    .update(setter -> setter.set(RoomEntity::getCurrentUsers, persistedCurrentUsers));
            return null;
        });
    }

    private static List<RoomEntity> findByIdsInOrder(List<Integer> roomIds) {
        if (roomIds == null || roomIds.isEmpty()) {
            return List.of();
        }

        List<RoomEntity> rooms = EntityContext.withContext(context -> context.from(RoomEntity.class)
                .filter(filter -> filter.in(RoomEntity::getId, roomIds))
                .toList());

        Map<Integer, RoomEntity> byId = new LinkedHashMap<>();
        for (RoomEntity room : rooms) {
            byId.put(room.getId(), room);
        }

        List<RoomEntity> ordered = new ArrayList<>();
        for (Integer roomId : roomIds) {
            RoomEntity room = byId.get(roomId);
            if (room != null) {
                ordered.add(room);
            }
        }
        return ordered;
    }
}
