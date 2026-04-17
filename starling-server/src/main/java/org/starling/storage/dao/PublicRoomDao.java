package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.PublicRoomEntity;

import java.util.Collections;
import java.util.List;

public final class PublicRoomDao {

    private PublicRoomDao() {}

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

    public static PublicRoomEntity findById(int roomId) {
        return EntityContext.withContext(context -> context.from(PublicRoomEntity.class)
                .filter(filter -> filter.equals(PublicRoomEntity::getId, roomId))
                .first()
                .orElse(null));
    }

    public static PublicRoomEntity findByPort(int port) {
        return EntityContext.withContext(context -> context.from(PublicRoomEntity.class)
                .filter(filter -> filter
                        .equals(PublicRoomEntity::getPort, port)
                        .notEquals(PublicRoomEntity::getIsVisible, 0))
                .limit(1)
                .first()
                .orElse(null));
    }

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

    public static void resetCurrentUsers() {
        EntityContext.inTransaction(context -> {
            try (var statement = context.conn().prepareStatement("UPDATE public_rooms SET current_users = 0")) {
                statement.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("Failed to reset public room occupancy", e);
            }
            return null;
        });
    }

    public static void saveCurrentUsers(int roomId, int currentUsers) {
        int persistedCurrentUsers = Math.max(currentUsers, 0);
        EntityContext.inTransaction(context -> {
            try (var statement = context.conn()
                    .prepareStatement("UPDATE public_rooms SET current_users = ? WHERE id = ?")) {
                statement.setInt(1, persistedCurrentUsers);
                statement.setInt(2, roomId);
                statement.executeUpdate();
            } catch (Exception e) {
                throw new RuntimeException("Failed to save public room occupancy", e);
            }
            return null;
        });
    }
}
