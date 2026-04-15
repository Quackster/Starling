package org.starling.storage.dao;

import org.starling.storage.EntityContext;
import org.starling.storage.entity.RoomModelEntity;

public final class RoomModelDao {

    private RoomModelDao() {}

    public static RoomModelEntity findByModelName(String modelName, boolean publicModel) {
        if (modelName == null || modelName.isBlank() || !EntityContext.isInitialized()) {
            return null;
        }

        return EntityContext.withContext(context -> context.from(RoomModelEntity.class)
                .filter(filter -> filter
                        .equalsIgnoreCase(RoomModelEntity::getModelName, modelName.trim())
                        .equals(RoomModelEntity::getIsPublic, publicModel ? 1 : 0))
                .limit(1)
                .first()
                .orElse(null));
    }
}
