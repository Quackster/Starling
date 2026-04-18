package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.PublicRoomItemEntity;

import java.util.Collections;
import java.util.List;

public final class PublicRoomItemDao {

    /**
     * Creates a new PublicRoomItemDao.
     */
    private PublicRoomItemDao() {}

    /**
     * Finds by room model.
     * @param roomModel the room model value
     * @return the resulting find by room model
     */
    public static List<PublicRoomItemEntity> findByRoomModel(String roomModel) {
        if (roomModel == null || roomModel.isBlank() || !EntityContext.isInitialized()) {
            return Collections.emptyList();
        }

        return EntityContext.withContext(context -> context.from(PublicRoomItemEntity.class)
                .filter(filter -> filter.equalsIgnoreCase(PublicRoomItemEntity::getRoomModel, roomModel.trim()))
                .orderBy(order -> order.col(PublicRoomItemEntity::getId).asc())
                .toList());
    }
}
