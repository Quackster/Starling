package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.RoomEntity;

import java.util.Collections;
import java.util.List;

public final class RoomDao {

    private RoomDao() {}

    public static List<RoomEntity> findByCategoryId(int categoryId) {
        return EntityContext.withContext(context -> context.from(RoomEntity.class)
                .filter(filter -> filter.equals(RoomEntity::getCategoryId, categoryId))
                .orderBy(order -> order
                        .col(RoomEntity::getCurrentUsers).desc()
                        .col(RoomEntity::getId).asc())
                .toList());
    }

    public static List<RoomEntity> findByOwner(String ownerName) {
        return EntityContext.withContext(context -> context.from(RoomEntity.class)
                .filter(filter -> filter.equalsIgnoreCase(RoomEntity::getOwnerName, ownerName))
                .orderBy(order -> order.col(RoomEntity::getId).asc())
                .toList());
    }

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

    public static RoomEntity findById(int roomId) {
        return EntityContext.withContext(context -> context.from(RoomEntity.class)
                .filter(filter -> filter.equals(RoomEntity::getId, roomId))
                .first()
                .orElse(null));
    }

    public static List<RoomEntity> findRecommended(int limit) {
        return EntityContext.withContext(context -> context.from(RoomEntity.class)
                .orderBy(order -> order
                        .col(RoomEntity::getCurrentUsers).desc()
                        .col(RoomEntity::getId).asc())
                .limit(limit)
                .toList());
    }

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

    public static void delete(int roomId) {
        EntityContext.inTransaction(context -> {
            context.from(RoomEntity.class)
                    .filter(filter -> filter.equals(RoomEntity::getId, roomId))
                    .delete();
            return null;
        });
    }
}
